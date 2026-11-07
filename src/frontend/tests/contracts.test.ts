import test from "node:test";
import assert from "node:assert/strict";
import type { FieldSpec } from "../src/lib/schema.ts";

// Fix the configured clinic zone, independently of the test runner's device zone.
process.env.NEXT_PUBLIC_FACILITY_ZONE = "Asia/Ho_Chi_Minh";
const {
  allowed, facilityPath, fieldDefaults, getValue, localDateTime, marker,
  queryPath, resolvePath, rows, serializeFields, validateFields, zonedInstant,
} = await import("../src/lib/contracts.ts");

const facility = "b1f3859e-7409-4de4-bd12-849c03e6b54f";
const medicationId = "740df50d-b96f-47a7-8dd3-d983deff709c";

test("permissions require explicit grants; an administrator title does not confer clinical access", () => {
  assert.equal(allowed(["schedule.manage"], "clinical.read"), false);
  assert.equal(allowed(["clinical.read"], "clinical.read"), true);
  assert.equal(allowed([], undefined), true);
  assert.equal(allowed([], []), false);
});

test("alternative grants and all-required grants compose without broadening access", () => {
  const permissions = ["appointment.write", "schedule.read"];
  assert.equal(allowed(permissions, ["appointment.write", "clinical.write"]), true);
  assert.equal(allowed(permissions, ["appointment.write", "clinical.write"], ["schedule.read"]), true);
  assert.equal(allowed(permissions, "appointment.write", ["schedule.read", "queue.read"]), false);
});

test("business and administration paths remain under the selected facility", () => {
  assert.equal(facilityPath("/patients", facility), `/api/facilities/${facility}/patients`);
  assert.equal(facilityPath("/@admin/accounts", facility), `/api/admin/facilities/${facility}/accounts`);
});

test("malformed facility identifiers are rejected before making a request", () => {
  for (const invalid of ["", "not-a-facility", "../other-clinic", "-".repeat(36), "a".repeat(36)]) {
    assert.throws(() => facilityPath("/patients", invalid), Error, invalid || "empty facility");
  }
});

test("record identifiers are encoded as one path segment", () => {
  assert.equal(
    resolvePath("/encounters/{id}/notes/{note}", { id: "visit/one?scope=all", note: "ghi chú #1" }),
    "/encounters/visit%2Fone%3Fscope%3Dall/notes/ghi%20ch%C3%BA%20%231",
  );
});

test("missing path inputs stop the request instead of producing an undefined URL", () => {
  for (const id of [undefined, null, ""]) {
    assert.throws(() => resolvePath("/patients/{id}", { id }), Error);
  }
});

test("path parameters are not duplicated in query; meaningful zero and false filters survive", () => {
  const result = queryPath("/appointments/{id}/events", { id: "appointment one", page: 0, enabled: false, q: "Đỗ An & Mai", unused: null });
  const url = new URL(result, "https://medora.invalid");
  assert.equal(url.pathname, "/appointments/appointment%20one/events");
  assert.equal(url.searchParams.has("id"), false);
  assert.equal(url.searchParams.has("unused"), false);
  assert.equal(url.searchParams.get("page"), "0");
  assert.equal(url.searchParams.get("enabled"), "false");
  assert.equal(url.searchParams.get("q"), "Đỗ An & Mai");
});

test("clinic wall times become UTC instants while patient birth dates stay date-only", () => {
  const fields: FieldSpec[] = [
    { name: "from", label: "Từ", type: "datetime", required: true },
    { name: "to", label: "Đến", type: "datetime", required: true },
    { name: "dateOfBirth", label: "Ngày sinh", type: "date", required: true },
  ];
  const serialized = serializeFields(fields, { from: "2026-11-07T08:15", to: "2026-11-07T09:15", dateOfBirth: "2000-01-01" });
  assert.deepEqual(serialized, { from: "2026-11-07T01:15:00.000Z", to: "2026-11-07T02:15:00.000Z", dateOfBirth: "2000-01-01" });
  const url = new URL(queryPath("/availability", serialized), "https://medora.invalid");
  assert.equal(url.searchParams.get("from"), "2026-11-07T01:15:00.000Z");
  assert.equal(url.searchParams.get("dateOfBirth"), "2000-01-01");
  assert.equal(localDateTime(serialized.from), "2026-11-07T08:15");
});

test("impossible wall times cannot silently normalize into another day", () => {
  assert.throws(() => zonedInstant("2026-02-30T08:00"), Error);
  assert.throws(() => zonedInstant("not-a-date"), Error);
});

test("day range defaults are clinic-midnight instants and form inputs use clinic wall time", () => {
  const start = String(marker("$todayStart"));
  const end = String(marker("$todayEnd"));
  assert.equal(localDateTime(start).slice(11), "00:00");
  assert.equal(localDateTime(end).slice(11), "00:00");
  assert.equal(Date.parse(end) - Date.parse(start), 24 * 60 * 60 * 1000);
  assert.deepEqual(fieldDefaults([
    { name: "startsAt", label: "Bắt đầu", type: "datetime" },
    { name: "dateOfBirth", label: "Ngày sinh", type: "date" },
    { name: "version", label: "Phiên bản", type: "number", defaultValue: 9 },
  ], { startsAt: "2026-11-07T01:00:00Z", dateOfBirth: "2000-01-01", version: 0 }), {
    startsAt: "2026-11-07T08:00", dateOfBirth: "2000-01-01", version: 0,
  });
});

test("unassigned optional UUIDs serialize to null rather than an invalid empty UUID", () => {
  assert.deepEqual(serializeFields([
    { name: "nurseId", label: "Điều dưỡng", type: "uuid" },
    { name: "appointmentId", label: "Lịch hẹn", type: "uuid" },
  ], { nurseId: "" }), { nurseId: null, appointmentId: null });
});

const prescription: FieldSpec[] = [{
  name: "items", label: "Thuốc kê", type: "array", required: true, min: 1, max: 40,
  fields: [
    { name: "medicationId", label: "Thuốc", type: "uuid", required: true },
    { name: "dose", label: "Liều", required: true },
    { name: "route", label: "Đường dùng", required: true },
    { name: "frequency", label: "Tần suất", required: true },
    { name: "durationDays", label: "Số ngày", type: "number", required: true, min: 1, max: 365 },
    { name: "quantity", label: "Số lượng", type: "number", required: true, min: 0.001 },
    { name: "instructions", label: "Hướng dẫn", required: true },
  ],
}];

test("prescription lines serialize as typed DTO items without losing decimal quantity", () => {
  const form = { items: [{ medicationId, dose: "Liều thử nghiệm", route: "ORAL", frequency: "Theo chỉ định", durationDays: "7", quantity: "0.125", instructions: "Dữ liệu kiểm thử" }] };
  assert.deepEqual(validateFields(prescription, form), {});
  const result = serializeFields(prescription, form);
  assert.deepEqual(result, { items: [{ medicationId, dose: "Liều thử nghiệm", route: "ORAL", frequency: "Theo chỉ định", durationDays: 7, quantity: 0.125, instructions: "Dữ liệu kiểm thử" }] });
});

test("invalid prescription lines are blocked before submission", () => {
  assert.ok(validateFields(prescription, { items: [] }).items);
  assert.ok(validateFields(prescription, { items: [{ medicationId: "not-a-uuid", durationDays: 0 }] }).items);
});

test("patient identity confirmation requires an affirmative checkbox", () => {
  const fields: FieldSpec[] = [{ name: "identityVerified", label: "Đã xác minh", type: "checkbox", required: true }];
  assert.ok(validateFields(fields, { identityVerified: false }).identityVerified);
  assert.ok(validateFields(fields, {}).identityVerified);
  assert.deepEqual(validateFields(fields, { identityVerified: true }), {});
});

test("zero is a valid version, but missing, negative, fractional and unsafe versions are rejected", () => {
  const fields: FieldSpec[] = [{ name: "version", label: "Phiên bản", type: "number", required: true, min: 0 }];
  assert.deepEqual(validateFields(fields, { version: 0 }), {});
  for (const value of [undefined, "", -1, 0.5, Number.MAX_SAFE_INTEGER + 1, "NaN"]) {
    assert.ok(validateFields(fields, { version: value }).version, `version ${String(value)} must be invalid`);
  }
});

test("query and schedule date ranges must end after they start", () => {
  for (const [start, end] of [["from", "to"], ["startsAt", "endsAt"]]) {
    const fields: FieldSpec[] = [
      { name: start, label: "Bắt đầu", type: "datetime", required: true },
      { name: end, label: "Kết thúc", type: "datetime", required: true },
    ];
    assert.deepEqual(validateFields(fields, { [start]: "2026-11-07T08:00", [end]: "2026-11-07T09:00" }), {});
    for (const values of [
      { [start]: "2026-11-07T09:00", [end]: "2026-11-07T08:00" },
      { [start]: "2026-11-07T08:00", [end]: "2026-11-07T08:00" },
    ]) {
      assert.ok(Object.keys(validateFields(fields, values)).length, `${start}/${end} must have a positive duration`);
    }
  }
});

test("nested data and paginated responses preserve zero and false values", () => {
  assert.equal(getValue({ nested: { version: 0, enabled: false } }, "nested.version"), 0);
  assert.equal(getValue({ nested: { version: 0, enabled: false } }, "nested.enabled"), false);
  assert.deepEqual(rows({ items: [{ id: "one" }], page: 0, size: 20, totalElements: 1 }), [{ id: "one" }]);
  assert.deepEqual(rows({ items: [], totalElements: 0 }), []);
});
