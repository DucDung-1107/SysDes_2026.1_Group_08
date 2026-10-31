# Kiểm thử Docker — 2026-10-31

**Bản ghi mốc Stage 4.** Môi trường này đã được nâng cấp đến Stage 6 và kiểm thử
33 ca; xem [STAGE56-VERIFICATION.md](STAGE56-VERIFICATION.md).

AI đã chạy trực tiếp trên Docker Desktop do người dùng bật, với working tree
`SysDes_2026.1_Group_08`. Phạm vi vẫn là backend Stage 1–4; chưa triển khai Stage 5–6.

## Kết quả

| Kiểm tra | Actual |
| --- | --- |
| Docker | Docker Desktop 4.55.0, Engine 29.1.3, Linux amd64 |
| Build image | Dockerfile multi-stage build executable JAR thành công; 8 security tests trong bước build qua |
| Kiểm thử lại trong Linux | `mvn -B -ntp -Ppostgres-it verify`: 8 SecurityFlowTest + 10 Stage4IT, 0 failures/errors/skipped |
| Thời điểm bộ test hoàn tất | 09:43:13 +07:00; BUILD SUCCESS; 2 phút 1 giây |
| Runtime | `java -jar app.jar` trong Java 21 JRE container, user 10001:10001 |
| Database Compose | PostgreSQL 17.11 (`postgres:17-bookworm`); Flyway V1–V4 thành công, JPA validate qua |
| Database integration test | PostgreSQL 17.6 tạm trong Linux test container, chạy dưới user không phải root |
| HTTP | `/health` và `/ready` đều 200 UP; `/api/auth/me` chưa login trả 401; login thiếu CSRF trả 403 |
| Demo qua API Docker | Hai lần PASS / COMPLETED, trước và sau recreate |
| Persistence | Tạo lại cả API và DB container không xóa volume; dữ liệu trước/sau giống nhau |

Demo chạy qua HTTP thật: tạo nhân sự theo role, bệnh nhân, phòng, dịch vụ và ca
làm; đặt lịch/retry, xác nhận, check-in; ghi sinh hiệu/tiền sử; khám, chẩn đoán,
nháp, ký, đính chính, ký lại và đóng Encounter.

Snapshot trước/sau recreate cùng có: 4 tài khoản, 1 bệnh nhân, 1 lịch hẹn, 1 lượt
đã Completed, 2 note đã ký, 19 audit events và 4 migration thành công. ID của cả
hai container thay đổi, nhưng ID dữ liệu và nội dung note gốc/đính chính còn nguyên.
Sau đó demo được chạy thêm một lần và tiếp tục PASS; snapshot persistence là
bằng chứng tại thời điểm trước lần demo bổ sung đó.

## Môi trường đang chạy

- Compose project: `medora-stage4-test`.
- API: `medora-stage4-test-api-1`, chỉ publish `127.0.0.1:8080`.
- DB: `medora-stage4-test-db-1`, không publish port ra host.
- Volume: `medora-stage4-test_postgres_data`.
- Tài khoản bootstrap: `admin.demo`; mật khẩu ngẫu nhiên chỉ lưu trong
  `src/.env.docker-test`, đã được Git ignore.

Volume cũ `medora_postgres_data` không được mount vào môi trường test. `.env`
gốc không bị thay đổi; không xóa volume, không Flyway repair. Container chạy
bộ integration test tạm đã được dọn sau khi lưu report; API và DB demo vẫn chạy
để người dùng kiểm tra. Có thêm image builder test trong Docker cache.

Từ `src/`, có thể chạy lại:

```powershell
docker compose -p medora-stage4-test --env-file .env.docker-test ps
Invoke-RestMethod http://127.0.0.1:8080/ready
python scripts/demo_stage4.py --env-file .env.docker-test
```

Mỗi lần chạy demo tạo thêm dữ liệu giả lập. Script đã được bổ sung tùy chọn
`--env-file` để dùng đúng credential của môi trường test; biến môi trường
`MEDORA_BOOTSTRAP_PASSWORD` vẫn được ưu tiên nếu đã đặt. `python -m py_compile`
và demo thực tế với tùy chọn này đều thành công.

Dừng môi trường test, giữ dữ liệu:

```powershell
docker compose -p medora-stage4-test --env-file .env.docker-test down
```

Môi trường agent dùng thêm `--config <thư-mục-Docker-tạm>` do sandbox chặn đọc
config Docker trong home. Lựa chọn đó không thay đổi Compose project, image hay
volume. Chưa commit/push/deploy production.

## Evidence đã lưu

- [DockerSecurityFlowTest.txt](evidence/2026-10-31/DockerSecurityFlowTest.txt)
- [DockerStage4IT.txt](evidence/2026-10-31/DockerStage4IT.txt)
- [Tóm tắt build](evidence/2026-10-31/docker-build-summary.txt)
- [Tóm tắt kiểm thử Linux](evidence/2026-10-31/docker-test-summary.txt)
- [HTTP smoke](evidence/2026-10-31/http-smoke.json)
- [Demo trước recreate](evidence/2026-10-31/docker-demo-result.json)
- [Demo sau recreate](evidence/2026-10-31/docker-demo-after-recreate.json)
- [DB trước recreate](evidence/2026-10-31/persistence-before.json)
- [DB sau recreate](evidence/2026-10-31/persistence-after.json)
- [Nội dung hồ sơ sau recreate](evidence/2026-10-31/persistence-content.json)
- [Container trước](evidence/2026-10-31/containers-before.txt), [sau](evidence/2026-10-31/containers-after.txt)
- [Runtime/image](evidence/2026-10-31/docker-runtime.txt), [log startup](evidence/2026-10-31/api-startup-summary.txt)

Log đầy đủ ở `backend/docker-stage4-build.log`, `backend/docker-stage4-it.log`
và `backend/docker-stage4-persistence.log` (được ignore). PowerShell có thể bọc
stdout/stderr tiến độ của Docker trong `NativeCommandError` dù lệnh exit 0;
kết quả ở đây được đối chiếu exit code, report và HTTP/SQL thực tế.

Đây là bằng chứng build, runtime và persistence local. Chưa kiểm tra tải,
backup/restore, UAT lâm sàng, frontend/accessibility hay deployment production.
