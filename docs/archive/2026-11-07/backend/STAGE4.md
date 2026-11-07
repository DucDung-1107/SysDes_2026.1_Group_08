# MEDORA — triển khai backend đến Stage 4

**Mốc lịch sử Stage 4.** API đã được mở rộng đến Stage 6; xem
[API-STAGE56.md](API-STAGE56.md) và [evidence mới nhất](STAGE56-VERIFICATION.md).
Các dòng “Stage 5–6 chưa triển khai” bên dưới mô tả snapshot trước khi mở rộng.

Cập nhật ngày 2026-10-31, Asia/Saigon. Phạm vi yêu cầu mới là AI trực tiếp hoàn
thiện code đến Stage 4, thay cho chế độ chỉ hướng dẫn theo checkpoint trước đây.

## Phạm vi hiện có

| Stage | Source và hành vi |
| --- | --- |
| 1 | Spring Boot 4.1.2, Java 21, PostgreSQL 17, Flyway V1–V4, JPA validate, Dockerfile/Compose; `/health` kiểm tra tiến trình, `/ready` kiểm tra kết nối DB |
| 2 | Phiên đăng nhập/CSRF, thu hồi phiên khi đổi mật khẩu/trạng thái/quyền, quản lý tài khoản theo cơ sở, tìm/tạo/sửa bệnh nhân, kiểm tra nghi trùng và lưu lý do xác minh |
| 3 | Phòng, dịch vụ/thời lượng/đệm, năng lực bác sĩ, ca làm/nghỉ, tìm khả dụng, đặt/xác nhận/đổi/hủy/no-show, chống gửi lặp, check-in có hẹn/walk-in và hàng đợi |
| 4 | Lượt khám được phân công, tiền sử/dị ứng/thuốc đang dùng nhập tay, sinh hiệu và bản sửa, triệu chứng/chẩn đoán, nháp SOAP, ký/đính chính, tóm tắt/hướng dẫn/tái khám và đóng lượt không có dịch vụ |
| 5–6 | Chưa triển khai: thuốc/cảnh báo, chỉ định và thực hiện dịch vụ, tệp đính kèm, tài chính, hợp nhất, báo cáo và quản trị còn lại |

Đây là backend/API. Frontend Next.js chưa được tạo. UC-014 ghi thuốc đang dùng
trong tiền sử không đồng nghĩa đã có kê đơn, cảnh báo tương tác hoặc cấp thuốc.
Danh mục `DEMO-001` là dữ liệu giảng dạy giả lập, không phải ICD hay danh mục
được duyệt để dùng cho người bệnh thật. Không có tuyên bố sẵn sàng vận hành thật.

## Chạy bằng Docker Compose

**Docker đã được kiểm tra ngày 2026-10-31:** image build/run thành công, 18 tests
trên Linux qua, demo HTTP và persistence qua recreate đều PASS. Hiện môi trường
test `medora-stage4-test` đang dùng port 8080; xem
[lệnh chạy lại và evidence Docker](DOCKER-VERIFICATION.md). Demo cho môi trường
này dùng `python scripts/demo_stage4.py --env-file .env.docker-test`.

Từ `src/`, sao chép `.env.example` thành `.env` nếu chưa có. Đặt
`MEDORA_BOOTSTRAP_PASSWORD` của riêng bạn: ít nhất 12 ký tự và tối đa 72 byte UTF-8.
Giữ `SPRING_PROFILES_ACTIVE=dev` để tạo `admin.demo` và cơ sở `DEMO` khi bảng tài
khoản rỗng. Bootstrap không ghi đè tài khoản, mật khẩu hay role đã có.

```powershell
docker compose up --build -d
Invoke-RestMethod http://127.0.0.1:8080/health
Invoke-RestMethod http://127.0.0.1:8080/ready
python scripts/demo_stage4.py
```

Script demo dùng Python standard library, đọc mật khẩu bootstrap từ biến môi
trường hoặc `src/.env`. Mỗi lần chạy tạo nhân sự, phòng/dịch vụ và bệnh nhân giả
lập mới; thực hiện đặt lịch và retry, xác nhận/check-in, sàng lọc, khám, ký,
đính chính, ký lại rồi đóng lượt. Kết quả là JSON `result: PASS` cùng ID để tra
cứu; không in mật khẩu. Nếu lịch demo vượt qua nửa đêm ở cơ sở, script hủy lịch
đó và dùng walk-in cho lượt khám.

Dockerfile vẫn chạy unit/security tests khi build image. Integration tests có
PostgreSQL riêng chạy qua profile bên dưới, không chạy dưới user root trong bước
build Docker. Không cần cài PostgreSQL toàn cục để chạy bộ kiểm thử.

Nếu đã có DB thử nghiệm dùng migration V2/V3 cũ, kiểm tra checksum và nội dung
schema trước khi nâng cấp: V2 cũ trong working tree thiếu `app_role`, V3 còn rỗng.
Phiên này sửa baseline đó và kiểm tra trên DB mới. Không tự `repair`, xóa volume
hoặc sửa lịch sử Flyway của database đang có dữ liệu.

## Kiểm thử và bằng chứng

Từ `src/backend/`, với Maven 3.9.11 và JDK 21:

```powershell
mvn -B -ntp test
mvn -B -ntp -Ppostgres-it verify
```

Profile `postgres-it` chạy PostgreSQL 17.6 tạm trong `target/postgres-it/`, chỉ
nghe `127.0.0.1`, khởi động Tomcat ở cổng ngẫu nhiên và tự dừng DB sau kiểm thử.
Datasource của developer không được dùng trong test này. Binaries được tải từ
Maven Central lần đầu; Windows/Linux/macOS amd64 được dependency hỗ trợ. Không
chạy PostgreSQL native dưới user root trên Linux.

`SecurityFlowTest` kiểm tra 8 ca với MVC/Security thật và repository mock:
CSRF, đổi session ID, lỗi đăng nhập chung, xóa credentials, thu hồi phiên,
logout, facility scope, chống tự cấp quyền và stale version.

`Stage4IT` kiểm tra 10 ca trên PostgreSQL thật và toàn bộ Spring context:

- Flyway V1–V4 và Hibernate validate; trigger chặn sửa/xóa audit.
- Trùng bệnh nhân có/không xác minh, tìm theo tên Việt có dấu, stale update,
  quyền của admin và scope khác, giới hạn phân trang.
- Retry cùng khóa/nội dung trả lịch cũ; đổi nội dung cùng khóa bị chặn;
  thời gian đệm được giữ; hủy giải phóng cả hai booking.
- Hai request đồng thời tranh một slot: một thành công, một 409; không booking dở.
- Check-in trùng không tạo lượt thứ hai; queue/read/write theo phân công và role.
- Availability, đổi lịch/stale version, xác nhận và check-in liên kết lịch;
  lịch sử trạng thái còn đủ. Fixture ngày khám được bố trí riêng để test qua nửa đêm.
- Nghỉ không cho nhận lịch mới; no-show quá sớm bị chặn; no-show hợp lệ giải phóng booking.
- Sinh hiệu sửa bằng bản kế tiếp, giữ người/thời điểm/đơn vị, chống nhánh sửa trùng;
  tiền sử không được nối sai loại dữ liệu.
- Thiếu trường không ký; signed note không sửa qua API hoặc SQL; đính chính có
  predecessor và lý do; checklist không đủ giữ lượt mở; đóng lượt khóa nội dung;
  không cho đính chính sau đóng.
- HTTP qua Tomcat thật: `/health`, `/ready`, cookie HttpOnly/SameSite, đăng nhập,
  `/me` không có secret và phiên cũ bị chặn sau khi tăng `auth_version`.

Kết quả và thời điểm chạy cuối được lưu tại [VERIFICATION.md](VERIFICATION.md).
Các log Surefire/Failsafe đầy đủ nằm trong `backend/target/`. Kiểm thử ở đây
không thay thế load test, backup/restore, UAT lâm sàng hoặc kiểm thử giao diện.

## Quyết định triển khai và giới hạn

Các lựa chọn dưới là chính sách MVP dùng cho source/demo hiện tại, cần chủ cơ
sở duyệt trước khi áp dụng thực tế. Chúng không thay đổi phạm vi của báo cáo.

**Phân lớp.** Một backend chia package `identity`, `patient`, `scheduling`,
`clinical`, `common`, `health`. Controller nhận DTO có validation; service giữ
transaction và kiểm tra quyền/trạng thái; repository làm persistence. Identity/
Patient dùng JPA theo source sẵn có. Scheduling/Clinical dùng JdbcTemplate cho
SQL nguyên tử và truy vết constraint rõ ràng; dùng chung datasource và transaction
manager với JPA. Không bổ sung ORM hay microservice thứ hai.

**Phiên và quyền.** Spring Security 7.1.2 dùng HttpSession, idle timeout 30 phút,
HttpOnly/SameSite=Lax; CSRF mặc định được giữ. POST login dùng form-urlencoded,
không nhận JSON. Đổi quyền/khóa/reset mật khẩu tăng `auth_version`; filter kiểm
tra lại mỗi request. Cần bật `SESSION_COOKIE_SECURE=true` khi phục vụ qua HTTPS.
Phiên nằm trong một process; restart đăng xuất phiên, chưa có session store nhiều node.
Hiện chưa có chính sách rate limit/lockout tự động do nhập sai nhiều lần.

**Phạm vi tài khoản.** Admin chỉ quản lý tài khoản có membership trong cơ sở của
mình. Thay đổi trạng thái/profile/mật khẩu ảnh hưởng tài khoản toàn cục nên yêu
cầu quyền ở mọi scope của tài khoản đích. Không tự sửa tài khoản/quyền của mình.
Admin được cấu hình lịch/danh mục nhưng không mặc định có quyền đọc hồ sơ bệnh nhân.

**Nhận diện bệnh nhân.** Mã `PT-<UUID>` bất biến và duy nhất. Tên chuẩn hóa bỏ
dấu/khoảng trắng thừa để gợi ý nghi trùng; cùng tên+ngày sinh, số điện thoại hoặc
định danh tạo cảnh báo. `duplicateReviewed=true` cùng lý do xác minh mới cho
tiếp tục; không tự hợp nhất. Định danh quốc gia đã có trong cùng cơ sở không tạo
bản thứ hai dù xác nhận cảnh báo. Truy vấn và mutation có audit; log không sao
chép tên, điện thoại hoặc nội dung bệnh án. Lý do xác minh được lưu riêng trong
audit; chỉ nhập nội dung cần cho quyết định, không chép lại thông tin cá nhân.

**Lịch.** Phòng khám có sức chứa một lượt; phòng có loại, thiết bị, trạng thái.
Dịch vụ có thời lượng 5–240 phút và đệm 0–60 phút; bác sĩ phải được cấp năng lực
dịch vụ, có role đang hoạt động trong cơ sở, có ca WORK bao phủ toàn khoảng giữ
và không có ABSENCE. Khoảng giữ `[startsAt, reservedUntil)` bao gồm đệm sau
khám. Phiên bản lịch/danh mục được kiểm tra khi cập nhật; lịch đã đặt giữ snapshot
thời gian cũ dù danh mục dịch vụ đổi. Không bật appointment Draft tùy chọn.

Booking và hai resource booking (bác sĩ/phòng) commit cùng transaction. Khóa cơ
sở tuần tự hóa mutation của MVP; khóa bác sĩ bảo vệ thao tác giữa các scope;
PostgreSQL exclusion constraint là hàng rào cuối cho overlap, kể cả request từ
nhiều process. Đổi lịch không giải phóng tài nguyên cũ nếu request mới thất bại.
ABSENCE đụng lịch đang nhận phải đổi/hủy lịch trước. Chưa có recurring calendar,
phân phòng theo thiết bị tự động hay nguồn lực của dịch vụ Stage 5.

**Check-in.** Nhân viên xác minh danh tính trước khi gửi `identityVerified=true`.
Appointment và patient/doctor phải khớp, trạng thái Booked/Confirmed, đúng ngày
khám tại `MEDORA_FACILITY_ZONE` (mặc định Asia/Ho_Chi_Minh). Walk-in bật bởi
`MEDORA_WALK_IN_ENABLED`; nhân viên chọn bác sĩ chịu trách nhiệm. Mỗi patient
có tối đa một Encounter chưa Completed trong cơ sở, không tự hết hạn qua ngày.
Check-in lặp trả 409; không trả một lần thành công giả. Queue number tăng toàn
hệ thống, không reset theo ngày. Reception thấy queue hành chính; bác sĩ/y tá
chỉ thấy lượt được gán. Reception gán/đổi y tá với version và audit.

**Hồ sơ.** SOAP gồm subjective/objective/assessment/plan. Nháp được phép thiếu
nội dung, ký đòi đủ bốn phần và ít nhất một chẩn đoán trong danh mục đang bật.
Người ký/đính chính là bác sĩ được gán lượt. Note có sequence number, optimistic
version, signedBy/signedAt, predecessor/reason; trigger giữ nguyên signed row.
Đính chính bắt đầu từ bản ký mới nhất và có tối đa một draft đang mở; có API bỏ
amendment draft. Sinh hiệu/tiền sử sửa bằng bản kế tiếp, có lý do, không ghi đè.

**Đóng lượt.** Chỉ nhánh chưa có đơn/chỉ định dịch vụ: Waiting → In Consultation
→ Awaiting Closure → Completed. Có thể quay về In Consultation để sửa trước
khi đóng. Checklist tối thiểu: không còn draft, có signed note, chẩn đoán và lần
sinh hiệu mới nhất đã xác minh. Đóng cần summary/instructions/followUp (có thể ghi
rõ không chỉ định tái khám); lưu người/thời điểm và trigger khóa lượt đã đóng.
Chưa mở sửa sau đóng khi chưa có chính sách; Stage 5 phải mở rộng checklist cho
công việc dịch vụ, không được dùng checklist này để bỏ qua chỉ định tồn.

**Đọc hồ sơ theo quan hệ chăm sóc.** Bác sĩ/y tá được gán đọc lượt và dữ liệu
của lượt đó. Khi còn lượt hoạt động, họ có thể xem timeline, các lần khám trước
và tiền sử của cùng patient trong cơ sở. Lượt đã đóng không được dùng làm cớ để
đọc tiền sử/timeline mới trong tương lai. Lịch sử có timestamp/superseded flag
để không nhầm bản cũ là dữ liệu hiện hành. Note action audit gắn cả encounter
và ID phiên bản note; không đưa nội dung SOAP vào log.

**Giới hạn truy vấn.** `page` từ 0, `size` 1–100, offset tối đa 1.000.000.
Availability tối đa một ngày, bước 5 phút, trả tối đa 100 slot. Lịch và ca làm
lọc tối đa 31 ngày. Danh mục nhỏ, chẩn đoán và event lịch trả tối đa 100 mục.
UUID ngoài scope bị chặn trước khi trả dữ liệu. Lỗi API dùng mã cố định, không
trả SQL, stack trace hoặc password hash.

## API contract

Tiền tố `F = /api/facilities/{facility}` và `A = /api/admin/facilities/{facility}`.
JSON response dùng camelCase, ngày `YYYY-MM-DD`, timestamp ISO-8601 có offset/UTC.
Sinh hiệu dùng đơn vị ngay trong tên trường: `temperatureC`, `pulseBpm`,
`systolicMmhg`, `diastolicMmhg`, `respiratoryRate` (lần/phút), `spo2Percent`,
`weightKg`, `heightCm`. `measuredAt` và `recordedAt` là hai thời điểm khác nhau.
Mutation có thể trả 400 (input), 401 (phiên), 403 (quyền), 404 (tài nguyên),
409 (version, trạng thái, duplicate hoặc conflict). Tạo trả 201, cập nhật/đọc
trả 200. Login/logout, cấp năng lực và bỏ nháp đính chính trả 204, không có body.

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

GET history/vitals/notes hỗ trợ `page,size`. Các endpoint còn lại trả mảng hoặc
object như script demo. Enum source dùng dạng `CHECKED_IN`, `IN_CONSULTATION`,
`AWAITING_CLOSURE`, `AMENDMENT_DRAFT`, `AMENDED_SIGNED_VERSION`, ánh xạ trực tiếp
với tên canonical CheckedIn, In Consultation, Awaiting Closure, Amendment Draft,
Amended Signed Version trong inventory. `Draft` appointment không được bật;
`Awaiting Service` được giữ trong schema cho Stage 5, chưa có transition API.

## Traceability và phần tài liệu chưa đồng bộ

| Quy tắc / yêu cầu | Module / bằng chứng |
| --- | --- |
| BR-001, FR-003/031, UC-010–012/018 | PatientController/Service/Repository, V4, patient test |
| BR-004/010, FR-001/002/030, UC-001/070–071 | identity, V2/V3/V4, SecurityFlowTest và HTTP revocation test |
| BR-002, FR-005/016/017, UC-020–026 | scheduling, resource_booking exclusion, booking/concurrency/absence tests |
| BR-003, FR-006, UC-027–031 | check-in/queue/encounter, partial unique index, assignment tests |
| FR-007/008/010, UC-013–014/032–035 | history/vitals/diagnoses, correction/timeline APIs, screening test |
| BR-005, FR-009/032, UC-036–038 | clinical_note và trigger, version/amendment tests; FR-032 phía UI chưa có |
| FR-020, UC-041–042 | checklist/summary/completion trigger; nhánh không dịch vụ |

Không đánh dấu FR-004 merge hoặc toàn FR-032 giao diện đã hoàn tất. `paper.tex`
vẫn có đề xuất NestJS/Prisma và bảng Not Implemented/Not Run của snapshot cũ;
phiên này không sửa báo cáo/UML hay coi các diagram đã qua một lượt render/review
mới. Thiết kế lớp/ERD chính thức và báo cáo cần cập nhật từ source đã chạy khi
làm phần tài liệu. Bước tiếp theo: Stage 5, thuốc và chỉ định dịch vụ; frontend
vẫn là hạng mục riêng theo lộ trình API trước màn hình.

Nguồn framework đã đối chiếu:
[Spring Boot testing](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html),
[Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html),
[Embedded PostgreSQL](https://github.com/zonkyio/embedded-postgres).
