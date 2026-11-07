# MEDORA

Ứng dụng nội bộ cho phòng khám ngoại trú: tiếp nhận, lịch hẹn, hồ sơ khám, thuốc, dịch vụ, tệp riêng tư, thanh toán và quản trị. Frontend có 36 workspace, dùng API thật và phân quyền theo cơ sở/phân công.

| Thành phần | Công nghệ |
| --- | --- |
| Frontend | Next.js 16.4.5, React 19.3.1, TypeScript, Lucide, CSS/SVG |
| Backend | Java 21, Spring Boot 4.1.2, Spring Security, Maven |
| Dữ liệu | PostgreSQL 17, JPA/Hibernate + JDBC, Flyway V1–V7 |
| Đăng nhập | Session cookie HttpOnly/SameSite=Lax, CSRF; idle 30 phút |
| Runtime | Linux containers qua Docker Compose, thao tác source trên Windows hoặc Linux |

## Dựng trên máy mới

Cần Git, Docker Desktop chạy **Linux containers** (hoặc Docker Engine + Compose v2 trên Linux) và Python 3.10+. Cách chạy này không yêu cầu Java/Maven/Node trên host. Lần build đầu cần Internet.

Từ PowerShell, checkout nhánh có phiên bản ứng dụng này:

```powershell
git clone --branch feature https://github.com/DucDung-1107/SysDes_2026.1_Group_08.git
cd SysDes_2026.1_Group_08
python src/scripts/init_env.py
docker compose --project-directory src --env-file src/.env up --build -d
docker compose --project-directory src --env-file src/.env ps
Invoke-RestMethod http://127.0.0.1:8080/ready
python src/scripts/seed_demo.py --base-url http://127.0.0.1:8080 --env-file src/.env
```

Trên Linux dùng cùng lệnh Docker và `python3` nếu không có alias `python`. Health/readiness phải trả `UP`; nếu API còn khởi động, đợi rồi kiểm tra lại trước khi seed.

- Giao diện: **http://127.0.0.1:3000**.
- Backend: **http://127.0.0.1:8080**, `/health` và `/ready`.
- PostgreSQL không mở port ra host.
- `init_env.py` sinh mật khẩu vào `src/.env`, **không ghi đè file đã có**. Mẫu biến nằm tại [src/.env.example](src/.env.example).
- Bootstrap `admin.demo` chỉ được tạo ở profile `dev` khi bảng tài khoản rỗng. Mật khẩu ở `MEDORA_BOOTSTRAP_PASSWORD` trong env tương ứng. Đổi biến bootstrap không reset tài khoản đã có.
- Tài khoản demo theo vai trò và mật khẩu sinh ngẫu nhiên nằm trong `src/.demo/demo.json`, đã được Git ignore. Xem [DEMO.md](docs/DEMO.md).

Clone không mang theo DB/mật khẩu. Máy mới cần init cấu hình và seed; không copy `node_modules`, `.next` hoặc backend `target` từ Windows sang máy khác.

## Môi trường test đã có trên máy hiện tại

DB test đang dùng Compose project `medora-stage4-test`, file `src/.env.docker-test`. Tên project được giữ để chọn đúng volume cũ; đó không phải phiên bản ứng dụng. Không đổi project nếu muốn tiếp tục dữ liệu cũ.

```powershell
docker compose --project-directory src --env-file src/.env.docker-test -p medora-stage4-test up -d db api
python src/scripts/seed_demo.py --base-url http://127.0.0.1:8080 --env-file src/.env.docker-test
```

Nếu frontend Node local đang chiếm port 3000, dùng phiên đó hoặc dừng nó trước khi bật service `frontend`. Không bật hai frontend cùng port.

## Dùng và kiểm thử

- [Dữ liệu demo, tài khoản, UUID và trạng thái](docs/DEMO.md).
- [Hướng dẫn thao tác theo vai trò](docs/USER_GUIDE.md): tiếp nhận → khám → thuốc/dịch vụ → kết thúc → thu tiền; ca lỗi và animation.
- [Lệnh kiểm thử và kết quả thực thi](docs/TESTING.md).
- [Hợp đồng API](docs/API.md).
- [Kiến trúc và giới hạn](docs/ARCHITECTURE.md), [thiết kế giao diện](docs/DESIGN.md), [nguồn tham khảo UI](docs/frontend-references.md).

Admin cấu hình nhân sự/danh mục, không tự có quyền khám bệnh. Bác sĩ/điều dưỡng, kỹ thuật viên và dược sĩ cần đúng phân công. Các tab cùng browser profile dùng chung cookie: đăng xuất rồi đổi tài khoản, hoặc dùng profile riêng. Seed ghi dữ liệu giả lập có tiền tố DEMO vào PostgreSQL thật; frontend không dùng hồ sơ hard-code.

## Phát triển frontend trên host

Cần Node 24 LTS; backend có thể tiếp tục chạy bằng Docker. Từ `src/frontend`:

```powershell
npm.cmd ci
# Chỉ copy khi chưa có .env.local:
Copy-Item .env.example .env.local
npm.cmd run dev
```

Trên Linux dùng `npm` thay `npm.cmd`. API local mặc định `http://127.0.0.1:8080`; sửa `MEDORA_API_ORIGIN` nếu đổi port. `NEXT_PUBLIC_FACILITY_ZONE` phải khớp backend; `NEXT_PUBLIC_FACILITY_NAMES` là map tên cơ sở công khai, không chứa secret.

```powershell
npm.cmd run lint
npm.cmd run typecheck
npm.cmd test
npm.cmd run build
npm.cmd start
```

Docker frontend dùng API nội bộ `http://api:8080`, build standalone Linux và chạy user không phải root. Rewrite origin và biến `NEXT_PUBLIC_*` cố định khi build; đổi chúng cần build lại frontend. Không đặt mật khẩu vào biến public/build args.

Backend host tùy chọn cần JDK 21 và Maven 3.9+: từ `src/backend`, chạy `mvn -B -ntp test`, rồi `mvn -B -ntp -Ppostgres-it verify`. Integration tests dùng PostgreSQL tạm, không dùng DB demo. Cách kiểm thử Linux không phải root nằm trong [TESTING.md](docs/TESTING.md).

## Vận hành và dữ liệu

Với môi trường cũ, thay env và thêm `-p medora-stage4-test` vào các lệnh sau:

```powershell
docker compose --project-directory src --env-file src/.env logs --tail 100 api frontend
docker compose --project-directory src --env-file src/.env up --build -d
docker compose --project-directory src --env-file src/.env stop
docker compose --project-directory src --env-file src/.env down
```

`stop` và `down` giữ volume. **Không thêm `-v` nếu muốn giữ DB/demo/tệp.** Backup cần cả `postgres_data`, `attachment_data` và cấu hình cần khôi phục. Manifest seed trong `.demo` cần giữ riêng để chạy lại an toàn; mất manifest không được script tự reset mật khẩu hay nhận bừa tài khoản đã tồn tại.

Nếu port bận, sửa `API_PORT`/`WEB_PORT` trong env. Nếu login lỗi sau đổi env trên DB cũ, dùng mật khẩu ban đầu; env không phải lệnh reset. Nếu 403, kiểm tra vai trò/cơ sở/phân công/CSRF. Nếu 409, đọc lỗi version/trạng thái/xung đột; không gửi lặp khi chưa rõ kết quả.

## Cấu trúc

```text
src/backend/       Spring Boot, migrations, backend tests
src/frontend/      Next.js, component, token, frontend tests
src/scripts/       Init cấu hình, seed và kiểm tra HTTP
src/compose.yaml   PostgreSQL + API + frontend
docs/              Tài liệu ứng dụng hiện hành
doc/report/        PRD, báo cáo LaTeX, đặc tả môn học
doc/asset/         UML và hình của báo cáo
ref/, tools/       Tham chiếu và công cụ báo cáo
```

[AGENT.md](AGENT.md) chứa quy tắc dự án. Báo cáo/UML là tài liệu môn học; một số snapshot đề xuất stack cũ chưa đồng bộ source. Không dùng mô tả NestJS/Prisma cũ để dựng ứng dụng này.

## Đẩy lên remote

```powershell
git status --short
git diff --check
git diff --stat
git add README.md .gitignore docs src
git diff --cached --stat
git commit -m "Complete MEDORA frontend, demo fixtures, API tests and deployment docs"
git push -u origin feature
```

Nhánh hiện tại là `feature`; không dùng force. Env, `.demo`, log/cache/dependency/output build đã ignore; không thêm bằng `git add -f`. Nếu remote có commit mới, fetch/rebase và xử lý xung đột trước khi push, giữ lịch sử người khác.
