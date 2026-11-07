import type { ColumnSpec, DataRecord, FieldSpec, ModuleSpec } from "@/lib/schema";

// These definitions follow PatientController, SchedulingController and the
// operational endpoints in ClinicalController. No staff-directory API exists.
const staffHelp =
  "Nhập mã định danh nhân sự do quản trị cơ sở cung cấp. Danh bạ nhân sự dùng cho tiếp nhận chưa có API; hệ thống kiểm tra vai trò và cơ sở khi gửi.";

const version: FieldSpec = {
  name: "version", label: "Phiên bản đang chỉnh sửa", type: "number",
  required: true, min: 0, source: "version",
  help: "Giữ phiên bản của dữ liệu đã mở. Nếu có xung đột, tải lại và đối chiếu trước khi gửi lại.",
};
const patient: FieldSpec = {
  name: "patientId", label: "Bệnh nhân", type: "uuid", required: true,
  lookup: { path: "/patients", valueKey: "id", labelKey: "fullName", permission: "patient.admin.read" },
  help: "Tìm và chọn đúng hồ sơ; đối chiếu mã bệnh nhân và ngày sinh. Nếu không có quyền tìm, cần mã hồ sơ được cung cấp hợp lệ.",
};
const provider: FieldSpec = {
  name: "providerId", label: "Bác sĩ phụ trách", type: "uuid", required: true,
  source: "providerId", help: staffHelp,
};
const room: FieldSpec = {
  name: "roomId", label: "Phòng", type: "uuid", required: true, source: "roomId",
  lookup: { path: "/rooms", valueKey: "id", labelKey: "name", permission: "schedule.read" },
};
const service: FieldSpec = {
  name: "serviceId", label: "Dịch vụ khám", type: "uuid", required: true, source: "serviceId",
  lookup: { path: "/services", valueKey: "id", labelKey: "name", permission: "schedule.read" },
};
const nurse: FieldSpec = {
  name: "nurseId", label: "Điều dưỡng phụ trách", type: "uuid", source: "nurseId",
  help: `Có thể để trống nếu chưa phân công. ${staffHelp}`,
};
const identityVerified: FieldSpec = {
  name: "identityVerified", label: "Tôi đã đối chiếu và xác minh danh tính bệnh nhân",
  type: "checkbox", required: true, defaultValue: false,
  help: "Xác minh trực tiếp trước khi tiếp nhận; không dựa vào việc trùng tên.",
};
const range: FieldSpec[] = [
  { name: "from", label: "Từ thời điểm", type: "datetime", required: true, defaultValue: "$todayStart" },
  { name: "to", label: "Đến trước thời điểm", type: "datetime", required: true, defaultValue: "$todayEnd" },
];
const patientColumns: ColumnSpec[] = [
  { key: "code", label: "Mã bệnh nhân" },
  { key: "fullName", label: "Họ và tên" },
  { key: "dateOfBirth", label: "Ngày sinh", format: "date" },
  { key: "sex", label: "Giới tính", format: "status" },
  { key: "phone", label: "Điện thoại" },
];
const demographics: FieldSpec[] = [
  { name: "fullName", label: "Họ và tên", required: true, maxLength: 160, source: "fullName" },
  { name: "dateOfBirth", label: "Ngày sinh", type: "date", required: true, source: "dateOfBirth", help: "Ngày sinh không được ở tương lai." },
  { name: "sex", label: "Giới tính", type: "select", required: true, source: "sex", options: [
    { value: "MALE", label: "Nam" }, { value: "FEMALE", label: "Nữ" },
    { value: "OTHER", label: "Khác" }, { value: "UNKNOWN", label: "Chưa xác định" },
  ] },
  { name: "phone", label: "Điện thoại", type: "tel", maxLength: 21, pattern: "\\+?[0-9]{7,20}", source: "phone", help: "7–20 chữ số, có thể bắt đầu bằng dấu +; để trống nếu chưa có." },
  { name: "nationalId", label: "Số giấy tờ định danh", maxLength: 32, source: "nationalId" },
  { name: "address", label: "Địa chỉ", type: "textarea", maxLength: 500, source: "address" },
];
const duplicateReview: FieldSpec[] = [
  { name: "duplicateReviewed", label: "Đã xem xét các hồ sơ nghi trùng", type: "checkbox", defaultValue: false, help: "Chỉ xác nhận sau khi đối chiếu các hồ sơ nghi trùng." },
  { name: "duplicateReviewReason", label: "Lý do vẫn tạo hoặc cập nhật hồ sơ", type: "textarea", maxLength: 300, help: "Bắt buộc khi có hồ sơ nghi trùng và đã xác nhận xem xét; được ghi vào lịch sử kiểm toán." },
];
const bookingFields: FieldSpec[] = [
  patient, provider, service, room,
  { name: "startsAt", label: "Bắt đầu khám", type: "datetime", required: true, source: "startsAt", help: "Chọn một khung còn trống ở tương lai; máy chủ kiểm tra lại khi lưu." },
];
const optional = (value: unknown) => typeof value === "string" && !value.trim() ? null : value ?? null;
const patientBody = (values: DataRecord, record: DataRecord): DataRecord => ({
  fullName: values.fullName, dateOfBirth: values.dateOfBirth, sex: values.sex,
  phone: optional(values.phone), nationalId: optional(values.nationalId), address: optional(values.address),
  duplicateReviewed: values.duplicateReviewed === true,
  duplicateReviewReason: optional(values.duplicateReviewReason),
  ...(values.version !== undefined || record.version !== undefined ? { version: values.version ?? record.version } : {}),
});

export const receptionModules: ModuleSpec[] = [
  {
    id: "patients", title: "Bệnh nhân", group: "Tiếp nhận",
    description: "Tìm đúng người, đối chiếu thông tin và quản lý hồ sơ hành chính.",
    permission: "patient.admin.read", endpoint: "/patients", detailPath: "/patients/{id}", paginated: true,
    queryFields: [{ name: "q", label: "Tìm bệnh nhân", maxLength: 160, help: "Tìm theo tên, mã hồ sơ, điện thoại hoặc giấy tờ định danh." }],
    columns: patientColumns,
    detailFields: [...patientColumns, { key: "nationalId", label: "Giấy tờ định danh" }, { key: "address", label: "Địa chỉ" }, { key: "mergedIntoId", label: "Hồ sơ chính sau hợp nhất", module: "patients" }, { key: "version", label: "Phiên bản" }],
    actions: [
      {
        id: "duplicate-check", label: "Kiểm tra nghi trùng", scope: "collection", permission: "patient.admin.create",
        method: "POST", path: "/patients/duplicate-check", fields: demographics,
        body: patientBody, resultColumns: patientColumns,
        help: "Kết quả là danh sách hồ sơ cần đối chiếu, tối đa 100 ứng viên. Thao tác này không tạo bệnh nhân.",
      },
      {
        id: "create", label: "Tạo hồ sơ bệnh nhân", scope: "collection", permission: "patient.admin.create",
        method: "POST", path: "/patients", fields: [...demographics, ...duplicateReview], body: patientBody,
        help: "Kiểm tra nghi trùng trước khi tạo. Máy chủ sẽ từ chối hồ sơ nghi trùng chưa được xem xét và ghi lý do.",
      },
      {
        id: "edit", label: "Cập nhật hành chính", scope: "record", permission: "patient.admin.update",
        method: "PUT", path: "/patients/{id}", fields: [...demographics, ...duplicateReview, version], body: patientBody,
        when: { field: "mergedIntoId", values: [null] },
        help: "Hồ sơ đã hợp nhất phải được cập nhật tại hồ sơ chính. Bản ghi thay đổi bởi người khác cần được tải lại và đối chiếu.",
      },
    ],
  },
  {
    id: "appointments", title: "Lịch hẹn", group: "Tiếp nhận",
    description: "Theo dõi lịch khám theo thời gian, đặt lịch và xác nhận bệnh nhân đến.",
    permission: "schedule.read", endpoint: "/appointments", paginated: true, queryFields: range,
    columns: [
      { key: "startsAt", label: "Bắt đầu", format: "datetime" },
      { key: "patientName", label: "Bệnh nhân" }, { key: "patientCode", label: "Mã bệnh nhân" },
      { key: "status", label: "Trạng thái lịch", format: "status" }, { key: "endsAt", label: "Kết thúc", format: "datetime" },
    ],
    detailFields: [
      { key: "patientName", label: "Bệnh nhân" }, { key: "patientCode", label: "Mã bệnh nhân" },
      { key: "patientId", label: "Định danh hồ sơ", module: "patients" },
      { key: "providerId", label: "Định danh bác sĩ" }, { key: "roomId", label: "Định danh phòng", module: "rooms" },
      { key: "serviceId", label: "Định danh dịch vụ", module: "services" },
      { key: "startsAt", label: "Bắt đầu", format: "datetime" }, { key: "endsAt", label: "Kết thúc", format: "datetime" },
      { key: "status", label: "Trạng thái", format: "status" }, { key: "version", label: "Phiên bản" },
    ],
    help: "Khoảng tìm tối đa 31 ngày. Nhân sự chỉ thấy lịch được phép; không có số tổng lịch hẹn trong phản hồi. Danh bạ bác sĩ chưa có API, cần định danh nhân sự được cơ sở cung cấp.",
    actions: [
      {
        id: "book", label: "Tạo lịch hẹn", scope: "collection", permission: "appointment.write",
        method: "POST", path: "/appointments", fields: bookingFields, idempotent: true,
        help: "Tra khung trống trước khi đặt. Không đổi nội dung khi thử lại cùng một yêu cầu có mã chống trùng.",
      },
      {
        id: "reschedule", label: "Đổi lịch", scope: "record", permission: "appointment.write",
        method: "PUT", path: "/appointments/{id}/schedule", when: { field: "status", values: ["BOOKED", "CONFIRMED"] },
        fields: [provider, service, room, { name: "startsAt", label: "Bắt đầu mới", type: "datetime", required: true, source: "startsAt" }, { name: "reason", label: "Lý do đổi lịch", type: "textarea", required: true, maxLength: 500 }, version],
        help: "Máy chủ kiểm tra lại phòng, năng lực, ca làm, thời gian nghỉ và xung đột. Đổi lịch đưa trạng thái về đã đặt.",
      },
      {
        id: "confirm", label: "Xác nhận lịch", scope: "record", permission: "appointment.write",
        path: "/appointments/{id}/transitions", fields: [version], payload: { action: "CONFIRM" },
        when: { field: "status", values: ["BOOKED"] },
      },
      {
        id: "cancel", label: "Hủy lịch", scope: "record", permission: "appointment.write", danger: true,
        path: "/appointments/{id}/transitions", fields: [{ name: "reason", label: "Lý do hủy lịch", type: "textarea", required: true, maxLength: 500 }, version], payload: { action: "CANCEL" },
        when: { field: "status", values: ["BOOKED", "CONFIRMED"] }, confirm: "Hủy lịch hẹn đã chọn và giải phóng khung giờ? Lý do sẽ được lưu trong lịch sử.",
      },
      {
        id: "no-show", label: "Ghi nhận không đến", scope: "record", permission: "appointment.write",
        path: "/appointments/{id}/transitions", fields: [{ name: "reason", label: "Ghi chú", type: "textarea", maxLength: 500 }, version], payload: { action: "NO_SHOW" },
        when: { field: "status", values: ["BOOKED", "CONFIRMED"] }, confirm: "Xác nhận bệnh nhân không đến lịch hẹn đã chọn?",
        help: "Chỉ được ghi nhận sau thời điểm kết thúc lịch hẹn. Máy chủ kiểm tra thời điểm này.",
      },
      {
        id: "check-in", label: "Xác minh và tiếp nhận", scope: "record", permission: "appointment.write",
        path: "/check-ins", fields: [nurse, identityVerified],
        when: { field: "status", values: ["BOOKED", "CONFIRMED"] },
        confirm: "Tiếp nhận đúng bệnh nhân của lịch hẹn này và tạo lượt khám?",
        body: (values, record) => ({ patientId: record.patientId, appointmentId: record.id, doctorId: record.providerId, nurseId: optional(values.nurseId), identityVerified: values.identityVerified === true }),
        help: "Lịch phải thuộc ngày hiện tại theo múi giờ cơ sở. Bệnh nhân có lượt khám đang hoạt động sẽ không được tiếp nhận trùng.",
      },
    ],
    views: [{
      id: "events", label: "Lịch sử lịch hẹn", path: "/appointments/{id}/events", permission: "appointment.write",
      columns: [{ key: "occurredAt", label: "Thời điểm", format: "datetime" }, { key: "action", label: "Thay đổi", format: "status" }, { key: "reason", label: "Lý do" }, { key: "actorId", label: "Định danh người thực hiện" }],
    }],
  },
  {
    id: "queue", title: "Hàng đợi tiếp nhận", group: "Tiếp nhận",
    description: "Theo dõi lượt khám đang hoạt động và điều dưỡng được phân công.",
    permission: "queue.read", endpoint: "/queue", paginated: true,
    columns: [{ key: "queueNumber", label: "Số thứ tự" }, { key: "patientName", label: "Bệnh nhân" }, { key: "patientCode", label: "Mã bệnh nhân" }, { key: "status", label: "Trạng thái lượt", format: "status" }, { key: "createdAt", label: "Tiếp nhận lúc", format: "datetime" }],
    detailFields: [{ key: "patientName", label: "Bệnh nhân" }, { key: "patientCode", label: "Mã bệnh nhân" }, { key: "patientId", label: "Định danh hồ sơ", module: "patients" }, { key: "doctorId", label: "Định danh bác sĩ" }, { key: "nurseId", label: "Định danh điều dưỡng" }, { key: "appointmentId", label: "Định danh lịch hẹn" }, { key: "queueNumber", label: "Số thứ tự" }, { key: "status", label: "Trạng thái", format: "status" }, { key: "version", label: "Phiên bản" }],
    help: "Chỉ hiển thị lượt chưa hoàn tất trong phạm vi được phép. Thứ tự là số do máy chủ cấp; không tự sắp lại hàng đợi khi thao tác.",
    actions: [
      {
        id: "walk-in", label: "Tiếp nhận không hẹn", scope: "collection", permission: "appointment.write",
        path: "/check-ins", fields: [patient, { name: "doctorId", label: "Bác sĩ phụ trách", type: "uuid", required: true, help: staffHelp }, nurse, identityVerified],
        confirm: "Đã xác minh danh tính và tiếp nhận bệnh nhân không có lịch hẹn?",
        body: (values) => ({ patientId: values.patientId, doctorId: values.doctorId, appointmentId: null, nurseId: optional(values.nurseId), identityVerified: values.identityVerified === true }),
        help: "Chỉ thực hiện khi cơ sở cho phép tiếp nhận không hẹn; không được tạo lượt hoạt động trùng.",
      },
      {
        id: "assign-nurse", label: "Phân công điều dưỡng", scope: "record", permission: "appointment.write",
        method: "PUT", path: "/encounters/{id}/nurse", fields: [nurse, version],
        body: (values, record) => ({ nurseId: optional(values.nurseId), version: values.version ?? record.version }),
        help: "Để trống điều dưỡng để gỡ phân công hiện tại. Máy chủ kiểm tra nhân sự thuộc cơ sở và vai trò điều dưỡng.",
      },
    ],
  },
  {
    id: "availability", title: "Khung giờ trống", group: "Tiếp nhận",
    description: "Đối chiếu bác sĩ, phòng và dịch vụ trước khi tạo lịch hẹn.",
    permission: "schedule.read", endpoint: "/availability",
    queryFields: [provider, room, service, ...range],
    columns: [{ key: "startsAt", label: "Bắt đầu", format: "datetime" }, { key: "endsAt", label: "Kết thúc khám", format: "datetime" }, { key: "reservedUntil", label: "Giữ nguồn lực đến", format: "datetime" }],
    detailFields: [{ key: "startsAt", label: "Bắt đầu", format: "datetime" }, { key: "endsAt", label: "Kết thúc khám", format: "datetime" }, { key: "reservedUntil", label: "Bao gồm thời gian đệm đến", format: "datetime" }, { key: "providerId", label: "Định danh bác sĩ" }, { key: "roomId", label: "Định danh phòng", module: "rooms" }],
    help: "Tra tối đa một ngày; các khung cách nhau 5 phút, tối đa 100 kết quả. Khung trống chỉ là ảnh chụp tại thời điểm tra, máy chủ kiểm tra lại khi đặt lịch.",
    actions: [{
      id: "book-slot", label: "Đặt khung giờ", scope: "record", permission: "appointment.write",
      path: "/appointments", fields: bookingFields, idempotent: true,
      help: "Giữ đúng bác sĩ, phòng, dịch vụ đã tra và xác minh bệnh nhân. Các khung trùng nhau có thể dùng chung nguồn lực.",
    }],
  },
  {
    id: "rooms", title: "Phòng khám", group: "Nguồn lực",
    description: "Quản lý phòng, thiết bị và khả năng sử dụng khi xếp lịch.",
    permission: "schedule.read", endpoint: "/rooms",
    columns: [{ key: "code", label: "Mã phòng" }, { key: "name", label: "Tên phòng" }, { key: "roomType", label: "Loại phòng" }, { key: "enabled", label: "Đang sử dụng", format: "boolean" }],
    detailFields: [{ key: "code", label: "Mã phòng" }, { key: "name", label: "Tên phòng" }, { key: "roomType", label: "Loại phòng" }, { key: "equipment", label: "Thiết bị" }, { key: "capacity", label: "Sức chứa" }, { key: "enabled", label: "Đang sử dụng", format: "boolean" }, { key: "version", label: "Phiên bản" }],
    help: "Danh mục trả tối đa 100 phòng. Sức chứa hiện được backend cố định là 1, không phải trường cấu hình.",
    actions: [
      ...(["create", "edit"] as const).map((mode) => ({
        id: mode, label: mode === "create" ? "Thêm phòng" : "Cập nhật phòng", scope: mode === "create" ? "collection" as const : "record" as const,
        permission: "schedule.manage", method: mode === "create" ? "POST" as const : "PUT" as const, path: mode === "create" ? "/rooms" : "/rooms/{id}",
        fields: [
          { name: "code", label: "Mã phòng", required: true, maxLength: 32, pattern: "[A-Z0-9_-]{1,32}", source: "code", help: "Chữ in hoa, chữ số, dấu gạch ngang hoặc gạch dưới." },
          { name: "name", label: "Tên phòng", required: true, maxLength: 160, source: "name" },
          { name: "roomType", label: "Loại phòng", required: true, maxLength: 40, source: "roomType" },
          { name: "equipment", label: "Thiết bị", type: "textarea" as const, maxLength: 500, source: "equipment", defaultValue: "" },
          { name: "enabled", label: "Cho phép sử dụng", type: "checkbox" as const, defaultValue: true, source: "enabled" },
          ...(mode === "edit" ? [version] : []),
        ],
        body: (values: DataRecord, record: DataRecord) => ({ code: values.code, name: values.name, roomType: values.roomType, equipment: values.equipment ?? "", enabled: values.enabled === true, ...(mode === "edit" ? { version: values.version ?? record.version } : {}) }),
      })),
    ],
  },
  {
    id: "services", title: "Dịch vụ khám", group: "Nguồn lực",
    description: "Cấu hình thời lượng và khoảng đệm phục vụ việc đặt lịch.",
    permission: "schedule.read", endpoint: "/services",
    columns: [{ key: "code", label: "Mã dịch vụ" }, { key: "name", label: "Tên dịch vụ" }, { key: "durationMinutes", label: "Khám (phút)" }, { key: "bufferMinutes", label: "Đệm (phút)" }, { key: "enabled", label: "Đang sử dụng", format: "boolean" }],
    detailFields: [{ key: "code", label: "Mã dịch vụ" }, { key: "name", label: "Tên dịch vụ" }, { key: "durationMinutes", label: "Thời lượng khám (phút)" }, { key: "bufferMinutes", label: "Thời gian đệm (phút)" }, { key: "requiredRoomType", label: "Loại phòng yêu cầu" }, { key: "requiredEquipment", label: "Thiết bị yêu cầu" }, { key: "enabled", label: "Đang sử dụng", format: "boolean" }, { key: "version", label: "Phiên bản" }],
    help: "Danh mục trả tối đa 100 dịch vụ. Đây là thời lượng xếp lịch; bảng giá và hiệu lực giá được quản lý riêng.",
    actions: [
      ...(["create", "edit"] as const).map((mode) => ({
        id: mode, label: mode === "create" ? "Thêm dịch vụ" : "Cập nhật dịch vụ", scope: mode === "create" ? "collection" as const : "record" as const,
        permission: "schedule.manage", method: mode === "create" ? "POST" as const : "PUT" as const, path: mode === "create" ? "/services" : "/services/{id}",
        fields: [
          { name: "code", label: "Mã dịch vụ", required: true, maxLength: 32, pattern: "[A-Z0-9_-]{1,32}", source: "code" },
          { name: "name", label: "Tên dịch vụ", required: true, maxLength: 160, source: "name" },
          { name: "durationMinutes", label: "Thời lượng khám (phút)", type: "number" as const, required: true, min: 5, max: 240, source: "durationMinutes" },
          { name: "bufferMinutes", label: "Thời gian đệm (phút)", type: "number" as const, required: true, min: 0, max: 60, defaultValue: 0, source: "bufferMinutes" },
          { name: "enabled", label: "Cho phép sử dụng", type: "checkbox" as const, defaultValue: true, source: "enabled" },
          ...(mode === "edit" ? [version] : []),
        ],
      })),
      {
        id: "requirements", label: "Yêu cầu phòng và thiết bị", scope: "record", permission: "catalog.manage",
        method: "PUT", path: "/services/{id}/requirements",
        fields: [
          { name: "requiredRoomType", label: "Loại phòng yêu cầu", maxLength: 40, source: "requiredRoomType", help: "Để trống nếu không ràng buộc loại phòng." },
          { name: "requiredEquipment", label: "Thiết bị yêu cầu", type: "textarea", maxLength: 500, defaultValue: "", source: "requiredEquipment", help: "Để trống nếu không có yêu cầu thiết bị." },
          version,
        ],
        body: (values, record) => ({ requiredRoomType: optional(values.requiredRoomType), requiredEquipment: values.requiredEquipment ?? "", version: values.version ?? record.version }),
        help: "Cập nhật yêu cầu nguồn lực của dịch vụ. Phiên bản được kiểm tra để tránh ghi đè thay đổi của người khác.",
      },
    ],
  },
  {
    id: "staff-schedules", title: "Ca làm và nghỉ", group: "Nguồn lực",
    description: "Theo dõi thời gian làm việc và thời gian vắng mặt của nhân sự.",
    permission: "schedule.read", endpoint: "/staff-schedules", queryFields: range,
    columns: [{ key: "providerName", label: "Nhân sự" }, { key: "kind", label: "Loại thời gian", format: "status" }, { key: "startsAt", label: "Bắt đầu", format: "datetime" }, { key: "endsAt", label: "Kết thúc", format: "datetime" }],
    detailFields: [{ key: "providerName", label: "Nhân sự" }, { key: "providerId", label: "Định danh nhân sự" }, { key: "kind", label: "Loại", format: "status" }, { key: "startsAt", label: "Bắt đầu", format: "datetime" }, { key: "endsAt", label: "Kết thúc", format: "datetime" }],
    help: "Khoảng tra và ca làm tối đa 31 ngày; tối đa 100 kết quả. Chỉ bác sĩ hoặc kỹ thuật viên đang hoạt động trong cơ sở được cấu hình. Chưa có API sửa hoặc xóa ca.",
    actions: [{
      id: "add-schedule", label: "Thêm ca làm hoặc nghỉ", scope: "collection", permission: "schedule.manage",
      path: "/staff-schedules", fields: [
        { name: "providerId", label: "Nhân sự", type: "uuid", required: true, help: staffHelp },
        { name: "kind", label: "Loại thời gian", type: "select", required: true, options: [{ value: "WORK", label: "Ca làm" }, { value: "ABSENCE", label: "Vắng mặt / nghỉ" }] },
        { name: "startsAt", label: "Bắt đầu", type: "datetime", required: true }, { name: "endsAt", label: "Kết thúc", type: "datetime", required: true },
      ],
      help: "Phải đổi các lịch hẹn đang hoạt động bị ảnh hưởng trước khi thêm thời gian nghỉ. Thời điểm bắt đầu phải trước kết thúc.",
    }],
  },
  {
    id: "provider-services", title: "Năng lực dịch vụ", group: "Nguồn lực",
    description: "Bổ sung dịch vụ mà bác sĩ hoặc kỹ thuật viên có thể thực hiện.",
    permission: "schedule.manage", columns: [],
    help: "Backend hiện hỗ trợ bổ sung năng lực; chưa có API liệt kê hoặc gỡ năng lực. Trang này không thể xác nhận danh sách phân công hiện hành.",
    actions: [{
      id: "add-capability", label: "Bổ sung năng lực", scope: "collection", permission: "schedule.manage",
      path: "/provider-services", fields: [{ name: "providerId", label: "Bác sĩ hoặc kỹ thuật viên", type: "uuid", required: true, help: staffHelp }, service],
      help: "Nhân sự phải đang hoạt động, có vai trò bác sĩ hoặc kỹ thuật viên tại cơ sở. Thêm lại cùng một năng lực không tạo bản ghi trùng.",
    }],
  },
];
