# MEDORA

MEDORA — Integrated Clinic Management and Electronic Medical Records System.

## Ngữ cảnh triển khai

**Đọc [src/README.md](src/README.md) trước khi tiếp tục dự án.** Đây là hồ sơ
ngữ cảnh chung, có mục tiêu và cách cộng tác, phạm vi/MVP, tóm tắt đủ 34 FR và
16 NFR, quyết định kỹ thuật/nghiệp vụ, cấu trúc source, bản đồ UML, lộ trình
6 stage, tiêu chí nghiệm thu, evidence và các điểm chưa đồng bộ trong tài liệu.

Snapshot 2026-10-31: API đã triển khai đến Stage 6 trên nhánh `feature`, gồm
thuốc, dịch vụ/kết quả, tệp private, tài chính, hợp nhất, báo cáo/audit và
thông báo nội bộ, bên cạnh các luồng Stage 1–4. Frontend chưa triển khai.
Xem [API và demo Stage 5–6](src/docs/API-STAGE56.md),
[API Stage 1–4](src/docs/STAGE4.md) và
[bằng chứng kiểm thử mới nhất](src/docs/STAGE56-VERIFICATION.md).
Docker image đã build/run trên Linux, 33 kiểm thử qua trên Windows và Linux;
demo HTTP và persistence database/file qua recreate đều PASS.
Môi trường test riêng `medora-stage4-test` đang chạy tại `127.0.0.1:8080`;
volume MEDORA cũ được giữ nguyên. Tên môi trường test giữ như trước để nâng cấp
dữ liệu từ schema 4 lên schema 7.

Hướng triển khai người dùng đã chọn: Java 21 + Spring Boot, PostgreSQL,
JPA/Hibernate, Flyway; build và chạy bằng Linux containers qua Docker Compose.
Frontend dự kiến là Next.js + TypeScript.

## Tài liệu

- [Quy tắc dự án](AGENT.md).
- [Yêu cầu sản phẩm](doc/report/%5BSYSDES_2026.1%5D%20WorkStation.md).
- [Báo cáo](doc/report/paper.tex).
- [Đặc tả use case và test case](doc/report/appendices.tex).
- [Danh mục use case, business rule và trạng thái](doc/asset/03-system-requirements/use-case-model/model-inventory.md).
- [Tài liệu và diagram](doc/asset/).
- [Audit diagram đã lưu](doc/asset/audit.md) — không phải kết quả kiểm thử ứng dụng.

Nội dung báo cáo về stack cần được đồng bộ với quyết định triển khai;
trạng thái thiết kế, triển khai và xác minh được ghi riêng trong src/README.md.
Hồ sơ đủ để nắm hướng đi; khi code/review vẫn đọc source và đặc tả của phần
đang làm, không cần đọc lại toàn bộ repo mỗi phiên.
