import type { ActionSpec, ColumnSpec, FieldSpec, ModuleSpec } from "@/lib/schema";

const uuid = (name: string, label: string, required = true): FieldSpec => ({
  name, label, type: "uuid", required,
});
const version: FieldSpec = {
  name: "version", label: "Phiên bản đang xem", type: "number", required: true, min: 0, pattern: "[0-9]+",
  help: "Giữ phiên bản của bản ghi vừa đọc. Nếu dữ liệu đã thay đổi, tải lại trước khi xác nhận.",
};
const reason = (maxLength = 500): FieldSpec => ({
  name: "reason", label: "Lý do", type: "textarea", required: true, maxLength,
});
const enabled: FieldSpec = { name: "enabled", label: "Đang sử dụng", type: "checkbox", defaultValue: true };
const money = (name: string, label: string, min = 1): FieldSpec => ({
  name, label, type: "number", required: true, min, max: 999999999999999, pattern: "[0-9]+",
  help: "Nhập số nguyên, đơn vị đồng (VND).",
});
const currency: FieldSpec = {
  name: "currency", label: "Tiền tệ", type: "select", required: true,
  options: [{ value: "VND", label: "Đồng Việt Nam (VND)" }], defaultValue: "VND",
};
const period: FieldSpec[] = [
  { name: "from", label: "Từ thời điểm", type: "datetime", required: true, defaultValue: "$todayStart" },
  { name: "to", label: "Đến trước thời điểm", type: "datetime", required: true, defaultValue: "$todayEnd",
    help: "Khoảng báo cáo không gồm thời điểm kết thúc; tối đa 366 ngày." },
];
const approval: FieldSpec = {
  name: "action", label: "Quyết định", type: "select", required: true,
  options: [{ value: "APPROVE", label: "Phê duyệt" }, { value: "REJECT", label: "Từ chối" }],
};
const reference: ColumnSpec = { key: "id", label: "Mã tham chiếu" };
const status: ColumnSpec = { key: "status", label: "Trạng thái", format: "status" };
const accountColumns: ColumnSpec[] = [
  { key: "displayName", label: "Nhân sự" }, { key: "username", label: "Tên đăng nhập" },
  { key: "roles", label: "Vai trò tại cơ sở", format: "list" }, status,
];
const roleField: FieldSpec = {
  name: "roles", label: "Vai trò tại cơ sở", type: "string-list", required: true,
  lookup: { path: "/@admin/roles", valueKey: "code", labelKey: "name", permission: "account.assign_role" },
  help: "Chỉ chọn vai trò cần cho công việc. Đổi quyền sẽ thu hồi các phiên hiện có của tài khoản.",
};
const password: FieldSpec = {
  name: "password", label: "Mật khẩu mới", type: "password", required: true, min: 12, maxLength: 72,
  help: "Ít nhất 12 ký tự, tối đa 72 byte UTF-8. Không dùng mật khẩu có sẵn trong dữ liệu minh họa.",
};
const refundColumns: ColumnSpec[] = [
  { key: "id", label: "Yêu cầu hoàn", module: "refunds" }, status,
  { key: "amount", label: "Số tiền hoàn", format: "money" },
  { key: "reason", label: "Lý do" }, { key: "requestedAt", label: "Ngày yêu cầu", format: "datetime" },
];
const refundActions: ActionSpec[] = [
  {
    id: "approve-refund", label: "Duyệt yêu cầu hoàn", scope: "record", path: "/refunds/{id}/approval",
    permission: "refund.approve", idempotent: true, when: { field: "status", values: ["PENDING_APPROVAL"] },
    fields: [version, approval, reason()],
    confirm: "Xác nhận quyết định cho yêu cầu hoàn này? Bạn không được duyệt yêu cầu do chính mình tạo.",
    help: "Phê duyệt giữ nguyên số dư hóa đơn. Chỉ bước thực hiện hoàn mới ghi nhận tiền đã hoàn.",
  },
  {
    id: "execute-refund", label: "Thực hiện hoàn tiền", scope: "record", path: "/refunds/{id}/execute",
    permission: "refund.request", idempotent: true, danger: true,
    when: { field: "status", values: ["APPROVED"] }, fields: [version],
    confirm: "Ghi nhận hoàn tiền đã được người khác phê duyệt? Giao dịch gốc sẽ được giữ lại trong lịch sử.",
  },
];

export const administrationModules: ModuleSpec[] = [
  {
    id: "accounts", title: "Tài khoản nhân sự", group: "Quản trị",
    description: "Quản lý tài khoản và quyền làm việc tại cơ sở đang chọn.",
    permission: "account.read", endpoint: "/@admin/accounts", detailPath: "/@admin/accounts/{id}",
    paginated: true, columns: accountColumns,
    detailFields: [...accountColumns, reference, { key: "createdAt", label: "Ngày tạo", format: "datetime" },
      { key: "version", label: "Phiên bản" }],
    help: "Không tự sửa tài khoản của mình. Thay đổi tên, trạng thái, mật khẩu hoặc thu hồi phiên cần quyền ở mọi cơ sở của tài khoản đích.",
    actions: [
      {
        id: "create-account", label: "Tạo tài khoản", scope: "collection", path: "/@admin/accounts",
        allPermissions: ["account.create", "account.assign_role"],
        fields: [
          { name: "username", label: "Tên đăng nhập", required: true, maxLength: 64,
            pattern: "[A-Za-z0-9][A-Za-z0-9._-]{2,63}", help: "3–64 ký tự: chữ không dấu, số, dấu chấm, gạch nối hoặc gạch dưới." },
          { name: "displayName", label: "Tên hiển thị", required: true, maxLength: 160 }, password, roleField,
        ],
      },
      {
        id: "update-account", label: "Sửa thông tin tài khoản", scope: "record", method: "PUT",
        path: "/@admin/accounts/{id}", permission: "account.update",
        fields: [{ name: "displayName", label: "Tên hiển thị", required: true, maxLength: 160 },
          { name: "status", label: "Trạng thái", type: "select", required: true,
            options: [{ value: "ACTIVE", label: "Đang hoạt động" }, { value: "LOCKED", label: "Đã khóa" },
              { value: "DISABLED", label: "Ngừng sử dụng" }] }, version],
        confirm: "Lưu thông tin và trạng thái tài khoản? Thay đổi trạng thái có thể kết thúc các phiên đang hoạt động.",
      },
      {
        id: "replace-roles", label: "Thay đổi vai trò", scope: "record", method: "PUT",
        path: "/@admin/accounts/{id}/roles", permission: "account.assign_role", fields: [roleField, version],
        confirm: "Thay thế vai trò tại cơ sở này và thu hồi phiên đăng nhập của tài khoản?",
      },
      {
        id: "reset-password", label: "Đặt lại mật khẩu", scope: "record", method: "PUT",
        path: "/@admin/accounts/{id}/password", permission: "account.update", fields: [password, version],
        confirm: "Đặt mật khẩu mới và kết thúc các phiên hiện tại của tài khoản?",
      },
      {
        id: "revoke-sessions", label: "Thu hồi phiên đăng nhập", scope: "record",
        path: "/@admin/accounts/{id}/revoke-sessions", permission: "account.update", fields: [version], danger: true,
        confirm: "Buộc tài khoản này đăng nhập lại trên mọi phiên đang hoạt động?",
      },
    ],
  },
  {
    id: "roles", title: "Danh mục vai trò", group: "Quản trị",
    description: "Các vai trò do hệ thống cung cấp để phân công công việc tại cơ sở.",
    permission: "account.assign_role", endpoint: "/@admin/roles",
    columns: [{ key: "name", label: "Vai trò" }, { key: "code", label: "Mã vai trò" }],
    help: "Danh mục chỉ đọc. Quyền thực tế còn phụ thuộc cơ sở và quan hệ phân công; vai trò quản trị không mặc định cho phép đọc hồ sơ lâm sàng.",
  },
  {
    id: "invoices", title: "Hóa đơn", group: "Tài chính",
    description: "Theo dõi hóa đơn, số tiền đã nhận và số dư còn lại.",
    permission: "invoice.read", endpoint: "/invoices", detailPath: "/invoices/{id}", paginated: true,
    columns: [reference, status, { key: "total", label: "Tổng cần thu", format: "money" },
      { key: "netPaid", label: "Đã thu ròng", format: "money" }, { key: "balance", label: "Còn phải thu", format: "money" },
      { key: "createdAt", label: "Ngày lập", format: "datetime" }],
    detailFields: [reference, status, { key: "encounterId", label: "Lượt khám" },
      { key: "subtotal", label: "Tạm tính", format: "money" }, { key: "discount", label: "Giảm giá", format: "money" },
      { key: "total", label: "Tổng cần thu", format: "money" }, { key: "netPaid", label: "Đã thu ròng", format: "money" },
      { key: "balance", label: "Còn phải thu", format: "money" }],
    help: "Hóa đơn được lập từ lượt khám đã kết thúc. Kết thúc lâm sàng và thanh toán là hai trạng thái độc lập.",
    actions: [
      {
        id: "create-invoice", label: "Lập hóa đơn từ lượt khám", scope: "collection",
        path: "/encounters/{encounterId}/invoice", permission: "invoice.write", idempotent: true,
        fields: [uuid("encounterId", "Mã lượt khám đã kết thúc")], body: () => undefined,
        help: "Giá và các dòng hóa đơn được tính bởi hệ thống từ dịch vụ đã thực hiện; không nhập tổng tiền thủ công.",
      },
      {
        id: "discount-invoice", label: "Áp dụng giảm giá", scope: "record", path: "/invoices/{id}/discount",
        permission: "invoice.discount", fields: [money("amount", "Số tiền giảm", 0), reason(), version],
        when: { field: "status", values: ["DRAFT"] }, confirm: "Áp dụng số tiền giảm và lưu lý do trên hóa đơn nháp?",
      },
      {
        id: "issue-invoice", label: "Phát hành hóa đơn", scope: "record", path: "/invoices/{id}/issue",
        permission: "invoice.write", fields: [version], when: { field: "status", values: ["DRAFT"] },
        confirm: "Phát hành hóa đơn với các dòng và số tiền đang hiển thị? Nội dung phát hành được giữ trong lịch sử.",
      },
      {
        id: "record-payment", label: "Ghi nhận thanh toán", scope: "record", path: "/invoices/{id}/payments",
        permission: "payment.write", idempotent: true, when: { field: "status", values: ["ISSUED", "PARTIALLY_PAID"] },
        fields: [money("amount", "Số tiền đã nhận"), currency,
          { name: "method", label: "Phương thức", type: "select", required: true,
            options: [{ value: "CASH", label: "Tiền mặt" }, { value: "BANK_TRANSFER", label: "Chuyển khoản đã đối soát" }] },
          { name: "receivedVerified", label: "Tôi đã xác minh cơ sở nhận đủ số tiền vừa nhập", type: "checkbox", required: true, defaultValue: false }],
        confirm: "Ghi nhận khoản tiền đã xác minh vào hóa đơn? Thao tác này không yêu cầu ngân hàng chuyển tiền.",
      },
      {
        id: "request-refund", label: "Yêu cầu hoàn tiền", scope: "record", path: "/refunds",
        permission: "refund.request", idempotent: true,
        fields: [uuid("paymentId", "Mã khoản thanh toán gốc"), money("amount", "Số tiền đề nghị hoàn"), reason()],
        help: "Chọn khoản thanh toán gốc trong lịch sử bên dưới. Yêu cầu trong ngưỡng tự hoàn có thể được hoàn tất ngay; xem trạng thái hệ thống trả về.",
        confirm: "Gửi yêu cầu hoàn khoản tiền này? Chính sách cơ sở quyết định yêu cầu cần phê duyệt hay được thực hiện ngay.",
      },
    ],
    views: [
      { id: "invoice-items", label: "Chi tiết dịch vụ", dataKey: "items", columns: [
        { key: "description", label: "Dịch vụ" }, { key: "sourceType", label: "Nguồn" },
        { key: "unitPrice", label: "Đơn giá", format: "money" }, { key: "quantity", label: "Số lượng" },
      ] },
      { id: "invoice-payments", label: "Các khoản đã thu", dataKey: "payments", columns: [
        { key: "receiptNumber", label: "Biên nhận" }, { key: "amount", label: "Đã nhận", format: "money" },
        { key: "id", label: "Chi tiết khoản thu", module: "receipts" },
        { key: "method", label: "Phương thức", format: "status" },
        { key: "recordedAt", label: "Thời điểm ghi nhận", format: "datetime" },
      ], actions: [{ id: "payment-receipt", label: "Tải biên nhận", scope: "record", method: "DOWNLOAD",
        path: "/payments/{id}/receipt", permission: "invoice.read", help: "Tải biên nhận do hệ thống cung cấp." }] },
      { id: "invoice-refunds", label: "Lịch sử hoàn tiền", dataKey: "refunds", columns: refundColumns, actions: refundActions },
    ],
  },
  {
    id: "receipts", title: "Biên nhận thanh toán", group: "Tài chính",
    description: "Tra cứu khoản tiền đã được ghi nhận và tham chiếu biên nhận gốc.",
    permission: "invoice.read", endpoint: "/payments/{id}/receipt", single: true,
    queryFields: [uuid("id", "Mã khoản thanh toán")],
    columns: [{ key: "receiptNumber", label: "Số biên nhận" }, { key: "amount", label: "Đã nhận", format: "money" },
      { key: "method", label: "Phương thức", format: "status" },
      { key: "recordedAt", label: "Thời điểm ghi nhận", format: "datetime" },
      { key: "invoiceId", label: "Hóa đơn", module: "invoices" }, { key: "recordedBy", label: "Người ghi nhận" }],
    help: "Biên nhận là bằng chứng khoản thu đã ghi nhận trong hệ thống; không phải hóa đơn điện tử hay xác nhận của ngân hàng.",
    actions: [{ id: "download-receipt", label: "Tải biên nhận", scope: "record", method: "DOWNLOAD",
      path: "/payments/{id}/receipt", permission: "invoice.read" }],
  },
  {
    id: "refunds", title: "Tra cứu hoàn tiền", group: "Tài chính",
    description: "Kiểm tra trạng thái một yêu cầu hoàn và xử lý theo quyền được cấp.",
    permission: "invoice.read", endpoint: "/refunds/{id}", single: true,
    queryFields: [uuid("id", "Mã yêu cầu hoàn tiền")], columns: refundColumns,
    detailFields: [reference, status, { key: "paymentId", label: "Khoản thanh toán gốc" },
      { key: "amount", label: "Số tiền đề nghị", format: "money" }, { key: "reason", label: "Lý do" },
      { key: "requestedBy", label: "Người yêu cầu" }, { key: "approvedBy", label: "Người phê duyệt" },
      { key: "completedAt", label: "Thời điểm hoàn tất", format: "datetime" },
      { key: "invoice.balance", label: "Số dư sau hoàn tất", format: "money" }],
    actions: refundActions,
    help: "Mã yêu cầu có trong chi tiết hóa đơn. Đang chờ duyệt hoặc đã duyệt chưa phải hoàn tiền hoàn tất; số dư sau hoàn chỉ có khi hệ thống đã thực hiện thành công.",
  },
  {
    id: "prices", title: "Bảng giá dịch vụ", group: "Quản trị",
    description: "Quản lý các khoảng giá có hiệu lực và giữ nguyên giá lịch sử.",
    permission: "price.manage", endpoint: "/service-prices",
    queryFields: [{ ...uuid("serviceId", "Dịch vụ"), lookup: { path: "/services", labelKey: "name", permission: "schedule.read" } }],
    columns: [reference, { key: "amount", label: "Giá dịch vụ", format: "money" },
      { key: "effectiveFrom", label: "Có hiệu lực từ", format: "datetime" },
      { key: "effectiveTo", label: "Kết thúc trước", format: "datetime" }],
    actions: [
      { id: "create-price", label: "Thêm khoảng giá", scope: "collection", path: "/service-prices", permission: "price.manage",
        fields: [{ ...uuid("serviceId", "Dịch vụ"), lookup: { path: "/services", labelKey: "name", permission: "schedule.read" } },
          money("amount", "Giá dịch vụ"), currency,
          { name: "effectiveFrom", label: "Có hiệu lực từ", type: "datetime", required: true },
          { name: "effectiveTo", label: "Kết thúc trước (nếu có)", type: "datetime" }],
        help: "Các khoảng giá của cùng dịch vụ không được chồng nhau. Giá lịch sử đã dùng cho hóa đơn không bị thay đổi." },
      { id: "end-price", label: "Kết thúc khoảng giá", scope: "record", path: "/service-prices/{id}/end",
        permission: "price.manage", when: { field: "effectiveTo", values: [null] },
        fields: [{ name: "effectiveTo", label: "Thời điểm kết thúc trong tương lai", type: "datetime", required: true }],
        confirm: "Kết thúc khoảng giá đang mở tại thời điểm đã chọn? Giá trị của khoảng giá được giữ nguyên." },
    ],
  },
  {
    id: "clinic-policy", title: "Chính sách cơ sở", group: "Quản trị",
    description: "Cấu hình ngưỡng tự hoàn tiền và quy trình duyệt ra về.", permission: "policy.manage",
    endpoint: "/clinic-policy", single: true,
    columns: [{ key: "autoRefundLimit", label: "Ngưỡng tự hoàn tiền", format: "money" },
      { key: "dischargeApprovalRequired", label: "Cần duyệt ra về", format: "boolean" }, { key: "version", label: "Phiên bản" }],
    actions: [{ id: "update-policy", label: "Cập nhật chính sách", scope: "record", method: "PUT", path: "/clinic-policy",
      permission: "policy.manage", fields: [money("autoRefundLimit", "Ngưỡng tự hoàn tiền", 0),
        { name: "dischargeApprovalRequired", label: "Yêu cầu phê duyệt ra về trước khi kết thúc lượt", type: "checkbox" }, version],
      confirm: "Áp dụng chính sách này cho các thao tác tiếp theo tại cơ sở?",
      help: "Ngưỡng bằng 0 yêu cầu mọi khoản hoàn dương qua phê duyệt. Duyệt ra về không thay thế checklist lâm sàng." }],
  },
  {
    id: "merges", title: "Hợp nhất hồ sơ", group: "Hồ sơ & kiểm soát",
    description: "Theo dõi yêu cầu xác minh hai hồ sơ cùng một người bệnh.",
    permission: ["merge.request", "merge.execute"], endpoint: "/merge-requests", paginated: true,
    columns: [reference, { key: "sourceId", label: "Hồ sơ nguồn", module: "patients" },
      { key: "targetId", label: "Hồ sơ giữ lại", module: "patients" }, status,
      { key: "createdAt", label: "Ngày yêu cầu", format: "datetime" }],
    detailFields: [reference, status, { key: "sourceId", label: "Hồ sơ nguồn" }, { key: "targetId", label: "Hồ sơ giữ lại" },
      { key: "reason", label: "Lý do yêu cầu" }, { key: "verificationReason", label: "Kết quả xác minh" }],
    help: "Giữ lại hồ sơ nguồn và lịch sử. Giải quyết lượt khám đang mở hoặc lịch hẹn liên quan trước khi duyệt hợp nhất.",
    actions: [
      { id: "request-merge", label: "Tạo yêu cầu hợp nhất", scope: "collection", path: "/merge-requests",
        permission: "merge.request", idempotent: true,
        fields: [uuid("sourceId", "Mã hồ sơ nguồn"), uuid("targetId", "Mã hồ sơ giữ lại"), reason(2000)],
        help: "Lễ tân tạo yêu cầu; người có quyền xác minh hợp nhất mới được quyết định." },
      { id: "decide-merge", label: "Xác minh và quyết định", scope: "record", path: "/merge-requests/{id}/decision",
        permission: "merge.execute", idempotent: true, danger: true, when: { field: "status", values: ["PENDING"] },
        fields: [version, approval,
          { name: "identityVerified", label: "Đã xác minh hai hồ sơ là cùng một người bệnh", type: "checkbox", defaultValue: false,
            help: "Bắt buộc xác minh khi phê duyệt; từ chối không yêu cầu đánh dấu." }, reason(2000)],
        confirm: "Lưu quyết định xác minh? Hợp nhất được phê duyệt giữ lịch sử nguồn và yêu cầu đối chiếu lại hồ sơ an toàn thuốc." },
    ],
  },
  {
    id: "discharge-approvals", title: "Duyệt ra về", group: "Hồ sơ & kiểm soát",
    description: "Ghi quyết định cho yêu cầu ra về theo chính sách của cơ sở.",
    permission: "discharge.approve", columns: [],
    help: "Dùng mã yêu cầu và phiên bản được bác sĩ cung cấp. Hệ thống hiện chưa có danh sách tra cứu yêu cầu ra về; phê duyệt không tự kết thúc lượt khám.",
    actions: [{ id: "decide-discharge", label: "Ghi quyết định ra về", scope: "collection",
      path: "/discharge-requests/{id}/decision", permission: "discharge.approve",
      fields: [uuid("id", "Mã yêu cầu ra về"), version, approval, reason(2000)],
      body: ({ version, action, reason }) => ({ version, action, reason }),
      confirm: "Lưu quyết định cho yêu cầu ra về này? Người yêu cầu không được tự phê duyệt." }],
  },
  {
    id: "audit", title: "Nhật ký kiểm toán", group: "Hồ sơ & kiểm soát",
    description: "Tra cứu ai đã thực hiện thao tác và thời điểm xảy ra.", permission: "audit.read",
    endpoint: "/audit-events", paginated: true,
    queryFields: [...period, uuid("actor", "Mã người thực hiện (nếu lọc)", false),
      uuid("target", "Mã đối tượng (nếu lọc)", false),
      { name: "action", label: "Mã thao tác (nếu lọc)", maxLength: 64, pattern: "[A-Z0-9_]{1,64}" }],
    columns: [{ key: "occurredAt", label: "Thời điểm", format: "datetime" }, { key: "action", label: "Thao tác" },
      { key: "targetType", label: "Loại đối tượng" }, { key: "actorId", label: "Người thực hiện" }, { key: "targetId", label: "Đối tượng" }],
    detailFields: [reference, { key: "occurredAt", label: "Thời điểm", format: "datetime" },
      { key: "action", label: "Thao tác" }, { key: "actorId", label: "Người thực hiện" },
      { key: "targetType", label: "Loại đối tượng" }, { key: "targetId", label: "Đối tượng" },
      { key: "relatedId", label: "Tham chiếu liên quan" }, { key: "detail", label: "Chi tiết sự kiện" }],
    help: "Nhật ký chỉ đọc. Quyền kiểm toán không cho phép sửa hoặc xóa bằng chứng, hay đọc nội dung lâm sàng ngoài phạm vi.",
  },
  {
    id: "patient-audit", title: "Lịch sử kiểm toán hồ sơ", group: "Hồ sơ & kiểm soát",
    description: "Các sự kiện liên quan một hồ sơ và các lượt khám, đơn, dịch vụ, tài chính của hồ sơ đó.",
    permission: "audit.read", endpoint: "/patients/{patientId}/audit-events", paginated: true,
    queryFields: [uuid("patientId", "Mã hồ sơ bệnh nhân")],
    columns: [{ key: "occurredAt", label: "Thời điểm", format: "datetime" }, { key: "action", label: "Thao tác" },
      { key: "targetType", label: "Loại đối tượng" }, { key: "targetId", label: "Đối tượng" }, { key: "actorId", label: "Người thực hiện" }],
  },
  {
    id: "operations", title: "Báo cáo vận hành", group: "Báo cáo",
    description: "Số lượt khám theo khoảng thời gian và phạm vi được phân công.", permission: "report.read",
    endpoint: "/reports/operations", single: true, queryFields: period,
    columns: [{ key: "encounters", label: "Lượt khám" }, { key: "completed", label: "Đã kết thúc" },
      { key: "waiting", label: "Đang chờ khám" }, { key: "awaitingService", label: "Chờ dịch vụ" },
      { key: "scope", label: "Phạm vi thống kê", format: "status" },
      { key: "from", label: "Từ thời điểm", format: "datetime" }, { key: "to", label: "Đến trước", format: "datetime" }],
    help: "Bác sĩ và điều dưỡng chỉ thấy lượt được gán. Báo cáo không cung cấp thời gian chờ trung bình hay tỷ lệ tăng trưởng.",
  },
  {
    id: "revenue", title: "Báo cáo thu chi", group: "Báo cáo",
    description: "Tiền đã nhận, hoàn đã hoàn tất và dòng tiền ròng trong kỳ.",
    allPermissions: ["report.read", "invoice.read"], endpoint: "/reports/revenue", single: true, queryFields: period,
    columns: [{ key: "paymentsReceived", label: "Tiền đã nhận", format: "money" },
      { key: "refundsCompleted", label: "Tiền đã hoàn", format: "money" }, { key: "netCash", label: "Dòng tiền ròng", format: "money" },
      { key: "from", label: "Từ thời điểm", format: "datetime" }, { key: "to", label: "Đến trước", format: "datetime" }],
    help: "Ghi nhận theo thời điểm nhận tiền và hoàn tất hoàn tiền, không suy doanh thu từ số lượt khám.",
    actions: [
      { id: "export-revenue", label: "Xuất thu chi CSV", scope: "collection", method: "DOWNLOAD",
        path: "/reports/revenue.csv", allPermissions: ["report.export", "invoice.read"], fields: period },
      { id: "export-invoices", label: "Xuất trang hóa đơn CSV", scope: "collection", method: "DOWNLOAD",
        path: "/reports/invoices.csv", allPermissions: ["report.export", "invoice.read"],
        fields: [...period, { name: "page", label: "Trang (bắt đầu từ 0)", type: "number", min: 0, required: true, defaultValue: 0 },
          { name: "size", label: "Số dòng tối đa", type: "number", min: 1, max: 100, required: true, defaultValue: 100 }],
        help: "Chỉ xuất trang được chọn, tối đa 100 dòng mỗi lần; không phải toàn bộ hóa đơn của kỳ." },
    ],
  },
  {
    id: "notification-templates", title: "Mẫu thông báo", group: "Quản trị",
    description: "Mẫu nội dung cho thông báo nội bộ về lịch hẹn.",
    permission: ["notification.manage", "notification.send"], endpoint: "/notification-templates",
    columns: [{ key: "code", label: "Mã mẫu" }, { key: "body", label: "Nội dung" },
      { key: "enabled", label: "Đang sử dụng", format: "boolean" }],
    help: "Chỉ có kênh nội bộ. Mẫu hỗ trợ {{appointmentId}} và {{startsAt}}; không thêm dữ liệu sức khỏe nhạy cảm.",
    actions: [
      { id: "create-template", label: "Tạo mẫu thông báo", scope: "collection", path: "/notification-templates", permission: "notification.manage",
        fields: [{ name: "code", label: "Mã mẫu", required: true, maxLength: 40, pattern: "[A-Z0-9_-]{1,40}" },
          { name: "body", label: "Nội dung mẫu", type: "textarea", required: true, maxLength: 2000,
            help: "Biến được phép: {{appointmentId}}, {{startsAt}}." }, enabled] },
      { id: "update-template", label: "Sửa mẫu thông báo", scope: "record", method: "PUT", path: "/notification-templates/{id}",
        permission: "notification.manage", fields: [
          { name: "code", label: "Mã mẫu", required: true, maxLength: 40, pattern: "[A-Z0-9_-]{1,40}" },
          { name: "body", label: "Nội dung mẫu", type: "textarea", required: true, maxLength: 2000 }, enabled, version] },
    ],
  },
  {
    id: "notifications", title: "Thông báo đã gửi", group: "Tiếp nhận",
    description: "Theo dõi hàng đợi chuyển thông báo nội bộ đến nhân sự.", permission: "notification.send",
    endpoint: "/notifications", paginated: true,
    columns: [{ key: "body", label: "Nội dung" }, status, { key: "recipientId", label: "Người nhận" },
      { key: "createdAt", label: "Ngày tạo", format: "datetime" }, { key: "attempts", label: "Số lần thử" }],
    help: "Đưa vào hàng đợi chưa có nghĩa đã chuyển đến hộp thư. Xem trạng thái gửi; không có SMS hay email trong luồng này.",
    actions: [{ id: "queue-notification", label: "Gửi thông báo nội bộ", scope: "collection", path: "/notifications",
      permission: "notification.send", idempotent: true, fields: [
        { ...uuid("templateId", "Mẫu thông báo"), lookup: { path: "/notification-templates", labelKey: "code", permission: "notification.send" } },
        uuid("appointmentId", "Mã lịch hẹn"), { ...uuid("recipientId", "Mã nhân sự nhận tại cơ sở"),
          help: "Nhập mã nhân sự đã được xác nhận. Hệ thống chưa cung cấp danh bạ nhân sự cho vai trò lễ tân." }],
      confirm: "Đưa thông báo lịch hẹn vào hàng đợi chuyển đến nhân sự được chọn?" }],
  },
  {
    id: "inbox", title: "Hộp thư nội bộ", group: "Công việc",
    description: "Thông báo đã được chuyển đến tài khoản của bạn tại cơ sở này.", endpoint: "/inbox", paginated: true,
    columns: [{ key: "body", label: "Nội dung" }, { key: "createdAt", label: "Thời điểm", format: "datetime" },
      { key: "readAt", label: "Đã đọc lúc", format: "datetime" }],
    actions: [{ id: "read-notification", label: "Đánh dấu đã đọc", scope: "record", path: "/inbox/{id}/read",
      when: { field: "readAt", values: [null] }, body: () => undefined }],
  },
  {
    id: "diagnosis-admin", title: "Cấu hình chẩn đoán", group: "Quản trị",
    description: "Thêm hoặc cập nhật một mã chẩn đoán thuộc cơ sở.", permission: "catalog.manage", columns: [],
    help: "Quyền cấu hình không cho phép đọc danh mục lâm sàng qua giao diện bác sĩ. Chỉ cập nhật mã thuộc cơ sở; mã dùng chung hoặc của cơ sở khác được bảo vệ.",
    actions: [{ id: "save-diagnosis", label: "Lưu mã chẩn đoán", scope: "collection", method: "PUT", path: "/diagnosis-catalog",
      permission: "catalog.manage", fields: [
        { name: "code", label: "Mã chẩn đoán", required: true, maxLength: 32, pattern: "[A-Z0-9._-]{1,32}" },
        { name: "name", label: "Tên chẩn đoán", required: true, maxLength: 200 }, enabled],
      help: "Dữ liệu minh họa trong hệ thống không phải danh mục ICD được phê duyệt cho người bệnh thật." }],
  },
];
