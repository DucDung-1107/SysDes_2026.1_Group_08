# Kiến trúc MEDORA

MEDORA phục vụ nhân sự phòng khám ngoại trú. Source hiện hành dùng Next.js/TypeScript và Spring Boot/Java 21; không dùng đề xuất NestJS/Prisma trong snapshot báo cáo cũ. Báo cáo, PRD và UML môn học vẫn giữ tại `doc/`.

## Thành phần

Nhân sự thao tác trong Next.js. Frontend gửi request cùng origin qua `/api/*` đến Spring Boot; backend quản lý PostgreSQL và tệp private (filesystem hoặc S3 adapter).

Compose chạy ba service `frontend`, `api`, `db` trong một network. Host chỉ publish frontend/API trên loopback; DB không publish port. Frontend Docker chuyển API đến `http://api:8080`. Hai volume giữ PostgreSQL và nội dung đính kèm. Backend là một ứng dụng chia module, không tách microservice theo use case.

| Module backend | Trách nhiệm |
| --- | --- |
| identity | Session/CSRF, tài khoản, vai trò, quyền theo cơ sở, thu hồi phiên |
| patient | Hồ sơ hành chính, tìm/kiểm tra nghi trùng, version |
| scheduling | Phòng, dịch vụ, ca làm/nghỉ, năng lực, availability, lịch hẹn |
| clinical | Check-in, phân công, tiền sử/sinh hiệu, chẩn đoán, SOAP, ký/bổ sung, kết thúc |
| care | Danh mục/chính sách/hồ sơ thuốc, kiểm tra an toàn, đơn thuốc, cấp/dùng thuốc, chỉ định |
| attachment | Metadata/checksum, kiểm tra nội dung, download có quyền, archive |
| finance | Khoảng giá, snapshot hóa đơn, thanh toán, hoàn tiền và biên nhận |
| governance | Chính sách cơ sở, duyệt ra về, merge, audit, báo cáo và thông báo nội bộ |
| common | Transactional JDBC Store, scope/assignment guard, lỗi, idempotency |

JPA và JDBC dùng chung datasource/transaction manager. Flyway quản lý schema; Hibernate `validate`, không tự sửa schema. Constraint/lock PostgreSQL bảo vệ lịch tài nguyên, lượt hoạt động, ledger và evidence bất biến.

## Ranh giới quyền

- Lễ tân quản lý hành chính/đặt lịch/tiếp nhận; không ký ghi chú.
- Doctor/Nurse cần được phân công lượt. Ghi/chỉnh đơn thuốc và review chỉ định áp dụng guard bác sĩ phụ trách.
- Pharmacist/Technician chỉ xử lý đơn/chỉ định được phân công.
- Cashier lập/phát hành/thu tiền; Finance giảm giá và yêu cầu/thực hiện hoàn; người duyệt hoàn phải khác người yêu cầu.
- Manager quản lý giá/chính sách/danh mục và phê duyệt. Admin quản lý nhân sự và audit; quyền quản trị không tự mở nội dung lâm sàng.
- Thay đổi tài khoản có tác động toàn cục cần quyền tại mọi cơ sở của tài khoản đích. Không tự quản trị tài khoản mình qua màn account admin.
- Menu frontend chỉ hỗ trợ sử dụng. Backend luôn kiểm tra lại scope, quyền, phân công, version và trạng thái.

## Quy tắc dữ liệu

- Tìm nghi trùng không tạo/hợp nhất bệnh nhân. Tiếp tục hồ sơ nghi trùng cần xác minh và lý do; merge giữ source/evidence và liên kết về hồ sơ chính.
- Availability chỉ là kết quả tra cứu; booking kiểm tra/khóa lại. Idempotency và exclusion constraint ngăn đặt trùng.
- SOAP nháp có thể chưa đủ phần; ký cần đủ SOAP và chẩn đoán. Bản ký giữ nguyên; bổ sung tạo bản kế tiếp có lý do. Kết thúc lượt không mở lại để chỉnh tùy ý.
- Checklist kết thúc cần note ký, chẩn đoán, sinh hiệu xác minh, không còn đơn nháp hoặc chỉ định chưa giải quyết/review. Duyệt ra về là bước riêng nếu policy bật.
- Thuốc cần policy đang bật và profile có trạng thái dị ứng rõ ràng. Server tính lại cảnh báo khi phát hành; không suy tương tác từ tên thuốc hoặc diễn giải tự do.
- DISPENSED và ADMINISTERED là hai sự kiện khác nhau. Phát hành đơn không tự ghi cấp thuốc.
- Lượt `COMPLETED` và hóa đơn `PAID` độc lập. Giá khám lấy tại lúc tạo lượt; giá dịch vụ lấy lúc hoàn tất dịch vụ, không lấy lúc lập hóa đơn.
- Invoice giữ snapshot dòng/giá; Payment append-only. Refund pending/approved chưa đổi số dư. Chỉ refund completed mới giảm tiền đã thu ròng.
- Sinh nhật là date-only; thời điểm là Instant. Múi giờ cơ sở cấu hình rõ. Tiền VND nguyên; không tính bằng số thực JavaScript để quyết định ledger.
- Tệp không nằm trong public. Download kiểm tra quyền/checksum; archive giữ lịch sử. Backup cần đồng bộ DB và object storage.

## Frontend

App Router tại `src/frontend/src/app`. Các module trong `src/modules` khai báo endpoint, field, view và action theo DTO. Component dùng chung ở `src/components`; transport/session/CSRF, validation và timezone ở `src/lib`. Không ghi credential hay bản nháp hồ sơ vào browser storage. CSS/token nằm hoàn toàn trong frontend để build độc lập.

## Giới hạn hiện tại

Chưa có danh bạ staff chung cho lễ tân/bác sĩ, directory tên/múi giờ cơ sở, danh sách hoàn tiền/duyệt ra về toàn cục hoặc danh sách medication events riêng. UI dùng UUID xác minh, config cơ sở và lookup theo API có sẵn. Không tạo endpoint giả.

SMS/email, portal bệnh nhân, LIS/PACS, payment gateway, bảo hiểm/hóa đơn điện tử, clinical drug database, antivirus/CDR và triển khai nhiều node chưa được xác minh/triển khai thành tích hợp hoàn chỉnh. S3 adapter có source nhưng bài test hiện tại dùng filesystem. Không suy chứng nhận, SLA, backup/restore production hay khả năng điều trị từ test phần mềm.

Hướng dẫn chạy ở [README](../README.md), hợp đồng ở [API](API.md), phạm vi bằng chứng ở [TESTING](TESTING.md).
