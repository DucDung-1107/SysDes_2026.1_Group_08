"""Create reusable, clearly synthetic MEDORA staff and workflow fixtures through real APIs.

No database reset, record deletion, password reset or existing policy replacement is performed.
Credentials and resumable progress are saved only in the ignored src/.demo/<namespace>.json.
"""

import argparse
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
import re
import secrets
import sys
import urllib.error
from email.utils import parsedate_to_datetime

from api_client import Api, ApiError, atomic_json, bootstrap_password, local_origin, query


ROLES = {
    "rec": "RECEPTIONIST", "doc": "DOCTOR", "nurse": "NURSE", "tech": "TECHNICIAN",
    "pharm": "PHARMACIST", "cash": "CASHIER", "finance": "FINANCE", "manager": "CLINIC_MANAGER",
    "registrar": "AUTHORIZED_REGISTRAR", "audit": "AUDITOR",
}
SRC = Path(__file__).resolve().parents[1]


def utcnow():
    return datetime.now(timezone.utc)


def instant(value):
    return value.astimezone(timezone.utc).isoformat()


def clinic_zone(name):
    if name in {"Asia/Ho_Chi_Minh", "Asia/Saigon"}:
        return timezone(timedelta(hours=7), name)
    from zoneinfo import ZoneInfo
    return ZoneInfo(name)


class Seed:
    def __init__(self, base, namespace, manifest, username, env_file, facility=None, zone="Asia/Ho_Chi_Minh", allow_remote=False):
        self.base = local_origin(base, allow_remote)
        if not re.fullmatch(r"[a-z][a-z0-9-]{1,18}", namespace):
            raise RuntimeError("Namespace must be 2-19 lowercase letters/digits/hyphens, starting with a letter.")
        self.namespace, self.path = namespace, Path(manifest or SRC / ".demo" / (namespace + ".json"))
        self.existed = self.path.exists()
        self.data = json.loads(self.path.read_text(encoding="utf-8")) if self.existed else {
            "schema": 1, "namespace": namespace, "createdAt": instant(utcnow()),
            "password": secrets.token_urlsafe(24), "accounts": {}, "steps": {}, "planned": {}, "fixtures": {},
        }
        if self.data.get("namespace") != namespace or self.data.get("schema") != 1:
            raise RuntimeError("Manifest schema/namespace mismatch. Use a different --namespace/--manifest; no records were reset.")
        self.admin = Api(self.base)
        me = self.admin.login(username, bootstrap_password(env_file))
        chosen = facility or self.data.get("facilityId") or next(iter(me["facilityIds"]), None)
        if chosen not in me["facilityIds"]:
            raise RuntimeError("Selected facility is not assigned to the bootstrap account.")
        if self.data.get("facilityId") and self.data["facilityId"] != chosen:
            raise RuntimeError("Manifest belongs to a different facility. Use a new namespace; existing data was not reset.")
        self.facility = chosen
        self.root, self.admin_root = f"/api/facilities/{chosen}", f"/api/admin/facilities/{chosen}"
        self.data.update({"facilityId": chosen, "baseUrl": self.base, "zone": zone})
        self.zone = clinic_zone(zone)
        self.clients = {}
        self.save()

    def save(self):
        atomic_json(self.path, self.data)

    def server_now(self):
        response = self.admin.raw("GET", "/api/auth/me")
        return parsedate_to_datetime(response.headers["date"])

    def key(self, label):
        return "seed-" + hashlib.sha256(f"{self.namespace}:{self.facility}:{label}".encode()).hexdigest()[:40]

    def step(self, label, callback):
        if label not in self.data["steps"]:
            self.data["steps"][label] = callback()
            self.save()
        return self.data["steps"][label]

    def plan(self, label, callback):
        if label not in self.data["planned"]:
            self.data["planned"][label] = callback()
            self.save()
        return self.data["planned"][label]

    def post(self, role, path, body, label, idempotent=False):
        return self.step(label, lambda: self.clients[role].request("POST", self.root + path, body,
            headers={"Idempotency-Key": self.key(label)} if idempotent else None))

    def account(self, short, role):
        username = f"{self.namespace}.{short}"
        saved = self.data["accounts"].get(short)
        existing = None
        page = 0
        while True:
            listing = self.admin.request("GET", self.admin_root + "/accounts" + query(page=page, size=100))
            existing = next((item for item in listing["items"] if item["username"] == username), None)
            if existing or (page + 1) * 100 >= listing["totalElements"]:
                break
            page += 1
        if existing and not self.existed and not saved:
            raise RuntimeError(f"Username {username} already exists but no original manifest was supplied. No password reset was attempted.")
        if saved:
            if not existing or existing["id"] != saved["id"]:
                raise RuntimeError(f"Account/manifest mismatch for {username}; use a separate namespace for a new database.")
        elif existing:
            if not existing["displayName"].startswith("DEMO "):
                raise RuntimeError(f"Refusing to adopt non-DEMO account {username}.")
            saved = existing
        else:
            saved = self.admin.request("POST", self.admin_root + "/accounts", {
                "username": username, "displayName": f"DEMO {self.namespace} · {role}",
                "password": self.data["password"], "roles": [role],
            })
        client = Api(self.base)
        try:
            me = client.login(username, self.data["password"])
        except ApiError as error:
            raise RuntimeError(f"Saved demo credentials no longer authenticate {username} (HTTP {error.status}). No password reset was attempted.") from None
        if me["id"] != saved["id"]:
            raise RuntimeError("Authenticated account does not match manifest.")
        self.data["accounts"][short] = {"id": saved["id"], "username": username, "role": role}
        self.clients[short] = client
        self.save()
        return saved["id"]

    def patient(self, label):
        name = f"DEMO {self.namespace} · {label}"
        def create():
            found = self.clients["rec"].request("GET", self.root + "/patients" + query(q=name, page=0, size=100))["items"]
            existing = next((item for item in found if item["fullName"] == name), None)
            if existing:
                return existing
            return self.clients["rec"].request("POST", self.root + "/patients", {
                "fullName": name, "dateOfBirth": "1990-01-02", "sex": "UNKNOWN",
                "address": "Dữ liệu giả lập để kiểm thử và trình diễn; không phải người thật.",
            })
        return self.step("patient:" + label, create)

    def catalog(self):
        prefix = "DEMO-" + self.namespace.upper()
        for kind, staff, price in [("CONSULT", "doc", 120000), ("LAB", "tech", 60000)]:
            code = f"{prefix}-{kind}"
            def resource(resource_type, body, code=code):
                existing = self.admin.request("GET", self.root + "/" + resource_type)
                match = next((item for item in existing if item["code"] == code), None)
                return match or self.admin.request("POST", self.root + "/" + resource_type, body)
            room = self.step("room:" + kind, lambda kind=kind, code=code: resource("rooms", {
                "code": code, "name": f"DEMO {self.namespace} · Phòng {kind}", "roomType": "CONSULTATION" if kind == "CONSULT" else "LAB", "equipment": "", "enabled": True,
            }))
            service = self.step("service:" + kind, lambda kind=kind, code=code: resource("services", {
                "code": code, "name": f"DEMO {self.namespace} · Dịch vụ {kind}", "durationMinutes": 15, "bufferMinutes": 5, "enabled": True,
            }))
            self.step("capability:" + kind, lambda staff=staff, service=service: self.admin.request("POST", self.root + "/provider-services", {"providerId": self.data["accounts"][staff]["id"], "serviceId": service["id"]}))
            start = self.plan("schedule-start", lambda: instant(utcnow().astimezone(self.zone).replace(hour=0, minute=0, second=0, microsecond=0)))
            end = self.plan("schedule-end", lambda: instant(utcnow().astimezone(self.zone).replace(hour=0, minute=0, second=0, microsecond=0) + timedelta(days=3)))
            self.step("schedule:" + kind, lambda staff=staff: self.admin.request("POST", self.root + "/staff-schedules", {"providerId": self.data["accounts"][staff]["id"], "kind": "WORK", "startsAt": start, "endsAt": end}))
            self.step("price:" + kind, lambda service=service, price=price: self.clients["manager"].request("POST", self.root + "/service-prices", {"serviceId": service["id"], "amount": price, "currency": "VND", "effectiveFrom": start}))
            self.data["fixtures"]["resource:" + kind] = {"roomId": room["id"], "serviceId": service["id"], "staffId": self.data["accounts"][staff]["id"]}
            self.save()
        for name, ingredient in [("A", "A"), ("B", "B")]:
            code = f"{prefix}-MED-{name}"
            def medicine(code=code, name=name, ingredient=ingredient):
                existing = self.clients["manager"].request("GET", self.root + "/medications" + query(q=code, page=0, size=100))
                return next((item for item in existing if item["code"] == code), None) or self.clients["manager"].request("POST", self.root + "/medications", {"code": code, "name": f"DEMO {self.namespace} · Thuốc giả lập {name}", "ingredientCode": f"{prefix}-ING-{ingredient}", "unit": "đơn vị giả lập", "enabled": True})
            row = self.step("medicine:" + name, medicine)
            self.data["fixtures"]["medicine:" + name] = row
        self.save()
        self.step("medication-rule", lambda: self.clients["manager"].request("POST", self.root + "/medication-rules", {
            "medicationId": self.data["fixtures"]["medicine:A"]["id"], "otherMedicationId": self.data["fixtures"]["medicine:B"]["id"],
            "kind": "INTERACTION", "severity": "WARN", "overridable": False,
            "message": f"DEMO {self.namespace}: cảnh báo quy tắc thử nghiệm khi ghép hai thuốc giả lập.",
            "source": "DEMO synthetic test rule only; not a clinical drug interaction database", "enabled": True,
        }))
        try:
            policy = self.clients["manager"].request("GET", self.root + "/medication-policy")
        except ApiError as error:
            if error.status != 404:
                raise
            policy = self.step("new-medication-policy", lambda: self.clients["manager"].request("PUT", self.root + "/medication-policy", {"source": f"DEMO {self.namespace}: synthetic reviewed teaching rules; not clinical guidance", "dataVersion": "demo-1", "enabled": True}))
        if not policy["enabled"]:
            raise RuntimeError("Existing medication policy is disabled. Seed will not overwrite it. Enable an approved TEST policy explicitly before creating issued-prescription fixtures.")
        self.data["policyObserved"] = {"source": policy["source"], "dataVersion": policy["dataVersion"], "enabled": policy["enabled"], "version": policy["version"]}
        self.data["clinicPolicyObserved"] = self.clients["manager"].request("GET", self.root + "/clinic-policy")
        self.save()
        diagnosis = self.clients["doc"].request("GET", self.root + "/diagnosis-catalog")
        if not diagnosis:
            self.step("diagnosis", lambda: self.clients["manager"].request("PUT", self.root + "/diagnosis-catalog", {"code": prefix[:23] + ".01", "name": f"DEMO {self.namespace}: chẩn đoán thử nghiệm", "enabled": True}))
            diagnosis = self.clients["doc"].request("GET", self.root + "/diagnosis-catalog")
        self.data["diagnosisCode"] = diagnosis[0]["code"]
        self.save()

    def booking(self, label, offset, confirmed=False):
        patient = self.patient(label)
        resource = self.data["fixtures"]["resource:CONSULT"]
        at = self.plan("booking-time:" + label, lambda: instant((utcnow() + timedelta(minutes=offset)).replace(second=0, microsecond=0)))
        appointment = self.post("rec", "/appointments", {"patientId": patient["id"], "providerId": resource["staffId"], "roomId": resource["roomId"], "serviceId": resource["serviceId"], "startsAt": at}, "appointment:" + label, True)
        if confirmed:
            appointment = self.post("rec", f"/appointments/{appointment['id']}/transitions", {"action": "CONFIRM", "version": appointment["version"]}, "confirm:" + label)
        self.data["fixtures"][label] = {"patientId": patient["id"], "appointmentId": appointment["id"], "startsAt": at}
        self.save()
        return appointment

    def encounter(self, label, stage="waiting"):
        patient = self.patient(label)
        encounter = self.step("intake:" + label, lambda: next((item for item in self.clients["rec"].request("GET", self.root + "/queue?page=0&size=100") if item["patientId"] == patient["id"]), None) or self.clients["rec"].request("POST", self.root + "/check-ins", {"patientId": patient["id"], "doctorId": self.data["accounts"]["doc"]["id"], "nurseId": self.data["accounts"]["nurse"]["id"], "identityVerified": True}))
        visit = f"/encounters/{encounter['id']}"
        self.step("consult-service:" + label, lambda: self.clients["doc"].request("PUT", self.root + visit + "/consultation-service", {"serviceId": self.data["fixtures"]["resource:CONSULT"]["serviceId"]}))
        self.data["fixtures"][label] = {"patientId": patient["id"], "encounterId": encounter["id"]}
        self.save()
        if stage == "waiting":
            return visit
        self.post("nurse", visit + "/history", {"kind": "HISTORY", "content": "DEMO: hồ sơ giả lập để thực hành; không phải tư vấn hay dữ liệu bệnh nhân thật."}, "history:" + label)
        measured = encounter["createdAt"]
        self.post("nurse", visit + "/vitals", {"pulseBpm": 80, "temperatureC": 37, "systolicMmhg": 120, "diastolicMmhg": 80, "measuredAt": measured, "verified": True}, "vitals:" + label)
        if stage == "screened":
            return visit
        self.step("start:" + label, lambda: self.clients["doc"].request("POST", self.root + visit + "/start", {"version": self.clients["doc"].request("GET", self.root + visit)["version"]}))
        self.post("doc", visit + "/diagnoses", {"code": self.data["diagnosisCode"], "symptoms": "DEMO: triệu chứng giả lập; không dùng để quyết định điều trị."}, "diagnosis:" + label)
        note = self.post("doc", visit + "/notes", {"subjective": "DEMO: thông tin chủ quan giả lập.", "objective": "DEMO: thông tin khách quan giả lập.", "assessment": "DEMO: nhận định để thử luồng phần mềm.", "plan": "DEMO: kế hoạch giả lập, không phải chỉ dẫn y khoa."}, "note:" + label)
        self.data["fixtures"][label]["noteId"] = note["id"]
        self.save()
        if stage == "draft":
            return visit
        self.post("doc", visit + f"/notes/{note['id']}/sign", {"version": note["version"]}, "sign:" + label)
        if stage == "signed":
            return visit
        if self.data["clinicPolicyObserved"]["dischargeApprovalRequired"]:
            discharge = self.post("doc", visit + "/discharge-requests", {"reason": "DEMO: yêu cầu ra về của hồ sơ giả lập."}, "discharge:" + label)
            self.post("manager", f"/discharge-requests/{discharge['id']}/decision", {"version": discharge["version"], "action": "APPROVE", "reason": "DEMO: phê duyệt độc lập trong môi trường thử nghiệm."}, "discharge-approve:" + label)
        submitted = self.step("submit:" + label, lambda: self.clients["doc"].request("POST", self.root + visit + "/submit-for-closure", {"version": self.clients["doc"].request("GET", self.root + visit)["version"]}))
        self.post("doc", visit + "/complete", {"version": submitted["version"], "summary": "DEMO: lượt khám giả lập đã kết thúc.", "instructions": "Dữ liệu thử nghiệm phần mềm, không sử dụng làm hướng dẫn chăm sóc.", "followUp": "DEMO: không có lịch tái khám thực."}, "complete:" + label)
        return visit

    def prescription(self):
        label = "Đơn thuốc chờ cấp"
        visit = self.encounter(label, "signed")
        self.step("profile:rx", lambda: self.clients["nurse"].request("PUT", self.root + visit + "/medication-profile", {"allergyStatus": "KNOWN_NONE", "allergyCodes": [], "conditionCodes": [], "currentMedicationIds": []}))
        rx = self.post("doc", visit + "/prescriptions", {"items": [{"medicationId": self.data["fixtures"]["medicine:A"]["id"], "dose": "Liều giả lập", "route": "Đường dùng giả lập", "frequency": "Tần suất giả lập", "durationDays": 2, "quantity": 4, "instructions": "DEMO: không phải đơn dùng cho người thật."}]}, "rx:draft")
        safety = self.clients["doc"].request("GET", self.root + f"/prescriptions/{rx['id']}/safety")
        issued = self.post("doc", f"/prescriptions/{rx['id']}/issue", {"version": rx["version"], "pharmacistId": self.data["accounts"]["pharm"]["id"], "acknowledgedWarnings": [warning["id"] for warning in safety["warnings"]]}, "rx:issue", True)
        self.post("pharm", f"/prescriptions/{rx['id']}/medicine-events", {"itemId": issued["items"][0]["id"], "kind": "DISPENSED", "quantity": 1, "performedAt": issued["issuedAt"]}, "rx:partial-dispense", True)
        self.data["fixtures"][label].update({"prescriptionId": rx["id"], "itemId": issued["items"][0]["id"]})
        self.save()

    def procedure(self, label, in_progress=False):
        visit = self.encounter(label, "signed")
        resource = self.data["fixtures"]["resource:LAB"]
        order = self.post("doc", visit + "/orders", {"serviceId": resource["serviceId"], "reason": "DEMO: chỉ định thử nghiệm.", "priority": "ROUTINE", "preparation": "DEMO: không chuẩn bị cho thủ thuật thật.", "required": True, "kind": "LAB"}, "order:" + label)
        at = self.plan("allocation:" + label, lambda: instant(self.server_now() - timedelta(seconds=1) if in_progress else self.server_now() + timedelta(minutes=30)))
        allocated = self.step("allocate:" + label, lambda: self.clients["doc"].request("PUT", self.root + f"/orders/{order['id']}/allocation", {"version": order["version"], "staffId": resource["staffId"], "roomId": resource["roomId"], "startsAt": at, "reason": "DEMO: phân lịch tài nguyên thử nghiệm."}))
        if in_progress:
            if "order-start:" + label not in self.data["steps"]:
                current = self.clients["doc"].request("GET", self.root + f"/orders/{order['id']}")
                now = self.server_now()
                if current["status"] == "SCHEDULED" and not (
                    datetime.fromisoformat(current["startsAt"].replace("Z", "+00:00")) <= now < datetime.fromisoformat(current["endsAt"].replace("Z", "+00:00"))
                ):
                    allocated = self.clients["doc"].request("PUT", self.root + f"/orders/{order['id']}/allocation", {
                        "version": current["version"], "staffId": resource["staffId"], "roomId": resource["roomId"],
                        "startsAt": instant(now - timedelta(seconds=1)), "reason": "DEMO: resume an unfinished seed in the current server time slot.",
                    })
                    self.data["steps"]["allocate:" + label] = allocated
                    self.save()
            self.post("tech", f"/orders/{order['id']}/actions", {"action": "START", "version": allocated["version"]}, "order-start:" + label)
            content = "DEMO: tệp kết quả thử nghiệm, không có thông tin người thật.\n".encode("utf-8")
            attachment = self.step("attachment:" + label, lambda: self.clients["tech"].upload(self.root + visit + "/attachments", "DEMO-result.txt", content, order_id=order["id"]))
            self.data["fixtures"][label]["attachmentId"] = attachment["id"]
        self.data["fixtures"][label]["orderId"] = order["id"]
        self.save()

    def invoice(self, label, payment_mode):
        visit = self.encounter(label, "complete")
        invoice = self.post("cash", visit + "/invoice", None, "invoice:" + label, True)
        issued = self.post("cash", f"/invoices/{invoice['id']}/issue", {"version": invoice["version"]}, "invoice-issue:" + label)
        amount = int(issued["total"]) // 2 if payment_mode == "partial" else int(issued["total"])
        payment = self.post("cash", f"/invoices/{invoice['id']}/payments", {"amount": amount, "currency": "VND", "method": "CASH", "receivedVerified": True}, "payment:" + label, True)
        self.data["fixtures"][label].update({"invoiceId": invoice["id"], "paymentId": payment["id"]})
        if payment_mode == "refund":
            refund = self.post("finance", "/refunds", {"paymentId": payment["id"], "amount": 20000, "reason": "DEMO: hoàn một phần giao dịch giả lập."}, "refund:" + label, True)
            if refund["status"] == "PENDING_APPROVAL":
                refund = self.post("manager", f"/refunds/{refund['id']}/approval", {"version": refund["version"], "action": "APPROVE", "reason": "DEMO: phê duyệt độc lập."}, "refund-approve:" + label, True)
            if refund["status"] == "APPROVED":
                self.post("finance", f"/refunds/{refund['id']}/execute", {"version": refund["version"]}, "refund-execute:" + label, True)
            self.data["fixtures"][label]["refundId"] = refund["id"]
        self.save()

    def governance(self, appointment):
        template = self.step("notification-template", lambda: self.clients["manager"].request("POST", self.root + "/notification-templates", {"code": "DEMO_" + self.namespace.upper().replace("-", "_") + "_REMINDER", "body": "DEMO: lịch hẹn {{appointmentId}} lúc {{startsAt}}. Thông báo nội bộ thử nghiệm, không gửi SMS/email.", "enabled": True}))
        notification = self.post("rec", "/notifications", {"templateId": template["id"], "appointmentId": appointment["id"], "recipientId": self.data["accounts"]["doc"]["id"]}, "notification", True)
        source, target = self.patient("Hồ sơ trùng nguồn"), self.patient("Hồ sơ trùng chính")
        merge = self.post("rec", "/merge-requests", {"sourceId": source["id"], "targetId": target["id"], "reason": "DEMO: hai hồ sơ giả lập để minh họa hợp nhất."}, "merge-request", True)
        merged = self.post("registrar", f"/merge-requests/{merge['id']}/decision", {"version": merge["version"], "action": "APPROVE", "identityVerified": True, "reason": "DEMO: xác minh hai hồ sơ thử nghiệm."}, "merge-decision", True)
        self.data["fixtures"]["Thông báo nội bộ"] = {"notificationId": notification["id"]}
        self.data["fixtures"]["Hợp nhất hồ sơ"] = {"mergeRequestId": merged["id"], "sourcePatientId": source["id"], "targetPatientId": target["id"]}
        self.save()

    def run(self):
        for short, role in ROLES.items():
            self.account(short, role)
        if not self.data.get("completedAt"):
            self.catalog()
            booked = self.booking("Lịch đã đặt", 60)
            self.booking("Lịch đã xác nhận", 100, True)
            self.encounter("Đang chờ tiếp nhận khám", "waiting")
            self.encounter("Đã sàng lọc chờ bác sĩ", "screened")
            self.encounter("Đang khám có ghi chú nháp", "draft")
            self.prescription()
            self.procedure("Dịch vụ đã phân lịch")
            self.procedure("Dịch vụ đang thực hiện", True)
            self.encounter("Đã kết thúc chưa lập hóa đơn", "complete")
            self.invoice("Hóa đơn thanh toán một phần", "partial")
            self.invoice("Hóa đơn đã thanh toán", "paid")
            self.invoice("Hóa đơn có lịch sử hoàn", "refund")
            self.governance(booked)
            self.data["completedAt"] = instant(utcnow())
            self.save()
        return self.summary()

    def summary(self):
        fixtures = {}
        for label, fixture in self.data["fixtures"].items():
            if "patientId" not in fixture:
                continue
            observed = dict(fixture)
            if fixture.get("encounterId"):
                visit = self.clients["doc"].request("GET", self.root + f"/encounters/{fixture['encounterId']}")
                observed["encounterStatus"] = visit["status"]
            if fixture.get("invoiceId"):
                bill = self.clients["cash"].request("GET", self.root + f"/invoices/{fixture['invoiceId']}")
                observed.update({"invoiceStatus": bill["status"], "balance": bill["balance"]})
            if fixture.get("orderId"):
                order = self.clients["doc"].request("GET", self.root + f"/orders/{fixture['orderId']}")
                observed["orderStatus"] = order["status"]
            fixtures[label] = observed
        self.data["lastVerifiedAt"] = instant(utcnow())
        self.data["summary"] = fixtures
        self.save()
        return {"result": "PASS", "namespace": self.namespace, "facilityId": self.facility,
            "manifest": str(self.path.resolve()), "accounts": list(self.data["accounts"].values()),
            "patientsCreatedOrReused": len([name for name in self.data["steps"] if name.startswith("patient:")]),
            "fixtures": fixtures, "password": "Stored only in the local manifest; never printed"}


def arguments():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--namespace", default="demo")
    parser.add_argument("--manifest", type=Path)
    parser.add_argument("--username", default="admin.demo", help="Existing bootstrap administrator username")
    parser.add_argument("--env-file", type=Path, default=SRC / ".env")
    parser.add_argument("--facility-id")
    parser.add_argument("--zone", default="Asia/Ho_Chi_Minh")
    parser.add_argument("--allow-remote", action="store_true", help="Explicitly authorize a non-loopback TEST origin")
    return parser.parse_args()


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    args = arguments()
    try:
        seed = Seed(args.base_url, args.namespace, args.manifest, args.username, args.env_file, args.facility_id, args.zone, args.allow_remote)
        print(json.dumps(seed.run(), ensure_ascii=False, indent=2))
    except (RuntimeError, urllib.error.URLError) as error:
        sys.exit("Seed stopped safely: " + str(error))
