"""Synthetic Stage 2-4 demo against a running MEDORA API. Python standard library only."""

import argparse
from datetime import datetime, timedelta, timezone
import http.cookiejar
import json
import os
from pathlib import Path
import secrets
import sys
import urllib.error
import urllib.parse
import urllib.request


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
        )

    def request(self, method, path, body=None, *, headers=None, form=False):
        headers = dict(headers or {})
        if method != "GET":
            csrf = self.request("GET", "/api/auth/csrf")
            headers[csrf["headerName"]] = csrf["token"]
        payload = None
        if body is not None:
            payload = urllib.parse.urlencode(body).encode() if form else json.dumps(body).encode()
            headers["Content-Type"] = "application/x-www-form-urlencoded" if form else "application/json"
        request = urllib.request.Request(self.base + path, data=payload, headers=headers, method=method)
        try:
            with self.opener.open(request, timeout=30) as response:
                content = response.read()
                return json.loads(content) if content and content.startswith((b"{", b"[")) else None
        except urllib.error.HTTPError as error:
            try:
                code = json.loads(error.read()).get("error", "HTTP_ERROR")
            except (ValueError, AttributeError):
                code = "HTTP_ERROR"
            raise RuntimeError(f"{method} {path}: HTTP {error.code} {code}") from None

    def login(self, username, password):
        self.request("POST", "/api/auth/login", {"username": username, "password": password}, form=True)


def bootstrap_password(env_file=None):
    value = os.environ.get("MEDORA_BOOTSTRAP_PASSWORD")
    if value:
        return value
    env_file = Path(env_file) if env_file is not None else Path(__file__).resolve().parents[1] / ".env"
    if env_file.exists():
        for line in env_file.read_text(encoding="utf-8-sig").splitlines():
            if line.startswith("MEDORA_BOOTSTRAP_PASSWORD="):
                return line.partition("=")[2].strip().strip("\"'")
    raise RuntimeError("Set MEDORA_BOOTSTRAP_PASSWORD in src/.env or the environment first.")


def run(base, username, env_file=None):
    password = bootstrap_password(env_file)
    administrator = Api(base)
    administrator.login(username, password)
    me = administrator.request("GET", "/api/auth/me")
    if not me["facilityIds"]:
        raise RuntimeError("The administrator has no enabled facility assignment.")
    facility = me["facilityIds"][0]
    root = f"/api/facilities/{facility}"
    admin_root = f"/api/admin/facilities/{facility}"
    suffix = secrets.token_hex(4)
    demo_password = secrets.token_urlsafe(24)
    staff = {}
    for prefix, role in (("rec", "RECEPTIONIST"), ("nurse", "NURSE"), ("doc", "DOCTOR")):
        account = administrator.request("POST", admin_root + "/accounts", {
            "username": f"{prefix}.{suffix}", "displayName": f"Synthetic {prefix} {suffix}",
            "password": demo_password, "roles": [role],
        })
        api = Api(base)
        api.login(account["username"], demo_password)
        staff[prefix] = (account["id"], api)
    doctor, clinician = staff["doc"]
    nurse, screening = staff["nurse"]
    _, reception = staff["rec"]
    room = administrator.request("POST", root + "/rooms", {
        "code": "R-" + suffix.upper(), "name": "Synthetic demo room", "roomType": "CONSULTATION",
        "equipment": "", "enabled": True,
    })
    service = administrator.request("POST", root + "/services", {
        "code": "S-" + suffix.upper(), "name": "Synthetic consultation", "durationMinutes": 20,
        "bufferMinutes": 10, "enabled": True,
    })
    administrator.request("POST", root + "/provider-services", {"providerId": doctor, "serviceId": service["id"]})
    now = datetime.now(timezone.utc)
    at = now + timedelta(minutes=10)
    administrator.request("POST", root + "/staff-schedules", {
        "providerId": doctor, "kind": "WORK", "startsAt": now.isoformat(),
        "endsAt": (now + timedelta(hours=4)).isoformat(),
    })
    patient = reception.request("POST", root + "/patients", {
        "fullName": "Synthetic Demo " + suffix, "dateOfBirth": "2000-01-02", "sex": "UNKNOWN",
    })
    booking = {"patientId": patient["id"], "providerId": doctor, "roomId": room["id"],
               "serviceId": service["id"], "startsAt": at.isoformat()}
    key = "demo-" + suffix
    appointment = reception.request("POST", root + "/appointments", booking, headers={"Idempotency-Key": key})
    retry = reception.request("POST", root + "/appointments", booking, headers={"Idempotency-Key": key})
    if retry["id"] != appointment["id"]:
        raise RuntimeError("Booking idempotency assertion failed.")
    appointment = reception.request("POST", root + f"/appointments/{appointment['id']}/transitions", {
        "action": "CONFIRM", "version": appointment["version"],
    })
    intake = {"patientId": patient["id"], "appointmentId": appointment["id"], "doctorId": doctor,
              "nurseId": nurse, "identityVerified": True}
    try:
        encounter = reception.request("POST", root + "/check-ins", intake)
    except RuntimeError as error:
        if "CHECK_IN_REQUIRES_APPOINTMENT_DATE" not in str(error):
            raise
        reception.request("POST", root + f"/appointments/{appointment['id']}/transitions", {
            "action": "CANCEL", "version": appointment["version"], "reason": "Demo crossed clinic midnight",
        })
        intake.pop("appointmentId")
        encounter = reception.request("POST", root + "/check-ins", intake)
    visit = root + f"/encounters/{encounter['id']}"
    screening.request("POST", visit + "/vitals", {
        "temperatureC": 37, "pulseBpm": 80, "systolicMmhg": 120, "diastolicMmhg": 80,
        "measuredAt": (datetime.now(timezone.utc) - timedelta(seconds=1)).isoformat(), "verified": True,
    })
    screening.request("POST", visit + "/history", {"kind": "HISTORY", "content": "Synthetic teaching data only"})
    encounter = clinician.request("POST", visit + "/start", {"version": encounter["version"]})
    clinician.request("POST", visit + "/diagnoses", {"code": "DEMO-001", "symptoms": "Synthetic demonstration"})
    note = clinician.request("POST", visit + "/notes", {
        "subjective": "Synthetic symptoms", "objective": "Synthetic examination",
        "assessment": "Teaching example", "plan": "Synthetic plan; no actual medical advice",
    })
    note = clinician.request("POST", visit + f"/notes/{note['id']}/sign", {"version": note["version"]})
    amendment = clinician.request("POST", visit + f"/notes/{note['id']}/amendments", {
        "version": note["version"], "reason": "Demonstrate linked correction history",
    })
    amendment = clinician.request("PUT", visit + f"/notes/{amendment['id']}", {
        "subjective": "Synthetic symptoms", "objective": "Synthetic examination",
        "assessment": "Teaching example", "plan": "Corrected synthetic plan",
        "version": amendment["version"],
    })
    clinician.request("POST", visit + f"/notes/{amendment['id']}/sign", {"version": amendment["version"]})
    encounter = clinician.request("POST", visit + "/submit-for-closure", {"version": encounter["version"]})
    encounter = clinician.request("POST", visit + "/complete", {
        "version": encounter["version"], "summary": "Completed synthetic teaching encounter",
        "instructions": "Demonstration only", "followUp": "No follow-up in this synthetic example",
    })
    if encounter["status"] != "COMPLETED":
        raise RuntimeError("Closure assertion failed.")
    print(json.dumps({"result": "PASS", "facilityId": facility, "patientId": patient["id"],
        "appointmentId": appointment["id"], "encounterId": encounter["id"], "status": encounter["status"]}, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--username", default="admin.demo")
    parser.add_argument("--env-file", type=Path, help="Read bootstrap password from this local env file")
    args = parser.parse_args()
    try:
        run(args.base_url, args.username, args.env_file)
    except (RuntimeError, urllib.error.URLError) as error:
        sys.exit(str(error))
