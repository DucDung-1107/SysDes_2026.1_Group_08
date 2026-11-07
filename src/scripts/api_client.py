"""Small standard-library HTTP client shared by MEDORA local seed and workflow checks."""

from dataclasses import dataclass
import http.cookiejar
import json
import os
from pathlib import Path
import secrets
import urllib.error
import urllib.parse
import urllib.request


class ApiError(RuntimeError):
    def __init__(self, method, path, status, code):
        self.method, self.path, self.status, self.code = method, path, status, code
        super().__init__(f"{method} {path}: HTTP {status} {code}")


@dataclass
class Response:
    status: int
    headers: dict
    content: bytes

    def json(self):
        return json.loads(self.content) if self.content else None


def local_origin(base, allow_remote=False):
    parsed = urllib.parse.urlsplit(base)
    if parsed.scheme not in {"http", "https"} or not parsed.hostname or parsed.username or parsed.password or parsed.query or parsed.fragment or parsed.path not in {"", "/"}:
        raise RuntimeError("--base-url must be an HTTP(S) origin without credentials, path or query.")
    if not allow_remote and parsed.hostname not in {"127.0.0.1", "localhost", "::1"}:
        raise RuntimeError("Only loopback targets are accepted by default. Use --allow-remote for an explicitly authorized test environment.")
    return base.rstrip("/")


def env_values(path):
    values = {}
    path = Path(path)
    if path.exists():
        for line in path.read_text(encoding="utf-8-sig").splitlines():
            if line.strip() and not line.lstrip().startswith("#") and "=" in line:
                key, _, value = line.partition("=")
                values[key.strip()] = value.strip().strip("\"'")
    return values


def bootstrap_password(env_file=None):
    values = env_values(env_file or Path(__file__).resolve().parents[1] / ".env")
    value = os.environ.get("MEDORA_BOOTSTRAP_PASSWORD") or values.get("MEDORA_BOOTSTRAP_PASSWORD")
    if not value:
        raise RuntimeError("Set MEDORA_BOOTSTRAP_PASSWORD in the environment or the selected --env-file.")
    return value


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.calls = []
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def raw(self, method, path, body=None, *, headers=None, form=False, csrf=True):
        headers = dict(headers or {})
        if method not in {"GET", "HEAD"} and csrf:
            token = self.request("GET", "/api/auth/csrf")
            headers[token["headerName"]] = token["token"]
        payload = body
        if body is not None and not isinstance(body, bytes):
            payload = urllib.parse.urlencode(body).encode() if form else json.dumps(body, ensure_ascii=False).encode("utf-8")
            headers.setdefault("Content-Type", "application/x-www-form-urlencoded" if form else "application/json")
        req = urllib.request.Request(self.base + path, data=payload, headers=headers, method=method)
        try:
            with self.opener.open(req, timeout=30) as response:
                result = Response(response.status, {key.lower(): value for key, value in response.headers.items()}, response.read())
                self.calls.append({"method": method, "path": path.partition("?")[0], "status": result.status})
                return result
        except urllib.error.HTTPError as error:
            try:
                code = json.loads(error.read()).get("error", "HTTP_ERROR")
            except (ValueError, AttributeError):
                code = "HTTP_ERROR"
            self.calls.append({"method": method, "path": path.partition("?")[0], "status": error.code})
            raise ApiError(method, path.partition("?")[0], error.code, code) from None
        except urllib.error.URLError:
            raise RuntimeError(f"Cannot reach {self.base}; no automatic mutation retry was attempted.") from None

    def request(self, method, path, body=None, **options):
        response = self.raw(method, path, body, **options)
        return response.json() if response.content else None

    def login(self, username, password):
        self.request("POST", "/api/auth/login", {"username": username, "password": password}, form=True)
        return self.request("GET", "/api/auth/me")

    def upload(self, path, filename, content, *, media_type="text/plain", order_id=None):
        if any(character in filename for character in ['"', "\r", "\n", "\\", "/"]):
            raise RuntimeError("Unsafe multipart filename.")
        boundary = "medora-" + secrets.token_hex(20)
        payload = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\n'
                   f"Content-Type: {media_type}\r\n\r\n").encode() + content + f"\r\n--{boundary}--\r\n".encode()
        if order_id:
            path += "?" + urllib.parse.urlencode({"orderId": order_id})
        return self.request("POST", path, payload, headers={"Content-Type": "multipart/form-data; boundary=" + boundary})


def atomic_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    try:
        os.chmod(temporary, 0o600)
    except OSError:
        pass
    os.replace(temporary, path)


def query(**values):
    return "?" + urllib.parse.urlencode({key: value for key, value in values.items() if value is not None})
