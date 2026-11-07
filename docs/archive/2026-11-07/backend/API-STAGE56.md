# MEDORA API — Stage 5 và 6

Cập nhật 2026-10-31, Asia/Saigon. Nhánh `feature`, mục tiêu commit
`api chua co front`. Backend Stage 1–6 đã có source; frontend chưa được tạo.
Các quyết định dưới áp dụng cho MVP nội bộ và dữ liệu giảng dạy. Báo cáo/UML
cũ vẫn là snapshot thiết kế; không có lượt render/review diagram mới trong phiên này.

## Chạy

Từ `src/`, cấu hình `.env` theo `.env.example`, đặt mật khẩu riêng cho DB và
bootstrap admin, rồi chạy:

```powershell
docker compose up --build -d
Invoke-RestMethod http://127.0.0.1:8080/ready
python scripts/demo_full_api.py
```

Môi trường test đang có dùng project `medora-stage4-test` và `.env.docker-test`:

```powershell
docker compose -p medora-stage4-test --env-file .env.docker-test ps
python scripts/demo_full_api.py --env-file .env.docker-test
```

Tên project test được giữ để nâng cấp volume Stage 4 hiện có; API trong đó đã
chạy đến Stage 6. Mỗi lần demo tạo thêm dữ liệu giả lập. Script chạy cả thuốc,
dịch vụ, tệp, tài chính, report/audit và merge, kiểm tra tổng tiền và xuất JSON
PASS cùng ID. Trong fixture cấp thuốc, timestamp lấy từ bản đơn do server trả
để tránh lệch đồng hồ Windows/Docker; API vẫn chặn sự kiện thực hiện trước đơn.

Từ `src/backend/`:

```powershell
mvn -B -ntp test
mvn -B -ntp -Ppostgres-it verify
```

Bộ test gồm 8 security tests, 10 Stage4IT và 15 Stage56IT. PostgreSQL thật và
Tomcat cổng ngẫu nhiên được dùng trong integration tests. Migration V1–V7 chạy
trên DB mới; môi trường Compose đã được nâng từ V4 lên V7 mà không xóa volume.

## Cấu trúc và quyền

Package mới: `care` (catalog thuốc, đơn và dịch vụ), `attachment`, `finance`,
`governance`; giữ các module Stage 1–4. `Store` là gateway JDBC với tham số bind,
chung datasource/transaction manager với JPA. Service giữ ranh giới transaction,
quyền, trạng thái và khóa; controller nhận DTO có validation. Đây là lựa chọn
MVP để các command SQL và constraint PostgreSQL nằm trong cùng một transaction;
không tạo service/container riêng cho từng use case.

Các role bổ sung ánh xạ actor canonical:

| Role code | Trách nhiệm |
| --- | --- |
| TECHNICIAN | Thực hiện dịch vụ được gán, tải/đọc tệp gắn với dịch vụ đó |
| PHARMACIST | Đọc đơn được gán và ghi nhận cấp/dùng thuốc |
| CASHIER | Tạo/phát hành hóa đơn, ghi thanh toán và xuất biên nhận; không hoàn tiền |
| FINANCE | Giảm giá có lý do, yêu cầu/thực hiện hoàn tiền và duyệt yêu cầu của người khác |
| CLINIC_MANAGER | Danh mục/luật, bảng giá, policy, report, duyệt hoàn tiền/ra về |
| AUTHORIZED_REGISTRAR | Xác minh và thực hiện hợp nhất hồ sơ |
| AUDITOR | Chỉ đọc báo cáo/audit/hóa đơn theo cơ sở |

Doctor/Nurse tiếp tục cần quan hệ phân công Encounter. Admin cấu hình catalog,
template, tài khoản và xem audit; quyền cấu hình không cho phép đọc nội dung khám.
Cashier/Finance không được đọc note lâm sàng. Các thao tác ngoài cơ sở bị chặn
phía server. Nhật ký tự động không phải quyền để Auditor sửa/xóa log.

## Chính sách đã cụ thể hóa

**Thuốc.** Catalog và rules theo cơ sở, có enabled/version/source. Chưa có bộ
dữ liệu tương tác thuốc bên ngoài. Trước phát hành phải có policy đang bật và
profile dị ứng có trạng thái rõ ràng; UNKNOWN hoặc thiếu nguồn chặn phát hành.
Mã hoạt chất/dị ứng/chống chỉ định được đối chiếu chính xác, không đoán bệnh hay
trích xuất luật từ tiền sử tự do. Luật INTERACTION so thuốc mới với thuốc trong
profile và đơn còn hiệu lực; CONTRAINDICATION so với mã tình trạng cấu hình.
Dị ứng hoạt chất là chặn cứng. WARN cần xác nhận; BLOCK chỉ override khi chính
luật cho phép, bác sĩ có quyền và ghi lý do. Server tính lại cảnh báo lúc issue.

Đơn có DRAFT/ISSUED/CANCELED/SUPERSEDED. Items lưu snapshot tên/hoạt chất/đơn vị
và đủ liều, đường, tần suất, số ngày, số lượng, hướng dẫn. Đính chính tạo draft
kế tiếp và kiểm tra lại an toàn; khi phát hành bản mới, bản cũ thành SUPERSEDED,
giữ nguyên nội dung. Hủy có lý do riêng. Trigger giữ nguyên evidence đã phát hành.
Issue không tạo medication event. PHARMACIST được gán mới ghi DISPENSED hoặc
ADMINISTERED, có người/thời điểm, giới hạn số lượng theo từng loại sự kiện và
idempotency. Đây là hai loại sự kiện riêng, không suy ra loại này từ loại kia.

**Dịch vụ.** Doctor tạo order có lý do, mức ưu tiên, chuẩn bị, required và loại
PROCEDURE/LAB/IMAGING. Chưa có chỗ thì PENDING_ALLOCATION và không giữ booking.
Phân bổ kiểm tra ca làm/nghỉ, năng lực nhân sự, phòng đang bật, loại phòng,
thiết bị, thời lượng và đệm. Exclusion constraint chung bảo vệ cả lịch khám và
lịch dịch vụ; đổi lịch thất bại không làm mất phân bổ cũ. Start phải trong slot;
chỉ nhân sự được gán thực hiện. Complete cần kết quả và lưu thời điểm/người;
delay/cancel/not performed cần lý do, giải phóng nguồn lực. Doctor review kết quả
hoặc lý do không thực hiện trước khi quay về khám và đóng lượt. Tệp xét nghiệm/
hình ảnh nhập tay, không khẳng định có kết nối LIS/PACS.

**Đóng lượt.** Bổ sung chặn khi còn đơn nháp, order chưa giải quyết hoặc chưa
được bác sĩ review. Policy cơ sở có thể yêu cầu duyệt ra về (mặc định tắt cho
MVP ngoại trú); yêu cầu và quyết định được lưu riêng. Duyệt ra về không bỏ qua
note/chẩn đoán/sinh hiệu và checklist công việc. Thanh toán không sửa nội dung
lâm sàng đã ký hoặc mở lại lượt đã đóng.

**Tệp.** Tối đa 10 MiB, PDF/PNG/JPEG/UTF-8 plain text có kiểm tra nội dung cơ bản;
không nhận HTML/SVG/script. Filename được chuẩn hóa, storage key ngẫu nhiên,
SHA-256 và metadata trong DB; nội dung không đặt trong thư mục public. Download
kiểm tra lại quyền và checksum, trả attachment/no-store/nosniff. Archive cần
version/lý do, giữ bản lưu; upload rollback dọn object chưa commit khi có thể.
Chưa có antivirus/CDR, nên đây không phải cam kết mọi PDF/image đều an toàn.

Driver filesystem là môi trường dev và dùng volume `attachment_data`. Driver
`MEDORA_STORAGE_DRIVER=s3` dùng AWS SDK Java 2.x, bucket private và tùy chọn S3
endpoint/region/access key/secret qua env; server truyền file sau authorization,
không trả public URL. S3 adapter đã biên dịch; phiên này kiểm chứng driver local,
chưa kết nối bucket S3 bên ngoài. Không commit credential. Backup thực tế cần
backup đồng bộ DB và object store; recreate volume không thay thế restore test.

**Giá/hóa đơn.** Tiền VND số nguyên, không dùng float; chưa có thuế/hóa đơn điện
tử/gateway ngoài. Giá có khoảng hiệu lực không chồng nhau; có thể kết thúc một
khoảng đang mở trong tương lai rồi thêm giá mới. Không sửa số tiền giá lịch sử.
Hóa đơn từ lượt Completed, gồm consultation của appointment (hoặc service do
bác sĩ chọn cho walk-in) và các order Completed. Giá áp dụng tại createdAt của
lượt hoặc completedAt của order; line lưu snapshot giá và nguồn. Một Encounter
tối đa một Invoice. Finance giảm giá khi Draft, cần lý do; Cashier phát hành.

Payment append-only, cho thanh toán một phần và không vượt số dư. Phương thức
CASH/BANK_TRANSFER là ghi nhận nội bộ sau xác minh đã nhận tiền, không gọi ngân
hàng. Retry cùng key/body trả kết quả gốc; đổi body cùng key bị 409. Ledger net
paid = tổng Payment − Refund Completed; trạng thái Invoice tính từ ledger đó.

Refund tham chiếu Payment gốc, có reason và số tiền riêng. Mặc định ngưỡng tự
hoàn là 0 nên mọi refund cần duyệt. Policy do Manager đặt có thể cho tự hoàn
dưới ngưỡng. Pending/Approved giữ phần tiền yêu cầu để tránh hai yêu cầu tranh
vượt Payment, nhưng chưa đổi số dư Invoice. Người duyệt phải khác requester;
chỉ khi execute thành Completed mới trả số dư kết quả. Reject giải phóng phần
giữ. Payment gốc không đổi. Trigger giữ payment/refund hoàn tất/invoice đã phát
hành và log phê duyệt; owner DB có thể thay đổi schema, không phải kho chống
can thiệp của quản trị viên DB.

**Hợp nhất.** Reception chỉ tạo yêu cầu; Registrar xác minh và thực hiện. Chặn
khi còn lượt mở hoặc lịch source chưa giải quyết; hai patient phải trong cùng
cơ sở, còn canonical và khác nhau. Source vẫn tồn tại, trỏ mergedIntoId tới
target; không chuyển/sửa signed rows hay ledger. Timeline/history ở target đọc
cả chuỗi nguồn, giữ ID gốc. Check-in/đặt lịch/sửa hành chính source bị chặn để
dùng canonical ID. Profile an toàn của target chuyển UNKNOWN để đối chiếu lại,
không tự hợp nhất dữ liệu dị ứng thành một quyết định lâm sàng.

**Báo cáo/audit/thông báo.** Cashflow lấy Payment theo recordedAt và Refund
Completed theo completedAt, cùng cơ sở và khoảng thời gian nửa mở. Dashboard
Doctor/Nurse giới hạn lượt được gán; báo cáo tiền cần invoice.read. CSV có
escape và chống formula injection, export có audit. Audit có filter/page/size,
gộp identity và nghiệp vụ; patient audit lần theo hồ sơ/lượt/đơn/dịch vụ/tài chính.
Tài khoản thường không có endpoint sửa/xóa evidence.

Thông báo hiện có kênh IN_APP cho nhân sự nội bộ, template với placeholder
appointmentId/startsAt, queue có idempotency và worker retry tối đa 5 lần.
Template/recipient không hợp lệ làm job Failed, không làm mất Appointment đã
commit. Inbox chỉ của người nhận. SMS/email và portal bệnh nhân vẫn ngoài phạm
vi tích hợp hiện tại; không gửi thông báo ra ngoài bằng tài khoản cá nhân.

## API bổ sung

Tiền tố `F=/api/facilities/{facility}`. Dùng session cookie và CSRF như
[Stage 1–4](STAGE4.md). Các POST/PUT bên dưới mặc định trả 200; tạo tài nguyên
Stage 1–4 giữ mã phản hồi cũ. Mutation có version hoặc Idempotency-Key như bảng.
Key dài 8–100 ký tự `[A-Za-z0-9._-]`; retry dùng cùng body ban đầu.

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

Profile coded lists được lưu dạng chuỗi phân cách dấu phẩy và trả cùng tên
trường. Enum/mã chỉ cho phép tập ký tự được định nghĩa; không suy ra tương tác
từ tên thuốc. Các request mẫu đầy đủ nằm trong script demo và integration tests.

## Trace và phần chưa bao gồm

| Nhóm | FR / UC | Bằng chứng |
| --- | --- | --- |
| Thuốc | FR-011–013/028; UC-043–052 | medication policy/profile/rules, prescription, medication_event, safety/amendment/dispense tests |
| Dịch vụ | FR-014–019; UC-039–040/056–057 | order, resource_booking chung, execution/result/review; conflict/rollback tests |
| Tệp | FR-029; UC-015 | private storage/metadata/authorization/validation/archive và multipart tests |
| Kết thúc | FR-020–021; UC-041–042 | checklist mở rộng và optional discharge approval tests |
| Tài chính | FR-022–023; UC-060–069 | effective prices, snapshots, discount, ledger, idempotency, refund approval/concurrency tests |
| Hồ sơ/audit | FR-004/030; UC-016–019/077–079 | merge link/retention, audit, revoke-session tests |
| Báo cáo/thông báo | FR-024/026–028; UC-054/072–076 | in-app outbox, report/CSV/audit tests |

Phase 2 portal, tích hợp ngoài, billing/bảo hiểm/hóa đơn điện tử, drug knowledge
database, production backup/restore, load/SLA và giao diện không nằm trong phần
đã xác minh. Không đánh dấu toàn bộ 34 FR/16 NFR hoặc tất cả TC trong báo cáo là
Passed chỉ từ bộ test backend. Evidence thực thi và việc dọn file được lưu ở
[STAGE56-VERIFICATION.md](STAGE56-VERIFICATION.md).

Nguồn API đã đối chiếu: [AWS SDK Java 2.x S3](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_s3_code_examples.html),
[Spring MVC multipart](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/multipart-forms.html).
