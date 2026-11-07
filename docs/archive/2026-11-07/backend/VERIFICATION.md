# Evidence — backend Stage 1–4

**Evidence hiện hành Stage 1–6:** [STAGE56-VERIFICATION.md](STAGE56-VERIFICATION.md).

**Cập nhật 2026-10-31:** Docker đã được bật và kiểm tra thành công: build/run
Linux, 18 tests qua, demo HTTP và persistence qua recreate. Xem
[DOCKER-VERIFICATION.md](DOCKER-VERIFICATION.md). Các giới hạn về Docker ở
bản ghi ngày 2026-10-30 bên dưới là lịch sử và đã được kiểm tra bổ sung.

Ngày chạy: **2026-10-30, Asia/Saigon**. AI chạy trực tiếp trên working tree
`PTTK/SysDes_2026.1_Group_08`, nhánh local `backend`; không chỉ kiểm tra bộ mẫu
riêng ở thư mục tmp. Chưa commit hoặc push.

## Môi trường và kết quả

| Hạng mục | Expected / actual |
| --- | --- |
| Build | Maven 3.9.11, Oracle JDK 21, Windows amd64; Spring Boot 4.1.2 / Security 7.1.2; compile 47 file main và đóng gói executable JAR thành công |
| SecurityFlowTest | Expected tất cả qua; actual 8 tests, 0 failures/errors/skipped |
| Stage4IT | Expected tất cả qua; actual 10 tests, 0 failures/errors/skipped |
| Database | PostgreSQL 17.6 thật, tạm và riêng; Flyway V1–V4 qua, Hibernate validate qua |
| HTTP | Tomcat thật trên localhost/cổng ngẫu nhiên; health/readiness, cookie, login/me và thu hồi phiên qua |
| Booking concurrency | Hai transaction cùng slot: actual một 201 và một 409; một Appointment và đúng hai ResourceBooking |
| Clinical integrity | Thiếu trường/stale version bị chặn; SQL sửa signed note hoặc encounter Completed bị trigger từ chối; source cũ còn nguyên sau đính chính |
| Demo CLI | `demo_stage4.py` chạy qua HTTP với profile dev và DB mới; tạo admin bằng bootstrap, tạo nhân sự qua API, đặt lịch/retry, xác nhận/check-in, sinh hiệu, note, ký/đính chính/ký lại, hoàn tất Encounter; actual PASS / COMPLETED |
| Python | `python -m py_compile scripts/demo_stage4.py` exit 0 |
| Compose | `docker --config <thư-mục-tạm> compose --env-file .env.example config --quiet` exit 0 |
| Git whitespace | `git diff --check` không có lỗi; có thông báo chuẩn hóa LF/CRLF của Git |

Lệnh build cuối, từ `src/backend/` (cache Maven trong thư mục tmp được chỉ định
riêng vì môi trường sandbox không truy cập cache mặc định):

```text
mvn -B -ntp -Dmaven.repo.local=<cache-tạm> -Ppostgres-it verify dependency:build-classpath -Dmdep.outputFile=target/test-classpath.txt -DincludeScope=test
```

Actual: **BUILD SUCCESS**, kết thúc **09:41:29 +07:00**, tổng thời gian 1 phút
47 giây. JAR: `backend/target/medora-api-0.0.1-SNAPSHOT.jar`.

SHA-256 của JAR đã kiểm tra:

```text
7A09F36CA308E7F5F5FBAB2C4D08032B6C6E834941623A82A0DB832E8426A3CA
```

Demo được chạy sau build bằng harness tạm: mở PostgreSQL riêng, khởi động app
từ `target/classes` với profile dev, truyền mật khẩu ngẫu nhiên qua environment
cho subprocess Python và đóng app/DB khi xong. Kết quả PASS lúc **09:44:49 +07:00**.
Không đưa mật khẩu vào source, đối số tiến trình Python hoặc output. Các ID demo
trong evidence thuộc DB đã dừng và dọn, không phải dữ liệu trong Compose của user.

## Bản lưu ngắn

- [SecurityFlowTest.txt](evidence/2026-10-30/SecurityFlowTest.txt)
- [Stage4IT.txt](evidence/2026-10-30/Stage4IT.txt)
- [build-summary.txt](evidence/2026-10-30/build-summary.txt)
- [demo-result.json](evidence/2026-10-30/demo-result.json)

Log đầy đủ ở `backend/stage4-build.log`, `backend/target/surefire-reports/`,
`backend/target/failsafe-reports/` và `backend/target/demo-run.log`; các output
build không được đưa vào Git. Các lỗi PostgreSQL trong negative tests như
“immutable”, “append-only” hay unique constraint là kết quả mong đợi, không phải
test failure. Dòng FATAL khi tắt DB tạm là đóng kết nối test còn trong pool.

## Giới hạn của evidence

Docker daemon chưa sẵn sàng; chưa build/chạy image Compose hoặc kiểm tra volume
qua recreate trong phiên này. Không chạy deployment production. Việc build JAR
thành công và chạy app từ classes không thay thế kiểm thử image Linux.

Chưa chạy load/performance, backup/restore, trình duyệt/accessibility, UAT,
session timeout đủ 30 phút, nhiều node, hoặc nghiệm thu vận hành thật. Bộ test
không khẳng định bao phủ toàn bộ FR/NFR hay tất cả 13 TC trong báo cáo. TC-001–005
có các hành vi liên quan được thực thi ở backend theo danh sách trong STAGE4;
không tự cập nhật các ô Not Run của báo cáo thành Passed cho toàn TC.

Baseline V2 thiếu bảng role và V3 rỗng đã được sửa trên source; chỉ có bằng chứng
migration từ DB mới, không có bằng chứng nâng cấp database đã từng áp dụng V2/V3
khác checksum. Không chạy Flyway repair, không xóa volume hay dữ liệu cũ.

Code gốc trước khi sửa được sao lưu cục bộ ở
`C:/Users/chanl/Downloads/tmp/medora-before-stage4-20261030/`. Báo cáo/UML cũ chưa
được render/review hay đồng bộ; không coi chúng đã hoàn tất từ kết quả code này.
