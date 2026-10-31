# MEDORA — ngữ cảnh và tiến độ

**Snapshot hiện hành: 2026-10-31, nhánh `feature`.** Backend đã có API Stage 1–6;
frontend chưa được tạo. Nguồn hiện hành là [API Stage 5–6](docs/API-STAGE56.md),
[API Stage 1–4](docs/STAGE4.md) và [evidence mới nhất](docs/STAGE56-VERIFICATION.md).

33 kiểm thử qua trên Windows và Linux Docker; demo thuốc/dịch vụ/tệp/tài chính/
merge và persistence database/file qua recreate đều PASS. Môi trường test riêng
đang chạy tại `127.0.0.1:8080`. Chạy lại từ src bằng
`python scripts/demo_full_api.py --env-file .env.docker-test`.

Phần dưới là **hồ sơ lịch sử ngày 2026-10-24**: giữ lại yêu cầu/FR/NFR, bản đồ
UML và các trao đổi học theo checkpoint. Những câu “chưa triển khai”, tiến độ,
cơ chế phiên chưa chốt và chế độ AI chỉ hướng dẫn ở snapshot này đã được thay
thế bằng hồ sơ API ngày 2026-10-31. Chỉ frontend, portal và các tích hợp ngoài
chưa triển khai theo phạm vi hiện hành.

---

# Hồ sơ lịch sử — 2026-10-24

**Cập nhật:** 2026-10-24

**Nhánh local:** backend

**Điểm tiếp tục:** Checkpoint 1.6 — xác minh build/container, health và Flyway trước
Stage 2. Source nền Stage 1 đã có; kiểm tra Compose hợp lệ, nhưng Docker daemon và
HTTP health chưa kết nối được trong phiên 2026-10-24. Người dùng xác nhận tiếp tục
tự code theo từng checkpoint, AI hướng dẫn và review.

Đây là hồ sơ ngữ cảnh để người dùng và các phiên AI tiếp nối cùng một dự án.
Nội dung gồm quyết định, cấu trúc, liên kết UML, phạm vi, trạng thái và bằng chứng.
Các bài hướng dẫn, đoạn code mẫu và lệnh thực hành được trao đổi trong hội thoại.

## 0. Đọc nhanh để tiếp tục dự án

| Câu hỏi | Ngữ cảnh cần giữ |
| --- | --- |
| Đang xây gì? | Web nội bộ cho phòng khám ngoại trú, nối liền tiếp nhận, khám, hồ sơ, thuốc/dịch vụ, tài chính và quản trị |
| Đích của người dùng? | Tự viết và hiểu code theo từng checkpoint; triển khai bám UML và có thể đối chiếu lại với báo cáo môn System Design của Group 08 |
| AI cộng tác thế nào? | Giải thích việc đang làm, lý do và cách kiểm tra; người dùng xác nhận ngày 2026-10-24 muốn tự code theo checkpoint, AI hướng dẫn và review |
| Hướng kỹ thuật? | Java 21, Spring Boot, Maven, PostgreSQL, JPA/Hibernate, Flyway; frontend Next.js/TypeScript; một backend chia module nghiệp vụ |
| Môi trường? | Source/editor/terminal trên Windows; build và chạy bằng Linux containers qua Docker Desktop/Compose tại `src/compose.yaml` |
| Đã có gì chạy? | Source app/health, Dockerfile, Compose, datasource/Flyway và migration tạo schema đã có; Compose qua kiểm tra cấu hình. Chưa xác minh build/runtime trong phiên này vì chưa kết nối được Docker daemon |
| Làm tiếp ở đâu? | Checkpoint 1.6 xác minh nền chạy, rồi Stage 2: tài khoản/quyền và hồ sơ hành chính bệnh nhân; checkpoint ở mục 6 |
| Điểm lệch dễ gây nhầm? | `paper.tex` còn đề xuất NestJS/Prisma và cấu trúc `apps/`; hướng triển khai hiện hành là Spring Boot và khung `src/backend/` |
| Điều chưa thể coi là xong? | Chức năng nghiệp vụ, thiết kế cấu trúc Chương 4, schema/API chính thức, kiểm thử ứng dụng và production deployment |

README đủ để nắm mục tiêu, phạm vi và điểm tiếp tục mà không phải đọc lại toàn
bộ repo. Khi viết/review một chức năng, vẫn đọc source và đúng đặc tả/UML của
chức năng đó; khi thay đổi UML/báo cáo, đọc quy tắc đầy đủ trong `AGENT.md`.
Không dùng bản tóm tắt thay cho việc kiểm tra code vừa được sửa.

**Bản đồ nội dung:** [mục tiêu và phạm vi](#1-mục-tiêu-và-cách-cộng-tác),
[quyết định kỹ thuật](#2-quyết-định-triển-khai-đã-chốt),
[cấu trúc](#3-cấu-trúc-source-thực-tế), [nguồn thiết kế](#4-nguồn-thiết-kế-và-uml),
[nghiệp vụ](#5-nội-dung-nghiệp-vụ-phải-giữ-nhất-quán),
[lộ trình và việc tiếp theo](#6-lộ-trình-xây-dựng-6-stage),
[nghiệm thu và evidence](#7-evidence-và-mức-độ-xác-minh),
[điểm chưa chốt](#8-phần-còn-thiếu-và-điểm-cần-giữ-lại-cho-phiên-sau),
[nhật ký](#9-nhật-ký-ngắn), [cách cập nhật](#10-quy-ước-cập-nhật-hồ-sơ).

## 1. Mục tiêu và cách cộng tác

MEDORA là hệ thống quản lý phòng khám và hồ sơ bệnh án điện tử, tập trung vào
hành trình ngoại trú: bệnh nhân, lịch hẹn, tiếp nhận, khám, thuốc/dịch vụ và tài chính.

Người dùng muốn tự viết code để học và bám thiết kế UML trong repo. AI giải thích,
chia công việc thành checkpoint nhỏ và review kết quả. Việc tạo file trống,
cung cấp hướng dẫn hay có diagram không chứng minh chức năng đã được triển khai.

README này là điểm lưu ngữ cảnh triển khai chính. Trước mỗi phiên cần đối chiếu
nội dung với source và trạng thái Git hiện tại. Ưu tiên yêu cầu mới của người dùng;
không giữ một quyết định cũ nếu người dùng đã thay đổi nó.

### Mục tiêu sản phẩm và đầu ra của dự án

Vấn đề cần giải quyết là dữ liệu và công việc bị rời rạc giữa quầy tiếp nhận,
phòng khám, nhân sự thực hiện dịch vụ và thu ngân. MEDORA dùng định danh bệnh
nhân ổn định, lượt khám liên kết, trạng thái rõ ràng, quyền theo vai trò/phạm
vi công việc và audit để nối các bước này, giảm nhập lặp và tránh xung đột.

Đích triển khai là một luồng có thể chạy và chứng minh bằng dữ liệu giả lập:
tìm/tạo bệnh nhân → đặt lịch hoặc walk-in → check-in/hàng đợi → sàng lọc →
khám/chẩn đoán/ghi chú → đơn thuốc và/hoặc dịch vụ khi có → ký/tóm tắt kết thúc →
hóa đơn/thanh toán → báo cáo/audit. Kết thúc lâm sàng và thanh toán là hai việc
riêng; hóa đơn chưa trả tiền không tự mở lại hoặc sửa bệnh án đã ký.

Đầu ra gồm ứng dụng, thiết kế/API/schema được đồng bộ với hành vi thực tế,
kiểm thử và bằng chứng thực thi, cùng báo cáo/UML truy vết được. Repo hiện có
nhiều tài liệu phân tích và mô hình hành vi; phần triển khai đã có source nền
Spring Boot/Compose/Flyway, chưa có source nghiệp vụ.
Chương 4 được tài liệu hiện tại dành cho thành viên phụ trách thiết kế cấu trúc;
không tự coi các đề mục trống là thiết kế đã hoàn tất.

### Ranh giới và mức ưu tiên

- Phạm vi chính: phòng khám ngoại trú, web nội bộ, desktop/tablet, tiếng Việt;
  ngày giờ theo cấu hình cơ sở. Không mở rộng mặc nhiên thành hệ thống bệnh viện.
- P0: bắt buộc cho MVP theo PRD; P1: mở rộng quan trọng sau/phù hợp MVP;
  P2: cần quyết định tổ chức hoặc hợp đồng tích hợp. Thứ tự stage là thứ tự
  xây dựng, không thay đổi mức ưu tiên này.
- Cổng bệnh nhân là Phase 2. Đa chi nhánh, SMS/email, tích hợp xét nghiệm/hình
  ảnh, hệ thống thanh toán ngoài, bảo hiểm, hóa đơn điện tử, kho thuốc nâng cao
  và liên thông quốc gia không được tự thêm vào MVP.
- Cảnh báo thuốc hỗ trợ bác sĩ bằng dữ liệu/quy tắc cấu hình; không tự chẩn
  đoán hay quyết định điều trị. Không mặc nhiên có bộ dữ liệu tương tác thuốc.
- PRD WorkStation là bản dự thảo 1.0. Các mục tiêu đo lường và chính sách cơ sở
  trong đó cần xác nhận; thiết kế được chọn và tiến độ thực tế được ghi riêng.

### Tóm tắt đầy đủ FR theo PRD

Các dòng dưới tóm tắt yêu cầu, **chưa phải chức năng đã triển khai**. Giữ nguyên
ID để tra đặc tả chi tiết khi đến module, không tạo bộ ID mới trong code.

| FR | Ưu tiên | Phạm vi và điều kiện chính |
| --- | --- | --- |
| FR-001 | P0 | Đăng nhập tài khoản cá nhân, đăng xuất, hết hạn/thu hồi phiên, khóa tài khoản; lỗi không tiết lộ tài khoản tồn tại |
| FR-002 | P0 | Quản lý tài khoản, role/quyền và phạm vi; API chặn ngoài quyền, thay đổi quyền có audit |
| FR-003 | P0 | Hồ sơ hành chính, mã bệnh nhân duy nhất, tìm kiếm và gợi ý nghi trùng trước khi tạo |
| FR-004 | P1 | Hợp nhất sau xác minh bởi vai trò được phép; giữ hồ sơ nguồn, liên kết và lịch sử |
| FR-005 | P0 | Tạo/xem/sửa/đổi/xác nhận/hủy/no-show lịch; kiểm tra khả dụng và lưu lịch sử trạng thái |
| FR-006 | P0 | Check-in có hẹn/walk-in, Encounter và queue; ngăn lượt hoạt động trùng theo quy tắc cơ sở |
| FR-007 | P0 | Sinh hiệu/sàng lọc có đơn vị, thời điểm, người ghi, trạng thái xác minh và lịch sử sửa |
| FR-008 | P0 | Hồ sơ sức khỏe theo thời gian: lượt khám, tiền sử, dị ứng, thuốc, tài liệu theo quyền |
| FR-009 | P0 | Ghi chú khám theo mẫu, lưu nháp/ký; đủ trường bắt buộc, giữ nguyên bản đã ký |
| FR-010 | P0 | Triệu chứng/chẩn đoán gắn Encounter, người ghi, thời gian; dùng danh mục được duyệt |
| FR-011 | P0 | Cảnh báo dị ứng/tương tác/chống chỉ định trong giới hạn nguồn luật; override đúng quyền và có lý do |
| FR-012 | P0 | Danh mục và đơn thuốc có liều, đường dùng, tần suất, thời gian, số lượng, hướng dẫn; kiểm tra trước phát hành |
| FR-013 | P1 | Ghi nhận cấp/dùng thuốc riêng, có liên kết đơn/y lệnh, người và thời điểm thực hiện |
| FR-014 | P0 | Chỉ định dịch vụ có lý do, ưu tiên, chuẩn bị và trạng thái; chuyển trạng thái có lịch sử |
| FR-015 | P0 | Lập/đổi/hủy lịch dịch vụ và phân bổ nguồn lực; kiểm tra đồng thời, giải phóng khi hủy |
| FR-016 | P0 | Danh mục phòng, loại/sức chứa/thiết bị/trạng thái; không phân phòng không khả dụng |
| FR-017 | P0 | Ca làm, nghỉ, thời lượng, thời gian đệm và năng lực nhân sự; kiểm tra lịch bằng dữ liệu mới nhất |
| FR-018 | P0 | Nhân sự được phân công bắt đầu/hoãn/hủy/hoàn tất dịch vụ và ghi kết quả |
| FR-019 | P1 | Yêu cầu xét nghiệm/hình ảnh, trạng thái và tệp kết quả; nhập tay hoặc adapter chỉ khi được duyệt |
| FR-020 | P0 | Tóm tắt, kết luận, hướng dẫn, tái khám và kết thúc theo checklist |
| FR-021 | P1 | Yêu cầu ra về/ra viện và phê duyệt nếu cơ sở có quy trình; không bỏ qua công việc bắt buộc |
| FR-022 | P0 | Danh mục dịch vụ, yêu cầu nguồn lực, bảng giá có hiệu lực; giữ giá đã áp dụng cho hóa đơn lịch sử |
| FR-023 | P0 | Hóa đơn, giảm giá theo quyền, thanh toán/hoàn tiền có lịch sử; tổng và số dư nhất quán |
| FR-024 | P1 | Thông báo theo kênh cấu hình, ghi gửi/thất bại; lỗi gửi không làm mất lịch |
| FR-025 | P1 / Phase 2 | Bệnh nhân xác thực và chỉ thao tác lịch/tài liệu được công bố của mình |
| FR-026 | P0 | Dashboard theo vai trò; định nghĩa chỉ số và phạm vi dữ liệu thống nhất |
| FR-027 | P0 | Báo cáo/lọc/xuất theo quyền, audit và số liệu đối soát được |
| FR-028 | P0 | Quản trị danh mục; ngừng dùng không làm mất tham chiếu lịch sử |
| FR-029 | P0 | Upload/xem/download tệp có quyền, giới hạn loại/kích thước, metadata và nơi lưu riêng tư |
| FR-030 | P0 | Audit hoạt động nhạy cảm, ký/sửa đơn/xuất/đổi quyền/hợp nhất; người dùng thường không sửa/xóa log |
| FR-031 | P0 | Tìm kiếm theo trường được phép, lọc/phân trang/giới hạn; gợi ý không rò rỉ dữ liệu |
| FR-032 | P1 | Nháp/khôi phục và cảnh báo chưa lưu; server xác nhận mới báo thành công, không ghi đè phiên bản mới hơn |
| FR-033 | P2 | Tổ chức và cách ly dữ liệu theo cơ sở/chi nhánh khi được duyệt |
| FR-034 | P2 | Connector/API ngoài theo hợp đồng được duyệt, có xác thực, giới hạn quyền, log, lỗi/retry/versioning |

### Tóm tắt đầy đủ NFR và mức cam kết

| NFR | Yêu cầu cần giữ / cách kiểm chứng |
| --- | --- |
| NFR-01 | Mục tiêu dự thảo p95 màn hình thường ≤ 2 giây, tìm kiếm ≤ 3 giây; cần tải/dataset/môi trường đại diện |
| NFR-02 | PRD đề xuất 99,5%/tháng ngoài bảo trì; báo cáo vẫn để mục tiêu khả dụng chờ chốt; chưa phải SLA |
| NFR-03 | PRD đề xuất RPO ≤ 15 phút, RTO ≤ 4 giờ; báo cáo vẫn để chờ chốt; cần restore thực tế, không chỉ log backup |
| NFR-04 | Bảo vệ truyền tải dữ liệu nhạy cảm bằng HTTPS/TLS; kiểm tra cấu hình khi có môi trường phù hợp |
| NFR-05 | Bảo vệ dữ liệu lưu, backup, object storage và khóa/secrets; rà soát cấu hình và quyền |
| NFR-06 | RBAC/quyền tối thiểu/phạm vi/phiên ở server; kiểm thử âm bằng gọi API trực tiếp |
| NFR-07 | Audit đủ truy vết, đồng bộ thời gian, chống sửa trái phép và có retention được chốt |
| NFR-08 | Lỗi phụ thuộc không làm mất dữ liệu đã xác nhận; mô phỏng timeout, retry/gửi lặp |
| NFR-09 | Module theo miền, API rõ, phân trang và truy vấn có giới hạn; đánh giá bằng tải và kiến trúc |
| NFR-10 | Chrome/Edge gần đây, desktop/tablet; kiểm tra trên phiên bản trình duyệt được chọn |
| NFR-11 | Mục tiêu dự thảo WCAG 2.1 AA: bàn phím, nhãn, tương phản và lỗi rõ; cần kiểm tra tự động và thủ công |
| NFR-12 | Bảo trì: quy ước code/test, logging, tài liệu API, migration kiểm soát, tách môi trường và rollback |
| NFR-13 | Logs/metrics/traces và cảnh báo phù hợp; cần có người chịu trách nhiệm khi vận hành |
| NFR-14 | Tiếng Việt, ngày giờ/số/tiền tệ/múi giờ nhất quán; kiểm tra dấu tiếng Việt và ranh giới ngày |
| NFR-15 | Transaction, ràng buộc dữ liệu và chống gửi lặp; kiểm tra đồng thời đặt lịch/thanh toán/cập nhật trạng thái |
| NFR-16 | Unit/integration cho nghiệp vụ quan trọng, e2e cho luồng chính; dữ liệu giả lập, kết quả gắn build/môi trường |

Chưa có phép đo ứng dụng, kết quả restore hay chứng nhận tuân thủ. Các KPI PRD
như giảm 25% thời gian tiếp nhận, ≥ 95% hồ sơ đủ trường và 100% truy vết thay đổi
quan trọng là mục tiêu gợi ý; cần baseline, quy tắc đo và xác nhận trước khi cam kết.

## 2. Quyết định triển khai đã chốt

| Hạng mục | Quyết định | Hiện trạng |
| --- | --- | --- |
| Backend | Java 21 + Spring Boot REST API | POM đã có cấu hình Java 21 và Spring Boot 4.1.2 |
| Build | Maven | Người dùng đã gửi ảnh BUILD SUCCESS ở checkpoint validate |
| Web backend | Spring MVC | POM có spring-boot-starter-webmvc |
| Persistence | Spring Data JPA/Hibernate + PostgreSQL | Chưa triển khai |
| Schema migration | Flyway | Chưa triển khai |
| Xác thực/phân quyền | Spring Security, kiểm tra phía backend | Chưa triển khai; cơ chế phiên cụ thể chưa chốt |
| Frontend | Next.js + TypeScript | Chưa tạo project |
| Môi trường làm việc | Source/editor trên Windows; build/test/runtime hướng tới Linux containers | Đã dùng container Maven để kiểm tra POM theo kết quả người dùng gửi |
| Quản lý service | Docker Desktop + Docker Compose | compose.yaml đã có API + PostgreSQL; kiểm tra cấu hình qua, runtime chưa xác minh |
| Vị trí Compose | src/compose.yaml | Giữ vị trí hiện tại, không tự đổi giữa root và src |
| Khởi tạo project | Tự tạo và hiểu từng file | Spring Initializr không bắt buộc |
| Kiến trúc backend | Một ứng dụng chia module theo nghiệp vụ | Không tạo container/microservice riêng cho từng use case |
| Lưu tệp | S3-compatible object storage theo phương án đã chọn | Thêm khi đến phần đính kèm; chưa triển khai |

Các chi tiết môi trường đã thống nhất:

- Không chuyển repo sang Ubuntu/WSL và không cài thêm Docker Engine trong Ubuntu
  chỉ để tiếp tục lộ trình hiện tại.
- Docker Desktop có thể dùng WSL 2 ở bên dưới; người dùng thao tác bằng editor
  và terminal Windows, ứng dụng chạy trong Linux containers.
- Không yêu cầu chạy Maven Wrapper .cmd trên máy; Maven/JDK có thể chạy trong container.
- Image công cụ đã dùng trong hướng dẫn: maven:3.9.11-eclipse-temurin-21.
- Source hiện dùng PostgreSQL `postgres:17-bookworm` và runtime
  `eclipse-temurin:21-jre-jammy`; chưa xác minh build/chạy trong phiên này.

Báo cáo còn đề xuất NestJS/Prisma. Quyết định của người dùng cho hướng triển khai
hiện tại là Spring Boot/JPA/Flyway. ADR-001 và các phần báo cáo liên quan cần được
đồng bộ; việc đồng bộ này chưa thực hiện.

## 3. Cấu trúc source thực tế

Snapshot tại lần cập nhật này:

~~~text
src/
├── README.md                  Hồ sơ ngữ cảnh triển khai này
├── .env.example               Biến PostgreSQL và API_PORT cho môi trường dev
├── .gitignore                 Bỏ qua env riêng và backend/target
├── compose.yaml               API + PostgreSQL, network, volume và db healthcheck
└── backend/
    ├── pom.xml                Đã có cấu hình Maven/Spring Boot
    ├── Dockerfile             Maven build, Java 21 JRE runtime, user 10001
    ├── .dockerignore          Loại target, metadata editor và env riêng
    └── src/main/
        ├── java/vn/medora/api/
        │   ├── MedoraApplication.java        Class khởi động Spring Boot
        │   └── health/
        │       └── HealthController.java    GET /health trả chuỗi UP
        └── resources/
            ├── application.properties      Datasource, JPA validate, Flyway
            └── db/migration/
                └── V1__create_medora_schema.sql   CREATE SCHEMA medora
~~~

POM hiện có:

- Parent: org.springframework.boot:spring-boot-starter-parent:4.1.2.
- Project: vn.medora:medora-api:0.0.1-SNAPSHOT.
- Java: 21.
- Dependency: spring-boot-starter-webmvc, spring-boot-starter-data-jpa,
  PostgreSQL JDBC, spring-boot-starter-flyway và flyway-database-postgresql.
- Plugin: spring-boot-maven-plugin.

Source nền đã có nội dung. Migration V1 chỉ tạo schema, chưa có bảng nghiệp vụ;
chưa có test, module identity/patient hoặc frontend. `/health` chỉ trả chuỗi
`UP`, không thực hiện truy vấn database. Build và chạy thật cần xác minh ở 1.6.

### Cấu trúc đích dự kiến

Đây là định hướng tổ chức tiếp theo. Package gốc, resources và migration V1 đã có
như cây trên; các module nghiệp vụ, test và frontend chưa được tạo:

| Vị trí | Trách nhiệm dự kiến |
| --- | --- |
| backend/src/main/java/vn/medora/api/ | Package gốc và class khởi động |
| Các package nghiệp vụ bên dưới api/ | Identity, patient, appointment, encounter, clinical note, prescription, procedure order, billing, catalog và audit |
| backend/src/main/resources/ | Cấu hình ứng dụng và migration |
| backend/src/test/ | Kiểm thử phù hợp với các luồng đã triển khai |
| frontend/ | Giao diện Next.js theo vai trò, bổ sung sau API đầu tiên |
| compose.yaml | Định nghĩa các service; ban đầu API và database |
| .env.example | Mẫu cấu hình môi trường; không chứa thông tin xác thực thật |

Tên package chi tiết và thiết kế lớp được cụ thể hóa theo use case đang làm.
Controller/DTO xử lý giao tiếp, Service điều phối nghiệp vụ, Repository phục vụ
persistence; phân tích Boundary/Control/Entity là căn cứ phân chia trách nhiệm.
Không suy ra một actor, một ellipse use case hoặc một entity phải tương ứng
một bảng hay một container.

## 4. Nguồn thiết kế và UML

**Repo nguồn:** [SysDes_2026.1_Group_08](https://github.com/DucDung-1107/SysDes_2026.1_Group_08).

**Mốc Git local đã đối chiếu lại ngày 2026-10-24:** HEAD của nhánh `backend` và
ref `main` local cùng ở `ff3d4e6703ab1a7b81b39b21b9cecb48ae411333`, commit
ngày 2026-10-22, thông điệp "list of abbreviation". README và khung backend
có thay đổi chưa commit; commit này không chứa toàn bộ snapshot triển khai.
Lần rà soát này dùng working tree local, không xác nhận nó là bản GitHub mới nhất.

### Tài liệu nền

| Tài liệu | Vai trò |
| --- | --- |
| [AGENT.md](../AGENT.md) | Quy tắc chất lượng, traceability, ECB, phân biệt phân tích/thiết kế/triển khai |
| [WorkStation](../doc/report/%5BSYSDES_2026.1%5D%20WorkStation.md) | Phạm vi, FR/NFR, ưu tiên và tiêu chí nghiệm thu |
| [paper.tex](../doc/report/paper.tex) | Báo cáo phân tích và thiết kế |
| [appendices.tex](../doc/report/appendices.tex) | Đặc tả use case quan trọng và test case dự kiến |
| [Model inventory](../doc/asset/03-system-requirements/use-case-model/model-inventory.md) | Tên actor, UC/BR, quyết định và trạng thái chuẩn |
| [Audit hiện hành](../doc/asset/audit.md) | Kết quả review được ghi ngày 2026-10-22; thay thế bảng cũ trong figure-audit.md, không phải kết quả kiểm thử ứng dụng |
| [Cấu trúc asset](../doc/asset/README.md) | Quy ước nơi đặt source diagram, figure và asset theo chương báo cáo |
| [Analysis & Design](../ref/l04-Analysis%20%26%20Design.pdf), [Architecture](../ref/l05-Architecture%20%281%29.pdf) | Hai tài liệu môn học cho ECB, UML, kiến trúc và 4+1 views; đọc khi xử lý đúng phần mô hình liên quan |

Repo gồm `src/` cho ứng dụng; `doc/report/` cho báo cáo/phụ lục;
`doc/asset/` cho source/figure của mô hình; `ref/` cho tài liệu môn học;
`tools/plantuml.jar` cho công cụ dựng hình. Các file `.aux`, `.log`, `.toc`,
`.xdv` quanh báo cáo là đầu ra công cụ, không phải mã hay evidence ứng dụng.

### Đọc đúng nguồn khi cần chi tiết

| Việc đang làm | Chỉ cần mở thêm |
| --- | --- |
| Tiếp tục nền chạy Stage 1 | `backend/pom.xml`, file cấu hình/source thực tế và checkpoint ở mục 6; chưa cần đọc tất cả use case |
| Triển khai chức năng | FR/BR tóm tắt ở đây, diagram của module bên dưới và use case chi tiết trong `appendices.tex` nếu đã có |
| Viết API/schema/thiết kế lớp | Domain, luồng và trạng thái liên quan; các phần Chương 4 còn thiếu; không chuyển JSON minh họa thành contract đã duyệt |
| Đổi scope/quyền/trạng thái | PRD, `model-inventory.md`, bảng RBAC và traceability trong `paper.tex`, diagram bị ảnh hưởng |
| Sửa UML/báo cáo hoặc review để nộp | `AGENT.md`, nguồn mô hình, render thực tế và audit liên quan; đọc tài liệu môn học khi cần đối chiếu notation |

Khi nguồn lệch nhau: ưu tiên yêu cầu/quyết định mới nhất của người dùng cho
hướng đi; dùng source/config/schema để xác định **đã làm gì**; dùng inventory
cho tên/ID/trạng thái và D-1–D-5, PRD/đặc tả cho **cần làm gì**. Ghi rõ chỗ
chưa đồng bộ thay vì coi code hiện tại tự động thay thế mọi yêu cầu nghiệp vụ.

### Nhóm diagram và quan hệ với code

| Nhóm | Nguồn trong repo | Thông tin chi phối triển khai |
| --- | --- | --- |
| Domain model | [conceptual-domain-model.puml](../doc/asset/02-business-analysis/domain-model/uml_code/conceptual-domain-model.puml) | Khái niệm, thuộc tính và cardinality; chưa phải schema vật lý |
| Use case tổng thể | [overall-use-case-model.puml](../doc/asset/03-system-requirements/use-case-model/uml_code/overall-use-case-model.puml) | Ranh giới hệ thống và nhóm mục tiêu theo actor |
| Bệnh nhân | [patient-management.puml](../doc/asset/03-system-requirements/use-case-by-function/patient-management/uml_code/patient-management.puml) | UC-010–019; UC-055 là Phase 2 |
| Lịch hẹn | [appointment-and-queue.puml](../doc/asset/03-system-requirements/use-case-by-function/appointment/uml_code/appointment-and-queue.puml) | UC-020–026 và thông báo UC-054 |
| Check-in/hàng đợi | [check-in-and-queue.puml](../doc/asset/03-system-requirements/use-case-by-function/appointment/uml_code/check-in-and-queue.puml) | UC-027–030, tạo Encounter và queue theo quyền |
| Khám và ghi chú | [medical-examination](../doc/asset/03-system-requirements/use-case-by-function/medical-examination/uml_code/) | Khám, note, đóng lượt, chỉ định và thực hiện dịch vụ |
| Thuốc | [prescription-and-medication-safety.puml](../doc/asset/03-system-requirements/use-case-by-function/prescription/uml_code/prescription-and-medication-safety.puml) | UC-043–052 |
| Tài chính | [billing-and-payment.puml](../doc/asset/03-system-requirements/use-case-by-function/billing/uml_code/billing-and-payment.puml) | UC-060–069 |
| Quản trị | [administration-and-governance.puml](../doc/asset/03-system-requirements/use-case-by-function/administration/uml_code/administration-and-governance.puml) | UC-070–079 |
| System sequence | [sequence-by-function](../doc/asset/05-behavioral-modeling-implementation/behavioral-uml/sequence-by-function/) | Đặt lịch, check-in, ký/đính chính note, kê đơn, dịch vụ, thanh toán/hoàn tiền |
| Activity | [activity-by-workflow](../doc/asset/05-behavioral-modeling-implementation/behavioral-uml/activity-by-workflow/) | Nhánh xử lý dịch vụ và đóng lượt/tài chính |
| State machine | [state-machine-by-entity](../doc/asset/05-behavioral-modeling-implementation/behavioral-uml/state-machine-by-entity/uml_code/) | Trạng thái và guard của Appointment, Encounter, ClinicalNote, ProcedureOrder, Invoice |
| Wireframe | [ui-api-evidence/ui/code](../doc/asset/05-behavioral-modeling-implementation/ui-api-evidence/ui/code/) | Màn hình lễ tân, bác sĩ, thu ngân; là thiết kế, chưa phải UI hoạt động |
| Thiết kế cấu trúc | [04-system-design](../doc/asset/04-system-design/) | Class/ERD/package/component/deployment hiện mới có placeholder |

Các sequence hiện có là SSD ở mức actor ↔ MEDORA System. Chúng chưa chỉ ra
Controller/Service/Repository bên trong. Phần thiết kế triển khai cần bổ sung
theo từng module, đồng thời giữ đúng luồng và hậu điều kiện đã mô tả.

## 5. Nội dung nghiệp vụ phải giữ nhất quán

### Actor và quyền nhạy cảm

- Receptionist: tìm/đăng ký/sửa thông tin hành chính, lịch, check-in/hàng đợi;
  được yêu cầu hợp nhất, không tự thực hiện hợp nhất.
- Authorized Registrar: vai trò thực hiện hợp nhất hồ sơ đã được xác minh.
- Nurse: sàng lọc/sinh hiệu và công việc được giao trong phạm vi quyền.
- Doctor: khám, ghi chú, ký/đính chính, đơn và chỉ định trong lượt được phân công.
- Technician / Service Staff: thực hiện dịch vụ được phân công.
- Pharmacist / Medicine Staff: ghi nhận cấp/dùng thuốc khi có trong quy trình.
- Cashier: hóa đơn và ghi nhận thanh toán; không được yêu cầu, duyệt hoặc
  thực hiện hoàn tiền theo bảng RBAC hiện hành.
- Authorized Finance Staff: hoàn tiền theo quyền; trường hợp cần phê duyệt,
  người duyệt là Clinic Manager hoặc Authorized Finance Staff khác người yêu cầu.
- System Administrator: tài khoản, quyền và cấu hình; không mặc nhiên được đọc
  toàn bộ nội dung lâm sàng.
- Clinic Manager: giám sát vận hành/báo cáo theo phạm vi.
- Auditor: xem bằng chứng audit theo quyền, chỉ đọc.
- Patient portal: Phase 2, không tự đưa vào MVP.

Chi tiết quyền và phạm vi vẫn cần cụ thể hóa ở API; actor trên diagram không
thay thế việc kiểm tra quyền ở backend.
`Authorized Clinic User` là actor khái quát cho nhân sự đăng nhập qua UC-001,
không phải tài khoản dùng chung hay vai trò được cấp toàn quyền.

### Domain và business rule chính

| BR | Nội dung giữ nguyên khi triển khai |
| --- | --- |
| BR-001 | Patient có định danh ổn định; đăng ký kiểm tra nghi trùng; hợp nhất giữ lịch sử và đúng quyền |
| BR-002 | Kiểm tra khả dụng nguồn lực khi chốt giao dịch; yêu cầu bị từ chối không giữ tài nguyên |
| BR-003 | Appointment là lịch dự kiến, Encounter là lượt thực tế; check-in/hàng đợi không tạo lượt hoạt động trùng |
| BR-004 | Backend kiểm tra vai trò, phạm vi và trạng thái theo quyền tối thiểu |
| BR-005 | Nội dung đã ký được giữ nguyên; đính chính bằng phiên bản mới có liên kết và lý do |
| BR-006 | Cảnh báo thuốc dựa trên quy tắc/dữ liệu cấu hình; override cần đúng quyền và lý do |
| BR-007 | ProcedureOrder, phân bổ nguồn lực và kết quả thực hiện là các thông tin khác nhau |
| BR-008 | Phát hành Prescription không tự tạo bằng chứng thuốc đã cấp/dùng |
| BR-009 | Invoice, Payment và Refund tách biệt; giữ giao dịch gốc và chống gửi lặp; thanh toán không sửa nội dung lâm sàng |
| BR-010 | Hoạt động nhạy cảm có audit đủ truy vết, không sao chép thừa dữ liệu lâm sàng vào log |

Các quyết định D-1–D-5 đã được ghi trong inventory ngày 2026-10-22 ở mức thiết kế:

| Quyết định | Nội dung hiện hành |
| --- | --- |
| D-1 | Authorized Registrar là role thực hiện hợp nhất; Receptionist chỉ được yêu cầu |
| D-2 | Refund là thực thể riêng, tham chiếu một Payment; Encounter có tối đa một Invoice, Invoice có nhiều Payment |
| D-3 | Năm bộ trạng thái ở bảng dưới là tên chuẩn dùng cho mô hình |
| D-4 | Refund hoàn tất đưa Invoice về Partially Paid hoặc Issued theo số tiền đã trả ròng; pending không đổi Invoice |
| D-5 | Refund cần duyệt phải do Clinic Manager hoặc Authorized Finance Staff khác người yêu cầu duyệt |

Một số quan hệ trong domain model:

- Patient có patientId, fullName, dateOfBirth; liên kết 1–1 với MedicalRecord
  ở mức phân tích và có nhiều Appointment/Encounter.
- Một Appointment có thể dẫn tới 0 hoặc 1 Encounter; walk-in có thể không có Appointment.
- Encounter là ngữ cảnh cho note, đơn và chỉ định.
- Prescription có ít nhất một PrescriptionItem.
- Một Encounter có tối đa một Invoice theo quyết định D-2 hiện hành.
- Invoice có nhiều Payment; Refund tham chiếu một Payment gốc.

Kiểu khóa, nullability, cascade, constraint và cách persistence của các quan hệ
này chưa được thiết kế vật lý. Không coi domain model là danh sách bảng đã có.

Các khái niệm hỗ trợ trong PRD gồm UserAccount/Employee/Role/Permission,
PatientHistory/VitalSign/Diagnosis, MedicationAdministration, lịch nhân sự,
Room/ClinicService/ResourceBooking, Attachment/AuditLog và Notification.
Chúng giúp phân trách nhiệm module; không khẳng định tất cả đã có lớp, bảng
hoặc cần mô hình kế thừa như tên vai trò. Consent, RequestForm và dữ liệu
xét nghiệm/hình ảnh chỉ cụ thể hóa theo quy trình/phạm vi được xác nhận.

### Trạng thái chuẩn hiện hành

| Đối tượng | Trạng thái trong inventory |
| --- | --- |
| Appointment | Draft, Booked, Confirmed, CheckedIn, NoShow, Canceled |
| Encounter | Waiting, In Consultation, Awaiting Service, Awaiting Closure, Completed |
| ClinicalNote version | Draft, Signed, Amendment Draft, Amended Signed Version |
| ProcedureOrder | Created, Pending Allocation, Scheduled, Delayed, In Progress, Completed, Not Performed, Canceled |
| Invoice | Draft, Issued, Partially Paid, Paid |

Trạng thái là thiết kế, chưa có enum hoặc logic chuyển trạng thái trong source.
Appointment kết thúc lifecycle của sơ đồ hiện tại ở check-in; hoạt động khám
tiếp tục theo lifecycle Encounter. Refund chờ phê duyệt chưa làm đổi số dư/trạng thái Invoice.

### Hợp đồng hành vi của các luồng rủi ro cao

Tóm tắt từ phụ lục, SSD và quyết định hiện hành; đây là điều kiện phải giữ khi
viết code, không phải API/DTO hay cơ chế database đã có.

| Luồng | Thành công | Từ chối / biến thể phải xử lý |
| --- | --- | --- |
| UC-021 đặt lịch | Kiểm tra quyền, bệnh nhân, lịch/nguồn lực tại lúc commit; tạo Appointment và booking cần thiết trong cùng giao dịch | Xung đột/ngoài quyền không để lại booking dở; nháp không giữ nguồn lực; retry cùng khóa hợp lệ trả kết quả cũ; chưa tạo Encounter |
| UC-027 check-in | Xác minh bệnh nhân, trạng thái lịch hoặc walk-in được chấp nhận; tạo/kích hoạt Encounter và gán queue nguyên tử | Sai danh tính/phạm vi/trạng thái hoặc trùng lượt hoạt động không tạo lượt thứ hai; queue đọc theo quyền/phân công |
| UC-037/038 ký/đính chính | Bác sĩ được phân công, đủ trường và đúng version; ký lưu người/thời điểm; đính chính tạo bản kế tiếp có liên kết/lý do và ký riêng | Thiếu trường/stale version/ngoài quyền không đổi bản đã ký; không tự cho phép sửa sau đóng lượt khi chưa có chính sách |
| UC-048 phát hành đơn | Ít nhất một dòng đủ hướng dẫn, kiểm tra luật có nguồn dữ liệu; xử lý cảnh báo hoặc override đúng quyền/lý do; lưu audit | Thiếu dữ liệu/luật bắt buộc, cảnh báo chặn hoặc override trái quyền ngăn phát hành; không tạo sự kiện cấp/dùng thuốc |
| UC-039/040 dịch vụ | Tách chỉ định, phân bổ và kết quả thực hiện; kiểm tra nguồn lực tại commit; chỉ nhân sự được phép thực hiện và hoàn tất khi đủ kết quả | Chưa có slot giữ Pending Allocation, không giữ booking giả; conflict/sai trạng thái/thiếu kết quả giữ trạng thái hợp lệ trước đó; hủy có lý do và giải phóng tài nguyên |
| UC-063 thanh toán | Cashier đủ quyền; kiểm tra số tiền, tiền tệ, phương thức, số dư/trạng thái và khóa chống lặp; thêm giao dịch rồi trả số dư | Cho phép thanh toán một phần; retry cùng khóa/nội dung trả kết quả gốc, khóa xung đột hoặc yêu cầu sai không thêm hiệu ứng tài chính |
| UC-065/066 hoàn tiền | Finance đủ quyền, tham chiếu Payment gốc, số tiền/lý do hợp lệ, có phê duyệt nếu cần; tạo Refund riêng và audit | Phân biệt rejected, pending approval và completed; pending/rejected không đổi số dư/Invoice; không tự duyệt; chỉ completed trả số dư sau hoàn |
| UC-041/042 kết thúc | Bác sĩ hoàn tất tóm tắt/hướng dẫn và checklist công việc bắt buộc; xử lý chỉ định tồn trước khi đóng | Checklist chưa đạt giữ Encounter mở; chưa thanh toán giữ Invoice còn nợ; không dùng thanh toán để sửa/ký nội dung lâm sàng |

Phụ lục có 6 nhóm đặc tả chi tiết cho đặt lịch, check-in, ký/đính chính,
kê đơn, dịch vụ và tài chính; chưa phải mọi UC đều có đặc tả đầy đủ. Prescription,
Payment/Refund, RequestForm và các đối tượng khác chưa có state machine chuẩn
đầy đủ trong inventory; không tự chuyển các trạng thái gợi ý của PRD thành enum
đã được duyệt. API mẫu/JSON trong tài liệu đều là đề xuất với dữ liệu giả lập.

## 6. Lộ trình xây dựng 6 stage

Đây là kế hoạch đã trao đổi; trạng thái hoàn thành được ghi riêng bằng evidence.
API được làm trước màn hình tương ứng. Quyền, validation, audit và kiểm thử đi
cùng chức năng, không dồn toàn bộ đến stage cuối.

| Stage | Phạm vi/use case | Thiết kế liên quan | Kết quả mong đợi | Trạng thái |
| --- | --- | --- | --- | --- |
| 1. Nền chạy | Maven, app khởi động, health, Dockerfile, Compose API + PostgreSQL, migration | Quyết định stack và môi trường; chưa triển khai UC nghiệp vụ | Build/chạy và kết nối/migration thực sự được kiểm tra | Source nền đã có; Compose config hợp lệ; chờ xác minh build/runtime/migration ở 1.6 |
| 2. Tài khoản và bệnh nhân | UC-001; phần UC-070–071; UC-010, 011, 012, 018 và audit tương ứng | FR-001–004, BR-001/004/010, patient use case, domain model | Đăng nhập/quyền, tìm/đăng ký/sửa hành chính, phát hiện nghi trùng | Đã chia checkpoint hướng dẫn; chưa có source nghiệp vụ |
| 3. Lịch và tiếp nhận | Danh mục/lịch/nguồn lực tối thiểu; UC-020–030 | BR-002/003; SSD đặt lịch/check-in; state Appointment/Encounter | Đặt/đổi/hủy không xung đột; check-in và queue đúng trạng thái/quyền | Chưa bắt đầu |
| 4. Khám và hồ sơ | UC-013–014, UC-031–038, UC-041–042 | BR-005; SSD ký/đính chính; state note/Encounter | Khám, sinh hiệu, note/chẩn đoán, ký/đính chính và đóng ca chưa cần dịch vụ | Chưa bắt đầu |
| 5. Thuốc và dịch vụ | UC-039–040, UC-043–052, UC-056–057; UC-015 khi có đính kèm | BR-002/006/007/008; SSD thuốc/dịch vụ, activity và state order | Đơn, cảnh báo cấu hình, phân bổ, thực hiện/kết quả, tệp có quyền; hoàn thiện đóng ca có dịch vụ | Chưa bắt đầu |
| 6. Tài chính và hoàn thiện | UC-060–079 còn lại; UC-016–017, UC-019; audit/report và demo | BR-001/009/010; SSD payment/refund, state Invoice, wireframe | Giá/hóa đơn, thanh toán/hoàn tiền/phê duyệt, hợp nhất, quản trị/báo cáo và xác minh toàn luồng | Chưa bắt đầu |

Các phần quản trị danh mục và nguồn lực được bổ sung ngay khi module phụ thuộc
cần chúng; không đợi stage 6 mới có dữ liệu phục vụ đặt lịch/kê đơn.
Cổng bệnh nhân, thông báo ngoài và tích hợp chưa chốt giữ trong backlog theo
phạm vi/ưu tiên của PRD; không ghi là implemented vì có tên trong diagram.
Stage 2–6 là kế hoạch triển khai, không phải sáu module đã có source. Những
phần P1 như hợp nhất, cấp/dùng thuốc, nháp/khôi phục nâng cao và yêu cầu ra về
giữ nguyên mức ưu tiên; chỉ đưa vào mốc thực hiện khi xác định rõ phạm vi.
FR-019 cũng là P1, chưa có adapter xét nghiệm/hình ảnh được chọn. Danh mục,
giá, quyền và audit tối thiểu phải đi cùng module phụ thuộc trước khi nghiệm thu.

### Chi tiết tiến độ Stage 1

| Checkpoint | Nội dung | Trạng thái thực tế |
| --- | --- | --- |
| 1.1 | Cấu hình Maven và validate | POM đã lưu, hiện thêm JPA/PostgreSQL/Flyway; giữ ảnh validate phiên trước như ghi nhận lịch sử, chưa build lại |
| 1.2 | Class khởi động, cấu hình ứng dụng, health endpoint | MedoraApplication.java, HealthController.java và application.properties có nội dung; GET /health dự kiến trả UP; chưa xác minh runtime |
| 1.3 | Dockerfile nhiều stage và .dockerignore | Đã có Maven build và Java 21 JRE runtime chạy bằng user 10001; chưa xác minh build |
| 1.4 | Compose API + db, biến môi trường, network/volume/healthcheck | Đã có cấu hình; docker compose config --quiet thành công ngày 2026-10-24; chưa xác minh container |
| 1.5 | Datasource PostgreSQL, persistence/Flyway và migration | Đã có dependency/cấu hình; V1 chỉ CREATE SCHEMA medora, chưa có bảng nghiệp vụ; chưa xác minh migration |
| 1.6 | Kiểm tra dựng lại, dữ liệu bền vững, migration và tổng kết | Chưa qua: Docker Desktop Linux daemon chưa kết nối được; localhost:8080/health chưa kết nối được trong phiên 2026-10-24 |

**Phần triển khai tiếp theo:** hoàn tất checkpoint 1.6 bằng build/chạy container,
HTTP health, kiểm tra lịch sử Flyway và tính bền vững qua recreate không xóa volume.
Sau đó bắt đầu 2.1. Chưa có use case bệnh nhân được triển khai.

### Điều kiện hoàn thành checkpoint 1.2

- Có class khởi động Spring Boot dưới package gốc `vn.medora.api` và cấu hình
  ứng dụng tối thiểu phù hợp với dependency thực có trong POM.
- Build/khởi động thật bằng hướng công cụ container đã chọn; ghi lại lệnh đã
  chạy, phiên bản/image, kết quả và lỗi còn lại. Chưa có Dockerfile hoàn chỉnh
  không đồng nghĩa phải chuyển sang cài Maven/JDK local.
- Có HTTP health endpoint gọi được và trả kết quả dự kiến. Route/payload và
  lựa chọn controller hay Actuator được quyết định khi làm; POM hiện chưa có Actuator.
- Ghi rõ health kiểm tra tiến trình/API hay cả dependency. Source hiện có
  PostgreSQL/Flyway nhưng `/health` không truy vấn chúng; cần evidence riêng
  cho kết nối database và migration.
- Người dùng hiểu vai trò file vừa viết; cập nhật cây source, evidence và điểm
  tiếp theo ở đây. Đạt checkpoint này mới chuyển sang Dockerfile ở 1.3.

### Checkpoint Stage 2 — kế hoạch hướng dẫn, chưa triển khai

Giữ cách học do người dùng xác nhận ngày 2026-10-24: giải thích mục đích,
giao một checkpoint để người dùng tự viết, rồi review source và kết quả chạy.
Không tự tạo toàn bộ module thay người dùng. Hướng dẫn/lệnh/code mẫu ở hội thoại;
README chỉ lưu phạm vi, quyết định, tiến độ và evidence.

| Checkpoint | Phạm vi | Điều kiện kiểm tra trước khi qua bước |
| --- | --- | --- |
| 2.1 | Thiết kế identity và schema tối thiểu cho tài khoản, role/permission, facility scope; chọn cơ chế phiên và chính sách mật khẩu | Thống nhất bảng/constraint, quyền tối thiểu, dữ liệu giả lập và vòng đời phiên; lựa chọn mới được ghi rõ là đề xuất trước khi chốt |
| 2.2 | Migration identity và ánh xạ JPA/Repository | Flyway tạo bảng đúng schema, Hibernate validate qua; mật khẩu được hash, dữ liệu test không chứa thông tin thật |
| 2.3 | UC-001: đăng nhập, nhận diện phiên, đăng xuất, hết hạn/thu hồi phiên | Đúng tài khoản vào được; sai/khóa trả lỗi chung; logout/hết hạn/thu hồi chặn phiên cũ; không lộ mật khẩu/hash |
| 2.4 | UC-070–071 tối thiểu: quản lý tài khoản, gán quyền/phạm vi, vô hiệu hóa | API kiểm tra quyền phía server; cập nhật quyền/khóa có hiệu lực theo chính sách phiên đã chọn; ghi audit |
| 2.5 | Schema và API tìm/đăng ký/sửa hành chính bệnh nhân: UC-010–012 | Mã bệnh nhân duy nhất, validation, phân trang có giới hạn, quyền/phạm vi đúng; sửa không ghi đè phiên bản mới hơn |
| 2.6 | UC-018 phát hiện nghi trùng trước đăng ký; audit và nghiệm thu Stage 2 | Cảnh báo để xác minh; không tự hợp nhất; kiểm tra các ca không quyền, ngoài scope, gửi lặp/đồng thời khi liên quan và audit |

Phạm vi quyền phải đối chiếu bảng RBAC trong `paper.tex`: Receptionist có
R/C/U hồ sơ hành chính; Nurse/Doctor có R trong phạm vi; System Administrator
quản lý tài khoản/quyền, không mặc định có quyền hồ sơ bệnh nhân. Các quyền R
có điều kiện của vai trò khác chỉ mở khi mô-đun và điều kiện tương ứng được làm.
FR-004 hợp nhất hồ sơ vẫn ở Stage 6/P1; phát hiện nghi trùng trong Stage 2
không có nghĩa FR-004 đã được triển khai. Stage 2 khác Phase 2 của cổng bệnh nhân.

## 7. Evidence và mức độ xác minh

| Nội dung | Bằng chứng đã có | Kết luận được phép ghi |
| --- | --- | --- |
| POM | Đã đọc file local, có parent 4.1.2, Java 21, Web MVC, JPA, PostgreSQL, Flyway và Maven plugin | Cấu hình đã được viết và đối chiếu; chưa có kết quả build mới |
| Maven validate | README trước lần rà soát này ghi người dùng gửi ảnh BUILD SUCCESS ngày 2026-10-23, có plugin 4.1.2; phiên này chưa mở lại ảnh hoặc chạy validate | Giữ như ghi nhận lịch sử từ người dùng, không coi là kiểm chứng độc lập trong phiên này |
| Dòng lệnh validate đầy đủ | Không có trong phần ảnh hiển thị | Không ghi đây là một lần chạy độc lập của AI |
| App/config/health | Đã đọc MedoraApplication.java, HealthController.java và application.properties ngày 2026-10-24; có nội dung | Source đã triển khai; /health chỉ trả UP, không kiểm tra DB |
| Compile/test/API/JAR | Chưa có kết quả build mới; HTTP localhost:8080/health chưa kết nối được | Chưa xác minh runtime; không suy ra code lỗi chỉ từ việc không có API chạy |
| Dockerfile/Compose | Đã đọc cấu hình; docker compose config --quiet exit 0 ngày 2026-10-24 | Compose hợp lệ về cấu hình; chưa chứng minh image build hay container chạy |
| Database/Flyway | Có datasource/Flyway và V1 CREATE SCHEMA medora; Docker context desktop-linux chưa kết nối được daemon khi gọi compose ps/logs | Đã có source/config; chưa xác minh kết nối, migration hoặc dữ liệu bền vững |
| Chức năng nghiệp vụ/UI | Chưa có source | Chưa triển khai |

BUILD SUCCESS của validate không chứng minh code Java đã biên dịch, unit test
đã chạy, API nhận HTTP, migration thành công hoặc database sẵn sàng. Health trả
UP cũng chỉ chứng minh đúng phạm vi mà endpoint kiểm tra.

### Bản đồ nghiệm thu và kiểm thử đã đặc tả

AC là tiêu chí tổng thể của PRD; TC là bộ kiểm thử dự kiến trong báo cáo/phụ
lục. Bảng này đối chiếu theo nội dung, không có nghĩa 13 TC đã bao phủ toàn bộ
34 FR, 16 NFR hoặc mọi AC. **TC-001–TC-013 đều Not Run** tại snapshot này.

| TC | Điều phải chứng minh | AC liên quan / giai đoạn |
| --- | --- | --- |
| TC-001 | Gọi API đọc/ghi trực tiếp khi thiếu quyền: không lộ dữ liệu và không đổi trạng thái | AC-09; bắt đầu Stage 2 và lặp theo module |
| TC-002 | Nhận diện nghi trùng; receptionist không hợp nhất; giữ nguồn và lịch sử | AC-01; Stage 2, phần hợp nhất Stage 6/P1 |
| TC-003 | Hai yêu cầu tranh cùng slot/nguồn lực: tối đa một được commit, không booking mồ côi | AC-02, AC-14; Stage 3 và phân bổ Stage 5 |
| TC-004 | Check-in lần đầu tạo Encounter/queue; lần trùng không tạo thêm lượt hoạt động | AC-01, AC-14; Stage 3 |
| TC-005 | Thiếu trường không ký; đính chính có lý do/liên kết, giữ bản ký cũ | AC-03, AC-07; Stage 4; checklist đóng lượt cần ca bổ sung |
| TC-006 | Cảnh báo/override đúng luật/quyền; phát hành đơn không tạo sự kiện cấp/dùng | AC-04; Stage 5 |
| TC-007 | Phân bổ không xung đột; thiếu kết quả không hoàn tất; lưu người/thời điểm thực hiện | AC-05, AC-06; Stage 5 |
| TC-008 | Thanh toán một phần và retry cùng khóa không tính tiền lần hai; số dư đối soát được | AC-08, AC-14; Stage 6; snapshot giá cần ca bổ sung |
| TC-009 | Refund sai bị từ chối; cần duyệt thì pending không đổi số dư; hoàn tất tạo sự kiện riêng, giữ Payment gốc | AC-08, AC-09; Stage 6; đối chiếu bản TC-009 đầy đủ ở paper.tex |
| TC-010 | Báo cáo/xuất/audit đúng scope và quyền, có bằng chứng sự kiện | AC-09, AC-10, AC-11; Stage 6; độ khớp số liệu cần ca đối soát |
| TC-011 | p95 màn hình/tìm kiếm dưới tải/dataset được duyệt, gắn cấu hình và build | AC-13; khi có môi trường và luồng chạy |
| TC-012 | Restore database và tệp, kiểm tra toàn vẹn, đo RPO/RTO so với mục tiêu đã duyệt | AC-12; khi có thiết kế backup/restore |
| TC-013 | Bàn phím, nhãn/lỗi/tương phản, tiếng Việt, ngày giờ/tiền tệ trên trình duyệt mục tiêu | NFR-10/11/14; từng màn hình và trước nghiệm thu |

AC-14 còn bao gồm timeout, retry và thông báo thành công giả; các ca concurrency
ở trên chưa thay thế toàn bộ kiểm thử lỗi mạng/phụ thuộc. Phần bệnh nhân tự xem
tài liệu trong AC-09 chỉ thực hiện khi bật Phase 2. Chỉ số dashboard/báo cáo,
checklist đóng lượt và các yêu cầu chưa có TC chi tiết phải được bổ sung tiêu
chí kiểm tra khi đến module, không đánh dấu đạt từ bảng tổng hợp này.

### Điều kiện nhận một chức năng và một artefact báo cáo

Chức năng chỉ được ghi **implemented** khi có code/config/schema tương ứng;
**verified** cần kết quả chạy với expected/actual, build, môi trường, vai trò,
thời điểm và evidence. Kiểm tra luồng chính, lỗi quan trọng, quyền phía server,
trạng thái/dữ liệu, audit và gửi lặp/đồng thời khi liên quan; dùng dữ liệu giả lập.

Giữ trace `mục tiêu/BR → FR/NFR → UC → thiết kế/API → TC → evidence`.
Domain model không thay ERD; SSD chỉ có actor và một MEDORA System, không có
Controller/Service/Repository. Khi cần ECB/sequence bên trong, tạo đúng mức
phân tích/thiết kế; không sửa SSD thành sơ đồ nội bộ rồi giữ nguyên nhãn.

Theo `AGENT.md`, diagram và lời giải thích cần review scope/UML/flow/nhất quán/
trách nhiệm/visual/văn phong, render ở kích thước báo cáo, ghi đủ bảy điểm;
overall ≥ 9,0 và các chiều critical ≥ 9 mới qua gate. Có file PNG hay bảng
audit cũ không chứng minh một bản sửa mới đã qua review. README lần này không
chấm lại diagram hoặc xác nhận toàn bộ báo cáo sẵn sàng nộp.

## 8. Phần còn thiếu và điểm cần giữ lại cho phiên sau

- ADR-001 và nội dung NestJS/Prisma trong báo cáo chưa đồng bộ với hướng Spring Boot.
- Class Diagram triển khai, ERD, API contract và schema vật lý chưa có.
- Cơ chế phiên, ma trận quyền chi tiết và cách thực thi facility scope chưa chốt.
- Chính sách cơ sở cho walk-in, active Encounter, checklist ký/đóng lượt,
  cảnh báo thuốc và ngưỡng duyệt hoàn tiền cần được cụ thể hóa khi đến module.
- Một số đặc tả dùng tên PaymentTransaction trong khi domain model dùng Payment;
  cần thống nhất tên và ánh xạ ở stage tài chính.
- Không có integration ngoài hay production deployment đã được chứng minh.
- Kiểu khóa, versioning/concurrency, idempotency và transaction là các quyết định
  triển khai sẽ cần gắn với luồng và constraint, không suy ra từ tên diagram.

### Các chỗ chưa đồng bộ đã thấy trong repo

| Nguồn | Chỗ cần lưu ý | Cách hiểu trong lúc chưa sửa |
| --- | --- | --- |
| paper.tex, phần Source Code Organization / Environment | Đề xuất NestJS/Prisma, layout apps/packages/infra và nói src/README là placeholder | Là đề xuất/snapshot trước; dùng Spring Boot và khung source thực tế ở mục 2–3. Cần cập nhật ADR-001, layout và bảng evidence trong báo cáo |
| Chương 4 và asset/04-system-design | Đề mục trống/thư mục placeholder; phụ lục ghi dành cho người phụ trách Chương 4 | Chưa có thiết kế kiến trúc/lớp/ERD hoàn chỉnh, không đánh dấu đã thiết kế vì có heading |
| WorkStation, mục trạng thái | Lifecycle gợi ý còn gộp lịch hẹn với bước phục vụ/hoàn tất | Dùng trạng thái canonical D-3 cho năm đối tượng đã mô hình hóa; phần gợi ý khác chưa là enum chuẩn |
| Model inventory, phần Modelling decisions | Vẫn nói cần viết tiếp đặc tả chi tiết cho luồng rủi ro | appendices.tex đã có 6 nhóm đặc tả; cần hoàn thiện phần còn thiếu, không coi như chưa có đặc tả nào |
| Phụ lục tài chính/TC-009 so với paper.tex và SSD | Tên PaymentTransaction khác Payment/Refund; TC-009 phụ lục ngắn hơn và không nêu rõ nhánh pending | Giữ Payment và Refund riêng theo D-2/D-4/D-5, dùng đầy đủ ba kết quả rejected/pending/completed; cần đồng bộ lại phụ lục |
| WorkStation NFR-02/03 và paper.tex | PRD có con số gợi ý, báo cáo vẫn để availability/RPO/RTO chưa xác định | Đây là mục tiêu chưa được chốt, không ghi thành cam kết hay kết quả đo |

### Quyết định cần chốt đúng lúc

| Thời điểm | Nội dung chưa chốt |
| --- | --- |
| Stage 1 | Source hiện chọn /health trả UP, port API mặc định 8080, postgres:17-bookworm, runtime Java 21 JRE; còn phải xác minh build/container, kết nối, lịch sử migration và dữ liệu bền vững |
| Trước Stage 2 | Cơ chế phiên/token, mật khẩu/thu hồi phiên, role-permission chi tiết, scope cơ sở và dữ liệu test tối thiểu |
| Trước Stage 3 | Chính sách walk-in, định nghĩa trùng active Encounter, ca/slot/đệm, ràng buộc nguồn lực và cách xử lý cạnh tranh |
| Trước Stage 4–5 | Mẫu bắt buộc khi ký/đóng, sửa sau ký/sau đóng, luật và nguồn cảnh báo, phân công/kết quả dịch vụ, quyền/loại/dung lượng tệp |
| Trước Stage 6 | Tiền tệ/làm tròn, giảm giá, giới hạn refund/phê duyệt, định nghĩa báo cáo/đối soát và yêu cầu hóa đơn áp dụng |
| Trước vận hành thật | Hosting, tải/SLA, retention, backup/restore, khóa/secrets, trách nhiệm vận hành và yêu cầu pháp lý cụ thể |

Chưa có deadline, phân công triển khai chi tiết hoặc thứ tự ticket ngoài lộ
trình được xác nhận trong hồ sơ này. Không cần hỏi lại toàn bộ backlog ở mỗi
phiên; chỉ giải quyết quyết định ảnh hưởng checkpoint đang làm. Nếu người dùng
chưa quyết định, ghi rõ đề xuất và tác động thay vì biến đề xuất thành sự thật.

## 9. Nhật ký ngắn

| Ngày | Sự kiện | Mốc sau sự kiện |
| --- | --- | --- |
| 2026-10-23 | Chốt Spring Boot, tự tạo project, build/runtime bằng Linux containers và Docker Compose; source ở Windows | Giữ một phương án môi trường ổn định |
| 2026-10-23 | Người dùng tạo khung backend, Dockerfile, Compose và env mẫu | Các file khung ban đầu trống |
| 2026-10-23 | POM đã có nội dung; người dùng gửi ảnh Maven BUILD SUCCESS | Ghi nhận checkpoint 1.1; chưa có app Java |
| 2026-10-23 | Người dùng làm rõ README trong src là hồ sơ cấu trúc/UML/tiến độ để AI đọc lại, không chứa bài hướng dẫn | README này trở thành nguồn ngữ cảnh triển khai chính; chưa tiến sang code 1.2 |
| 2026-10-23 | Rà soát README với PRD, báo cáo/phụ lục, inventory, source UML liên quan và khung backend local | Bổ sung mục tiêu, 34 FR/16 NFR, quyết định/luồng, nghiệm thu và điểm lệch tài liệu; không thay đổi tiến độ ứng dụng |
| 2026-10-23 | Người dùng yêu cầu tạo cây file rỗng cho checkpoint 1.2 và giải thích kiến thức để tự code | Đã tạo ba file rỗng, chưa build/chạy; điểm tiếp tục là người dùng viết MedoraApiApplication.java |
| 2026-10-24 | Đối chiếu source: app/health, Dockerfile, Compose, JPA/PostgreSQL/Flyway và migration V1 đã có nội dung; AI chạy Compose config thành công, nhưng Docker daemon và HTTP health chưa kết nối được | Cập nhật snapshot theo source; chờ xác minh 1.6, không ghi Stage 1 đã verified |
| 2026-10-24 | Người dùng yêu cầu tiếp tục đến Stage 2 và xác nhận muốn tự code theo checkpoint; AI đối chiếu PRD, patient use case và RBAC, chia Stage 2 thành 2.1–2.6 | Có kế hoạch hướng dẫn, chưa thêm code identity/patient hay chốt cơ chế phiên; điểm tiếp tục là 1.6 |

## 10. Quy ước cập nhật hồ sơ

Sau mỗi mốc thực sự có thay đổi hoặc kiểm chứng, cập nhật cấu trúc thực tế,
trạng thái stage/checkpoint, evidence, quyết định mới và phần triển khai tiếp theo.
Ghi rõ kết quả do người dùng cung cấp hay AI trực tiếp kiểm tra. Đối chiếu source
trước khi đổi trạng thái; giữ lịch sử vừa đủ để hiểu tình trạng hiện tại.

Hướng dẫn code, giải thích từng dòng, lệnh thực hành và ví dụ được trao đổi trong
hội thoại. README chỉ lưu bối cảnh, thiết kế, cấu trúc, phạm vi và tiến độ dự án.

### Cách dùng ở đầu và cuối mỗi phiên

1. Đầu phiên đọc README này, yêu cầu mới nhất và thay đổi Git/source ở phần
   đang làm; xác nhận checkpoint còn đúng. Không quét lại toàn bộ báo cáo nếu
   chưa có lý do hoặc thay đổi liên quan.
2. Dùng bản đồ mục 4 để mở đúng nguồn. Đọc hướng dẫn framework theo phiên bản
   dependency thực tế trước khi viết code; tên stack ở README không thay tài liệu API.
3. Trong phiên giữ một checkpoint rõ ràng, giải thích và review theo cách học
   người dùng đã chọn. Khi có yêu cầu trực tiếp sửa code, thực hiện đúng phạm vi đó.
4. Cuối phiên cập nhật nội dung đã đổi, file thật, việc đã chạy/kết quả, lỗi còn
   lại, quyết định mới và đúng một điểm tiếp tục. Không cộng tiến độ từ hướng
   dẫn đã đưa ra hay file rỗng; không ghi một lần chạy cũ thành evidence mới.

Khi thiếu ảnh/log từ phiên trước, giữ rõ nguồn là ghi nhận lịch sử, không bịa
kết quả. Nếu source mới mâu thuẫn README, ghi nhận sai lệch rồi sửa snapshot;
không bỏ qua yêu cầu nghiệp vụ hoặc âm thầm thay đổi scope để khớp code.
