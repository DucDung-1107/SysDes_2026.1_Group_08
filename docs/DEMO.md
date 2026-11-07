# Dữ liệu và tài khoản demo

Seed qua API thật vào PostgreSQL, không nhúng dữ liệu giả vào frontend. Mọi hồ sơ/danh mục được tạo nhận diện rõ `DEMO`; tên, bệnh sử, thuốc, liều, kết quả và giao dịch chỉ để thử phần mềm.

## Tạo hoặc kiểm tra lại bộ demo

Sau khi stack trong [README](../README.md) sẵn sàng, chạy từ root:

```powershell
python src/scripts/seed_demo.py --base-url http://127.0.0.1:8080 --env-file src/.env
```

Máy đang dùng DB test cũ thay env bằng `src/.env.docker-test`. Có thể kiểm tra qua origin frontend bằng `--base-url http://127.0.0.1:3000`. Mặc định chỉ nhận loopback; không trỏ vào môi trường người bệnh thật.

Bộ seed dùng tiếp nhận không hẹn cho các fixture lâm sàng nên môi trường demo cần `MEDORA_WALK_IN_ENABLED=true` như cấu hình được sinh mặc định. Nếu đổi múi giờ backend, truyền cùng tên zone bằng `--zone`. Namespace dài 2–19 ký tự thường/số/gạch nối, bắt đầu bằng chữ.

Namespace mặc định `demo`. Checkpoint và mật khẩu chung ngẫu nhiên của các tài khoản demo được lưu tại **`src/.demo/demo.json`**. File này bị ignore; không đưa vào remote. Script không in mật khẩu ra log, không reset mật khẩu và không ghi đè policy đang có. Nếu policy hiện có bị tắt, script dừng để người phụ trách môi trường test xử lý rõ ràng.

Nếu shell đã đặt biến `MEDORA_BOOTSTRAP_PASSWORD`, giá trị đó được ưu tiên hơn env file. Dùng đúng credential của DB đang chọn; đừng tạo lại file env để thử reset một DB đã có tài khoản.

## Đăng nhập theo vai trò

| Username | Vai trò |
| --- | --- |
| `demo.rec` | Lễ tân |
| `demo.nurse` | Điều dưỡng |
| `demo.doc` | Bác sĩ |
| `demo.tech` | Kỹ thuật viên |
| `demo.pharm` | Dược sĩ |
| `demo.cash` | Thu ngân |
| `demo.finance` | Tài chính |
| `demo.manager` | Quản lý cơ sở |
| `demo.registrar` | Nhân sự hợp nhất hồ sơ |
| `demo.audit` | Kiểm toán viên |

Đọc mật khẩu trên máy của bạn khi cần đăng nhập:

```powershell
$demoLocal = Get-Content -LiteralPath src/.demo/demo.json -Raw -Encoding UTF8 | ConvertFrom-Json
$demoLocal.password
$demoLocal.accounts
```

Bootstrap `admin.demo` là tài khoản riêng, dùng mật khẩu trong env. Không dùng mật khẩu demo ở hệ thống khác. Các tài khoản demo cùng facility; Doctor/Nurse, Pharmacist/Technician được gán các hồ sơ tương ứng. Dùng profile browser độc lập hoặc đăng xuất trước khi đổi vai trò.

## Dữ liệu chuẩn bị

Bộ mặc định tạo/tái sử dụng **10 tài khoản, 14 hồ sơ bệnh nhân giả lập**: 13 hồ sơ chính còn xuất hiện trong tìm kiếm và một hồ sơ nguồn đã hợp nhất được giữ lại. Có hai phòng/dịch vụ/nhóm năng lực và ca làm, hai thuốc giả lập, một rule WARN, cùng giá dịch vụ. Thuốc/policy là fixture thử nghiệm, không phải tri thức lâm sàng đã được kiểm định.

| Fixture | Trạng thái để demo |
| --- | --- |
| Lịch đã đặt | Appointment BOOKED |
| Lịch đã xác nhận | Appointment CONFIRMED |
| Đang chờ tiếp nhận khám | Encounter WAITING, đã gán Doctor/Nurse |
| Đã sàng lọc chờ bác sĩ | WAITING, đã có sinh hiệu xác minh/tiền sử |
| Đang khám có ghi chú nháp | IN_CONSULTATION, SOAP DRAFT |
| Đơn thuốc chờ cấp | Đơn ISSUED, Pharmacist đã được gán, cấp một phần |
| Dịch vụ đã phân lịch | Order SCHEDULED cho Technician |
| Dịch vụ đang thực hiện | Order IN_PROGRESS, có tệp kết quả private mẫu |
| Đã kết thúc chưa lập hóa đơn | Encounter COMPLETED |
| Hóa đơn thanh toán một phần | Tổng 120.000, thu 60.000, còn 60.000 VND |
| Hóa đơn đã thanh toán | Tổng/đã thu 120.000, số dư 0 |
| Hóa đơn có lịch sử hoàn | Thu 120.000, hoàn 20.000, số dư 20.000 |
| Hợp nhất hồ sơ | Hai hồ sơ giả lập nguồn/chính, source được giữ lại |
| Thông báo nội bộ | Notification/inbox cho Doctor; worker xử lý bất đồng bộ |

ID và trạng thái quan sát gần nhất ở `fixtures`/`summary` trong manifest. Trên UI mở **Chi tiết → Mã tham chiếu/Mã bản ghi**; không dùng mã `PT-…` thay UUID. Các nguồn/phân công ở `accounts` và `resource:*` trong manifest.

## Chạy lại, ngày mới và lỗi dở chừng

Chạy lại cùng manifest/namespace xác minh các tài khoản và dữ liệu, không tự reset lượt/hóa đơn đã thao tác. Script checkpoint sau từng bước; sau khi sửa nguyên nhân lỗi, chạy lại để tiếp tục. Một số mutation không có idempotency phía backend, nên nếu mất kết nối đúng lúc commit cần đối chiếu DB/API trước khi gửi lại.

Lịch và ca làm là dữ liệu theo thời điểm seed, không tự trôi theo ngày. Ca mẫu dài ba ngày.

Chỉ định SCHEDULED chỉ được bắt đầu trong slot được phân. Nếu slot mẫu đã qua, Doctor dùng **Phân lịch / đổi lịch** sang một khung hiện tại hợp lệ, rồi Technician bắt đầu; không sửa đồng hồ hoặc bỏ kiểm tra thời điểm của server.

Khi cần bộ mới để trình diễn vào ngày khác:

```powershell
python src/scripts/seed_demo.py --env-file src/.env --namespace demo2
```

Tài khoản thành `demo2.rec`, `demo2.doc`, …; credential/UUID ở `.demo/demo2.json`. Giữ bộ cũ để bảo toàn lịch sử; đừng sửa đồng hồ hoặc reset hồ sơ đã ký. Nếu mất manifest mà username đã tồn tại, script từ chối nhận bừa/reset mật khẩu; khôi phục manifest hoặc chọn namespace mới.

Máy mới chạy cùng seed tạo dữ liệu tương đương, **UUID và mật khẩu mới**. Muốn chuyển nguyên trạng cần backup/restore DB + tệp private và giữ manifest an toàn; Git không chứa DB, tệp upload hoặc credential.
