# MEDORA API

API REST của backend Spring Boot. Frontend gọi cùng origin qua `/api/*`; Next.js chuyển tiếp đến API. Không cần JWT hoặc CORS cho cách chạy mặc định.

## Phiên đăng nhập

1. `GET /api/auth/csrf`, giữ cookie và lấy `headerName`/`token`.
2. `POST /api/auth/login` với `application/x-www-form-urlencoded`, trường `username`, `password` và header CSRF nhận ở bước trước. Thành công trả **204**, không có JSON.
3. Lấy CSRF mới sau đăng nhập; đọc `/api/auth/me` và `/api/auth/permissions?facilityId=...`.
4. Các mutation tiếp theo gửi cookie phiên và CSRF. Logout là `POST /api/auth/logout`, trả 204.

`JSESSIONID` có HttpOnly, SameSite=Lax; phiên idle 30 phút. Đổi mật khẩu/quyền/trạng thái hoặc thu hồi phiên có thể kết thúc phiên đích. Nhiều tab cùng profile dùng chung cookie. Trình duyệt không lưu mật khẩu hoặc token vào localStorage.

## Quy ước

- `F = /api/facilities/{facility}`; `A = /api/admin/facilities/{facility}`. Dùng UUID thực, không dùng username hoặc mã bệnh nhân thay UUID.
- Quyền trong menu không thay thế kiểm tra server. Doctor/Nurse cần được phân công; Technician/Pharmacist chỉ xử lý công việc được gán. Admin không tự có quyền đọc hồ sơ khám.
- DTO và controller trong `backend/src/main/java/vn/medora/api/` là hợp đồng cuối cùng. Bảng dưới tóm tắt trường, không thay thế validation trong DTO.
- Trường `version` là optimistic locking: gửi phiên bản vừa đọc; lỗi `STALE_VERSION` cần tải lại và đối chiếu trước khi thử lại.
- Với API có `Idempotency-Key`, giữ cùng key và body cho cùng ý định. Sửa body cùng key bị conflict; không tự gửi lại mutation sau lỗi mạng chưa rõ kết quả.
- Instant dùng ISO-8601 có offset/UTC; frontend chuyển từ giờ cơ sở. Ngày sinh giữ dạng `YYYY-MM-DD`. Tiền VND là số nguyên.
- Mảng, object và `PageView {items,page,size,totalElements}` là ba kiểu khác nhau; không giả định mọi list đều trả tổng số.
- Lỗi thường gặp: 400 validation, 401 chưa có phiên, 403 thiếu quyền/CSRF/phân công, 404 không có tài nguyên, 409 trạng thái/version/xung đột. Các lệnh thành công có thể trả 200, 201 hoặc 204 tùy controller.
- Tệp private: multipart part `file`, tối đa 10 MiB, PDF/PNG/JPEG/UTF-8 text; download qua endpoint được phân quyền. Receipt/prescription export hiện là **JSON**, báo cáo là **CSV**.

## Nhận diện, hồ sơ, lịch và khám


| Route | Quyền và request |
| --- | --- |
| GET `/api/auth/csrf` | Công khai; lưu cookie và lấy `headerName/token` |
| POST `/api/auth/login` | CSRF + form `username,password`; 204; lấy CSRF mới sau login |
| GET `/api/auth/me` | Phiên hợp lệ; ID/name/facilityIds, không secret |
| GET `/api/auth/permissions?facilityId=...` | Quyền trong scope hiện tại |
| POST `/api/auth/logout` | CSRF; hủy phiên/cookie; 204 |
| GET/POST `A/accounts` | Admin; tạo với username/displayName/password/roles |
| GET/PUT `A/accounts/{id}` | Admin; update displayName/status/version |
| GET `A/roles` | Admin có account.assign_role |
| PUT `A/accounts/{id}/roles` | roles/version; thu hồi phiên đích |
| PUT `A/accounts/{id}/password` | password/version; thu hồi phiên đích |
| GET `F/patients?q=&page=0&size=20` | Reception/Nurse/Doctor; response items/page/size/totalElements |
| GET/PUT `F/patients/{id}` | Đọc hành chính; Reception sửa với version |
| POST `F/patients/duplicate-check` | Reception; cùng body với đăng ký; trả ứng viên nghi trùng |
| POST `F/patients` | fullName/dateOfBirth/sex; tùy chọn phone/nationalId/address/duplicateReviewed/duplicateReviewReason |
| GET/POST `F/rooms`, PUT `F/rooms/{id}` | Đọc schedule.read; ghi Admin; code/name/roomType/equipment/enabled/version |
| GET/POST `F/services`, PUT `F/services/{id}` | code/name/durationMinutes/bufferMinutes/enabled/version |
| POST `F/provider-services` | Admin; providerId/serviceId; 204 |
| GET `F/staff-schedules?from=&to=` | schedule.read; ca WORK/ABSENCE |
| POST `F/staff-schedules` | Admin; providerId/kind/startsAt/endsAt |
| GET `F/availability?providerId=&roomId=&serviceId=&from=&to=` | schedule.read; slot tham khảo, booking kiểm tra lại |
| GET `F/appointments?from=&to=&page=0&size=20` | Reception đọc scope; Doctor chỉ lịch của mình; không lộ danh sách patient cho Admin |
| POST `F/appointments` | Reception; header Idempotency-Key 8–100 ký tự `[A-Za-z0-9._-]`; patientId/providerId/serviceId/roomId/startsAt |
| PUT `F/appointments/{id}/schedule` | Reception; providerId/serviceId/roomId/startsAt/version/reason |
| POST `F/appointments/{id}/transitions` | Reception; action CONFIRM/CANCEL/NO_SHOW, version; CANCEL cần reason |
| GET `F/appointments/{id}/events` | Reception; lịch sử trạng thái |
| POST `F/check-ins` | Reception; patientId/doctorId/identityVerified, tùy chọn appointmentId/nurseId |
| GET `F/queue?page=0&size=20` | Reception hoặc clinician được gán |
| GET `F/encounters/{id}` | Clinician được gán; có audit |
| PUT `F/encounters/{id}/nurse` | Reception; nurseId/version; null bỏ gán |
| POST `F/encounters/{id}/start` | Bác sĩ được gán; version |
| GET `F/encounters/{id}/record?page=&size=` | Clinician được gán lượt đang mở; timeline |
| GET `F/encounters/{id}/record/{visit}` | Quan hệ chăm sóc hiện tại; cùng patient; nội dung lượt cũ |
| GET/POST `F/encounters/{id}/history` | Đọc theo quan hệ chăm sóc; Doctor/Nurse ghi kind ALLERGY/HISTORY/MEDICATION, content, tùy chọn supersedesId/correctionReason |
| GET/POST `F/encounters/{id}/vitals` | Doctor/Nurse đọc theo phân công, Nurse ghi; sửa dùng supersedesId/correctionReason |
| GET `F/diagnosis-catalog` | Clinician; danh mục giả lập |
| GET/POST `F/encounters/{id}/diagnoses` | Clinician đọc; Doctor ghi code/symptoms |
| GET/POST `F/encounters/{id}/notes` | Clinician đọc; Doctor tạo nháp subjective/objective/assessment/plan |
| PUT `F/encounters/{id}/notes/{note}` | Doctor; bốn phần SOAP và version; chỉ draft |
| POST `F/encounters/{id}/notes/{note}/sign` | Doctor; version; tự xác định ký ban đầu hay ký đính chính |
| POST `F/encounters/{id}/notes/{note}/amendments` | Doctor; version của bản ký mới nhất và reason; tạo nháp kế tiếp |
| POST `F/encounters/{id}/notes/{note}/abandon` | Doctor; version của amendment draft; 204 |
| POST `F/encounters/{id}/submit-for-closure` | Doctor; version; kiểm tra checklist |
| POST `F/encounters/{id}/return-for-correction` | Doctor; version |
| POST `F/encounters/{id}/complete` | Doctor; version/summary/instructions/followUp |

## Care, finance and governance

| Route | Input / quyền chính |
| --- | --- |
| GET/POST `F/medications`, PUT `F/medications/{id}` | q/page/size; code/name/ingredientCode/unit/enabled/version; catalog.manage để ghi |
| GET/POST `F/medication-rules`, PUT `F/medication-rules/{id}` | kind INTERACTION/CONTRAINDICATION, medicationId/otherMedicationId/conditionCode, severity/overridable/message/source/enabled/version |
| GET/PUT `F/medication-policy` | source/dataVersion/enabled/version; cấu hình bởi catalog.manage |
| GET/PUT `F/encounters/{id}/medication-profile` | allergyStatus, allergyCodes, conditionCodes, currentMedicationIds, version; Doctor/Nurse được gán |
| GET/POST `F/encounters/{id}/prescriptions` | items: medicationId/dose/route/frequency/durationDays/quantity/instructions |
| GET/PUT `F/prescriptions/{id}` | Đọc có scope, sửa Draft items/version |
| GET `F/prescriptions/{id}/safety` | Cảnh báo có id/severity/source/overridable |
| POST `F/prescriptions/{id}/issue` | Key; version/pharmacistId/acknowledgedWarnings/overrideReason |
| POST `F/prescriptions/{id}/amendments`, `/cancel` | version/reason; assigned Doctor |
| GET `F/prescriptions/{id}/export` | JSON đầy đủ cho xuất/in, có audit |
| GET `F/pharmacy-queue` | PHARMACIST được gán, page/size |
| POST `F/prescriptions/{id}/medicine-events` | Key; itemId/kind/quantity/performedAt |
| PUT `F/services/{id}/requirements` | requiredRoomType/requiredEquipment/version; equipment dùng các mã cách nhau bằng dấu phẩy |
| PUT `F/encounters/{id}/consultation-service` | serviceId; Doctor chọn cho walk-in trước đóng |
| GET/POST `F/encounters/{id}/orders` | serviceId/reason/priority/preparation/required/kind |
| GET `F/procedure-queue`, `F/orders/{id}` | Nhân sự được gán hoặc clinician có scope |
| PUT `F/orders/{id}/allocation` | staffId/roomId/startsAt/version/reason |
| POST `F/orders/{id}/actions` | action START/DELAY/COMPLETE/CANCEL/NOT_PERFORMED, version, result/reason |
| POST `F/orders/{id}/review` | version/reason; Doctor được gán |
| GET/POST `F/encounters/{id}/attachments` | GET page/size; POST multipart part file, tùy chọn orderId |
| GET `F/attachments/{id}/download` | File private sau kiểm tra scope |
| POST `F/attachments/{id}/archive` | version/reason |
| GET/POST `F/service-prices` | GET serviceId; POST serviceId/amount/currency/effectiveFrom/effectiveTo |
| POST `F/service-prices/{id}/end` | effectiveTo trong tương lai; price.manage |
| POST `F/encounters/{id}/invoice` | Key; Cashier, lượt đã Completed |
| GET `F/invoices`, `F/invoices/{id}` | invoice.read; list page/size |
| POST `F/invoices/{id}/discount` | amount/reason/version; Finance |
| POST `F/invoices/{id}/issue` | version; Cashier |
| POST `F/invoices/{id}/payments` | Key; amount/currency/method/receivedVerified |
| GET `F/payments/{id}/receipt` | Biên nhận JSON, audit |
| POST `F/refunds` | Key; paymentId/amount/reason; Finance |
| GET `F/refunds/{id}` | Pending không có balance kết quả |
| POST `F/refunds/{id}/approval` | Key; version/action APPROVE hoặc REJECT/reason; người khác requester |
| POST `F/refunds/{id}/execute` | Key; version; Finance |
| GET/PUT `F/clinic-policy` | autoRefundLimit/dischargeApprovalRequired/version; Manager |
| POST `F/encounters/{id}/discharge-requests` | reason; Doctor |
| POST `F/discharge-requests/{id}/decision` | version/action/reason; Manager |
| POST `F/merge-requests` | Key; sourceId/targetId/reason; Reception |
| GET `F/merge-requests` | Reception/Registrar, page/size |
| POST `F/merge-requests/{id}/decision` | Key; version/action/identityVerified/reason; Registrar |
| GET `F/dashboard`, `F/reports/operations` | from/to; report.read và scope phân công |
| GET `F/reports/revenue`, `/revenue.csv`, `/invoices.csv` | from/to; tài chính/controlled export; invoices CSV page/size |
| GET `F/audit-events` | from/to, actor/target/action, page/size; audit.read |
| GET `F/patients/{id}/audit-events` | page/size; audit.read |
| GET/POST `F/notification-templates`, PUT `F/notification-templates/{id}` | code/body/enabled/version; Admin/Manager |
| POST `F/notifications` | Key; templateId/appointmentId/recipientId; Reception |
| GET `F/notifications` | Trạng thái queue, page/size; Reception |
| GET `F/inbox`, POST `F/inbox/{id}/read` | Người nhận, page/size |
| POST `/api/admin/facilities/{facility}/accounts/{id}/revoke-sessions` | version; Admin, kiểm tra mọi scope của account đích |
| PUT `F/diagnosis-catalog` | code/name/enabled; catalog.manage; không sửa mã shared hoặc cơ sở khác |
