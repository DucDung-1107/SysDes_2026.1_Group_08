import type { ActionSpec, ColumnSpec, DataRecord, FieldSpec, ModuleSpec } from "@/lib/schema";

// Contracts come from ClinicalController/ClinicalDtos, CareDtos and the care/attachment services.
// Assignment, version, lifecycle and safety decisions remain authoritative on the server.
const version: FieldSpec = { name: "version", label: "Phiên bản đang xem", type: "number", required: true, min: 0, source: "version", help: "Dùng phiên bản từ lần đọc gần nhất. Khi có xung đột, tải lại và đối chiếu trước khi gửi lại." };
const reason: FieldSpec = { name: "reason", label: "Lý do", type: "textarea", required: true, maxLength: 500 };
const correctionReason: FieldSpec = { name: "correctionReason", label: "Lý do đính chính", type: "textarea", required: true, maxLength: 500 };
const encounterQuery: FieldSpec = { name: "encounterId", label: "Mã lượt khám", type: "uuid", required: true, help: "Mã lượt khám được phân công. Quyền xem do backend kiểm tra." };
const recordId: ColumnSpec = { key: "id", label: "Mã bản ghi" };
const recordedAt: ColumnSpec = { key: "recordedAt", label: "Thời điểm ghi", format: "datetime" };
const recordedBy: ColumnSpec = { key: "recordedBy", label: "Người ghi" };
const state: ColumnSpec = { key: "status", label: "Trạng thái", format: "status" };
const nullable = (value: unknown) => value === "" || value === undefined ? null : value;
const uuid = (name: string, label: string, required = true, help?: string): FieldSpec => ({ name, label, type: "uuid", required, help });
const text = (name: string, label: string, maxLength: number, required = true): FieldSpec => ({ name, label, maxLength, required });
const textarea = (name: string, label: string, maxLength: number, required = true): FieldSpec => ({ name, label, type: "textarea", maxLength, required });
const select = (name: string, label: string, options: {value:string;label:string}[], defaultValue?: string): FieldSpec => ({ name, label, type: "select", required: true, options, defaultValue });

const historyKinds = [
  { value: "ALLERGY", label: "Dị ứng" }, { value: "HISTORY", label: "Tiền sử" }, { value: "MEDICATION", label: "Thuốc đang sử dụng" },
];
const historyFields: FieldSpec[] = [select("kind", "Loại thông tin", historyKinds, "HISTORY"), textarea("content", "Nội dung", 4000)];
const vitalFields: FieldSpec[] = [
  { name: "temperatureC", label: "Nhiệt độ (°C)", type: "number", min: 25, max: 45 },
  { name: "pulseBpm", label: "Mạch (lần/phút)", type: "number", min: 20, max: 300, pattern: "[0-9]+" },
  { name: "systolicMmhg", label: "Huyết áp tâm thu (mmHg)", type: "number", min: 30, max: 300, pattern: "[0-9]+" },
  { name: "diastolicMmhg", label: "Huyết áp tâm trương (mmHg)", type: "number", min: 10, max: 200, pattern: "[0-9]+" },
  { name: "respiratoryRate", label: "Nhịp thở (lần/phút)", type: "number", min: 1, max: 100, pattern: "[0-9]+" },
  { name: "spo2Percent", label: "SpO₂ (%)", type: "number", min: 0, max: 100 },
  { name: "weightKg", label: "Cân nặng (kg)", type: "number", min: 0, max: 9999, help: "Phải lớn hơn 0 nếu nhập." },
  { name: "heightCm", label: "Chiều cao (cm)", type: "number", min: 0, max: 999, help: "Phải lớn hơn 0 nếu nhập." },
  textarea("screening", "Thông tin sàng lọc", 4000, false),
  { name: "measuredAt", label: "Thời điểm đo", type: "datetime", required: true, help: "Không ở tương lai; không sớm hơn thời điểm tạo lượt khám quá 5 phút." },
  { name: "verified", label: "Đã xác minh thông tin đo", type: "checkbox", defaultValue: false },
];
const soapFields: FieldSpec[] = [
  textarea("subjective", "S · Thông tin chủ quan", 10000, false), textarea("objective", "O · Thông tin khách quan", 10000, false),
  textarea("assessment", "A · Nhận định", 10000, false), textarea("plan", "P · Kế hoạch", 10000, false),
];
const soapBody = (values: DataRecord) => ({
  subjective: String(values.subjective ?? ""), objective: String(values.objective ?? ""),
  assessment: String(values.assessment ?? ""), plan: String(values.plan ?? ""), version: nullable(values.version),
});
const vitalColumns: ColumnSpec[] = [
  recordId, { key: "temperatureC", label: "°C" }, { key: "pulseBpm", label: "Mạch /phút" },
  { key: "systolicMmhg", label: "Tâm thu mmHg" }, { key: "diastolicMmhg", label: "Tâm trương mmHg" },
  { key: "respiratoryRate", label: "Nhịp thở /phút" }, { key: "spo2Percent", label: "SpO₂ %" },
  { key: "weightKg", label: "Cân nặng kg" }, { key: "heightCm", label: "Chiều cao cm" },
  { key: "measuredAt", label: "Thời điểm đo", format: "datetime" }, { key: "verified", label: "Đã xác minh", format: "boolean" },
  { key: "screening", label: "Sàng lọc" }, { key: "superseded", label: "Đã có đính chính", format: "boolean" },
  { key: "correctionReason", label: "Lý do đính chính" }, recordedBy, recordedAt,
];
const noteColumns: ColumnSpec[] = [
  recordId, { key: "sequenceNumber", label: "Lần ghi" }, state,
  { key: "subjective", label: "S · Chủ quan" }, { key: "objective", label: "O · Khách quan" },
  { key: "assessment", label: "A · Nhận định" }, { key: "plan", label: "P · Kế hoạch" },
  { key: "amendmentReason", label: "Lý do bổ sung" }, { key: "predecessorId", label: "Bản ghi trước" },
  { key: "signedBy", label: "Người ký" }, { key: "signedAt", label: "Thời điểm ký", format: "datetime" },
  { key: "version", label: "Phiên bản" },
];
const diagnosisColumns: ColumnSpec[] = [{ key: "code", label: "Mã chẩn đoán" }, { key: "symptoms", label: "Triệu chứng" }, recordedAt, recordedBy];
const noteActions: ActionSpec[] = [
  { id: "note-save", label: "Lưu bản nháp", scope: "record", method: "PUT", path: "/encounters/{encounterId}/notes/{id}", permission: "clinical.write", when: { field: "status", values: ["DRAFT", "AMENDMENT_DRAFT"] }, fields: [...soapFields, version], body: soapBody },
  { id: "note-sign", label: "Ký ghi chú", scope: "record", path: "/encounters/{encounterId}/notes/{id}/sign", permission: "clinical.write", when: { field: "status", values: ["DRAFT", "AMENDMENT_DRAFT"] }, fields: [version], danger: true, confirm: "Ký ghi chú đang chọn? Bản đã ký được giữ nguyên; sửa đổi tiếp theo cần tạo bản bổ sung có lý do.", help: "Cả bốn phần SOAP phải có nội dung và lượt khám phải có chẩn đoán. Chỉ ký sau khi đã đọc lại nội dung đang lưu trên máy chủ." },
  { id: "note-amend", label: "Tạo bản bổ sung", scope: "record", path: "/encounters/{encounterId}/notes/{id}/amendments", permission: "clinical.write", when: { field: "status", values: ["SIGNED", "AMENDED_SIGNED_VERSION"] }, fields: [reason, version], help: "Chỉ bản đã ký mới nhất được bổ sung. Backend giữ nguyên bản đã ký và tạo bản nháp kế tiếp." },
  { id: "note-abandon", label: "Bỏ bản nháp bổ sung", scope: "record", path: "/encounters/{encounterId}/notes/{id}/abandon", permission: "clinical.write", when: { field: "status", values: ["AMENDMENT_DRAFT"] }, fields: [version], danger: true, confirm: "Bỏ bản nháp bổ sung này? Nội dung nháp sẽ bị loại bỏ; bản đã ký trước đó vẫn được giữ." },
];

const rxItemFields: FieldSpec[] = [
  { ...uuid("medicationId", "Thuốc"), lookup: { path: "/medications", valueKey: "id", labelKey: "name" }, help: "Chọn thuốc đang được bật trong danh mục của cơ sở." },
  text("dose", "Liều dùng", 160), text("route", "Đường dùng", 80), text("frequency", "Tần suất", 160),
  { name: "durationDays", label: "Số ngày", type: "number", required: true, min: 1, max: 365, pattern: "[0-9]+" },
  { name: "quantity", label: "Số lượng", type: "number", required: true, min: 0.001, max: 999999999.999, help: "Tối đa 3 chữ số thập phân. Đơn vị theo thuốc trong danh mục." },
  textarea("instructions", "Hướng dẫn sử dụng", 2000),
];
const rxItems: FieldSpec = { name: "items", label: "Các thuốc trong đơn", type: "array", required: true, min: 1, max: 40, fields: rxItemFields, help: "Một thuốc chỉ xuất hiện một lần trong đơn. Việc sửa nháp thay toàn bộ danh sách thuốc." };
const prescriptionColumns: ColumnSpec[] = [
  recordId, state, { key: "encounterId", label: "Lượt khám", module: "encounters" },
  { key: "createdAt", label: "Tạo lúc", format: "datetime" }, { key: "issuedAt", label: "Phát hành lúc", format: "datetime" },
  { key: "pharmacistId", label: "Dược sĩ được phân công" }, { key: "version", label: "Phiên bản" },
];
const rxItemColumns: ColumnSpec[] = [
  recordId, { key: "medicationName", label: "Thuốc" }, { key: "ingredientCode", label: "Mã hoạt chất" }, { key: "unit", label: "Đơn vị" },
  { key: "dose", label: "Liều" }, { key: "route", label: "Đường dùng" }, { key: "frequency", label: "Tần suất" },
  { key: "durationDays", label: "Số ngày" }, { key: "quantity", label: "Số lượng kê" }, { key: "instructions", label: "Hướng dẫn" },
];
const prescriptionActions: ActionSpec[] = [
  { id: "rx-save", label: "Lưu nháp đơn thuốc", scope: "record", method: "PUT", path: "/prescriptions/{id}", permission: "clinical.write", when: { field: "status", values: ["DRAFT"] }, fields: [rxItems, version] },
  { id: "rx-issue", label: "Phát hành đơn thuốc", scope: "record", path: "/prescriptions/{id}/issue", permission: "clinical.write", when: { field: "status", values: ["DRAFT"] }, idempotent: true, danger: true, confirm: "Phát hành đơn thuốc đang chọn sau khi đã đọc lại từng thuốc và toàn bộ cảnh báo an toàn?", fields: [
    version, uuid("pharmacistId", "Mã dược sĩ được phân công", true, "Cần tài khoản PHARMACIST đang hoạt động tại cơ sở. Danh bạ nhân sự chưa có API tra cứu."),
    { name: "acknowledgedWarnings", label: "Các mã cảnh báo đã đọc và xác nhận", type: "string-list", defaultValue: [], max: 100, help: "Xem mục Kiểm tra an toàn trước. Nhập chính xác toàn bộ mã cảnh báo, phân cách bằng dấu phẩy; để trống khi máy chủ trả về không có cảnh báo. Không tự suy luận rằng thuốc an toàn." },
    textarea("overrideReason", "Lý do ghi đè cảnh báo cho phép ghi đè", 500, false),
  ], help: "Chỉ bác sĩ được phân công. Phải có hồ sơ dị ứng xác định và nguồn chính sách được bật. Cảnh báo BLOCK không cho ghi đè sẽ chặn phát hành; loại cho ghi đè còn cần prescription.override và lý do." },
  { id: "rx-amend", label: "Tạo đơn bổ sung", scope: "record", path: "/prescriptions/{id}/amendments", permission: "clinical.write", when: { field: "status", values: ["ISSUED"] }, fields: [version, reason], help: "Tạo nháp mới từ đơn đã phát hành. Đơn trước chỉ được thay thế khi đơn bổ sung được phát hành thành công." },
  { id: "rx-cancel", label: "Hủy đơn đã phát hành", scope: "record", path: "/prescriptions/{id}/cancel", permission: "clinical.write", when: { field: "status", values: ["ISSUED"] }, fields: [version, reason], danger: true, confirm: "Hủy đơn thuốc đang chọn? Đơn và lý do hủy vẫn được giữ trong lịch sử." },
  { id: "medicine-event", label: "Ghi nhận cấp / dùng thuốc", scope: "record", path: "/prescriptions/{id}/medicine-events", permission: "prescription.dispense", when: { field: "status", values: ["ISSUED"] }, idempotent: true, fields: [
    uuid("itemId", "Mã dòng thuốc", true, "Dùng mã dòng thuốc của đơn đang chọn trong bảng Các thuốc, không dùng mã thuốc danh mục."),
    select("kind", "Loại sự kiện", [{ value: "DISPENSED", label: "Đã cấp thuốc" }, { value: "ADMINISTERED", label: "Đã dùng thuốc" }], "DISPENSED"),
    { name: "quantity", label: "Số lượng thực tế", type: "number", required: true, min: 0.001, max: 999999999.999 },
    { name: "performedAt", label: "Thời điểm thực hiện", type: "datetime", required: true, help: "Không ở tương lai và không trước thời điểm phát hành đơn." },
  ], confirm: "Ghi nhận sự kiện cấp hoặc dùng thuốc thực tế? Kiểm tra đúng đơn, dòng thuốc, số lượng và thời điểm.", help: "Chỉ dược sĩ được phân công. Tổng lượng cùng loại sự kiện không vượt số lượng kê. Đây là sự kiện riêng, không tự đổi trạng thái đơn." },
  { id: "rx-export", label: "Tải dữ liệu đơn thuốc", scope: "record", method: "DOWNLOAD", path: "/prescriptions/{id}/export", permission: ["clinical.read", "prescription.read"], help: "Máy chủ trả về dữ liệu JSON của đơn thuốc được phép xem; đây không phải đơn PDF hoặc chữ ký số." },
];
const prescriptionViews = [
  { id: "rx-items", label: "Các thuốc", dataKey: "items", columns: rxItemColumns },
  { id: "rx-safety", label: "Kiểm tra an toàn", path: "/prescriptions/{id}/safety", permission: "clinical.read", fields: [{ key: "source", label: "Nguồn dữ liệu" }, { key: "dataVersion", label: "Phiên bản nguồn" }] as ColumnSpec[], columns: [
    { key: "id", label: "Mã cảnh báo" }, { key: "severity", label: "Mức cảnh báo", format: "status" },
    { key: "overridable", label: "Cho phép ghi đè", format: "boolean" }, { key: "message", label: "Nội dung" }, { key: "source", label: "Nguồn" },
  ] as ColumnSpec[], dataKey: "warnings" },
];
const orderColumns: ColumnSpec[] = [
  recordId, { key: "serviceName", label: "Dịch vụ" }, { key: "kind", label: "Loại", format: "status" }, state,
  { key: "priority", label: "Ưu tiên", format: "status" }, { key: "encounterId", label: "Lượt khám", module: "encounters" },
  { key: "staffId", label: "Nhân sự thực hiện" }, { key: "startsAt", label: "Bắt đầu", format: "datetime" }, { key: "version", label: "Phiên bản" },
];
const orderActions: ActionSpec[] = [
  { id: "order-allocate", label: "Phân lịch / đổi lịch", scope: "record", method: "PUT", path: "/orders/{id}/allocation", permission: "clinical.write", when: { field: "status", values: ["CREATED", "PENDING_ALLOCATION", "SCHEDULED", "DELAYED"] }, fields: [
    uuid("staffId", "Mã kỹ thuật viên", true, "Nhân sự TECHNICIAN đang hoạt động, có ca làm và năng lực dịch vụ phù hợp."),
    { ...uuid("roomId", "Phòng thực hiện"), lookup: { path: "/rooms", valueKey: "id", labelKey: "name", permission: "schedule.read" } },
    { name: "startsAt", label: "Thời điểm bắt đầu", type: "datetime", required: true }, version, textarea("reason", "Lý do phân lịch / đổi lịch", 2000),
  ], help: "Máy chủ kiểm tra phòng, thiết bị, ca làm, năng lực và xung đột tài nguyên. Không xem lịch trống là giữ chỗ thành công." },
  { id: "order-start", label: "Bắt đầu thực hiện", scope: "record", path: "/orders/{id}/actions", permission: "procedure.execute", when: { field: "status", values: ["SCHEDULED"] }, fields: [version], payload: { action: "START" }, help: "Chỉ nhân sự được phân công, trong khoảng thời gian đã phân lịch." },
  { id: "order-delay", label: "Hoãn thực hiện", scope: "record", path: "/orders/{id}/actions", permission: "procedure.execute", when: { field: "status", values: ["SCHEDULED"] }, fields: [version, textarea("reason", "Lý do hoãn", 2000)], payload: { action: "DELAY" } },
  { id: "order-complete", label: "Ghi kết quả và hoàn tất", scope: "record", path: "/orders/{id}/actions", permission: "procedure.execute", when: { field: "status", values: ["IN_PROGRESS"] }, fields: [version, textarea("result", "Kết quả thực hiện", 10000), textarea("reason", "Ghi chú thực hiện", 2000, false)], payload: { action: "COMPLETE" }, confirm: "Hoàn tất dịch vụ và ghi kết quả đang nhập? Kiểm tra đúng chỉ định và nội dung trước khi xác nhận." },
  { id: "order-not-performed", label: "Ghi nhận không thực hiện", scope: "record", path: "/orders/{id}/actions", permission: "procedure.execute", when: { field: "status", values: ["CREATED", "PENDING_ALLOCATION", "SCHEDULED", "DELAYED", "IN_PROGRESS"] }, fields: [version, textarea("reason", "Lý do không thực hiện", 2000)], payload: { action: "NOT_PERFORMED" }, confirm: "Ghi nhận dịch vụ không thực hiện và giải phóng lịch tài nguyên?" },
  { id: "order-cancel", label: "Hủy chỉ định", scope: "record", path: "/orders/{id}/actions", permission: "clinical.write", when: { field: "status", values: ["CREATED", "PENDING_ALLOCATION", "SCHEDULED", "DELAYED", "IN_PROGRESS"] }, fields: [version, textarea("reason", "Lý do hủy chỉ định", 2000)], payload: { action: "CANCEL" }, danger: true, confirm: "Hủy chỉ định đang chọn và giải phóng lịch tài nguyên? Lịch sử chỉ định được giữ lại." },
  { id: "order-review", label: "Xác nhận đã xem kết quả", scope: "record", path: "/orders/{id}/review", permission: "clinical.write", when: { field: "status", values: ["COMPLETED", "CANCELED", "NOT_PERFORMED"] }, fields: [version, reason], confirm: "Xác nhận đã đọc kết quả hoặc lý do không thực hiện của chỉ định này?", help: "Chỉ bác sĩ được phân công. Lượt khám trở về đang khám sau khi toàn bộ chỉ định đã giải quyết và được xem lại." },
];
const orderDetail: ColumnSpec[] = [
  ...orderColumns, { key: "reason", label: "Lý do chỉ định" }, { key: "preparation", label: "Chuẩn bị" },
  { key: "required", label: "Dịch vụ bắt buộc", format: "boolean" }, { key: "roomId", label: "Phòng" },
  { key: "endsAt", label: "Kết thúc dự kiến", format: "datetime" }, { key: "result", label: "Kết quả" },
  { key: "resolutionReason", label: "Lý do xử lý" }, { key: "reviewedBy", label: "Bác sĩ xem lại" },
  { key: "reviewedAt", label: "Thời điểm xem lại", format: "datetime" }, { key: "completedAt", label: "Hoàn tất lúc", format: "datetime" },
];
const medicationFields: FieldSpec[] = [
  { ...text("code", "Mã thuốc", 40), pattern: "[A-Z0-9._-]{1,40}" }, text("name", "Tên thuốc", 160),
  { ...text("ingredientCode", "Mã hoạt chất", 80), pattern: "[A-Z0-9._-]{1,80}" }, text("unit", "Đơn vị", 40),
  { name: "enabled", label: "Cho phép sử dụng", type: "checkbox", defaultValue: true },
];
const ruleFields: FieldSpec[] = [
  { ...uuid("medicationId", "Thuốc chính"), lookup: { path: "/medications", valueKey: "id", labelKey: "name" } },
  select("kind", "Loại quy tắc", [{ value: "INTERACTION", label: "Tương tác thuốc" }, { value: "CONTRAINDICATION", label: "Chống chỉ định" }], "INTERACTION"),
  { ...uuid("otherMedicationId", "Thuốc tương tác", false), lookup: { path: "/medications", valueKey: "id", labelKey: "name" }, help: "Bắt buộc với INTERACTION; phải khác thuốc chính." },
  { ...text("conditionCode", "Mã tình trạng chống chỉ định", 80, false), pattern: "[A-Z0-9._-]{1,80}", help: "Bắt buộc với CONTRAINDICATION; mã phải khớp hồ sơ thuốc có cấu trúc." },
  select("severity", "Mức cảnh báo", [{ value: "WARN", label: "Cảnh báo" }, { value: "BLOCK", label: "Chặn phát hành" }], "WARN"),
  { name: "overridable", label: "Cho phép ghi đè khi đủ quyền và có lý do", type: "checkbox", defaultValue: false },
  textarea("message", "Thông điệp cảnh báo", 500), textarea("source", "Nguồn quy tắc đã được phê duyệt", 500),
  { name: "enabled", label: "Bật quy tắc", type: "checkbox", defaultValue: true },
];
const ruleBody = (values: DataRecord) => ({ ...values, otherMedicationId: nullable(values.otherMedicationId), conditionCode: nullable(values.conditionCode) });
const attachmentColumns: ColumnSpec[] = [
  recordId, { key: "filename", label: "Tên tệp" }, { key: "mediaType", label: "Định dạng" }, { key: "byteSize", label: "Dung lượng (byte)" },
  { key: "orderId", label: "Chỉ định liên quan" }, { key: "uploadedBy", label: "Người tải lên" },
  { key: "uploadedAt", label: "Tải lên lúc", format: "datetime" }, { key: "version", label: "Phiên bản" },
];

export const clinicalModules: ModuleSpec[] = [
  {
    id: "encounters", title: "Lượt khám được phân công", group: "Lâm sàng", description: "Hàng đợi của bác sĩ / điều dưỡng, sinh hiệu, diễn biến và ghi chú khám.",
    allPermissions: ["queue.read", "clinical.read"], endpoint: "/queue", detailPath: "/encounters/{id}", paginated: true,
    columns: [
      { key: "queueNumber", label: "Số thứ tự" }, { key: "patientName", label: "Bệnh nhân" }, { key: "patientCode", label: "Mã bệnh nhân" }, state,
      { key: "createdAt", label: "Tiếp nhận lúc", format: "datetime" }, { key: "doctorId", label: "Bác sĩ" }, { key: "nurseId", label: "Điều dưỡng" }, recordId,
    ],
    detailFields: [recordId, { key: "patientId", label: "Mã hồ sơ bệnh nhân" }, state, { key: "doctorId", label: "Bác sĩ được phân công" }, { key: "nurseId", label: "Điều dưỡng được phân công" }, { key: "version", label: "Phiên bản" }, { key: "createdAt", label: "Tiếp nhận lúc", format: "datetime" }, { key: "consultationServiceId", label: "Dịch vụ khám" }, { key: "summary", label: "Tóm tắt kết thúc" }, { key: "instructions", label: "Hướng dẫn" }, { key: "followUp", label: "Tái khám" }],
    help: "Chỉ lượt khám được phân công. Hàng đợi chứa tên / mã bệnh nhân thật từ API; chi tiết vẫn kiểm tra quyền. Bản đã ký giữ nguyên; kết thúc lâm sàng và thanh toán là hai trạng thái riêng.",
    actions: [
      { id: "encounter-start", label: "Bắt đầu khám", scope: "record", path: "/encounters/{id}/start", permission: "clinical.write", when: { field: "status", values: ["WAITING"] }, fields: [version] },
      { id: "history-add", label: "Ghi tiền sử / dị ứng", scope: "record", path: "/encounters/{id}/history", permission: "history.write", fields: historyFields, help: "Đây là ghi nhận diễn giải. Hồ sơ dị ứng có cấu trúc để kiểm tra thuốc nằm ở Hồ sơ an toàn thuốc." },
      { id: "vitals-add", label: "Ghi sinh hiệu", scope: "record", path: "/encounters/{id}/vitals", permission: "screening.write", fields: vitalFields, help: "Cần ít nhất một số đo. Huyết áp tâm thu phải lớn hơn tâm trương nếu nhập cả hai. Màu sắc không đánh giá số đo có an toàn hay không." },
      { id: "diagnosis-add", label: "Ghi chẩn đoán", scope: "record", path: "/encounters/{id}/diagnoses", permission: "clinical.write", when: { field: "status", values: ["IN_CONSULTATION"] }, fields: [
        { name: "code", label: "Chẩn đoán trong danh mục", required: true, maxLength: 32, lookup: { path: "/diagnosis-catalog", valueKey: "code", labelKey: "name" } }, textarea("symptoms", "Triệu chứng", 4000),
      ] },
      { id: "note-create", label: "Tạo ghi chú SOAP", scope: "record", path: "/encounters/{id}/notes", permission: "clinical.write", when: { field: "status", values: ["IN_CONSULTATION"] }, fields: soapFields, body: soapBody, help: "Có thể lưu nháp chưa đủ nội dung. Nếu đã có nháp, dùng Lưu bản nháp; nếu đã có bản ký, tạo bản bổ sung từ bản ký mới nhất." },
      { id: "consultation-service", label: "Chọn dịch vụ khám cho walk-in", scope: "record", method: "PUT", path: "/encounters/{id}/consultation-service", permission: "clinical.write", fields: [
        { ...uuid("serviceId", "Dịch vụ khám"), lookup: { path: "/services", valueKey: "id", labelKey: "name", permission: "schedule.read" } },
      ], help: "Chỉ bác sĩ được phân công. Dịch vụ của lượt có lịch hẹn đã cố định theo lịch hẹn." },
      { id: "discharge-request", label: "Gửi yêu cầu duyệt ra về", scope: "record", path: "/encounters/{id}/discharge-requests", permission: "clinical.write", when: { field: "status", values: ["IN_CONSULTATION", "AWAITING_SERVICE", "AWAITING_CLOSURE"] }, fields: [textarea("reason", "Lý do đề nghị ra về", 2000)], confirm: "Gửi yêu cầu ra về cho người có quyền phê duyệt? Thao tác này không tự kết thúc lượt khám.", help: "Chỉ bác sĩ được phân công. Khi chính sách yêu cầu duyệt ra về, gửi yêu cầu trước bước kiểm tra kết thúc; cung cấp mã tham chiếu và phiên bản trong kết quả cho quản lý tại màn Duyệt ra về. Người yêu cầu không được tự phê duyệt." },
      { id: "encounter-submit", label: "Gửi kiểm tra kết thúc", scope: "record", path: "/encounters/{id}/submit-for-closure", permission: "clinical.write", when: { field: "status", values: ["IN_CONSULTATION"] }, fields: [version], confirm: "Gửi lượt khám để kết thúc sau khi đã kiểm tra ghi chú đã ký, chẩn đoán, sinh hiệu xác minh và các công việc liên quan?", help: "Backend kiểm tra không còn nháp, có ghi chú ký / chẩn đoán / sinh hiệu xác minh, không còn đơn thuốc nháp hoặc chỉ định chưa giải quyết / chưa xem lại; áp dụng duyệt ra về nếu chính sách cơ sở yêu cầu." },
      { id: "encounter-return", label: "Trả lại để bổ sung", scope: "record", path: "/encounters/{id}/return-for-correction", permission: "clinical.write", when: { field: "status", values: ["AWAITING_CLOSURE"] }, fields: [version] },
      { id: "encounter-complete", label: "Kết thúc lượt khám", scope: "record", path: "/encounters/{id}/complete", permission: "clinical.write", when: { field: "status", values: ["AWAITING_CLOSURE"] }, fields: [version, textarea("summary", "Tóm tắt kết thúc", 10000), textarea("instructions", "Hướng dẫn sau khám", 10000), textarea("followUp", "Hướng dẫn tái khám", 2000)], danger: true, confirm: "Kết thúc lượt khám này? Kiểm tra đúng bệnh nhân, tóm tắt, hướng dẫn và tái khám. Trạng thái tài chính không bị suy luận từ việc kết thúc khám." },
    ],
    views: [
      { id: "encounter-history", label: "Tiền sử / dị ứng", path: "/encounters/{id}/history", columns: [recordId, { key: "kind", label: "Loại", format: "status" }, { key: "content", label: "Nội dung" }, { key: "superseded", label: "Đã có đính chính", format: "boolean" }, { key: "supersedesId", label: "Thay bản ghi" }, { key: "correctionReason", label: "Lý do" }, recordedAt, recordedBy], actions: [
        { id: "history-correct", label: "Đính chính tiền sử", scope: "record", path: "/encounters/{contextEncounterId}/history", permission: "history.write", fields: [...historyFields, { ...uuid("supersedesId", "Bản ghi được đính chính"), source: "id" }, correctionReason], help: "Bản cũ được giữ nguyên; tạo bản kế tiếp cùng loại thông tin và cùng bệnh nhân trong ngữ cảnh lượt hiện tại." },
      ] },
      { id: "encounter-vitals", label: "Sinh hiệu", path: "/encounters/{id}/vitals", columns: vitalColumns, actions: [
        { id: "vitals-correct", label: "Đính chính sinh hiệu", scope: "record", path: "/encounters/{encounterId}/vitals", permission: "screening.write", fields: [...vitalFields, { ...uuid("supersedesId", "Bản đo được đính chính"), source: "id" }, correctionReason] },
      ] },
      { id: "encounter-diagnoses", label: "Chẩn đoán", path: "/encounters/{id}/diagnoses", columns: diagnosisColumns },
      { id: "encounter-notes", label: "Ghi chú SOAP", path: "/encounters/{id}/notes", columns: noteColumns, actions: noteActions },
      { id: "encounter-timeline", label: "Lịch sử lượt khám", path: "/encounters/{id}/record", columns: [recordId, { key: "createdAt", label: "Tiếp nhận lúc", format: "datetime" }, state, { key: "completedAt", label: "Kết thúc lúc", format: "datetime" }, { key: "summary", label: "Tóm tắt" }, { key: "instructions", label: "Hướng dẫn" }, { key: "followUp", label: "Tái khám" }] },
      { id: "encounter-rx", label: "Đơn thuốc", path: "/encounters/{id}/prescriptions", columns: prescriptionColumns },
      { id: "encounter-orders", label: "Chỉ định", path: "/encounters/{id}/orders", columns: orderColumns },
    ],
  },
  {
    id: "longitudinal", title: "Chi tiết lượt khám trước", group: "Lâm sàng", permission: "clinical.read", single: true,
    description: "Xem lượt trước của cùng bệnh nhân trong ngữ cảnh lượt khám đang được phân công.", endpoint: "/encounters/{encounterId}/record/{visitId}",
    queryFields: [encounterQuery, uuid("visitId", "Mã lượt khám trước", true, "Lấy từ bảng Lịch sử lượt khám; phải thuộc cùng cây hồ sơ bệnh nhân.")], columns: [],
    detailFields: [{ key: "encounter.id", label: "Lượt khám trước" }, { key: "encounter.status", label: "Trạng thái", format: "status" }, { key: "encounter.createdAt", label: "Tiếp nhận", format: "datetime" }, { key: "encounter.completedAt", label: "Kết thúc", format: "datetime" }, { key: "encounter.summary", label: "Tóm tắt" }, { key: "encounter.instructions", label: "Hướng dẫn" }, { key: "encounter.followUp", label: "Tái khám" }],
    views: [{ id: "past-notes", label: "Ghi chú đã lưu", dataKey: "notes", columns: noteColumns }, { id: "past-vitals", label: "Sinh hiệu", dataKey: "vitals", columns: vitalColumns }, { id: "past-diagnoses", label: "Chẩn đoán", dataKey: "diagnoses", columns: diagnosisColumns }],
    help: "Chỉ đọc; cần lượt hiện tại chưa hoàn tất và được phân công. Không dùng quyền quản trị để thay thế quyền hồ sơ lâm sàng.",
  },
  {
    id: "medication-profile", title: "Hồ sơ an toàn thuốc", group: "Lâm sàng", permission: ["clinical.read", "history.write"], single: true,
    description: "Dị ứng, tình trạng chống chỉ định và thuốc hiện tại có cấu trúc.", endpoint: "/encounters/{encounterId}/medication-profile", queryFields: [encounterQuery], columns: [],
    detailFields: [{ key: "patientId", label: "Bệnh nhân" }, { key: "allergyStatus", label: "Tình trạng dị ứng", format: "status" }, { key: "allergyCodes", label: "Mã hoạt chất dị ứng", format: "list" }, { key: "conditionCodes", label: "Mã tình trạng", format: "list" }, { key: "currentMedicationIds", label: "Mã thuốc đang dùng", format: "list" }, recordedBy, recordedAt, { key: "version", label: "Phiên bản" }],
    actions: [{ id: "profile-save", label: "Ghi hồ sơ an toàn thuốc", scope: "collection", method: "PUT", path: "/encounters/{encounterId}/medication-profile", permission: "history.write", fields: [
      encounterQuery, select("allergyStatus", "Tình trạng dị ứng", [{ value: "UNKNOWN", label: "Chưa xác định" }, { value: "KNOWN_NONE", label: "Đã xác minh không ghi nhận dị ứng" }, { value: "RECORDED", label: "Có ghi nhận dị ứng" }]),
      { name: "allergyCodes", label: "Các mã hoạt chất dị ứng", type: "string-list", max: 40, defaultValue: [], help: "KNOWN_NONE yêu cầu danh sách rỗng; RECORDED yêu cầu ít nhất một mã. Mã chữ hoa A–Z, số, dấu chấm, gạch dưới hoặc gạch ngang." },
      { name: "conditionCodes", label: "Các mã tình trạng chống chỉ định", type: "string-list", max: 40, defaultValue: [] },
      { name: "currentMedicationIds", label: "Các mã thuốc đang sử dụng", type: "string-list", max: 40, defaultValue: [], help: "Danh sách UUID thuốc thực tế trong danh mục; phân cách bằng dấu phẩy." },
      { ...version, required: false, help: "Bỏ trống khi chưa có hồ sơ. Khi sửa hồ sơ hiện có, dùng phiên bản vừa đọc." },
    ], body: (values) => ({ allergyStatus: values.allergyStatus, allergyCodes: values.allergyCodes ?? [], conditionCodes: values.conditionCodes ?? [], currentMedicationIds: values.currentMedicationIds ?? [], version: nullable(values.version) }) }],
    help: "404 có thể là chưa có hồ sơ; không đồng nghĩa không dị ứng. Trạng thái UNKNOWN chặn kiểm tra / phát hành đơn. Thông tin này hỗ trợ quy tắc đã cấu hình, không phải kết luận điều trị.",
  },
  {
    id: "prescriptions", title: "Đơn thuốc theo lượt khám", group: "Thuốc & dịch vụ", permission: "clinical.read",
    description: "Kê nháp, kiểm tra cảnh báo và phát hành cho dược sĩ được phân công.", endpoint: "/encounters/{encounterId}/prescriptions", detailPath: "/prescriptions/{id}", queryFields: [encounterQuery], columns: prescriptionColumns,
    detailFields: [...prescriptionColumns, { key: "predecessorId", label: "Đơn trước" }, { key: "reason", label: "Lý do bổ sung" }, { key: "safetySource", label: "Nguồn kiểm tra khi phát hành" }, { key: "safetyVersion", label: "Phiên bản nguồn" }, { key: "cancelReason", label: "Lý do hủy" }],
    actions: [{ id: "rx-create", label: "Tạo nháp đơn thuốc", scope: "collection", path: "/encounters/{encounterId}/prescriptions", permission: "clinical.write", fields: [encounterQuery, rxItems], body: (values) => ({ items: values.items }), help: "Chỉ bác sĩ được phân công; lượt đang khám hoặc đang chờ dịch vụ." }, ...prescriptionActions], views: prescriptionViews,
    help: "Mở mục Kiểm tra an toàn trước khi phát hành; luôn đọc nguồn và phiên bản dữ liệu. Không có cảnh báo không phải là bảo đảm an toàn lâm sàng.",
  },
  {
    id: "pharmacy", title: "Công việc dược được phân công", group: "Thuốc & dịch vụ", permission: "prescription.read", endpoint: "/pharmacy-queue", detailPath: "/prescriptions/{id}", paginated: true,
    description: "Các đơn được giao trực tiếp cho tài khoản hiện tại; ghi nhận cấp và dùng thuốc riêng biệt.", columns: prescriptionColumns,
    detailFields: [...prescriptionColumns, { key: "safetySource", label: "Nguồn kiểm tra khi phát hành" }, { key: "safetyVersion", label: "Phiên bản nguồn" }], actions: prescriptionActions, views: prescriptionViews,
    help: "Chỉ đơn được phân công cho dược sĩ hiện tại. API hiện chưa cung cấp danh sách sự kiện cấp / dùng thuốc; kết quả ghi nhận được trả về sau khi máy chủ xác nhận.",
  },
  {
    id: "procedures", title: "Chỉ định theo lượt khám", group: "Thuốc & dịch vụ", permission: "clinical.read", description: "Chỉ định dịch vụ, phân lịch nguồn lực và xác nhận xem kết quả.",
    endpoint: "/encounters/{encounterId}/orders", detailPath: "/orders/{id}", queryFields: [encounterQuery], columns: orderColumns, detailFields: orderDetail,
    actions: [{ id: "order-create", label: "Tạo chỉ định", scope: "collection", path: "/encounters/{encounterId}/orders", permission: "clinical.write", fields: [
      encounterQuery, { ...uuid("serviceId", "Dịch vụ"), lookup: { path: "/services", valueKey: "id", labelKey: "name", permission: "schedule.read" } },
      textarea("reason", "Lý do chỉ định", 2000), select("priority", "Mức ưu tiên", [{ value: "ROUTINE", label: "Thông thường" }, { value: "URGENT", label: "Ưu tiên" }], "ROUTINE"),
      textarea("preparation", "Hướng dẫn chuẩn bị", 2000, false), { name: "required", label: "Bắt buộc trước khi kết thúc lượt khám", type: "checkbox", defaultValue: true },
      select("kind", "Loại chỉ định", [{ value: "PROCEDURE", label: "Dịch vụ / thủ thuật" }, { value: "LAB", label: "Xét nghiệm" }, { value: "IMAGING", label: "Chẩn đoán hình ảnh" }], "PROCEDURE"),
    ], body: (values) => ({ serviceId: values.serviceId, reason: values.reason, priority: values.priority, preparation: String(values.preparation ?? ""), required: values.required, kind: values.kind }), help: "Chỉ bác sĩ được phân công. Tạo chỉ định đưa lượt sang chờ dịch vụ. Xét nghiệm / hình ảnh là ghi nhận nội bộ; không có kết nối máy hoặc hệ thống ngoài." }, ...orderActions],
    views: [{ id: "order-events", label: "Lịch sử chỉ định", dataKey: "events", columns: [{ key: "action", label: "Thao tác", format: "status" }, { key: "reason", label: "Lý do" }, { key: "actorId", label: "Người thực hiện" }, { key: "occurredAt", label: "Thời điểm", format: "datetime" }] }],
  },
  {
    id: "procedure-queue", title: "Dịch vụ được phân công", group: "Thuốc & dịch vụ", permission: "procedure.execute", description: "Công việc thực hiện dịch vụ của tài khoản hiện tại.", endpoint: "/procedure-queue", detailPath: "/orders/{id}", paginated: true,
    columns: orderColumns, detailFields: orderDetail, actions: orderActions,
    views: [{ id: "procedure-events", label: "Lịch sử thực hiện", dataKey: "events", columns: [{ key: "action", label: "Thao tác", format: "status" }, { key: "reason", label: "Lý do" }, { key: "actorId", label: "Người thực hiện" }, { key: "occurredAt", label: "Thời điểm", format: "datetime" }] }],
    help: "Chỉ chỉ định được phân công. Tệp kết quả được tải lên trong Tệp riêng tư, kèm mã lượt khám và mã chỉ định. Không thể bắt đầu ngoài khung giờ đã phân lịch.",
  },
  {
    id: "attachments", title: "Tệp riêng tư", group: "Lâm sàng", permission: ["attachment.read", "attachment.write"], description: "Tải lên, tải xuống có kiểm tra quyền và lưu trữ hồ sơ tệp.",
    endpoint: "/encounters/{encounterId}/attachments", queryFields: [encounterQuery], paginated: true, columns: attachmentColumns,
    detailFields: [...attachmentColumns, { key: "encounterId", label: "Lượt khám" }, { key: "sha256", label: "SHA-256" }, { key: "archived", label: "Đã lưu trữ", format: "boolean" }],
    actions: [
      { id: "attachment-upload", label: "Tải tệp lên", scope: "collection", method: "UPLOAD", path: "/encounters/{encounterId}/attachments", permission: "attachment.write", fields: [encounterQuery, uuid("orderId", "Mã chỉ định liên quan", false, "Bắt buộc với kỹ thuật viên; chỉ được tải lên cho chỉ định được phân công đang có lịch hoặc đang thực hiện."), { name: "file", label: "Tệp hồ sơ", type: "file", required: true, help: "Từ 1 byte đến 10 MiB. Chỉ PDF, PNG, JPEG hoặc văn bản UTF-8; không SVG / HTML. Backend kiểm tra nội dung tệp." }], help: "Gửi tệp theo multipart, không tạo URL công khai. Lỗi kho lưu trữ không được coi là tải lên thành công." },
      { id: "attachment-download", label: "Tải tệp có kiểm tra quyền", scope: "record", method: "DOWNLOAD", path: "/attachments/{id}/download", permission: "attachment.read" },
      { id: "attachment-archive", label: "Lưu trữ tệp", scope: "record", path: "/attachments/{id}/archive", permission: "attachment.write", fields: [version, reason], confirm: "Lưu trữ tệp đang chọn? Tệp sẽ không còn trong danh sách tệp đang dùng; lịch sử và lý do được giữ lại.", help: "Backend kiểm tra quyền và trạng thái lượt khám / chỉ định; đây là lưu trữ, không phải xóa tệp công khai." },
    ],
    help: "Dữ liệu không được công khai. Tải xuống yêu cầu phiên hợp lệ. Kỹ thuật viên chỉ thấy tệp thuộc chỉ định được giao; API danh sách chỉ trả về tệp chưa lưu trữ.",
  },
  {
    id: "medications", title: "Danh mục thuốc", group: "Danh mục", permission: ["clinical.read", "catalog.manage", "prescription.read"], description: "Thuốc, mã hoạt chất và đơn vị dùng trong đơn thuốc của cơ sở.", endpoint: "/medications", paginated: true,
    queryFields: [{ name: "q", label: "Tìm mã hoặc tên thuốc", maxLength: 160 }],
    columns: [recordId, { key: "code", label: "Mã thuốc" }, { key: "name", label: "Tên thuốc" }, { key: "ingredientCode", label: "Mã hoạt chất" }, { key: "unit", label: "Đơn vị" }, { key: "enabled", label: "Đang bật", format: "boolean" }, { key: "version", label: "Phiên bản" }],
    actions: [
      { id: "medication-create", label: "Thêm thuốc", scope: "collection", path: "/medications", permission: "catalog.manage", fields: medicationFields },
      { id: "medication-save", label: "Cập nhật thuốc", scope: "record", method: "PUT", path: "/medications/{id}", permission: "catalog.manage", fields: [...medicationFields, version], help: "Đổi mã hoạt chất có thể khiến đơn nháp cần làm mới trước khi phát hành. Không sửa nội dung của đơn đã phát hành." },
    ],
  },
  {
    id: "medication-rules", title: "Quy tắc an toàn thuốc", group: "Danh mục", permission: "catalog.manage", description: "Quy tắc tương tác / chống chỉ định theo nguồn được cơ sở phê duyệt.", endpoint: "/medication-rules",
    columns: [recordId, { key: "kind", label: "Loại", format: "status" }, { key: "medicationId", label: "Thuốc chính" }, { key: "otherMedicationId", label: "Thuốc tương tác" }, { key: "conditionCode", label: "Mã tình trạng" }, { key: "severity", label: "Mức cảnh báo", format: "status" }, { key: "message", label: "Thông điệp" }, { key: "source", label: "Nguồn" }, { key: "overridable", label: "Cho ghi đè", format: "boolean" }, { key: "enabled", label: "Đang bật", format: "boolean" }, { key: "version", label: "Phiên bản" }],
    actions: [
      { id: "rule-create", label: "Thêm quy tắc", scope: "collection", path: "/medication-rules", permission: "catalog.manage", fields: ruleFields, body: ruleBody },
      { id: "rule-save", label: "Cập nhật quy tắc", scope: "record", method: "PUT", path: "/medication-rules/{id}", permission: "catalog.manage", fields: [...ruleFields, version], body: ruleBody, confirm: "Cập nhật quy tắc an toàn thuốc? Kiểm tra nguồn, mức cảnh báo và quyền ghi đè trước khi áp dụng." },
    ],
    help: "Không tự sinh quy tắc hoặc coi đây là cơ sở dữ liệu tương tác thuốc đầy đủ. Danh sách API giới hạn 100 quy tắc; phải xem nguồn và phiên bản chính sách khi kiểm tra đơn.",
  },
  {
    id: "medication-policy", title: "Nguồn chính sách thuốc", group: "Danh mục", permission: ["clinical.read", "catalog.manage", "prescription.read"], description: "Nguồn và phiên bản dữ liệu được dùng khi máy chủ kiểm tra đơn thuốc.", endpoint: "/medication-policy", single: true, columns: [],
    detailFields: [{ key: "source", label: "Nguồn dữ liệu" }, { key: "dataVersion", label: "Phiên bản nguồn" }, { key: "enabled", label: "Đang bật", format: "boolean" }, { key: "approvedBy", label: "Người phê duyệt" }, { key: "approvedAt", label: "Phê duyệt lúc", format: "datetime" }, { key: "version", label: "Phiên bản bản ghi" }],
    actions: [{ id: "policy-save", label: "Ghi chính sách thuốc", scope: "collection", method: "PUT", path: "/medication-policy", permission: "catalog.manage", fields: [textarea("source", "Nguồn đã được phê duyệt", 500), text("dataVersion", "Phiên bản nguồn dữ liệu", 80), { name: "enabled", label: "Bật nguồn chính sách", type: "checkbox", defaultValue: true }, { ...version, required: false, help: "Bỏ trống khi tạo lần đầu; khi cập nhật dùng phiên bản hiện tại." }], confirm: "Ghi nguồn chính sách thuốc của cơ sở? Đây là nguồn tham chiếu cho kiểm tra đơn; cần dữ liệu được phê duyệt thực tế." }],
    help: "Chưa có chính sách hoặc chính sách chưa bật sẽ chặn kiểm tra an toàn và phát hành đơn. Không điền nguồn giả để vượt kiểm tra.",
  },
];
