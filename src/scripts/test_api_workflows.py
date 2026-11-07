"""Exercise the complete MEDORA workflow and rejection boundaries using synthetic data only."""

import argparse
from datetime import datetime, timedelta, timezone
from decimal import Decimal
import hashlib
import json
from pathlib import Path
import secrets
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from api_client import Api, ApiError, bootstrap_password, local_origin
from email.utils import parsedate_to_datetime
import re


def upload(api, path, filename, content, order):
    boundary = "medora-" + secrets.token_hex(16)
    csrf = api.request("GET", "/api/auth/csrf")
    payload = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{filename}\"\r\n"
               "Content-Type: text/plain\r\n\r\n").encode() + content + f"\r\n--{boundary}--\r\n".encode()
    request = urllib.request.Request(api.base + path + "?orderId=" + order, data=payload,
        headers={csrf["headerName"]: csrf["token"], "Content-Type": "multipart/form-data; boundary=" + boundary}, method="POST")
    with api.opener.open(request, timeout=30) as response:
        return json.loads(response.read())


def run(base, username, env_file):
    base = local_origin(base)
    administrator = Api(base)
    administrator.login(username, bootstrap_password(env_file))
    facility = administrator.request("GET", "/api/auth/me")["facilityIds"][0]
    root = f"/api/facilities/{facility}"
    admin_root = f"/api/admin/facilities/{facility}"
    suffix = secrets.token_hex(4)
    password = secrets.token_urlsafe(24)
    users = {}
    for name, role in (("rec", "RECEPTIONIST"), ("doc", "DOCTOR"), ("nurse", "NURSE"),
                       ("tech", "TECHNICIAN"), ("pharm", "PHARMACIST"), ("cash", "CASHIER"),
                       ("finance", "FINANCE"), ("manager", "CLINIC_MANAGER"), ("registrar", "AUTHORIZED_REGISTRAR"), ("audit", "AUDITOR")):
        account = administrator.request("POST", admin_root + "/accounts", {
            "username": f"{name}.{suffix}", "displayName": f"Synthetic {name} {suffix}", "password": password, "roles": [role]})
        client = Api(base)
        client.login(account["username"], password)
        users[name] = (account["id"], client)
    doctor, clinician = users["doc"]
    nurse, screening = users["nurse"]
    technician, executor = users["tech"]
    pharmacist, pharmacy = users["pharm"]
    _, reception = users["rec"]
    _, cashier = users["cash"]
    _, finance = users["finance"]
    _, manager = users["manager"]
    _, registrar = users["registrar"]
    _, auditor = users["audit"]
    now = datetime.now(timezone.utc)
    report_query = urllib.parse.urlencode({"from": (now-timedelta(days=1)).isoformat(), "to": (now+timedelta(days=1)).isoformat()})
    before = manager.request("GET", root + "/reports/revenue?" + report_query)
    resources = {}
    for kind, staff, price in (("CONSULTATION", doctor, 100000), ("LAB", technician, 50000)):
        room = administrator.request("POST", root + "/rooms", {"code": kind[:3]+"-"+suffix.upper(),
            "name": "Synthetic " + kind, "roomType": kind, "equipment": "", "enabled": True})
        service = administrator.request("POST", root + "/services", {"code": kind[:3]+"-"+suffix.upper(),
            "name": "Synthetic " + kind, "durationMinutes": 20, "bufferMinutes": 5, "enabled": True})
        administrator.request("POST", root + "/provider-services", {"providerId": staff, "serviceId": service["id"]})
        administrator.request("POST", root + "/staff-schedules", {"providerId": staff, "kind": "WORK",
            "startsAt": (now-timedelta(hours=1)).isoformat(), "endsAt": (now+timedelta(hours=8)).isoformat()})
        manager.request("POST", root + "/service-prices", {"serviceId": service["id"], "amount": price,
            "currency": "VND", "effectiveFrom": (now-timedelta(days=1)).isoformat()})
        resources[kind] = (room["id"], service["id"])
    medication = manager.request("POST", root + "/medications", {"code": "MED-"+suffix.upper(), "name": "Synthetic teaching medicine",
        "ingredientCode": "ING-"+suffix.upper(), "unit": "tablet", "enabled": True})
    try:
        policy = manager.request("GET", root + "/medication-policy")
    except RuntimeError as error:
        if "HTTP 404" not in str(error):
            raise
        policy = {}
    if not policy:
        manager.request("PUT", root + "/medication-policy", {"source": "Synthetic reviewed test rules; no clinical drug database", "dataVersion": "demo-1", "enabled": True})
    elif not policy["enabled"]:
        raise RuntimeError("Use an isolated test environment with an enabled synthetic policy; existing policy was not replaced.")
    clinic_policy = manager.request("GET", root + "/clinic-policy")
    if clinic_policy["autoRefundLimit"] != 0:
        raise RuntimeError("Full approval tests require an isolated environment with autoRefundLimit=0; existing policy was not changed.")
    patient = reception.request("POST", root + "/patients", {"fullName": "Synthetic Full Demo " + suffix, "dateOfBirth": "2000-01-02", "sex": "UNKNOWN"})
    room, service = resources["CONSULTATION"]
    appointment = reception.request("POST", root + "/appointments", {"patientId": patient["id"], "providerId": doctor,
        "roomId": room, "serviceId": service, "startsAt": (now+timedelta(minutes=10)).isoformat()}, headers={"Idempotency-Key": "full-book-"+suffix})
    intake = {"patientId": patient["id"], "appointmentId": appointment["id"], "doctorId": doctor, "nurseId": nurse, "identityVerified": True}
    try:
        encounter = reception.request("POST", root + "/check-ins", intake)
    except RuntimeError as error:
        if "CHECK_IN_REQUIRES_APPOINTMENT_DATE" not in str(error):
            raise
        reception.request("POST", root+f"/appointments/{appointment['id']}/transitions", {"action": "CANCEL", "version": appointment["version"], "reason": "Demo crossed midnight"})
        intake.pop("appointmentId")
        encounter = reception.request("POST", root+"/check-ins", intake)
        clinician.request("PUT", root+f"/encounters/{encounter['id']}/consultation-service", {"serviceId": service})
        encounter = clinician.request("GET", root+f"/encounters/{encounter['id']}")
    visit = root + f"/encounters/{encounter['id']}"
    clinician.request("POST", visit + "/start", {"version": encounter["version"]})
    screening.request("PUT", visit + "/medication-profile", {"allergyStatus": "KNOWN_NONE", "allergyCodes": [], "conditionCodes": [], "currentMedicationIds": []})
    rx = clinician.request("POST", visit + "/prescriptions", {"items": [{"medicationId": medication["id"],
        "dose": "Synthetic dose", "route": "Synthetic route", "frequency": "Synthetic frequency", "durationDays": 2,
        "quantity": 4, "instructions": "Teaching example only; no medical advice"}]})
    rx = clinician.request("POST", root+f"/prescriptions/{rx['id']}/issue", {"version": rx["version"], "pharmacistId": pharmacist,
        "acknowledgedWarnings": []}, headers={"Idempotency-Key": "full-rx-"+suffix})
    pharmacy.request("POST", root+f"/prescriptions/{rx['id']}/medicine-events", {"itemId": rx["items"][0]["id"],
        "kind": "DISPENSED", "quantity": 4, "performedAt": rx["issuedAt"]}, headers={"Idempotency-Key": "full-dispense-"+suffix})
    room, service = resources["LAB"]
    order = clinician.request("POST", visit+"/orders", {"serviceId": service, "reason": "Synthetic lab request", "priority": "ROUTINE",
        "preparation": "Teaching preparation", "required": True, "kind": "LAB"})
    order_path = root+f"/orders/{order['id']}"
    order = clinician.request("PUT", order_path+"/allocation", {"version": order["version"], "staffId": technician, "roomId": room,
        "startsAt": (parsedate_to_datetime(administrator.raw("GET", "/api/auth/me").headers["date"])-timedelta(seconds=1)).isoformat(), "reason": "Synthetic allocation"})
    content = b"Synthetic private laboratory result; no patient data."
    attachment = upload(executor, visit+"/attachments", "result.txt", content, order["id"])
    with clinician.opener.open(clinician.base+root+f"/attachments/{attachment['id']}/download", timeout=30) as response:
        assert hashlib.sha256(response.read()).hexdigest() == hashlib.sha256(content).hexdigest()
    order = executor.request("POST", order_path+"/actions", {"action": "START", "version": order["version"]})
    order = executor.request("POST", order_path+"/actions", {"action": "COMPLETE", "version": order["version"], "result": "Synthetic result recorded"})
    clinician.request("POST", order_path+"/review", {"version": order["version"], "reason": "Synthetic result reviewed"})
    clinician.request("POST", visit+"/diagnoses", {"code": "DEMO-001", "symptoms": "Synthetic symptoms"})
    note = clinician.request("POST", visit+"/notes", {"subjective": "Synthetic symptoms", "objective": "Synthetic examination",
        "assessment": "Teaching assessment", "plan": "Synthetic plan"})
    clinician.request("POST", visit+f"/notes/{note['id']}/sign", {"version": note["version"]})
    screening.request("POST", visit+"/vitals", {"pulseBpm": 80, "measuredAt": encounter["createdAt"], "verified": True})
    encounter = clinician.request("GET", visit)
    if clinic_policy["dischargeApprovalRequired"]:
        discharge = clinician.request("POST", visit + "/discharge-requests", {"reason": "Synthetic workflow approval"})
        manager.request("POST", root + f"/discharge-requests/{discharge['id']}/decision", {"version": discharge["version"], "action": "APPROVE", "reason": "Independent synthetic approval"})
    encounter = clinician.request("POST", visit+"/submit-for-closure", {"version": encounter["version"]})
    encounter = clinician.request("POST", visit+"/complete", {"version": encounter["version"], "summary": "Synthetic full demo completed",
        "instructions": "Teaching example", "followUp": "No follow-up in this synthetic example"})
    invoice = cashier.request("POST", visit+"/invoice", headers={"Idempotency-Key": "full-invoice-"+suffix})
    assert Decimal(str(invoice["subtotal"])) == 150000
    invoice = finance.request("POST", root+f"/invoices/{invoice['id']}/discount", {"version": invoice["version"], "amount": 10000, "reason": "Synthetic authorized discount"})
    invoice = cashier.request("POST", root+f"/invoices/{invoice['id']}/issue", {"version": invoice["version"]})
    first = cashier.request("POST", root+f"/invoices/{invoice['id']}/payments", {"amount": 40000, "currency": "VND", "method": "CASH", "receivedVerified": True}, headers={"Idempotency-Key": "full-pay1-"+suffix})
    cashier.request("POST", root+f"/invoices/{invoice['id']}/payments", {"amount": 100000, "currency": "VND", "method": "CASH", "receivedVerified": True}, headers={"Idempotency-Key": "full-pay2-"+suffix})
    refund = finance.request("POST", root+"/refunds", {"paymentId": first["id"], "amount": 30000, "reason": "Synthetic refund"}, headers={"Idempotency-Key": "full-refund-"+suffix})
    assert refund["status"] == "PENDING_APPROVAL" and "invoice" not in refund
    refund = manager.request("POST", root+f"/refunds/{refund['id']}/approval", {"version": refund["version"], "action": "APPROVE", "reason": "Independent teaching approval"}, headers={"Idempotency-Key": "full-approve-"+suffix})
    refund = finance.request("POST", root+f"/refunds/{refund['id']}/execute", {"version": refund["version"]}, headers={"Idempotency-Key": "full-execute-"+suffix})
    assert Decimal(str(refund["invoice"]["balance"])) == 30000
    after = manager.request("GET", root+"/reports/revenue?"+report_query)
    assert Decimal(str(after["netCash"]))-Decimal(str(before["netCash"])) == 110000
    audits = auditor.request("GET", root+"/audit-events?"+report_query+"&action=REFUND_COMPLETED")
    assert any(row["targetId"] == refund["id"] for row in audits)
    candidates = [reception.request("POST", root+"/patients", {"fullName": f"Synthetic Merge {i} {suffix}", "dateOfBirth": "2001-01-02", "sex": "UNKNOWN"}) for i in range(2)]
    merge = reception.request("POST", root+"/merge-requests", {"sourceId": candidates[0]["id"], "targetId": candidates[1]["id"], "reason": "Synthetic duplicate identity"}, headers={"Idempotency-Key": "full-merge-"+suffix})
    registrar.request("POST", root+f"/merge-requests/{merge['id']}/decision", {"version": 0, "action": "APPROVE", "identityVerified": True, "reason": "Synthetic registrar verification"}, headers={"Idempotency-Key": "full-merge-ok-"+suffix})
    boundaries = []
    def rejected(name, api, method, path, body, status, **kwargs):
        try:
            api.request(method, path, body, **kwargs)
        except ApiError as error:
            assert error.status == status, f"{name}: expected {status}, got {error.status}"
            boundaries.append(name)
        else:
            raise AssertionError(f"{name}: rejected request was unexpectedly accepted")
    rejected("cashier_cannot_read_clinical_notes", cashier, "GET", visit+"/notes", None, 403)
    rejected("doctor_cannot_read_financial_report", clinician, "GET", root+"/reports/revenue?"+report_query, None, 403)
    rejected("cashier_cannot_request_refund", cashier, "POST", root+"/refunds", {"paymentId": first["id"], "amount": 1, "reason": "Forbidden role test"}, 403, headers={"Idempotency-Key":"forbidden-refund-"+suffix})
    rejected("payment_cannot_exceed_balance", cashier, "POST", root+f"/invoices/{invoice['id']}/payments", {"amount": 30001, "currency":"VND", "method":"CASH", "receivedVerified":True}, 409, headers={"Idempotency-Key":"overpay-"+suffix})
    rejected("signed_closed_record_cannot_be_overwritten", clinician, "POST", visit+"/notes", {"subjective":"Attempted overwrite", "objective":"Test", "assessment":"Test", "plan":"Test"}, 409)
    rejected("cross_facility_scope_is_denied", reception, "GET", "/api/facilities/00000000-0000-0000-0000-000000000001/patients", None, 403)
    rejected("mutation_without_csrf_is_denied", administrator, "POST", admin_root+"/accounts", {}, 403, csrf=False)
    repeated = cashier.request("POST", root+f"/invoices/{invoice['id']}/payments", {"amount":40000,"currency":"VND","method":"CASH","receivedVerified":True}, headers={"Idempotency-Key":"full-pay1-"+suffix})
    assert repeated["id"] == first["id"]
    details = cashier.request("GET", root+f"/invoices/{invoice['id']}")
    assert len(details["payments"]) == 2 and Decimal(str(details["balance"])) == 30000
    for short, denied in [("cash", "invoice.read"), ("doc", "clinical.read")]:
        assert denied in users[short][1].request("GET", "/api/auth/permissions?facilityId="+facility)
    clients = [administrator] + [client for _, client in users.values()]
    routes = {}
    for client in clients:
        for call in client.calls:
            path = re.sub(r"[0-9a-fA-F]{8}-(?:[0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}", "{}", call["path"])
            key = call["method"]+" "+path
            statuses = routes.setdefault(key, {})
            statuses[str(call["status"])] = statuses.get(str(call["status"]),0)+1
    print(json.dumps({"result":"PASS", "fullWorkflow":"booking, clinical, medication, procedure, private-file checksum, invoice, payment, independently approved refund, report, audit and merge", "rejectionChecks":boundaries, "paymentRetryReturnedOriginal":True, "invoiceBalance":30000, "netCashIncrease":110000, "requestsRecorded":sum(len(c.calls) for c in clients), "routes":routes}, indent=2))



if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--username", default="admin.demo")
    parser.add_argument("--env-file", type=Path)
    args = parser.parse_args()
    try:
        run(args.base_url, args.username, args.env_file)
    except (RuntimeError, urllib.error.URLError, AssertionError) as error:
        sys.exit("API workflow check failed: " + str(error))
