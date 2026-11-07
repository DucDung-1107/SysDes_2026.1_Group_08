"""Read-only session/contract smoke checks against a local MEDORA API or proxy.

Credentials are read into memory from the explicitly selected test environment.
The report contains check names/status only, never cookies, tokens or records.
No application data is created or updated; ordinary security/audit events may be
recorded by the backend. The rejected POST has an invalid empty body and no CSRF.
"""

import argparse
import http.cookiejar
import json
from pathlib import Path
import sys
import urllib.error
import urllib.parse
import urllib.request


def test_password(env_file: Path) -> str:
    for line in env_file.read_text(encoding="utf-8-sig").splitlines():
        key, separator, value = line.partition("=")
        if separator and key.strip() == "MEDORA_BOOTSTRAP_PASSWORD":
            password = value.strip().strip("\"'")
            if password:
                return password
    raise ValueError("The selected test environment has no bootstrap password.")


def run(base: str, env_file: Path, username: str) -> dict:
    parsed = urllib.parse.urlparse(base)
    if (
        parsed.scheme not in ("http", "https")
        or parsed.hostname not in ("localhost", "127.0.0.1", "::1")
        or parsed.username
        or parsed.password
        or parsed.query
        or parsed.fragment
        or parsed.path not in ("", "/")
    ):
        raise ValueError("Smoke checks only accept a loopback HTTP(S) origin.")
    base = base.rstrip("/")
    cookies = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(
        urllib.request.ProxyHandler({}), urllib.request.HTTPCookieProcessor(cookies)
    )
    checks = []

    def request(method, path, body=None, headers=None):
        req = urllib.request.Request(
            base + path, data=body, headers=headers or {}, method=method
        )
        try:
            response = opener.open(req, timeout=30)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            raw = response.read()
            data = json.loads(raw) if raw.startswith((b"{", b"[")) else None
            return response.code, data, response.headers

    def check(name, actual, expected, condition=True):
        if actual != expected or not condition:
            raise RuntimeError(f"Check {name} did not meet its expected contract (HTTP {actual}).")
        checks.append({"check": name, "status": "PASS", "httpStatus": actual})

    def session_id():
        return next((cookie.value for cookie in cookies if cookie.name == "JSESSIONID"), None)

    status, _, _ = request("GET", "/api/auth/me")
    check("anonymous_session_is_rejected", status, 401)
    status, csrf, _ = request("GET", "/api/auth/csrf")
    check("csrf_bootstrap", status, 200, isinstance(csrf, dict) and bool(csrf.get("token")))
    previous_session = session_id()
    form = urllib.parse.urlencode(
        {"username": username, "password": test_password(env_file)}
    ).encode()
    status, _, headers = request(
        "POST", "/api/auth/login", form,
        {"Content-Type": "application/x-www-form-urlencoded", csrf["headerName"]: csrf["token"]},
    )
    check("form_login_returns_empty_success", status, 204)
    cookie_headers = " ".join(headers.get_all("Set-Cookie", []))
    check("session_cookie_flags_and_rotation", status, 204,
          "HttpOnly" in cookie_headers and "SameSite=Lax" in cookie_headers
          and bool(session_id()) and session_id() != previous_session)
    status, csrf, _ = request("GET", "/api/auth/csrf")
    check("refresh_csrf_after_login", status, 200, isinstance(csrf, dict) and bool(csrf.get("token")))
    status, me, _ = request("GET", "/api/auth/me")
    check("authorized_facility_bootstrap", status, 200,
          isinstance(me, dict) and bool(me.get("facilityIds")))
    facility = me["facilityIds"][0]
    status, permissions, _ = request("GET", "/api/auth/permissions?facilityId=" + facility)
    check("facility_permissions", status, 200, isinstance(permissions, list) and "account.read" in permissions)
    status, roles, _ = request("GET", f"/api/admin/facilities/{facility}/roles")
    check("roles_contract", status, 200,
          isinstance(roles, list) and all("code" in role and "name" in role for role in roles))
    status, accounts, _ = request("GET", f"/api/admin/facilities/{facility}/accounts?page=0&size=1")
    check("accounts_page_contract", status, 200,
          isinstance(accounts, dict) and all(key in accounts for key in ("items", "page", "size", "totalElements")))
    status, _, _ = request("GET", f"/api/facilities/{facility}/patients?page=0&size=1")
    check("administrator_has_no_implied_patient_access", status, 403)
    status, _, _ = request("POST", f"/api/admin/facilities/{facility}/accounts", b"{}",
                           {"Content-Type": "application/json"})
    check("invalid_mutation_without_csrf_is_rejected", status, 403)
    status, _, _ = request("POST", "/api/auth/logout", headers={csrf["headerName"]: csrf["token"]})
    check("logout_returns_empty_success", status, 204)
    status, _, _ = request("GET", "/api/auth/me")
    check("logged_out_session_is_rejected", status, 401)
    return {
        "result": "PASS", "base": base, "checks": checks,
        "applicationRecordsModified": False,
        "credentialTokenAndRecordValuesIncluded": False,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="http://127.0.0.1:3000")
    parser.add_argument("--env-file", type=Path, default=Path(__file__).resolve().parents[1] / ".env")
    parser.add_argument("--username", default="admin.demo")
    args = parser.parse_args()
    try:
        result = run(args.base, args.env_file, args.username)
    except Exception as error:
        # Never print urllib requests, exception bodies, credentials or response data.
        detail = str(error) if isinstance(error, (ValueError, RuntimeError)) else type(error).__name__
        print(json.dumps({"result": "FAIL", "reason": detail}), file=sys.stderr)
        return 1
    print(json.dumps(result, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
