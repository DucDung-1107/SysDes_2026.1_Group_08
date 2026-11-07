import { afterEach, beforeEach, describe, it } from "node:test";
import assert from "node:assert/strict";
import { ApiError, getCsrf, request, resetCsrf } from "../src/lib/api.ts";

const originalFetch = globalThis.fetch;
const originalWindow = Object.getOwnPropertyDescriptor(globalThis, "window");
type Call = { input: RequestInfo | URL; init?: RequestInit };
const token = { headerName: "X-CSRF-TOKEN", token: "test-csrf-token" };
const json = (value: unknown, status = 200) => new Response(JSON.stringify(value), {
  status, headers: { "Content-Type": "application/json" },
});

function mockFetch(respond: (call: Call) => Response | Promise<Response>) {
  const calls: Call[] = [];
  globalThis.fetch = async (input, init) => {
    const call = { input, init };
    calls.push(call);
    return respond(call);
  };
  return calls;
}

describe("session and CSRF transport", { concurrency: false }, () => {
  beforeEach(() => {
    resetCsrf();
    Reflect.deleteProperty(globalThis, "window");
  });
  afterEach(() => {
    resetCsrf();
    globalThis.fetch = originalFetch;
    if (originalWindow) Object.defineProperty(globalThis, "window", originalWindow);
    else Reflect.deleteProperty(globalThis, "window");
  });

  it("accepts login's 204 response without attempting to parse a JSON body", async () => {
    const loginResponse = new Response(null, { status: 204 });
    loginResponse.json = async () => { throw new Error("A 204 response must not be parsed"); };
    const calls = mockFetch(({ input }) => String(input) === "/api/auth/csrf" ? json(token) : loginResponse);

    const result = await request("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ username: "test-user", password: "not-a-real-password" }).toString(),
    });

    assert.equal(result, undefined);
    assert.equal(calls.length, 2);
    const sent = new Headers(calls[1].init?.headers);
    assert.equal(sent.get("Content-Type"), "application/x-www-form-urlencoded");
    assert.equal(sent.get(token.headerName), token.token);
  });

  it("deduplicates concurrent CSRF bootstrap calls and caches the resolved token", async () => {
    let release: ((response: Response) => void) | undefined;
    const calls = mockFetch(() => new Promise<Response>((resolve) => { release = resolve; }));

    const first = getCsrf();
    const second = getCsrf();
    assert.equal(calls.length, 1);
    assert.equal(String(calls[0].input), "/api/auth/csrf");
    assert.equal(calls[0].init?.credentials, "same-origin");
    assert.equal(calls[0].init?.cache, "no-store");
    assert.ok(release);
    release(json(token));
    const resolved = await Promise.all([first, second]);
    assert.deepEqual(resolved, [token, token]);
    assert.deepEqual(await getCsrf(), token);
    assert.equal(calls.length, 1);
  });

  it("sends cookie credentials and the server-selected CSRF header on JSON mutations", async () => {
    const calls = mockFetch(({ input }) => String(input) === "/api/auth/csrf" ? json(token) : json({ id: "confirmed-id" }));
    const body = JSON.stringify({ version: 3 });
    await request("/api/facilities/test/encounters/test/start", { method: "POST", body });

    const mutation = calls[1].init;
    const headers = new Headers(mutation?.headers);
    assert.equal(headers.get(token.headerName), token.token);
    assert.equal(headers.get("Content-Type"), "application/json");
    assert.equal(mutation?.credentials, "same-origin");
    assert.equal(mutation?.cache, "no-store");
    assert.equal(mutation?.body, body);
    assert.equal(mutation?.method, "POST");
  });

  it("retains JSON error status and code for version-conflict handling", async () => {
    mockFetch(() => json({ error: "STALE_VERSION" }, 409));
    await assert.rejects(request("/api/record"), (error: unknown) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 409);
      assert.equal(error.code, "STALE_VERSION");
      assert.match(error.message, /đối chiếu/);
      return true;
    });
  });

  it("handles a non-JSON gateway error as an API failure rather than a JSON parse error", async () => {
    mockFetch(() => new Response("<html>Bad gateway</html>", { status: 502, headers: { "Content-Type": "text/html" } }));
    await assert.rejects(request("/api/record"), (error: unknown) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 502);
      assert.ok(error.message.length > 0);
      assert.doesNotMatch(error.message, /<html>|Unexpected token/);
      return true;
    });
  });

  it("does not retry a rejected mutation even when its 403 response resets CSRF", async () => {
    const calls = mockFetch(({ input }) => String(input) === "/api/auth/csrf" ? json(token) : json({ error: "FORBIDDEN" }, 403));
    await assert.rejects(request("/api/mutate", { method: "POST", body: JSON.stringify({ version: 0 }) }), ApiError);
    assert.equal(calls.filter((call) => String(call.input) === "/api/mutate").length, 1);
    assert.equal(calls.length, 2);
  });

  it("does not retry a mutation after an uncertain network failure", async () => {
    const calls = mockFetch(({ input }) => {
      if (String(input) === "/api/auth/csrf") return json(token);
      throw new TypeError("Connection closed after sending the request");
    });
    await assert.rejects(request("/api/mutate", { method: "POST", body: "{}" }), (error: unknown) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 0);
      assert.equal(error.code, "NETWORK_ERROR");
      return true;
    });
    assert.equal(calls.filter((call) => String(call.input) === "/api/mutate").length, 1);
  });

  it("passes AbortSignal through and preserves AbortError for cancelled reads", async () => {
    const controller = new AbortController();
    controller.abort();
    const calls = mockFetch(({ init }) => {
      assert.equal(init?.signal, controller.signal);
      throw new DOMException("Cancelled by the caller", "AbortError");
    });
    await assert.rejects(request("/api/record", { signal: controller.signal }), (error: unknown) => {
      assert.ok(error instanceof DOMException);
      assert.equal(error.name, "AbortError");
      return true;
    });
    assert.equal(calls.length, 1);
  });

  it("handles 401 safely without a browser window and invalidates cached CSRF", async () => {
    let csrfReads = 0;
    mockFetch(({ input }) => {
      if (String(input) === "/api/auth/csrf") {
        csrfReads += 1;
        return json({ ...token, token: `token-${csrfReads}` });
      }
      return json({ error: "UNAUTHENTICATED" }, 401);
    });
    await getCsrf();
    await assert.rejects(request("/api/private-record"), (error: unknown) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 401);
      return true;
    });
    assert.equal(csrfReads, 1);
    assert.equal((await getCsrf()).token, "token-2");
  });

  it("dispatches the session-expired event when a browser-like window exists", async () => {
    const windowEvents = new EventTarget();
    let expired = 0;
    windowEvents.addEventListener("medora:expired", () => { expired += 1; });
    Object.defineProperty(globalThis, "window", { value: windowEvents, configurable: true });
    mockFetch(() => json({ error: "UNAUTHENTICATED" }, 401));

    await assert.rejects(request("/api/private-record"), ApiError);
    assert.equal(expired, 1);
  });

  it("leaves multipart Content-Type unset so fetch supplies the file boundary", async () => {
    const form = new FormData();
    form.append("file", new Blob(["test attachment"], { type: "text/plain" }), "attachment.txt");
    const calls = mockFetch(({ input }) => String(input) === "/api/auth/csrf" ? json(token) : json({ id: "attachment-id" }));

    await request("/api/facilities/test/encounters/test/attachments", { method: "POST", body: form });

    const mutation = calls[1].init;
    const headers = new Headers(mutation?.headers);
    assert.equal(headers.has("Content-Type"), false);
    assert.equal(headers.get(token.headerName), token.token);
    assert.equal(mutation?.body, form);
    assert.equal(mutation?.credentials, "same-origin");
  });
});
