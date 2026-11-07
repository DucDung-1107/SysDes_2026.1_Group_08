# UAT frontend MEDORA: từ tiếp nhận đến thanh toán

Cập nhật: **2026-11-07**, Asia/Saigon. Đây là hướng dẫn **kiểm thử thủ công trên trình duyệt**, đối chiếu nhãn giao diện hiện tại và controller/service backend. Các bước bên dưới là kết quả mong đợi, chưa phải biên bản UAT đã đạt.

Đã có bằng chứng HTTP: 13 kiểm tra phiên/CSRF/quyền qua frontend và một lượt giả lập Stage 4 đi từ đặt lịch đến ký bổ sung, kết thúc lượt. Chưa chạy toàn bộ luồng thuốc–dịch vụ–tài chính bằng trình duyệt; chưa xác minh phát animation vì phiên công cụ không có browser surface. Xem [frontend-verification.md](frontend-verification.md).

## Chọn mức kiểm thử

| Mức | Tài khoản cần dùng | Phạm vi |
| --- | --- | --- |
| Cốt lõi, nên làm trước | Admin chuẩn bị; Receptionist, Nurse, Doctor thực hiện | Tạo hồ sơ → lịch hẹn → tiếp nhận → sinh hiệu → chẩn đoán/SOAP → ký → kết thúc |
| Thêm tài chính | Thêm Manager chuẩn bị giá và xem báo cáo, Cashier thu tiền | Lượt đã kết thúc → hóa đơn → thanh toán một phần/đủ → biên nhận |
| Luồng mở rộng | Thêm Technician và Pharmacist | Thuốc/cấp thuốc, dịch vụ/kết quả/tệp; thực hiện **trước khi kết thúc lượt** |
| Nhánh hoàn tiền | Thêm Finance; Manager hoặc Finance khác duyệt | Yêu cầu → người khác phê duyệt → thực hiện hoàn |

Để làm nhanh lần đầu, bỏ nhánh thuốc/dịch vụ ở bước 5. Khi luồng cốt lõi chạy đúng, tạo **một lượt UAT mới** rồi thêm nhánh này; lượt `COMPLETED` không được mở lại để bổ sung nội dung khám.

## Trước khi bắt đầu

- Dùng môi trường thử nghiệm và dữ liệu mới có tiền tố `UAT`, ví dụ hậu tố riêng cho buổi chạy `1107A`. Không dùng người bệnh thật, số giấy tờ thật hoặc dữ liệu điều trị thật.
- Frontend: `http://127.0.0.1:3000`. Backend test: `http://127.0.0.1:8080`; `/health` và `/ready` phải trả `UP`. Nếu frontend đang chạy, dùng phiên đó; không mở thêm server trên cùng cổng.
- Khi cần khởi động frontend, dùng lệnh tại [frontend-implementation.md](frontend-implementation.md#launch-locally). Giữ `MEDORA_API_ORIGIN` trỏ đúng backend test. `NEXT_PUBLIC_FACILITY_ZONE` mặc định `Asia/Ho_Chi_Minh`; đối chiếu với múi giờ backend. Mọi trường ngày giờ bên dưới theo **giờ cơ sở**, không suy theo múi giờ máy người test.
- Chọn cùng một cơ sở ở góc trên cho mọi tài khoản. Tài khoản, bệnh nhân, phòng, dịch vụ, giá và lượt khám đều phải thuộc cơ sở đó.
- **Nhiều tab cùng browser profile dùng chung `JSESSIONID`.** Không đăng nhập mỗi vai trò trong một tab cùng profile. Đăng xuất/đăng nhập lần lượt, hoặc dùng các profile trình duyệt độc lập/Chrome và Edge. Các cửa sổ cùng phiên ẩn danh cũng có thể chia sẻ cookie.
- Mỗi biểu mẫu có bước xác nhận phải được đọc và đánh dấu xác nhận trước khi gửi. Chỉ ghi nhận thành công sau dòng **“Máy chủ đã xác nhận yêu cầu.”** và dữ liệu/trạng thái thực tế trả về. Với thanh toán, đọc thêm số dư mới.
- Sau thao tác, chọn **Đóng**, rồi **Tải lại** nếu cần để đọc phiên bản mới. Trường **Phiên bản đang xem** hoặc **Phiên bản đang chỉnh sửa** lấy từ dữ liệu đã đọc, không tự tăng số.

### Ghi lại đúng UUID

Chọn **Chi tiết** ở hàng dữ liệu và sao chép **Mã tham chiếu**; với bảng lâm sàng có thể là **Mã bản ghi**. Kết quả tạo mới cũng hiển thị mã trả về. Giữ một bảng ghi chú cục bộ gồm:

| Giá trị cần giữ | Nơi lấy |
| --- | --- |
| UUID từng nhân sự | Chi tiết trong **Tài khoản nhân sự** |
| UUID phòng/dịch vụ | Chi tiết **Phòng khám** / **Dịch vụ khám** |
| UUID bệnh nhân | Chi tiết/kết quả tạo **Bệnh nhân**; không dùng chuỗi mã `PT-…` thay UUID |
| UUID lịch hẹn | Kết quả tạo hoặc chi tiết **Lịch hẹn** |
| UUID lượt khám | Kết quả **Xác minh và tiếp nhận**, hoặc chi tiết hàng đợi |
| UUID thuốc | **Mã bản ghi** trong **Danh mục thuốc**, khác mã hoạt chất |
| UUID đơn và dòng thuốc | Chi tiết đơn; **Các thuốc → Xem thông tin → Mã bản ghi** của từng dòng, khác UUID thuốc danh mục |
| UUID chỉ định | Kết quả **Tạo chỉ định** / chi tiết **Chỉ định theo lượt khám** |
| UUID thanh toán/hoàn tiền | **Các khoản đã thu** / **Lịch sử hoàn tiền** trong chi tiết hóa đơn |

Chưa có API danh bạ nhân sự cho lễ tân/bác sĩ: nhập UUID đã xác minh do admin cung cấp. Không lấy username, tên hiển thị hay mã phòng làm UUID. Không ghi mật khẩu, cookie hoặc CSRF token vào bảng ghi chú/ảnh chụp.

## Bước 1 — Admin chuẩn bị người dùng và nguồn lực

### Tài khoản

Admin mở [Tài khoản nhân sự](http://127.0.0.1:3000/workspace/accounts), chọn **Tạo tài khoản**. Tạo username riêng như `uat.rec.1107a`; mật khẩu tự chọn ít nhất 12 ký tự, tối đa 72 byte UTF-8. Mỗi tài khoản UAT chỉ cần vai trò tương ứng:

| Người test | Role code | Công việc chính |
| --- | --- | --- |
| Lễ tân | `RECEPTIONIST` | Hồ sơ hành chính, đặt lịch, tiếp nhận, phân điều dưỡng |
| Điều dưỡng | `NURSE` | Sinh hiệu/tiền sử/hồ sơ an toàn của lượt được gán |
| Bác sĩ | `DOCTOR` | Khám, chẩn đoán, ký, thuốc, chỉ định, kết thúc |
| Kỹ thuật viên | `TECHNICIAN` | Thực hiện chỉ định được gán, tệp kết quả |
| Dược sĩ | `PHARMACIST` | Ghi cấp/dùng thuốc của đơn được gán |
| Thu ngân | `CASHIER` | Lập/phát hành hóa đơn, ghi nhận tiền, biên nhận |
| Tài chính | `FINANCE` | Giảm giá, yêu cầu/thực hiện hoàn, duyệt yêu cầu của người khác |
| Quản lý | `CLINIC_MANAGER` | Giá, chính sách/danh mục, báo cáo, duyệt hoàn/ra về |

Admin hiện có `SYSTEM_ADMINISTRATOR` dùng để chuẩn bị và xem audit; **không gán thêm Doctor cho admin để giả làm bác sĩ**. Admin không mặc định được đọc/sửa hồ sơ khám. Có thể tạo thêm Doctor/Nurse/Technician/Pharmacist thứ hai, không được phân công, để test 403.

### Phòng, dịch vụ, ca làm

1. [Phòng khám](http://127.0.0.1:3000/workspace/rooms) → **Thêm phòng**: tạo `UAT_CONS_1107A`, tên phòng khám UAT, loại `CONSULTATION`, bật **Cho phép sử dụng**. Nếu test dịch vụ, thêm phòng `UAT_LAB_1107A`, loại `LAB`.
2. [Dịch vụ khám](http://127.0.0.1:3000/workspace/services) → **Thêm dịch vụ**: tạo dịch vụ khám UAT, thời lượng `20` phút, đệm `5` phút, bật sử dụng. Nhánh dịch vụ thêm một dịch vụ xét nghiệm UAT với thời lượng/đệm tương tự.
3. Với dịch vụ xét nghiệm, mở chi tiết → **Yêu cầu phòng và thiết bị**: loại phòng yêu cầu `LAB`; để trống thiết bị nếu không kiểm thử ràng buộc thiết bị. Nếu có mã thiết bị, phòng phải chứa đủ đúng mã đó.
4. [Năng lực dịch vụ](http://127.0.0.1:3000/workspace/provider-services) → **Bổ sung năng lực**: nối UUID Doctor với dịch vụ khám; nối UUID Technician với dịch vụ xét nghiệm. Màn hình này là biểu mẫu, không có danh sách năng lực để suy đoán đã gán thành công.
5. [Ca làm và nghỉ](http://127.0.0.1:3000/workspace/staff-schedules) → **Thêm ca làm hoặc nghỉ**: loại **Ca làm**, nhân sự Doctor; khoảng từ trước giờ hiện tại đến đủ sau toàn bộ slot khám và thời gian đệm. Tạo tương tự cho Technician nếu test dịch vụ. Không có ca nghỉ chồng lên khoảng thực hiện.
6. Nếu không có chẩn đoán minh họa đang bật, Admin/Manager mở [Cấu hình chẩn đoán](http://127.0.0.1:3000/workspace/diagnosis-admin) → **Lưu mã chẩn đoán**, dùng mã UAT mới và tên ghi rõ giả lập. `DEMO-001` chỉ dùng khi đang có trong danh mục; đây không phải danh mục ICD đã được duyệt cho người bệnh thật.

### Chuẩn bị giá trước khi tạo lượt khám — nếu test tài chính

Đổi sang Manager, mở [Bảng giá dịch vụ](http://127.0.0.1:3000/workspace/prices), chọn dịch vụ vừa tạo → **Áp dụng bộ lọc** → **Thêm khoảng giá**:

- Dịch vụ khám: `100000` VND; xét nghiệm tùy chọn: `50000` VND.
- **Có hiệu lực từ**: trước lúc lễ tân check-in/tạo Encounter, ví dụ đầu ngày hôm qua cho dịch vụ UAT mới; **Kết thúc trước (nếu có)** để trống.
- Dùng dịch vụ UAT mới để tránh chồng khoảng giá. Không sửa giá lịch sử hoặc kết thúc lùi ngày một khoảng giá đang dùng.

Backend lấy giá khám tại `encounter.createdAt`, giá dịch vụ tại `order.completedAt`, **không lấy ngày lập hóa đơn**. Thêm giá sau khi tạo lượt với thời điểm hiệu lực quá muộn sẽ gây `EFFECTIVE_PRICE_NOT_CONFIGURED`. Đơn thuốc không tự sinh dòng tiền thuốc trong hóa đơn hiện tại.

## Bước 2 — Lễ tân tạo bệnh nhân, đặt lịch và tiếp nhận

1. Đăng nhập Receptionist. [Bệnh nhân](http://127.0.0.1:3000/workspace/patients) → **Kiểm tra nghi trùng**: tên `UAT Người bệnh 1107A`, ngày sinh giả lập, giới tính **Chưa xác định**; điện thoại/giấy tờ có thể để trống. Đọc kết quả, đóng biểu mẫu.
2. **Tạo hồ sơ bệnh nhân**, nhập lại thông tin đã đối chiếu; không đánh dấu đã xem nghi trùng nếu chưa thực sự đối chiếu. Giữ UUID từ kết quả.
3. [Khung giờ trống](http://127.0.0.1:3000/workspace/availability): nhập Doctor, phòng khám, dịch vụ khám; khoảng tra từ gần hiện tại đến vài giờ tới, tối đa một ngày. Chọn **Áp dụng bộ lọc**.
4. Chọn slot ở tương lai, **cùng ngày hiện tại của cơ sở**, ví dụ sau 15–30 phút; chọn **Chi tiết → Đặt khung giờ**. Chọn đúng bệnh nhân trong gợi ý, đối chiếu tên/ngày sinh/mã; kiểm tra lại Doctor/phòng/dịch vụ/thời điểm rồi gửi. Có thể dùng **Tạo lịch hẹn** ở [Lịch hẹn](http://127.0.0.1:3000/workspace/appointments) với cùng dữ liệu.
5. Lịch mới `BOOKED` hiển thị **Đã đặt**. Mở chi tiết → **Xác nhận lịch** → `CONFIRMED`, **Đã xác nhận**.
6. **Xác minh và tiếp nhận**: nhập UUID Nurse, đánh dấu **Tôi đã đối chiếu và xác minh danh tính bệnh nhân**, đọc xác nhận rồi gửi. Lịch chuyển `CHECKED_IN` (**Đã tiếp nhận**); Encounter mới `WAITING` (**Chờ khám**).
7. Giữ UUID Encounter. [Hàng đợi tiếp nhận](http://127.0.0.1:3000/workspace/queue) phải có đúng bệnh nhân/Doctor/Nurse. Nếu quên Nurse, mở hàng → **Phân công điều dưỡng**.

Không check-in lịch ngày mai trong hôm nay. Tránh chạy ca sát nửa đêm; ngày check-in do backend kiểm tra theo múi giờ cơ sở. Nhánh **Tiếp nhận không hẹn** chỉ dùng khi cơ sở cho phép; khi đó Doctor phải **Chọn dịch vụ khám cho walk-in** trước kết thúc nếu muốn lập hóa đơn khám.

## Bước 3 — Điều dưỡng sàng lọc lượt được phân công

Đăng nhập đúng Nurse đã gán, mở [Lượt khám được phân công](http://127.0.0.1:3000/workspace/encounters) → hàng bệnh nhân UAT → **Chi tiết**.

1. **Ghi sinh hiệu**: dùng số liệu giả lập, ví dụ mạch `80`, huyết áp `120/80`, nhiệt độ `37`; nhập **Thời điểm đo** không ở tương lai, đánh dấu **Đã xác minh thông tin đo**. Cần ít nhất một số đo; thời điểm không sớm hơn lúc tạo lượt quá 5 phút.
2. **Ghi tiền sử / dị ứng**: chọn **Tiền sử**, nội dung ghi rõ “Dữ liệu UAT, không phải hồ sơ thật”. Đây là thông tin diễn giải, không thay thế hồ sơ thuốc có cấu trúc.
3. Mở **Sinh hiệu → Xem thông tin** và **Tiền sử / dị ứng → Xem thông tin**, kiểm tra đơn vị, thời điểm, người ghi và trạng thái xác minh.
4. Nếu test thuốc, mở [Hồ sơ an toàn thuốc](http://127.0.0.1:3000/workspace/medication-profile), nhập **Mã lượt khám**, **Áp dụng bộ lọc** → **Ghi hồ sơ an toàn thuốc**. Với fixture UAT được xác nhận không có dị ứng, chọn **Đã xác minh không ghi nhận dị ứng**, các danh sách mã để trống. Khi tạo lần đầu, phiên bản để trống; khi cập nhật lấy phiên bản hiện tại.

Hồ sơ thuốc chưa có có thể trả 404 trước lần ghi đầu tiên; biểu mẫu ghi vẫn là thao tác tạo hợp lệ. Không đổi dị ứng thật hoặc đổi `UNKNOWN` thành “không có” chỉ để phát hành đơn.

## Bước 4 — Bác sĩ khám, chẩn đoán và lưu SOAP

Đăng nhập đúng Doctor đã gán, [Lượt khám được phân công](http://127.0.0.1:3000/workspace/encounters) → **Chi tiết**:

1. **Bắt đầu khám** → `IN_CONSULTATION`, **Đang khám**.
2. **Ghi chẩn đoán**: chọn mã đang bật trong danh mục và nhập triệu chứng giả lập. Mở **Chẩn đoán → Xem thông tin** để đối chiếu.
3. **Tạo ghi chú SOAP**, nhập đủ bốn phần **S · Thông tin chủ quan**, **O · Thông tin khách quan**, **A · Nhận định**, **P · Kế hoạch**, mỗi phần ghi rõ nội dung kiểm thử. Tạo một nháp; nếu đã có nháp, mở **Ghi chú SOAP → Xem thông tin → Lưu bản nháp** thay vì tạo tiếp.
4. Bản nháp hiển thị `DRAFT`, **Bản nháp**. Chưa ký nếu còn sửa nội dung. Có thể thực hiện nhánh mở rộng ở bước 5 rồi quay lại ký/kết thúc.

## Bước 5 — Tùy chọn: thuốc, dịch vụ và tệp trước khi kết thúc

### 5A. Thuốc và dược sĩ

Điều kiện: fixture thuốc và nguồn chính sách thử nghiệm đã được người phụ trách test xác nhận. Manager có thể chuẩn bị tại [Danh mục thuốc](http://127.0.0.1:3000/workspace/medications) → **Thêm thuốc**, dùng mã thuốc/hoạt chất UAT mới, tên “Thuốc UAT”, đơn vị giả lập, bật sử dụng. [Nguồn chính sách thuốc](http://127.0.0.1:3000/workspace/medication-policy) phải có nguồn/phiên bản đang bật. **Không ghi đè nguồn của cơ sở hoặc điền nguồn giả để vượt chặn**; thiếu fixture được xác nhận thì ghi nhánh này là **Blocked**, vẫn có thể chạy lượt cốt lõi riêng không tạo đơn thuốc.

1. Doctor mở [Đơn thuốc theo lượt khám](http://127.0.0.1:3000/workspace/prescriptions), nhập UUID Encounter → **Áp dụng bộ lọc** → **Tạo nháp đơn thuốc**.
2. **Thêm dòng** trong **Các thuốc trong đơn**; chọn thuốc UAT, nhập các chuỗi liều/đường/tần suất/hướng dẫn ghi rõ “giả lập UAT”, số ngày `2`, số lượng `4`. Đây là dữ liệu kiểm thử, không phải chỉ định điều trị.
3. Mở chi tiết đơn → **Kiểm tra an toàn → Xem thông tin**. Đọc nguồn/phiên bản và từng cảnh báo. Không có cảnh báo không đồng nghĩa bảo đảm an toàn lâm sàng.
4. **Phát hành đơn thuốc**: nhập UUID Pharmacist; nếu có cảnh báo, sao chép đúng **Mã cảnh báo** đã đọc vào **Các mã cảnh báo đã đọc và xác nhận**; để trống khi máy chủ trả danh sách rỗng. `BLOCK` không được ghi đè phải tiếp tục chặn. Chỉ loại cho phép ghi đè mới dùng quyền `prescription.override` và lý do hợp lệ.
5. Thành công → `ISSUED`, **Đã phát hành**. Đăng nhập Pharmacist → [Công việc dược được phân công](http://127.0.0.1:3000/workspace/pharmacy) → đơn vừa gán → **Các thuốc → Xem thông tin**, giữ UUID **dòng thuốc**.
6. **Ghi nhận cấp / dùng thuốc**: chọn **Đã cấp thuốc**, số lượng trong giới hạn, thời điểm không trước `issuedAt` và không ở tương lai. Vì ô giờ có độ chính xác đến phút, nên đợi sang phút tiếp theo sau khi phát hành rồi chọn phút hiện tại. Đừng dùng thời điểm bị làm tròn về trước lúc phát hành.

Ghi `DISPENSED` và `ADMINISTERED` là hai sự kiện riêng; ghi cấp không tự có nghĩa đã dùng. Không mong trạng thái đơn đổi sang “hoàn tất cấp thuốc”; đơn vẫn có thể `ISSUED`. API hiện chưa có danh sách sự kiện thuốc riêng, nên giữ kết quả xác nhận và kiểm tra audit.

### 5B. Chỉ định, kỹ thuật viên, kết quả và tệp

1. Doctor mở [Chỉ định theo lượt khám](http://127.0.0.1:3000/workspace/procedures), nhập UUID Encounter → **Áp dụng bộ lọc** → **Tạo chỉ định**: dịch vụ xét nghiệm UAT, lý do, **Thông thường**, hướng dẫn chuẩn bị giả lập, loại **Xét nghiệm**. Lượt chuyển **Chờ dịch vụ** (`AWAITING_SERVICE`); order mới thường là `CREATED`.
2. Mở order → **Phân lịch / đổi lịch**: UUID Technician, phòng LAB, thời điểm nằm trong ca WORK và còn trống, lý do. Chọn phút hiện tại hoặc một phút sắp tới; không lùi quá 5 phút. Nếu chọn tương lai, chờ đúng giờ mới bắt đầu. Thành công → `SCHEDULED`, **Đã xếp lịch**.
3. Đăng nhập Technician → [Dịch vụ được phân công](http://127.0.0.1:3000/workspace/procedure-queue) → đúng order → **Bắt đầu thực hiện**, trong khoảng bắt đầu/kết thúc đã phân → `IN_PROGRESS`, **Đang thực hiện**. Technician không có quyền tự phân lịch thay Doctor.
4. Khi order đang `SCHEDULED`/`IN_PROGRESS`, có thể mở [Tệp riêng tư](http://127.0.0.1:3000/workspace/attachments) → nhập UUID Encounter → **Áp dụng bộ lọc** → **Tải tệp lên**. Technician phải nhập thêm UUID order được gán. Dùng tệp `.txt` UTF-8 nhỏ chỉ chứa “Kết quả UAT”, hoặc PDF/PNG/JPEG hợp lệ; giới hạn 10 MiB, không HTML/SVG.
5. Mở tệp → **Tải tệp có kiểm tra quyền**; đối chiếu nội dung tải xuống. Đây là file private qua request có phiên, không phải public URL. Technician không tải lên sau khi đã hoàn tất order; vì vậy thử file trước bước tiếp theo.
6. Technician → order → **Ghi kết quả và hoàn tất**: kết quả UAT không để trống → `COMPLETED`, **Hoàn tất**.
7. Đăng nhập lại Doctor → order → **Xác nhận đã xem kết quả**, ghi lý do. Chỉ khi mọi order đã giải quyết và được xem lại, Encounter mới trở về `IN_CONSULTATION`, **Đang khám**. Hoàn tất order chưa đủ để tự đóng Encounter.

Nếu tạo order chưa phân được lịch hoặc còn đơn thuốc nháp, không chuyển sang bước kết thúc như thể công việc đã xong. Hoãn/hủy/không thực hiện cũng cần đúng người, lý do và bác sĩ xem lại.

## Bước 6 — Ký và kết thúc lâm sàng

Doctor trở lại [Lượt khám được phân công](http://127.0.0.1:3000/workspace/encounters), **Tải lại** → **Chi tiết**:

1. **Ghi chú SOAP → Xem thông tin**, đọc lại bản nháp trên server → **Ký ghi chú** và xác nhận. Mong đợi **Đã ký** (`SIGNED`); không còn **Lưu bản nháp** trên bản đã ký.
2. Nếu test sửa sau ký, dùng **Tạo bản bổ sung**, nhập lý do → **Lưu bản nháp → Ký ghi chú**. Bản cũ còn nguyên; nháp bổ sung phải được ký hoặc bỏ hợp lệ trước khi đóng lượt.
3. **Gửi kiểm tra kết thúc**: cần bản ký, chẩn đoán, sinh hiệu mới nhất đã xác minh; không còn nháp thuốc/note; mọi chỉ định đã giải quyết và được Doctor xem lại. Mong đợi `AWAITING_CLOSURE`, **Chờ kết thúc**.
4. **Kết thúc lượt khám**: đủ **Tóm tắt kết thúc**, **Hướng dẫn sau khám**, **Hướng dẫn tái khám** (fixture không tái khám thì ghi rõ) và xác nhận. Mong đợi `COMPLETED`, **Hoàn tất**. Giữ UUID Encounter trước khi nó biến mất khỏi hàng đợi đang hoạt động.

Nếu chính sách cơ sở yêu cầu duyệt ra về, thực hiện trước **Gửi kiểm tra kết thúc**:

1. Doctor được gán → chi tiết Encounter → **Gửi yêu cầu duyệt ra về**, nhập **Lý do đề nghị ra về** và xác nhận. Giữ **Mã tham chiếu** request và **Phiên bản** trong kết quả; gửi yêu cầu không tự kết thúc lượt.
2. Manager đăng nhập → [Duyệt ra về](http://127.0.0.1:3000/workspace/discharge-approvals) → **Ghi quyết định ra về**: nhập UUID request, phiên bản, **Phê duyệt**, lý do và xác nhận. Người yêu cầu không được tự phê duyệt. Chưa có API danh sách request ra về; dùng mã kết quả vừa nhận, không tìm một inbox không tồn tại.
3. Doctor đăng nhập lại, **Tải lại** Encounter rồi **Gửi kiểm tra kết thúc → Kết thúc lượt khám**. Phê duyệt ra về không thay thế note đã ký, sinh hiệu, chẩn đoán hay review chỉ định.

Không tự tắt chính sách để vượt checklist. Môi trường cốt lõi mặc định của MVP không bật điều kiện này; khi đang tắt thì không cần tạo request chỉ để chạy happy path.

**Kết thúc lâm sàng và thanh toán độc lập.** `COMPLETED` không có nghĩa đã thu tiền; nợ tiền không mở lại/sửa bệnh án đã ký.

## Bước 7 — Thu ngân lập hóa đơn, thu tiền, xem biên nhận

Điều kiện: giá ở bước 1 đã có hiệu lực đúng thời điểm; Encounter đã `COMPLETED`.

1. Đăng nhập Cashier → [Hóa đơn](http://127.0.0.1:3000/workspace/invoices) → **Lập hóa đơn từ lượt khám**, nhập UUID Encounter. Hệ thống tính dòng dịch vụ, không nhập tổng thủ công. Mới tạo → `DRAFT`, **Bản nháp**.
2. Mở chi tiết → **Chi tiết dịch vụ → Xem thông tin**: chỉ khám mẫu là `100000`; có thêm xét nghiệm hoàn tất giá `50000` thì tổng mẫu `150000`, trước giảm giá. Đối chiếu nguồn và số dư, không tính thêm tiền thuốc ngoài API hiện có.
3. Nhánh giảm giá tùy chọn: Finance dùng **Áp dụng giảm giá** khi còn `DRAFT`, số giảm nhỏ hơn tạm tính và có lý do. Manager không mặc định có quyền giảm giá.
4. Cashier → **Phát hành hóa đơn** → `ISSUED`, **Đã phát hành**.
5. **Ghi nhận thanh toán**: lần đầu thu `50000` VND, chọn **Tiền mặt** và đánh dấu **Tôi đã xác minh cơ sở nhận đủ số tiền vừa nhập**, xác nhận. Mong đợi **Thanh toán một phần** (`PARTIALLY_PAID`) và số dư giảm đúng `50000`.
6. Ghi lần hai đúng **Còn phải thu** đang hiển thị, không lớn hơn số dư. Mong đợi `PAID`, **Đã thanh toán**, số dư `0`. Chuyển khoản chỉ là ghi nhận đã đối soát, không gọi ngân hàng chuyển tiền.
7. **Các khoản đã thu → Xem thông tin → Chi tiết khoản thu**, hoặc [Biên nhận thanh toán](http://127.0.0.1:3000/workspace/receipts) với UUID khoản thanh toán. Đối chiếu số biên nhận, số tiền, phương thức, thời điểm, invoice và người ghi. **Tải biên nhận** hiện tải dữ liệu JSON; không phải PDF, hóa đơn điện tử hay xác nhận ngân hàng.

Không lập hóa đơn lần hai cho cùng Encounter. Khi timeout sau gửi tiền, đọc lại hóa đơn và lịch sử khoản thu trước khi thử lại; đừng mặc định “chưa thành công” rồi tạo khoản thu mới.

### Nhánh hoàn tiền tùy chọn

Finance → chi tiết hóa đơn → **Yêu cầu hoàn tiền**, chọn UUID khoản thanh toán gốc còn đủ tiền có thể hoàn; ví dụ `20000` và lý do. Nếu khoản vượt ngưỡng tự hoàn, trạng thái **Chờ phê duyệt** (`PENDING_APPROVAL`). Manager hoặc Finance **khác người yêu cầu** → [Tra cứu hoàn tiền](http://127.0.0.1:3000/workspace/refunds), nhập UUID request → **Duyệt yêu cầu hoàn → Phê duyệt** → `APPROVED`; số dư chưa đổi. Finance → **Thực hiện hoàn tiền** → `COMPLETED`; lúc này số dư mới đổi.

Ngưỡng bằng 0 yêu cầu mọi khoản hoàn dương được duyệt; nếu chính sách đang cho tự hoàn trong ngưỡng, request có thể hoàn tất ngay. Không sửa chính sách dùng chung chỉ để ép nhánh test; xác nhận fixture/policy với người phụ trách. Mã hoàn có trong **Lịch sử hoàn tiền** của hóa đơn, không có inbox hoàn toàn cục. Hoàn `20000` sau khi đã thu đủ làm invoice còn phải thu `20000`, trong khi Encounter vẫn `COMPLETED`.

## Bước 8 — Báo cáo, audit và kết thúc phiên

1. Manager → [Báo cáo vận hành](http://127.0.0.1:3000/workspace/operations), chọn từ/đến bao trọn buổi test → **Áp dụng bộ lọc**. Doctor/Nurse dùng cùng màn hình chỉ thấy phạm vi lượt được phân công, không phải toàn cơ sở.
2. Manager → [Báo cáo thu chi](http://127.0.0.1:3000/workspace/revenue): đối chiếu **Tiền đã nhận**, **Tiền đã hoàn**, **Dòng tiền ròng**. Nếu test dùng chung nhiều fixture, so chênh lệch trước/sau hoặc chứng từ liên quan; đừng mong toàn báo cáo bằng đúng một hóa đơn. Khoảng thời gian gồm đầu, không gồm cuối, tối đa 366 ngày.
3. **Xuất thu chi CSV** hoặc **Xuất trang hóa đơn CSV** theo quyền. Bản hóa đơn chỉ xuất trang được chọn, tối đa 100 dòng; không phải nút xuất toàn bộ kỳ.
4. Admin hoặc Auditor → [Lịch sử kiểm toán hồ sơ](http://127.0.0.1:3000/workspace/patient-audit), nhập UUID patient → **Áp dụng bộ lọc**; kiểm tra các sự kiện khám/ký/tiền liên quan. [Nhật ký kiểm toán](http://127.0.0.1:3000/workspace/audit) cho lọc ngày/người/đối tượng/thao tác. Lọc target invoice sẽ không tự bao gồm mọi payment/refund vì chúng có UUID target riêng.
5. Chọn nút **Đăng xuất** ở góc trên. Truy cập lại workspace phải về đăng nhập. Không giữ thông tin khám hoặc biểu mẫu cũ trên màn hình sau hết phiên/đăng xuất.

## Tình huống lỗi bắt buộc

Dùng fixture UAT riêng cho những ca có thể để lại nháp/chỉ định đang mở; không phá lượt happy path vừa hoàn tất.

| Ca | Cách thử | Mong đợi |
| --- | --- | --- |
| Nghi trùng | Reception tạo cùng tên/ngày sinh như hồ sơ UAT, chưa xác minh | Bị chặn `POSSIBLE_DUPLICATE_REQUIRES_REVIEW`; phải đối chiếu và có lý do nếu tiếp tục. Không tự hợp nhất; trùng giấy tờ duy nhất không được vượt bằng checkbox. |
| Sai người/phân công | Doctor/Nurse thứ hai không được gán mở UUID Encounter UAT; Technician/Pharmacist không được gán mở công việc | Server từ chối, thường 403; không trả nội dung lâm sàng. Admin không đọc hồ sơ chỉ nhờ vai trò quản trị. |
| Mã không tồn tại | Nhập UUID hợp lệ nhưng không tồn tại vào màn hình tra cứu phù hợp | 404 hoặc thông báo thiếu tài nguyên; không bịa dữ liệu hay trạng thái thành công. Không thử dò mã hồ sơ thật. |
| Xung đột phiên bản | Cùng tài khoản, hai tab mở cùng hồ sơ/phòng UAT. Tab B lưu; tab A gửi form cũ | `STALE_VERSION`; nội dung A còn giữ. Dùng **Tải bản mới, giữ nội dung đang nhập**, đối chiếu bản mới trước khi xác nhận lại; không tự ghi đè. |
| Tranh slot | Hai yêu cầu khác nhau cùng Doctor/phòng/slot | Chỉ một booking được chấp nhận; bên còn lại conflict. Tải lại availability và chọn slot khác. |
| Gửi lặp | Double-click khi nút đang gửi; quan sát Network của chính phiên test | Nút busy ngăn kích hoạt lặp. Retry cùng nội dung trong cùng dialog giữ key; sửa nội dung là ý định mới. Không đóng/mở form thanh toán để gửi lại khi kết quả còn chưa rõ. Booking cùng key đã được kiểm tra bằng HTTP riêng. |
| Tiếp nhận trùng | Thử check-in bệnh nhân UAT đang có Encounter chưa hoàn tất | `ACTIVE_ENCOUNTER_EXISTS`, không sinh lượt thứ hai; kiểm tra hàng đợi. |
| Note thiếu dữ liệu | Nháp thiếu một phần SOAP hoặc chưa có chẩn đoán rồi ký | Không ký; nháp vẫn còn. Bản đã ký chỉ được bổ sung bằng successor có lý do. |
| Kết thúc sớm | Còn nháp, sinh hiệu chưa xác minh, order chưa review | Checklist chặn; không chuyển `COMPLETED` giả. Giải quyết việc còn lại rồi tải phiên bản mới. |
| Chính sách duyệt ra về | `dischargeApprovalRequired=true` nhưng chưa có request được duyệt | `DISCHARGE_APPROVAL_REQUIRED`. Doctor dùng **Gửi yêu cầu duyệt ra về**, Manager phê duyệt request rồi Doctor tải lại và gửi checklist; không tắt chính sách để bỏ qua. |
| Thuốc chưa đủ điều kiện | Thiếu/bất hoạt policy, thiếu profile hoặc dị ứng `UNKNOWN` | `SAFETY_POLICY_NOT_CONFIGURED`, `MEDICATION_PROFILE_REQUIRED` hoặc `ALLERGY_STATUS_UNKNOWN`; không phát hành. |
| Chưa đọc cảnh báo | Có WARN nhưng thiếu mã xác nhận; BLOCK không cho ghi đè | `SAFETY_WARNINGS_REQUIRE_REVIEW` hoặc `NON_OVERRIDABLE_SAFETY_WARNING`. Không xóa dị ứng/đổi nguồn để vượt kiểm tra. |
| Thời điểm sai | Ghi cấp trước lúc phát hành hoặc bắt đầu order ngoài slot | Bị chặn; không tự sửa đồng hồ máy hoặc ghi lùi thời điểm để vượt. |
| Tệp sai | HTML/SVG, MIME/nội dung không khớp, tệp rỗng/quá 10 MiB | Bị từ chối ở client/server; tệp chưa được lưu không hiện thành công. Sau archive, danh sách active không còn tệp đó; đây không phải xóa lịch sử. |
| Tiền sai | Thu quá số dư, tự duyệt hoàn của mình, hoàn quá khoản gốc | Bị từ chối, số dư/ledger không đổi vì request lỗi. Pending/approved refund chưa phải tiền đã hoàn. |
| Phiên hết hạn | Ở môi trường test, admin thu hồi phiên của account UAT hoặc chờ idle 30 phút rồi request | Yêu cầu kế tiếp bị từ chối; frontend yêu cầu đăng nhập lại, không giữ nội dung riêng tư trên UI. Không giảm timeout backend để test. |

Muốn test WARN/BLOCK có thể dùng [Quy tắc an toàn thuốc](http://127.0.0.1:3000/workspace/medication-rules) với **thuốc/mã tình trạng UAT riêng** và fixture đã xác nhận. Không thêm luật ảnh hưởng thuốc thật; không gọi dữ liệu UAT là cơ sở tri thức lâm sàng.

## Test animation, bàn phím và responsive

Các mục này **chưa được chạy bằng công cụ trình duyệt**. Thực hiện sau khi đã build/start đúng phiên bản frontend mới nhất.

| Thao tác | Kiểm tra mong đợi |
| --- | --- |
| Đăng xuất rồi tải lại màn đăng nhập | Minh họa SVG gốc vẽ connector/check một lần; không loop, không che/khóa form. Không có video nền. |
| Hover/focus nút và chuyển workspace | Màu nền/viền đổi ngắn, mục hiện tại rõ; focus nhìn thấy ngay. Không nhảy kích thước hoặc làm trôi vị trí bấm. |
| Mở/đóng biểu mẫu, nhấn Escape | Modal vào nhẹ khoảng 180ms; Tab ở trong dialog, Escape/Hủy có xác nhận khi còn nội dung chưa gửi; đóng trả focus về nút mở. Không giả định có animation thoát. |
| Chọn bản ghi khác | Detail panel vào nhẹ theo bản ghi; UUID/tên phải đổi đúng cùng dữ liệu, không hiện nội dung cũ như của người mới. |
| **Xem thông tin / Thu gọn** | Chevron xoay khoảng 160ms, nội dung vào nhẹ; mở/đóng liên tiếp không kẹt trạng thái, không gây focus mất dấu. |
| **Tải lại** hoặc gửi request chậm | Có spinner/nhãn busy trong lúc chờ; nút gửi không kích hoạt hai lần; dữ liệu cuối xuất hiện đúng. Có thể dùng Network throttling cho read; không cần tạo thêm payment để nhìn spinner. |
| Lưu một thay đổi UAT hợp lệ | Check SVG chỉ chạy sau server xác nhận, khoảng 220ms một lần; 400/403/409 không được hiện check/thông báo thành công. |
| Bật giảm chuyển động | Lặp lại các bước trên: bỏ vẽ/di chuyển/quay, nhưng selected state, chữ tải/đã xác nhận và check cuối vẫn hiện. Form vẫn dùng bình thường. |

Để kiểm tra reduced motion bằng Chrome DevTools, mở tab **Rendering**, đặt **Emulate CSS media feature `prefers-reduced-motion` → `prefers-reduced-motion: reduce`**; kiểm tra lại rồi bỏ emulation để so sánh. Đây là quy trình trong [tài liệu Chrome chính thức](https://developer.chrome.com/docs/devtools/rendering/emulate-css). Kiểm tra thêm với thiết lập giảm chuyển động của hệ điều hành để không chỉ dựa vào emulation.

Kiểm tra tại 1440×900, 768×1024, 390×844; thêm 320 CSS px và zoom 200%. Đối chiếu dấu tiếng Việt, label không bị cắt, nút chạm được, bảng chỉ cuộn ngang trong vùng bảng. Thử chỉ bàn phím từ **Đến nội dung chính**, điều hướng, bộ lọc, **Chi tiết**, dialog và các disclosure. Mở Console/Network: không có lỗi runtime, request lặp vô hạn hoặc tài nguyên CSS/icon 404. Ảnh/video bằng chứng chỉ chứa fixture UAT, không chứa password/cookie/token.

## Ghi kết quả, không suy từ việc thấy nút

Mỗi ca ghi: ngày giờ/múi giờ, browser/version, tài khoản/vai trò, workspace, mã fixture nội bộ, bước thực hiện, kết quả mong đợi, kết quả thực tế, HTTP status khi cần, ảnh/log đã lọc và **Pass / Fail / Blocked / Not run**. Không điền Pass sẵn cho toàn bộ luồng vì trang tải được hoặc vì unit tests đã qua.

Nguồn đối chiếu nhãn/quyền: `src/frontend/src/modules/reception.ts`, `clinical.ts`, `administration.ts`; các component `auth`, `forms`, `workspace`, `ui`; backend `SchedulingService`, `ClinicalService`, `MedicationCatalogService`, `PrescriptionService`, `ProcedureService`, `AttachmentService`, `BillingService`, `ReportService` và migrations phân quyền V2/V4/V5/V6. Hướng dẫn này chỉ thêm tài liệu, không chạy script và không thay đổi dữ liệu backend.
