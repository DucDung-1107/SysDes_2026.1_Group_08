# Kiểm thử MEDORA

Cập nhật 2026-11-07. Kết quả dưới đây là bằng chứng chạy source hiện tại; không phải chứng nhận lâm sàng, load/SLA hoặc production readiness. Hướng dẫn sử dụng/UAT nằm ở [USER_GUIDE.md](USER_GUIDE.md).

## Kết quả đã chạy

| Kiểm tra | Kết quả và phạm vi |
| --- | --- |
| Backend unit/security | **10 passed**: 8 security-flow tests và 2 CSV escaping/formula tests |
| Backend integration | **36 passed**, 0 failure/error/skipped, PostgreSQL 17.6 tạm, JDK21/Maven3.9.11 trên Windows |
| Route coverage thực thi | **128/128 method + normalized path có phản hồi 2xx**; 79 phản hồi 4xx trong các ca từ chối, 0 phản hồi 5xx |
| Frontend | Lint, typecheck, **34 tests** và production build passed |
| Dependency audit frontend | `npm audit --omit=dev`: **0 advisory**. Full install còn **10 moderate** trong cây tooling dev ESLint/typescript-eslint/minimatch; không force downgrade framework để che kết quả |
| Docker build/runtime mới | API + frontend build từ source; DB mới, migration V1–V7, bootstrap và proxy chạy trên port kiểm tra 18080/13000 |
| HTTP end-to-end qua frontend Docker | **PASS**, 161 request được ghi nhận, 46 method/path khác nhau; booking, clinical, thuốc/cấp thuốc, dịch vụ/kết quả/review, checksum tệp, hóa đơn/thu/hoàn, báo cáo/audit/merge |
| HTTP từ chối | 7 ca: Cashier không đọc note/hoàn tiền, Doctor không đọc báo cáo tiền, overpay, sửa hồ sơ đã đóng, ngoài facility, mutation thiếu CSRF |
| Ledger/idempotency HTTP | Subtotal 150.000, discount 10.000, thu 140.000, hoàn 30.000 sau duyệt độc lập; số dư 30.000, dòng tiền tăng 110.000; retry payment trả ID gốc, vẫn đúng hai khoản thu |
| Session qua frontend demo | **13 checks passed**, gồm form login 204, cookie rotation/flags, fresh CSRF, permissions, PageView, denial, logout |
| Dữ liệu demo | 10 tài khoản vai trò; 14 hồ sơ (13 chính, một nguồn merge được giữ). Kiểm tra rerun/ID và trạng thái trong evidence riêng |
| Backend Linux | **46 passed** (10 unit + 36 integration), 0 failure/error/skipped; user 10001, PostgreSQL 17.6 tạm; 128/128 route thành công, 79 phản hồi 4xx, 0 phản hồi 5xx |
| Browser UAT/animation | **Chưa chạy**: công cụ không có browser surface; không có screenshot/visual acceptance |

`ApiCoverageRecorder` ghi request MockMvc qua Spring MVC/Security và DB thật; health, auth login/logout cũng được đưa vào inventory. Coverage là đã thực thi từng method/path với phản hồi thành công, **không phải** 100% branch/line coverage hay mọi tổ hợp nghiệp vụ. Test HTTP riêng chạy TCP thật qua Next.js rewrite đến API container. Hai loại bằng chứng bổ sung cho nhau.

## Những kiểm tra sâu có trong bộ backend

- Thật sự chạy Flyway/JPA validation, scope giữa hai cơ sở, quyền vai trò, phân công và thu hồi phiên.
- Account create/list/detail/update/roles/password qua API; không lộ password; global change cần quyền ở mọi cơ sở; stale version không ghi đè.
- Nghi trùng bệnh nhân, xác minh/merge và giữ source/timeline; hồ sơ chính không tự thay dữ liệu dị ứng.
- Booking retry/key conflict, buffer, nghỉ/đổi/hủy/no-show, hai booking đồng thời chỉ một thắng, không để booking dở.
- Check-in trùng, queue scope, phân điều dưỡng, sinh hiệu/tiền sử đính chính giữ bản cũ.
- SOAP nháp/ký/bổ sung/bỏ nháp bổ sung, trả lượt để sửa, checklist và kết thúc; signed evidence bất biến.
- Policy/profile thiếu hoặc UNKNOWN chặn thuốc; dị ứng chặn cứng, WARN cần acknowledgement, override cần đúng quyền/lý do; version catalog/rule/profile/draft.
- Đơn sửa/bổ sung/hủy giữ items/evidence, cấp/dùng thuốc đúng người/số lượng/thời gian, retry không sinh sự kiện trùng.
- Chỉ định chưa phân lịch không giữ tài nguyên; slot/phòng/thiết bị/năng lực/nhân sự được kiểm tra; start/complete/review và giải phóng tài nguyên.
- Private upload/content/size/scope/checksum/archive; URL storage không public.
- Giá hiệu lực, snapshot invoice, discount có quyền/lý do, thanh toán một phần, overpay và thanh toán đồng thời.
- Refund pending khác completed, người duyệt khác requester, reservation ngăn over-refund, reject giải phóng giữ chỗ, retry execute một lần.
- Cashflow/receipt/CSV khớp ledger, CSV quote/escape và formula prefixes; scope báo cáo và audit.
- Inbox chỉ người nhận, read idempotent; outbox retry không rollback appointment; duyệt ra về và chính sách cơ sở.

## Chạy lại backend

JDK21 và Maven3.9+ trên host, từ `backend`:

```powershell
mvn -B -ntp test
mvn -B -ntp -Ppostgres-it verify
```

Test PostgreSQL được tạo tạm riêng dưới `target/postgres-it`; không nhận URL/credential của DB demo. Kết quả JUnit ở `target/surefire-reports` và `target/failsafe-reports`; route report ở `target/api-route-coverage.tsv`. Các thư mục này được Git ignore.

Trên máy chỉ có Docker, từ `src/`:

```powershell
docker build -f backend/Dockerfile.test -t medora-api-tests backend
```

Dockerfile test chạy Maven và embedded PostgreSQL bằng user 10001. Không mount volume demo. Build đầu cần tải dependency; các lần sau dùng cache. Để bắt buộc chạy lại khi source không đổi, thêm `--no-cache` vào lệnh build; cache dependency vẫn được tái sử dụng.

## Chạy lại frontend

Từ `frontend` sau `npm.cmd ci`:

```powershell
npm.cmd run lint
npm.cmd run typecheck
npm.cmd test
npm.cmd run build
```

34 tests kiểm tra CSRF/session transport, 204/non-JSON errors, không auto-retry mutation, abort/401, permissions, UUID/path/query, múi giờ, DTO arrays, version/validation và các route của 36 module đối chiếu controller source. Chúng không bấm form trong browser.

## Chạy HTTP trên môi trường kiểm tra riêng

Đây là bài test **có tạo dữ liệu** và giao dịch giả lập; mỗi lần chạy tạo fixture mới. Không chạy lên DB người bệnh thật. Bộ mặc định cần policy thử nghiệm đang bật và auto-refund limit bằng 0 để kiểm tra duyệt độc lập; script không ghi đè policy đang có để ép pass.

Từ `src/`, dùng port/project riêng nếu demo đang chạy 3000/8080:

```powershell
python scripts/init_env.py --output .env.api-test --api-port 18080 --web-port 13000
docker compose --project-directory . --env-file .env.api-test -p medora-api-test up --build -d --wait
python scripts/test_api_workflows.py --base-url http://127.0.0.1:13000 --env-file .env.api-test
```

`init_env.py` sẽ giữ nguyên file đã có và báo lỗi; khi chạy lại cùng môi trường bỏ bước init. Chờ API `/ready` ở port18080 trả `UP` nếu backend còn khởi động. Giữ project/env nhất quán; project khác sẽ dùng volume khác.

Kiểm tra session không tạo dữ liệu nghiệp vụ, với stack demo sẵn sàng:

```powershell
python scripts/smoke_session.py --base http://127.0.0.1:3000 --env-file .env
```

Với máy hiện tại thay env bằng `.env.docker-test`. Script đọc bootstrap vào memory, không in cookie/token/password. Security/audit events thông thường vẫn có thể được ghi.

## Bằng chứng và giới hạn

- [Tổng hợp backend Windows](evidence/backend-tests.json) và [Linux](evidence/backend-linux-tests.json).
- [128 method/path Windows](evidence/api-route-coverage.tsv) và [Linux](evidence/api-route-coverage-linux.tsv), không có UUID/credential.
- [HTTP workflow và ca từ chối](evidence/http-workflows.json).
- [Session/CSRF qua frontend](evidence/session-proxy.json).
- [Đối chiếu bộ demo](evidence/demo-verification.json).

Các lỗi thật được sửa trong lượt này: repository account/assignment bị lệch tên method so với service; query account list trả sai kiểu và alias sắp xếp sai. Regression tests gọi các API đó trên PostgreSQL thật. Không sửa schema hoặc bỏ guard để làm test pass. Seed dùng thời gian backend cho sinh hiệu/start để không bị lệch đồng hồ host/container, giữ validation server.

Stack kiểm tra cài mới dùng project `medora-portable-check` ở 18080/13000. Sau khi lưu bằng chứng, ba container và hai volume chỉ chứa fixture thử nghiệm của project này đã được dọn. Project demo `medora-stage4-test`, DB/tệp và credential demo được giữ. Hai service app đang chạy bằng user không phải root (`10001:10001` cho API, `node` cho frontend).

Chưa xác minh bucket S3 ngoài, antivirus/CDR, clinical knowledge correctness, tải lớn/SLA, production backup/restore, nhiều node, external integrations hoặc mọi browser/thiết bị. UI manual còn cần login/form/navigation/focus/keyboard, zoom/reflow, reduced motion và console/network theo USER_GUIDE. Tài liệu cũ trong archive chỉ là bằng chứng lịch sử, không thay kết quả hiện hành.
