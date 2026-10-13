

**PRODUCT REQUIREMENTS**  
**DOCUMENT**

**HỆ THỐNG QUẢN LÝ PHÒNG KHÁM**  
**VÀ HỒ SƠ BỆNH ÁN ĐIỆN TỬ (EMR)**

Đặc tả sản phẩm chi tiết • Yêu cầu chức năng • Yêu cầu phi chức năng • Luồng nghiệp vụ • Mô hình dữ liệu • Tiêu chí nghiệm thu

| Thuộc tính | Thông tin |
| :---- | :---- |
| Phiên bản | 1.0 – Bản dự thảo chi tiết |
| Trạng thái | Tài liệu cơ sở để Product, Design, Engineering, QA và vận hành cùng rà soát |
| Phạm vi sản phẩm | Ứng dụng web nội bộ cho phòng khám/cơ sở khám chữa bệnh; cổng bệnh nhân có thể triển khai theo giai đoạn |
| Đối tượng đọc | Chủ sản phẩm, quản lý cơ sở y tế, BA/SA, UX/UI, Frontend/Backend, QA, DevOps và đại diện nghiệp vụ |
| Nguyên tắc | An toàn người bệnh, bảo mật dữ liệu, truy vết được thay đổi, giảm thao tác lặp và xuyên suốt hành trình khám chữa bệnh |

*Lưu ý: Các chỉ tiêu hiệu năng, phục hồi và phạm vi giai đoạn trong tài liệu là đề xuất ban đầu để các bên xác nhận. Tài liệu không tự khẳng định hệ thống đáp ứng một chứng nhận pháp lý hoặc tiêu chuẩn y tế cụ thể.*

# **Mục lục**

* 1\. Tổng quan và định hướng sản phẩm  
* 2\. Mục tiêu, chỉ số thành công và ngoài phạm vi  
* 3\. Đối tượng sử dụng và phân quyền  
* 4\. Phạm vi sản phẩm và giả định  
* 5\. Luồng nghiệp vụ đầu-cuối  
* 6\. Yêu cầu chức năng chi tiết  
* 7\. Danh mục màn hình và điều hướng  
* 8\. Mô hình dữ liệu khái niệm  
* 9\. Trạng thái nghiệp vụ và quy tắc chuyển trạng thái  
* 10\. Yêu cầu phi chức năng  
* 11\. Bảo mật, quyền riêng tư và an toàn lâm sàng  
* 12\. Tiêu chí nghiệm thu tổng thể  
* 13\. Phân kỳ triển khai đề xuất  
* 14\. Phụ thuộc, rủi ro và câu hỏi cần chốt  
* 15\. Bàn giao bắt buộc cho đội phát triển  
* 16\. Nguồn tham khảo và cách sử dụng

*Trong Microsoft Word, có thể cập nhật mục lục tự động từ các Heading sau khi mở tài liệu nếu cần mục lục có số trang chính xác.*

# **1\. Tổng quan và định hướng sản phẩm**

Hệ thống là nền tảng quản lý hoạt động khám chữa bệnh tại phòng khám/cơ sở y tế, kết nối các bước từ tiếp nhận bệnh nhân, xếp lịch và hàng đợi, khám lâm sàng, ghi nhận chẩn đoán, kê đơn, chỉ định thủ thuật/dịch vụ, phân bổ phòng và nhân sự, theo dõi thực hiện, kết thúc đợt khám, thanh toán và báo cáo.

Sản phẩm không chỉ là website đặt lịch hoặc nơi lưu file bệnh án. Mục tiêu là cung cấp một quy trình làm việc nhất quán, trong đó mỗi vai trò nhìn thấy đúng công việc cần xử lý, dữ liệu được dùng lại có kiểm soát, các quyết định lâm sàng được ghi nhận theo thời gian, và các thay đổi quan trọng có thể kiểm tra lại.

## **1.1. Tầm nhìn sản phẩm**

Tạo một nguồn dữ liệu vận hành và hồ sơ lâm sàng đáng tin cậy cho cơ sở y tế; giúp nhân viên xử lý bệnh nhân xuyên suốt, giảm nhập liệu trùng lặp, giảm xung đột lịch/phòng, tăng khả năng theo dõi tiến độ và hỗ trợ quản lý bằng dữ liệu.

## **1.2. Giá trị cốt lõi**

* Liên tục: thông tin bệnh nhân, lịch hẹn, lượt khám, chẩn đoán, đơn thuốc và chỉ định được liên kết xuyên suốt.  
* Đúng vai trò: bác sĩ tập trung vào khám và quyết định chuyên môn; điều dưỡng vào tiếp nhận lâm sàng và chăm sóc; lễ tân vào lịch và tiếp đón; quản lý vào vận hành và báo cáo.  
* An toàn: cảnh báo dị ứng/chống chỉ định, xác nhận thao tác nhạy cảm, không âm thầm ghi đè hồ sơ đã ký.  
* Có thể truy vết: lưu người thực hiện, thời điểm, dữ liệu trước/sau và lý do đối với thay đổi quan trọng.  
* Có thể mở rộng: thiết kế theo mô-đun, hỗ trợ danh mục cấu hình và tích hợp theo từng giai đoạn.

## **1.3. Nguồn tham khảo và diễn giải**

Tài liệu UML về Hospital Management System cung cấp tham khảo về tác nhân, use case, lớp dữ liệu và chuỗi tương tác như khám bệnh, thêm triệu chứng, đề xuất chẩn đoán, kê thuốc, kiểm tra chống chỉ định và đặt lịch thủ thuật. Đây là cơ sở để mở rộng các luồng nghiệp vụ, không phải đặc tả pháp lý hoặc đặc tả hoàn chỉnh cho sản phẩm này. citeturn151607view0

Repository Smart Hospital Management System trên GitHub được xem như tham khảo về cấu trúc dự án và cách tổ chức mã nguồn/tài liệu. README công khai không đủ để kết luận toàn bộ tính năng y tế đã được triển khai, vì vậy các yêu cầu trong PRD này là đặc tả sản phẩm đề xuất, không được gán nhầm cho repository. citeturn151607view1

# **2\. Mục tiêu, chỉ số thành công và ngoài phạm vi**

## **2.1. Mục tiêu sản phẩm**

* Chuẩn hóa hành trình từ đăng ký đến kết thúc lượt khám, với trạng thái và người chịu trách nhiệm rõ ràng.  
* Tập trung hồ sơ bệnh nhân theo một mã định danh nội bộ; giảm hồ sơ trùng và giảm việc tra cứu thủ công.  
* Hỗ trợ bác sĩ ghi nhận triệu chứng, khám, chẩn đoán, kê đơn, chỉ định thủ thuật và kế hoạch tái khám.  
* Quản lý lịch bác sĩ, khung giờ, phòng, thiết bị/nguồn lực và ngăn đặt trùng.  
* Tăng tính minh bạch của vận hành bằng dashboard, báo cáo, nhật ký kiểm toán và dữ liệu có cấu trúc.  
* Bảo vệ dữ liệu sức khỏe thông qua phân quyền, kiểm soát truy cập và quy trình sửa đổi hồ sơ phù hợp.

## **2.2. Chỉ số thành công gợi ý**

| Chỉ số | Cách đo | Mục tiêu ban đầu đề xuất |
| :---- | :---- | :---- |
| Thời gian tiếp nhận | Trung vị từ check-in đến hoàn tất tiếp nhận | Giảm ít nhất 25% sau giai đoạn ổn định |
| Nhập liệu trùng | Số trường hợp trùng hồ sơ được xác nhận mỗi tháng | Giảm liên tục theo tháng; mục tiêu cụ thể sau đo baseline |
| Xung đột lịch/phòng | Số lần đặt trùng được ghi nhận | 0 trường hợp được hệ thống chấp nhận |
| Hoàn chỉnh hồ sơ | Tỷ lệ lượt khám có đủ trường bắt buộc theo mẫu | ≥ 95% sau khi quy trình được thống nhất |
| Truy vết | Tỷ lệ thay đổi dữ liệu lâm sàng quan trọng có audit log | 100% |
| Mức sử dụng | Tỷ lệ nhân sự thuộc phạm vi dùng hệ thống hàng tuần | Xác định theo cơ sở và vai trò |

## **2.3. Ngoài phạm vi mặc định**

* Không để hệ thống tự động chẩn đoán hoặc tự quyết định điều trị thay bác sĩ.  
* Không tự động gửi đơn thuốc ra hệ thống bên ngoài khi chưa có phê duyệt và tích hợp được kiểm thử.  
* Không mặc định bao gồm thanh toán bảo hiểm, hóa đơn điện tử, kho thuốc nâng cao, LIS/RIS/PACS hoặc kết nối liên thông quốc gia trong MVP.  
* Không thay thế quy trình chuyên môn, quy chế bệnh án, biểu mẫu được cơ quan có thẩm quyền yêu cầu; các nội dung này phải được cơ sở và chuyên gia pháp chế xác nhận.  
* Không cho phép người dùng tùy ý xóa vĩnh viễn hồ sơ lâm sàng đã phát sinh.

# **3\. Đối tượng sử dụng và phân quyền**

Phân quyền cần được thiết kế theo vai trò (RBAC), kết hợp phạm vi dữ liệu, cơ sở/chi nhánh và quan hệ công việc khi cần. Mặc định áp dụng nguyên tắc quyền tối thiểu: chỉ cấp những quyền cần thiết để thực hiện công việc.

| Vai trò | Nhiệm vụ chính | Quyền điển hình | Giới hạn/kiểm soát |
| :---- | :---- | :---- | :---- |
| Quản trị hệ thống | Tài khoản, vai trò, cấu hình kỹ thuật, danh mục hệ thống | Quản lý người dùng và cấu hình | Không mặc định có quyền sửa nội dung chuyên môn thay bác sĩ; thao tác đặc quyền phải audit |
| Quản lý cơ sở | Theo dõi vận hành, lịch, phòng, nhân sự, báo cáo | Xem dashboard, cấu hình vận hành được ủy quyền | Dữ liệu lâm sàng nhạy cảm chỉ xem khi được cấp quyền |
| Lễ tân/tiếp đón | Tạo hồ sơ, đặt lịch, check-in, điều phối hàng đợi | Thông tin hành chính, lịch và trạng thái tiếp nhận | Không sửa chẩn đoán/đơn thuốc |
| Bác sĩ | Khám, ghi bệnh án, chẩn đoán, kê đơn, chỉ định, kết luận | Hồ sơ lâm sàng của bệnh nhân thuộc phạm vi công việc | Ký hồ sơ; sửa sau ký phải theo luồng bổ sung/đính chính |
| Điều dưỡng | Sinh hiệu, phân loại ban đầu, chăm sóc, ghi nhận thực hiện y lệnh | Trường chăm sóc và y lệnh được phân công | Không thay đổi chẩn đoán/kê đơn đã ký |
| Kỹ thuật viên | Thực hiện dịch vụ/thủ thuật/xét nghiệm theo chỉ định | Danh sách chỉ định và kết quả thuộc nhiệm vụ | Không xem toàn bộ hồ sơ nếu không cần thiết |
| Dược sĩ/nhân sự thuốc | Rà soát đơn, cấp phát, ghi nhận thuốc | Đơn thuốc và thông tin liên quan | Tồn kho nâng cao là mô-đun tùy chọn |
| Thu ngân | Tạo hóa đơn, ghi nhận thanh toán, đối soát | Dịch vụ, phí, hóa đơn và trạng thái thanh toán | Không sửa dữ liệu lâm sàng |
| Bệnh nhân | Tự quản lý lịch và xem thông tin được phép | Hồ sơ của chính mình, lịch, hướng dẫn và tài liệu được công bố | Cần xác thực danh tính; chỉ triển khai khi bật cổng bệnh nhân |
| Kiểm toán viên | Rà soát truy cập và lịch sử thay đổi | Audit log theo phạm vi được duyệt | Chỉ đọc; mọi lần xem dữ liệu nhạy cảm cũng có thể cần ghi log |

## **3.1. Quy tắc phân quyền bắt buộc**

* Quyền phải kiểm tra ở backend/API, không chỉ ẩn nút trên giao diện.  
* Hỗ trợ khóa tài khoản, thu hồi phiên đăng nhập và thu hồi quyền ngay khi nhân sự nghỉ việc/đổi vị trí.  
* Các thao tác xem, tạo, sửa, ký, hủy, xuất và in dữ liệu nhạy cảm phải được phân quyền riêng khi phù hợp.  
* Tài khoản dùng chung không được khuyến nghị; mỗi nhân sự có danh tính riêng.  
* Tài khoản đặc quyền và thao tác nhạy cảm nên hỗ trợ xác thực đa yếu tố (MFA) theo chính sách cơ sở.

# **4\. Phạm vi sản phẩm và giả định**

## **4.1. Giả định cơ sở**

* Ứng dụng web dùng trong môi trường phòng khám/cơ sở y tế; ưu tiên máy tính tại quầy và phòng khám, hỗ trợ tablet.  
* Ngôn ngữ giao diện mặc định là tiếng Việt; ngày giờ hiển thị theo múi giờ cấu hình của cơ sở.  
* Một cơ sở có thể có nhiều chuyên khoa, bác sĩ, nhân sự, phòng và loại dịch vụ; đa chi nhánh là khả năng mở rộng cần chốt.  
* Dữ liệu lâm sàng là dữ liệu nhạy cảm; quy trình truy cập, lưu trữ và chia sẻ phải được chủ cơ sở phê duyệt.  
* Danh mục chẩn đoán, thuốc, thủ thuật, biểu mẫu và mã dịch vụ phải được cấu hình/kiểm duyệt bởi người có thẩm quyền.

## **4.2. Phạm vi theo nhóm**

| Nhóm | Phạm vi mặc định | Ghi chú |
| :---- | :---- | :---- |
| Vận hành cơ sở | Người dùng, nhân sự, chuyên khoa, lịch, phòng, hàng đợi | MVP |
| Hồ sơ bệnh nhân | Thông tin hành chính, tiền sử, dị ứng, lượt khám, tài liệu | MVP |
| Quy trình khám | Sinh hiệu, ghi chú, triệu chứng, chẩn đoán, đơn thuốc, chỉ định | MVP |
| Thực hiện dịch vụ | Đặt lịch thủ thuật, phân bổ nguồn lực, cập nhật tiến độ | MVP theo nghiệp vụ xác nhận |
| Kết thúc lượt khám | Kết luận, hướng dẫn, tái khám, trạng thái ra về | MVP |
| Tài chính | Danh mục giá, hóa đơn cơ bản, thanh toán và trạng thái | MVP cơ bản; quy định hóa đơn cần xác nhận |
| Cổng bệnh nhân | Tự đặt lịch, xác nhận thủ thuật, xem tài liệu đã công bố | Có thể để giai đoạn 2 |
| Xét nghiệm/chẩn đoán hình ảnh | Chỉ định, nhận kết quả, liên kết tài liệu | Giai đoạn 2 hoặc tích hợp riêng |
| Kho thuốc/vật tư | Tồn kho, lô, hạn dùng, nhập/xuất | Không mặc định thuộc MVP |
| Tích hợp | SMS/email, hệ thống ngoài, thiết bị | Theo ưu tiên và khả năng nhà cung cấp |

# **5\. Luồng nghiệp vụ đầu-cuối**

## **5.1. Luồng khám ngoại trú tiêu chuẩn**

1. Bước 1 – Đăng ký/tìm hồ sơ: lễ tân tìm theo mã bệnh nhân, số điện thoại hoặc thông tin định danh được phép; hệ thống gợi ý hồ sơ trùng trước khi tạo mới.  
2. Bước 2 – Đặt lịch hoặc tiếp nhận không hẹn: chọn chuyên khoa/bác sĩ/khung giờ; khi bệnh nhân đến, check-in và tạo lượt khám.  
3. Bước 3 – Sàng lọc ban đầu: điều dưỡng xác minh thông tin cần thiết, ghi nhận sinh hiệu và ghi chú phân loại; cảnh báo khẩn cấp theo quy trình cơ sở nếu được cấu hình.  
4. Bước 4 – Khám: bác sĩ xem tiền sử, dị ứng, thuốc đang dùng và lượt khám trước; ghi nhận lý do đến khám, triệu chứng, khám thực thể và đánh giá.  
5. Bước 5 – Chẩn đoán: bác sĩ chọn hoặc nhập chẩn đoán theo danh mục được duyệt, ghi mức độ/chẩn đoán chính-phụ nếu nghiệp vụ yêu cầu.  
6. Bước 6 – Kế hoạch điều trị: tạo đơn thuốc, chỉ định thủ thuật/dịch vụ hoặc yêu cầu xét nghiệm; hệ thống hiển thị cảnh báo liên quan khi có dữ liệu phù hợp.  
7. Bước 7 – Điều phối thực hiện: chọn thời gian, phòng và nhân sự/nguồn lực; hệ thống kiểm tra khả dụng và tránh xung đột.  
8. Bước 8 – Thực hiện: điều dưỡng/kỹ thuật viên cập nhật trạng thái, ghi nhận người thực hiện, thời gian và kết quả theo phạm vi quyền.  
9. Bước 9 – Kết thúc: bác sĩ ghi kết luận, hướng dẫn, dấu hiệu cần theo dõi và lịch tái khám; hoàn tất/khóa hồ sơ theo quy tắc.  
10. Bước 10 – Thanh toán và rời cơ sở: thu ngân lập hóa đơn, ghi nhận thanh toán; hệ thống cập nhật trạng thái kết thúc lượt khám.

## **5.2. Luồng đặt lịch và tiếp nhận**

Lịch có thể được tạo bởi lễ tân hoặc bệnh nhân (nếu cổng bệnh nhân được bật). Mỗi lịch liên kết với bệnh nhân, loại dịch vụ/chuyên khoa, bác sĩ hoặc nhóm thực hiện, thời gian, trạng thái và ghi chú. Trước khi lưu, hệ thống phải kiểm tra lịch làm việc, thời lượng dịch vụ, xung đột của nhân sự/phòng và chính sách đặt lịch.

Các trạng thái gợi ý: Nháp → Đã đặt → Đã xác nhận → Đã check-in → Đang phục vụ → Hoàn tất; các nhánh khác gồm Đã hủy, Không đến, Đổi lịch. Việc chuyển trạng thái cần lưu người thực hiện, thời điểm và lý do nếu hủy/đổi.

## **5.3. Luồng khám và kê đơn**

Bác sĩ mở lượt khám đang phụ trách, xem thông tin lâm sàng cần thiết, nhập ghi chú và đánh giá. Khi chọn thuốc, hệ thống kiểm tra dữ liệu dị ứng, thuốc đang dùng và các cảnh báo đã cấu hình. Cảnh báo chỉ hỗ trợ ra quyết định, không thay thế đánh giá chuyên môn. Nếu cho phép bỏ qua cảnh báo, hệ thống cần yêu cầu xác nhận và ghi lý do.

## **5.4. Luồng thủ thuật/dịch vụ**

Bác sĩ tạo chỉ định với mục đích, mức ưu tiên và yêu cầu chuẩn bị. Nhân sự được phân công hoặc lễ tân/điều phối tìm khung giờ khả dụng. Hệ thống kiểm tra đồng thời lịch nhân sự, phòng và nguồn lực bắt buộc; sau khi bệnh nhân xác nhận (nếu quy trình yêu cầu), chỉ định được lên lịch. Khi thực hiện, nhân sự ghi nhận thời gian, kết quả và trạng thái; bác sĩ xem kết quả để hoàn tất kế hoạch.

## **5.5. Luồng kết thúc/ra viện hoặc rời cơ sở**

Bác sĩ xác nhận tình trạng, tóm tắt đợt khám, hướng dẫn chăm sóc, đơn thuốc, dấu hiệu cảnh báo và kế hoạch tái khám. Nếu cơ sở có quy trình yêu cầu ra viện/ra về, bệnh nhân hoặc nhân viên có thể tạo yêu cầu; nhân sự có thẩm quyền rà soát và xác nhận. Trạng thái kết thúc không được dùng để xóa hoặc bỏ qua các chỉ định chưa hoàn tất mà không có xử lý ngoại lệ.

## **5.6. Ngoại lệ phải được thiết kế**

* Không tìm được hồ sơ hoặc phát hiện hồ sơ trùng: cho phép lưu nháp/đưa vào hàng chờ xác minh; chỉ vai trò được cấp quyền mới được hợp nhất.  
* Bệnh nhân đến không hẹn: tạo lượt khám theo chính sách tiếp nhận và cập nhật hàng đợi.  
* Bác sĩ vắng mặt hoặc phòng không khả dụng: đổi lịch có kiểm tra xung đột, lưu lý do và thông báo liên quan.  
* Mất kết nối trong lúc nhập liệu: có cơ chế giữ nháp/khôi phục phù hợp; không hiển thị thành công nếu dữ liệu chưa được lưu máy chủ.  
* Sai sót trong hồ sơ đã ký: dùng bản bổ sung/đính chính có lý do và dấu thời gian, không âm thầm ghi đè.  
* Cảnh báo dị ứng/chống chỉ định: cho phép dừng quy trình, đổi lựa chọn hoặc override có kiểm soát tùy chính sách.  
* Hủy thủ thuật sau khi đã đặt phòng: giải phóng nguồn lực và ghi lịch sử hủy.

# **6\. Yêu cầu chức năng chi tiết**

Mức ưu tiên: P0 \= bắt buộc cho MVP; P1 \= quan trọng, có thể triển khai ngay sau MVP; P2 \= mở rộng. Các yêu cầu bên dưới được viết theo hướng có thể chuyển thành ticket và ca kiểm thử.

| ID | Chức năng | Ưu tiên | Mô tả yêu cầu | Tiêu chí kiểm tra chính |
| :---- | :---- | :---- | :---- | :---- |
| FR-001 | Đăng nhập và phiên làm việc | P0 | Người dùng đăng nhập bằng tài khoản cá nhân; hệ thống áp dụng chính sách mật khẩu, khóa/tạm ngưng tài khoản, hết hạn phiên và đăng xuất. Khi phiên hết hạn, yêu cầu xác thực lại trước khi xem/sửa dữ liệu nhạy cảm. | Đăng nhập hợp lệ thành công; sai thông tin không tiết lộ tài khoản có tồn tại hay không; tài khoản bị khóa không thể truy cập. |
| FR-002 | Quản lý người dùng và vai trò | P0 | Quản trị được tạo, sửa, vô hiệu hóa tài khoản; gán vai trò/quyền theo mô-đun và phạm vi cơ sở; thay đổi quyền có hiệu lực theo chính sách phiên. | Người không có quyền bị chặn ở cả UI và API; mọi thay đổi vai trò được ghi audit. |
| FR-003 | Hồ sơ bệnh nhân | P0 | Tạo và quản lý mã bệnh nhân duy nhất, thông tin định danh, liên hệ, người liên hệ khi cần, thông tin hành chính, ghi chú và trạng thái hồ sơ. Có tìm kiếm và gợi ý trùng trước khi tạo. | Không thể tạo mã nội bộ trùng; kết quả tìm kiếm tuân theo quyền; hồ sơ trùng được gợi ý để xác minh. |
| FR-004 | Hợp nhất hồ sơ trùng | P1 | Vai trò được ủy quyền có thể đề nghị và thực hiện hợp nhất sau khi xác minh. Hệ thống giữ liên kết nguồn, lịch sử, người duyệt và không làm mất dấu vết. | Chỉ người có quyền mới hợp nhất; thao tác có audit và có thể truy ngược hồ sơ nguồn. |
| FR-005 | Lịch hẹn | P0 | Tạo, xem, sửa, đổi lịch, xác nhận, hủy và đánh dấu không đến. Hỗ trợ xem theo ngày/tuần/tháng, bác sĩ, chuyên khoa và trạng thái; kiểm tra lịch làm việc và xung đột. | Hệ thống từ chối đặt trùng nguồn lực; thay đổi trạng thái lưu thời điểm/người thực hiện. |
| FR-006 | Check-in và hàng đợi | P0 | Tiếp nhận bệnh nhân đã hẹn hoặc không hẹn, tạo lượt khám, cấp số/hàng đợi theo cấu hình và cập nhật trạng thái chờ/đang phục vụ. | Bệnh nhân chỉ có một lượt khám hoạt động trùng theo quy tắc cơ sở; nhân viên nhìn thấy đúng hàng đợi theo quyền. |
| FR-007 | Sinh hiệu và sàng lọc | P0 | Điều dưỡng ghi nhận các chỉ số được cơ sở cấu hình, thời điểm, người đo và ghi chú. Hỗ trợ xem xu hướng theo thời gian và đánh dấu dữ liệu chưa xác minh. | Giá trị có đơn vị, thời gian và người ghi; sửa đổi sau lưu được ghi lịch sử. |
| FR-008 | Hồ sơ sức khỏe theo thời gian | P0 | Trang tổng quan bệnh nhân tập hợp các lượt khám, chẩn đoán, dị ứng, thuốc, thủ thuật, tài liệu và ghi chú được phép xem. | Thông tin hiển thị đúng thứ tự thời gian; dữ liệu nhạy cảm tuân theo quyền truy cập. |
| FR-009 | Ghi chú khám lâm sàng | P0 | Bác sĩ ghi lý do đến khám, bệnh sử, triệu chứng, khám thực thể, đánh giá, kế hoạch và ghi chú theo mẫu. Hỗ trợ lưu nháp, ký/xác nhận và khóa theo chính sách. | Không thể ký nếu thiếu trường bắt buộc; hồ sơ đã ký không bị ghi đè âm thầm. |
| FR-010 | Triệu chứng và chẩn đoán | P0 | Ghi nhận triệu chứng, dấu hiệu, chẩn đoán chính/phụ, loại chẩn đoán và ghi chú; tìm theo danh mục được duyệt và hỗ trợ nhập có kiểm soát. | Mỗi chẩn đoán gắn với lượt khám, người ghi và thời gian; thay đổi được truy vết. |
| FR-011 | Cảnh báo lâm sàng | P0 | Hiển thị cảnh báo dị ứng, tương tác/chống chỉ định khi dữ liệu nguồn có sẵn và luật được cấu hình. Cảnh báo nêu lý do và hành động đề xuất; không tự động thay quyết định bác sĩ. | Cảnh báo được kích hoạt đúng theo bộ quy tắc đã kiểm thử; override yêu cầu quyền/lý do theo chính sách. |
| FR-012 | Danh mục thuốc và kê đơn | P0 | Chọn thuốc từ danh mục; ghi hoạt chất/tên, hàm lượng, liều, đường dùng, tần suất, thời gian, số lượng, hướng dẫn và người kê. Hỗ trợ lưu nháp, ký, in/xuất bản theo chính sách. | Không phát hành đơn thiếu trường bắt buộc; đơn gắn với bệnh nhân và lượt khám; lịch sử sửa đơn được lưu. |
| FR-013 | Ghi nhận dùng/cấp thuốc | P1 | Nhân sự được cấp quyền ghi nhận thuốc đã cấp hoặc đã dùng, liều/thời điểm/người thực hiện và lý do không thực hiện nếu có. | Bản ghi dùng thuốc liên kết với đơn/y lệnh và người thực hiện; không tự suy ra thuốc đã dùng chỉ từ việc kê đơn. |
| FR-014 | Chỉ định thủ thuật/dịch vụ | P0 | Tạo chỉ định gắn với lượt khám, loại dịch vụ, lý do, ưu tiên, hướng dẫn chuẩn bị và trạng thái. Cho phép cập nhật tiến độ bởi vai trò được phân công. | Không thể hoàn tất chỉ định khi thiếu thông tin bắt buộc theo cấu hình; mọi chuyển trạng thái được lưu. |
| FR-015 | Lập lịch thủ thuật và phân bổ nguồn lực | P0 | Tìm slot dựa trên lịch nhân sự, thời lượng, phòng và nguồn lực cần thiết; xác nhận lịch; đổi/hủy lịch và giải phóng nguồn lực. | Không chấp nhận xung đột đồng thời; thay đổi slot lưu lịch sử và thông báo theo cấu hình. |
| FR-016 | Quản lý phòng | P0 | Quản lý mã phòng, loại phòng, sức chứa, trạng thái, cơ sở và thuộc tính/thiết bị cần thiết. Hiển thị phòng trống/đang sử dụng/bảo trì. | Không phân phòng không khả dụng; lịch sử phân bổ phòng được lưu. |
| FR-017 | Lịch làm việc nhân sự | P0 | Cấu hình ca làm việc, ngày nghỉ, thời gian đệm, thời lượng dịch vụ và năng lực/loại dịch vụ được thực hiện. | Bộ kiểm tra lịch sử dụng dữ liệu mới nhất; không cho đặt lịch ngoài thời gian cho phép trừ khi được override. |
| FR-018 | Thực hiện thủ thuật/dịch vụ | P0 | Kỹ thuật viên/nhân sự phụ trách cập nhật bắt đầu, hoàn tất, hoãn, hủy và kết quả/tài liệu cần thiết. | Chỉ nhân sự có quyền/phân công được cập nhật; thời gian và người thực hiện được lưu. |
| FR-019 | Yêu cầu xét nghiệm/hình ảnh | P1 | Tạo yêu cầu, theo dõi trạng thái, gắn kết quả và tệp báo cáo; hỗ trợ nhập thủ công hoặc tích hợp khi được duyệt. | Kết quả gắn đúng bệnh nhân/lượt khám/yêu cầu; có trạng thái chờ, có kết quả, đã xem. |
| FR-020 | Tóm tắt kết thúc lượt khám | P0 | Bác sĩ ghi kết luận, hướng dẫn chăm sóc, dấu hiệu cần quay lại, thuốc, lịch tái khám và tài liệu hướng dẫn. | Chỉ hoàn tất khi đáp ứng checklist cấu hình; bệnh nhân chỉ xem phần được công bố. |
| FR-021 | Yêu cầu ra về/ra viện | P1 | Hỗ trợ tạo yêu cầu, rà soát, chấp thuận/từ chối theo vai trò; ghi tình trạng và lý do nếu cần. | Không đóng lượt khám khi còn bước bắt buộc chưa xử lý nếu quy trình không cho phép. |
| FR-022 | Danh mục dịch vụ và bảng giá | P0 | Quản lý mã dịch vụ, tên, mô tả, thời lượng, yêu cầu phòng/nhân sự và mức giá có hiệu lực theo ngày. | Hóa đơn lưu giá áp dụng tại thời điểm phát sinh; thay đổi bảng giá không sửa ngược hóa đơn cũ. |
| FR-023 | Hóa đơn và thanh toán cơ bản | P0 | Tạo hóa đơn từ dịch vụ/đơn hàng hợp lệ; ghi giảm giá theo quyền, phương thức thanh toán, số tiền, thời gian và trạng thái chưa thanh toán/đã thanh toán/hoàn tiền. | Tổng tiền tính nhất quán; giao dịch hoàn/hủy có lý do và nhật ký; quy định hóa đơn cần được xác nhận. |
| FR-024 | Thông báo | P1 | Gửi nhắc lịch, xác nhận, thay đổi lịch và hướng dẫn theo kênh được cấu hình (trong ứng dụng/email/SMS). Mẫu thông báo không để lộ dữ liệu sức khỏe nhạy cảm không cần thiết. | Thông báo ghi trạng thái gửi/thất bại; lỗi gửi không làm mất dữ liệu lịch. |
| FR-025 | Cổng bệnh nhân | P1 | Cho phép bệnh nhân xác thực, xem lịch của mình, gửi yêu cầu đặt/đổi/hủy lịch và xem tài liệu được cơ sở công bố. | Bệnh nhân không thể truy cập hồ sơ người khác; tài liệu nhạy cảm cần chính sách công bố rõ ràng. |
| FR-026 | Dashboard vận hành | P0 | Hiển thị số lượt khám, lịch hôm nay, hàng đợi, trạng thái thủ thuật, công suất phòng và chỉ số phù hợp với vai trò. | Chỉ số có định nghĩa thống nhất; người dùng chỉ xem dữ liệu thuộc phạm vi được cấp. |
| FR-027 | Báo cáo và xuất dữ liệu | P0 | Báo cáo lịch hẹn, lượt khám, sử dụng phòng, dịch vụ, doanh thu và chỉ số hoạt động; lọc theo khoảng thời gian/chuyên khoa/bác sĩ. | Xuất dữ liệu phải kiểm tra quyền, ghi log và không vượt phạm vi; dữ liệu báo cáo đối soát được. |
| FR-028 | Quản trị danh mục | P0 | Quản lý chuyên khoa, triệu chứng, chẩn đoán, thuốc, thủ thuật, loại phòng, trạng thái, biểu mẫu và các enum nghiệp vụ. | Danh mục có trạng thái hoạt động/ngừng dùng; bản ghi lịch sử vẫn tham chiếu được mục cũ. |
| FR-029 | Tài liệu đính kèm | P0 | Tải lên, xem và tải xuống tệp theo loại tài liệu; gắn với bệnh nhân/lượt khám/chỉ định; kiểm soát quyền và loại tệp. | Kiểm tra loại/kích thước tệp; không công khai URL không bảo vệ; có người tải lên và thời gian. |
| FR-030 | Nhật ký kiểm toán | P0 | Ghi sự kiện đăng nhập, xem/sửa dữ liệu nhạy cảm theo chính sách, ký hồ sơ, sửa đơn, xuất dữ liệu, đổi quyền, hợp nhất hồ sơ và thao tác đặc quyền. | Log không cho người dùng thông thường sửa/xóa; có thể lọc theo người, thời gian, bệnh nhân và loại sự kiện. |
| FR-031 | Tìm kiếm toàn hệ thống | P0 | Tìm bệnh nhân, lịch, lượt khám và chỉ định theo trường được phép; có lọc, phân trang và giới hạn kết quả. | Tìm kiếm tôn trọng quyền; không hiển thị dữ liệu nhạy cảm qua gợi ý không được phép. |
| FR-032 | Lưu nháp và phục hồi nhập liệu | P1 | Giữ bản nháp cho biểu mẫu dài và cảnh báo khi người dùng rời trang có thay đổi chưa lưu; hiển thị trạng thái lưu rõ ràng. | Không báo lưu thành công trước khi server xác nhận; phục hồi không ghi đè phiên bản mới hơn. |
| FR-033 | Cấu hình cơ sở/chi nhánh | P2 | Cho phép tổ chức dữ liệu, người dùng, lịch, phòng và báo cáo theo cơ sở; hỗ trợ chính sách liên cơ sở nếu được phê duyệt. | Người dùng không nhìn thấy dữ liệu cơ sở khác nếu chưa được cấp quyền. |
| FR-034 | Tích hợp hệ thống ngoài | P2 | Cung cấp API/connector có xác thực, giới hạn quyền, ghi log, quản lý lỗi và cơ chế thử lại cho tích hợp được phê duyệt. | Không gửi dữ liệu thật đến môi trường thử nghiệm; hợp đồng API có versioning và kiểm thử. |

# **7\. Danh mục màn hình và điều hướng**

Danh mục dưới đây là inventory ban đầu cho UX/UI và phân rã frontend. Tên màn hình có thể điều chỉnh sau khi thống nhất quy trình; các màn hình dữ liệu nhạy cảm phải có trạng thái tải, rỗng, lỗi, không đủ quyền và xác nhận thao tác.

| Mã | Màn hình | Người dùng | Nội dung/điểm cần thiết kế |
| :---- | :---- | :---- | :---- |
| S-01 | Đăng nhập/khôi phục | Mọi nhân sự | Đăng nhập, lỗi xác thực, quên mật khẩu, khóa tài khoản, MFA nếu bật |
| S-02 | Dashboard theo vai trò | Tất cả vai trò nội bộ | Việc cần làm, lịch hôm nay, hàng đợi, cảnh báo vận hành |
| S-03 | Danh sách bệnh nhân | Lễ tân, bác sĩ, điều dưỡng theo quyền | Tìm kiếm, lọc, tạo hồ sơ, gợi ý trùng |
| S-04 | Tạo/sửa hồ sơ bệnh nhân | Lễ tân và vai trò được cấp | Thông tin hành chính, liên hệ, đồng ý/chấp thuận cần thiết |
| S-05 | Tổng quan hồ sơ bệnh nhân | Vai trò lâm sàng và liên quan | Dòng thời gian, dị ứng, thuốc, lượt khám, tài liệu |
| S-06 | Lịch hẹn dạng lịch | Lễ tân, bác sĩ, quản lý | Ngày/tuần/tháng, bộ lọc, tạo/đổi/hủy lịch |
| S-07 | Chi tiết lịch hẹn | Vai trò vận hành | Thông tin bệnh nhân, dịch vụ, bác sĩ, trạng thái và lịch sử |
| S-08 | Check-in/hàng đợi | Lễ tân, điều dưỡng | Tiếp nhận, số thứ tự, ưu tiên, trạng thái phục vụ |
| S-09 | Sàng lọc/sinh hiệu | Điều dưỡng | Biểu mẫu sinh hiệu, ghi chú và lịch sử đo |
| S-10 | Không gian làm việc khám | Bác sĩ | Thông tin lượt khám, tóm tắt tiền sử, cảnh báo và tác vụ |
| S-11 | Ghi chú khám | Bác sĩ | Triệu chứng, bệnh sử, khám thực thể, đánh giá, kế hoạch |
| S-12 | Chẩn đoán | Bác sĩ | Tìm danh mục, chẩn đoán chính/phụ, ghi chú |
| S-13 | Kê đơn thuốc | Bác sĩ | Chọn thuốc, liều, đường dùng, tần suất, thời gian, cảnh báo |
| S-14 | Theo dõi cấp/dùng thuốc | Dược sĩ/điều dưỡng theo quyền | Đơn, lịch sử cấp/dùng, lý do không thực hiện |
| S-15 | Tạo chỉ định | Bác sĩ | Thủ thuật/dịch vụ, mục đích, mức ưu tiên, hướng dẫn |
| S-16 | Lập lịch thủ thuật | Điều phối/kỹ thuật viên | Slot, phòng, nhân sự, nguồn lực, xác nhận |
| S-17 | Bảng điều phối phòng | Quản lý/lễ tân/điều phối | Trạng thái phòng, lịch sử sử dụng, bảo trì |
| S-18 | Lịch làm việc nhân sự | Quản lý/nhân sự được cấp | Ca, nghỉ, thời lượng dịch vụ, thời gian đệm |
| S-19 | Thực hiện dịch vụ | Kỹ thuật viên/điều dưỡng | Danh sách công việc, bắt đầu/hoàn tất, kết quả |
| S-20 | Xét nghiệm/hình ảnh | Bác sĩ, kỹ thuật viên | Yêu cầu, trạng thái, kết quả và tệp |
| S-21 | Tóm tắt kết thúc lượt khám | Bác sĩ | Kết luận, hướng dẫn, đơn thuốc, tái khám, ký |
| S-22 | Ra về/ra viện | Bác sĩ/điều dưỡng theo quyền | Yêu cầu, rà soát, xác nhận và lý do |
| S-23 | Hóa đơn | Thu ngân | Dịch vụ, bảng giá, giảm giá theo quyền, tổng tiền |
| S-24 | Thanh toán/hoàn tiền | Thu ngân/quản lý theo quyền | Giao dịch, phương thức, trạng thái, lý do hoàn |
| S-25 | Dashboard và báo cáo | Quản lý, tài chính, vận hành | Bộ lọc, KPI, bảng dữ liệu, xuất file |
| S-26 | Quản trị danh mục | Quản trị/được ủy quyền | Thuốc, chẩn đoán, dịch vụ, phòng, chuyên khoa |
| S-27 | Quản lý người dùng/quyền | Quản trị | Tài khoản, vai trò, phạm vi, trạng thái |
| S-28 | Audit log | Kiểm toán/quản trị được ủy quyền | Tìm kiếm sự kiện, xem lịch sử, xuất có kiểm soát |
| S-29 | Cổng bệnh nhân | Bệnh nhân (giai đoạn 2\) | Lịch của tôi, yêu cầu lịch, tài liệu đã công bố, xác nhận |
| S-30 | Cài đặt hệ thống | Quản trị | Cấu hình cơ sở, giờ làm, quy tắc lịch, mẫu thông báo |

## **7.1. Nguyên tắc UX**

* Đặt hành động chính theo vai trò và trạng thái hiện tại; hạn chế màn hình chứa quá nhiều thao tác không liên quan.  
* Thông tin quan trọng cho an toàn (dị ứng, cảnh báo, định danh bệnh nhân) phải dễ thấy nhưng không gây nhiễu quá mức.  
* Mỗi biểu mẫu dài có phân nhóm rõ ràng, lưu nháp và chỉ ra trường bắt buộc.  
* Mọi thao tác hủy, ký, hoàn tiền, hợp nhất hồ sơ, xuất dữ liệu hoặc override cảnh báo cần xác nhận phù hợp.  
* Không chỉ dựa vào màu sắc để truyền đạt trạng thái; dùng nhãn, biểu tượng và văn bản.  
* Giao diện cần hỗ trợ bàn phím và bố cục thích ứng cho desktop/tablet; tối ưu thao tác lặp tại quầy và phòng khám.

# **8\. Mô hình dữ liệu khái niệm**

Danh sách sau mô tả các thực thể nghiệp vụ chính để đội BA/SA thiết kế ERD và API. Đây chưa phải schema vật lý cuối cùng; kiểu dữ liệu, khóa, ràng buộc, chuẩn mã và chiến lược lưu tệp cần được chốt ở bước thiết kế.

| Thực thể | Ý nghĩa | Thuộc tính gợi ý | Quan hệ/quy tắc |
| :---- | :---- | :---- | :---- |
| Patient | Bệnh nhân | patient\_id/MRN, thông tin định danh, liên hệ, trạng thái | Một bệnh nhân có nhiều lượt khám, lịch, tài liệu |
| UserAccount | Tài khoản | user\_id, username, trạng thái, thông tin xác thực tham chiếu | Liên kết nhân sự hoặc bệnh nhân; không lưu mật khẩu dạng rõ |
| Role / Permission | Vai trò/quyền | role\_id, permission\_id, resource, action, scope | Nhiều-nhiều với người dùng/role theo chính sách |
| Employee | Nhân sự | employee\_id, họ tên, chức danh, trạng thái, cơ sở | Có thể gắn chuyên khoa và lịch làm việc |
| Doctor / Nurse / TechnicalStaff / Receptionist | Vai trò nghiệp vụ nhân sự | Thông tin chuyên môn/thuộc tính theo vai trò | Có thể triển khai bằng bảng mở rộng hoặc mô hình nhân sự thống nhất |
| Specialization | Chuyên khoa | specialization\_id, tên, mô tả, trạng thái | Bác sĩ có thể thuộc một/nhiều chuyên khoa theo quy tắc |
| Appointment | Lịch hẹn | appointment\_id, patient\_id, provider\_id, service\_id, start/end, status | Liên kết lượt khám khi check-in; có lịch sử trạng thái |
| Schedule / ScheduleTime | Lịch làm việc/khung giờ | schedule\_id, nhân sự, phòng, thời gian, quy tắc lặp | Nguồn để kiểm tra slot khả dụng |
| Encounter / Visit | Lượt khám | encounter\_id, patient\_id, thời gian, người phụ trách, trạng thái | Trung tâm liên kết ghi chú, chẩn đoán, đơn và chỉ định |
| PatientHistory | Tiền sử bệnh | history\_id, patient\_id/encounter\_id, loại, nội dung, thời điểm | Cần phân biệt tiền sử dài hạn với ghi chú một lượt khám |
| VitalSign | Sinh hiệu | vital\_id, encounter\_id, loại, giá trị, đơn vị, người ghi, thời điểm | Lưu giá trị có cấu trúc và lịch sử chỉnh sửa |
| Symptom | Triệu chứng | symptom\_id, mã, tên, mô tả | Danh mục chuẩn; có thể liên kết lượt khám |
| Diagnosis / DiagnosisType | Chẩn đoán/loại chẩn đoán | diagnosis\_id, mã, tên, loại, encounter\_id, người ghi | Cần lưu phiên bản/mã danh mục khi phù hợp |
| Medication / Drug | Danh mục thuốc | drug\_id, tên, hoạt chất, hàm lượng, dạng bào chế, trạng thái | Thông tin tương tác/chống chỉ định phụ thuộc nguồn dữ liệu |
| Prescription | Đơn thuốc | prescription\_id, encounter\_id, người kê, trạng thái, thời điểm | Có nhiều dòng thuốc; lưu bản đã ký bất biến về mặt lịch sử |
| PrescriptionItem | Dòng thuốc | drug\_id, liều, đường dùng, tần suất, thời gian, số lượng, hướng dẫn | Tách dòng thuốc để quản lý từng thuốc trong đơn |
| MedicationAdministration | Cấp/dùng thuốc | prescription\_item\_id, người thực hiện, thời gian, liều thực tế, trạng thái | Phân biệt kê đơn với cấp phát hoặc thực dùng |
| Procedure / Service | Danh mục thủ thuật/dịch vụ | service\_id, mã, tên, thời lượng, giá, yêu cầu nguồn lực | Có hiệu lực giá theo thời gian nếu cần |
| ProcedureOrder | Chỉ định thủ thuật | order\_id, encounter\_id, service\_id, chỉ định, trạng thái | Liên kết lịch thực hiện, kết quả và consent |
| Room / RoomType | Phòng/loại phòng | room\_id, mã, sức chứa, trạng thái, thuộc tính | Có thể có quan hệ phân cấp phòng nếu cơ sở cần |
| ResourceBooking | Đặt nguồn lực | resource\_id, thời gian, đối tượng tham chiếu, trạng thái | Dùng để chống trùng phòng/nhân sự/thiết bị |
| RequestForm | Yêu cầu | request\_id, loại, người tạo, trạng thái, lý do | Dùng cho yêu cầu ra về/đổi lịch hoặc quy trình có phê duyệt |
| Consent | Đồng ý/chấp thuận | consent\_id, bệnh nhân, loại, nội dung/phiên bản, thời điểm, cách xác nhận | Phạm vi cần được xác định theo quy trình và luật áp dụng |
| Invoice / Payment | Hóa đơn/thanh toán | invoice\_id, dòng phí, tổng, trạng thái; payment\_id, phương thức, số tiền | Lưu dấu vết giao dịch, hoàn tiền và đối soát |
| Attachment | Tài liệu | attachment\_id, loại, vị trí lưu, checksum, người tải lên, thời gian | Quyền truy cập và thời hạn lưu trữ phải cấu hình |
| AuditLog | Nhật ký kiểm toán | actor, action, entity, timestamp, before/after hoặc diff, reason | Bảo vệ khỏi chỉnh sửa trái phép; tránh ghi thừa dữ liệu nhạy cảm |
| Notification | Thông báo | recipient, channel, template, status, sent\_at | Không đưa nội dung y tế nhạy cảm vào kênh không an toàn |

## **8.1. Nguyên tắc dữ liệu**

* Dùng khóa nội bộ ổn định; mã bệnh nhân hiển thị không nên phụ thuộc vào tên hoặc số điện thoại.  
* Các thực thể lâm sàng phải liên kết rõ với bệnh nhân, lượt khám, người tạo, thời điểm và trạng thái.  
* Dùng danh mục có phiên bản hoặc lưu snapshot cần thiết để hồ sơ lịch sử không bị đổi nghĩa khi danh mục cập nhật.  
* Không lưu dữ liệu thẻ thanh toán hoặc thông tin xác thực nhạy cảm nếu không có nhu cầu và thiết kế bảo mật phù hợp.  
* Tệp lớn nên lưu ở object storage/private storage; cơ sở dữ liệu lưu metadata, quyền, checksum và tham chiếu.  
* Thiết kế cơ chế xóa mềm/vô hiệu hóa cho danh mục và tài khoản; hồ sơ y tế cần chính sách lưu giữ/đính chính riêng.

# **9\. Trạng thái nghiệp vụ và quy tắc chuyển trạng thái**

| Đối tượng | Trạng thái gợi ý | Quy tắc |
| :---- | :---- | :---- |
| Lịch hẹn | Nháp → Đã đặt → Đã xác nhận → Đã check-in → Đang phục vụ → Hoàn tất | Nhánh hủy/đổi lịch/không đến; lưu lý do và người thao tác. |
| Lượt khám | Mới tạo → Đang chờ → Đang khám → Chờ dịch vụ/kết quả → Chờ kết thúc → Hoàn tất | Chỉ vai trò phù hợp chuyển trạng thái; trạng thái hoàn tất không đồng nghĩa được xóa. |
| Ghi chú lâm sàng | Nháp → Đã ký → Bổ sung/đính chính | Không cho sửa âm thầm bản đã ký; lưu liên kết tới bản gốc. |
| Đơn thuốc | Nháp → Đã ký/phát hành → Đã cấp một phần/đã cấp → Hủy theo quy trình | Đơn đã phát hành cần lưu lịch sử; hủy phải có lý do và quyền. |
| Chỉ định | Mới tạo → Đã duyệt/đã lên lịch → Đang thực hiện → Hoàn tất | Nhánh hoãn/hủy/không thực hiện; không xóa lịch sử. |
| Hóa đơn | Nháp → Phát hành → Chưa thanh toán/Thanh toán một phần/Đã thanh toán | Hoàn tiền/điều chỉnh phải có giao dịch và audit tương ứng. |
| Yêu cầu ra về | Mới tạo → Đang rà soát → Được chấp thuận/Từ chối | Chỉ áp dụng khi cơ sở có quy trình; ghi người duyệt và lý do. |

## **9.1. Quy tắc chuyển trạng thái chung**

* Không cho phép chuyển trạng thái vượt bước bắt buộc nếu không có quyền override.  
* Mỗi chuyển trạng thái lưu trạng thái trước/sau, actor, timestamp và lý do khi cần.  
* API phải kiểm tra trạng thái hiện tại trước khi cập nhật để tránh hai người dùng cập nhật xung đột.  
* Giao diện phải phân biệt rõ hành động hoàn tất, hủy, xóa nháp và lưu tạm.

# **10\. Yêu cầu phi chức năng**

| ID | Thuộc tính | Yêu cầu | Cách xác minh |
| :---- | :---- | :---- | :---- |
| NFR-01 | Hiệu năng | Màn hình nghiệp vụ thông thường có thời gian phản hồi mục tiêu p95 ≤ 2 giây trong tải dự kiến; tìm kiếm mục tiêu p95 ≤ 3 giây. Các báo cáo nặng có thể chạy nền. | Đo bằng kiểm thử hiệu năng trên cấu hình môi trường đã thống nhất. |
| NFR-02 | Khả dụng | Mục tiêu khởi điểm 99,5% theo tháng cho môi trường production, không tính cửa sổ bảo trì đã thông báo; cần xác nhận theo ngân sách và hạ tầng. | Theo dõi uptime và thời gian gián đoạn; định nghĩa SLA rõ. |
| NFR-03 | Sao lưu/khôi phục | Mục tiêu đề xuất RPO ≤ 15 phút và RTO ≤ 4 giờ cho dữ liệu trọng yếu; có sao lưu mã hóa và diễn tập khôi phục. | Bằng chứng từ bài kiểm thử khôi phục định kỳ, không chỉ log sao lưu. |
| NFR-04 | Bảo mật truyền tải | Dùng HTTPS/TLS hiện đại cho mọi luồng có dữ liệu nhạy cảm; không gửi thông tin xác thực qua kênh không an toàn. | Quét cấu hình TLS và kiểm thử đường dẫn HTTP. |
| NFR-05 | Bảo mật lưu trữ | Mã hóa dữ liệu nhạy cảm khi lưu trữ theo khả năng nền tảng; quản lý khóa tách biệt và hạn chế quyền truy cập. | Rà soát cấu hình database, backup, object storage và secrets. |
| NFR-06 | Phân quyền | RBAC, quyền tối thiểu, kiểm tra phía server, cách ly phạm vi cơ sở và quản lý phiên. | Kiểm thử âm: người không quyền không thể đọc/ghi qua API trực tiếp. |
| NFR-07 | Auditability | Sự kiện nhạy cảm có nhật ký đủ để điều tra; log được bảo vệ, có đồng bộ thời gian và chính sách lưu giữ. | Kiểm thử các luồng sửa bệnh án, kê đơn, xuất dữ liệu, đổi quyền. |
| NFR-08 | Khả năng phục hồi | Lỗi phụ thuộc ngoài không làm mất dữ liệu đã lưu; tác vụ gửi thông báo có retry/idempotency phù hợp. | Mô phỏng timeout, lỗi mạng và gửi lặp. |
| NFR-09 | Khả năng mở rộng | Tách mô-đun theo miền nghiệp vụ, API rõ ràng, có phân trang và giới hạn truy vấn; không giả định cần microservices ngay từ đầu. | Kiểm thử tải và đánh giá kiến trúc theo quy mô dự kiến. |
| NFR-10 | Tương thích | Hỗ trợ các phiên bản ổn định gần đây của Chrome/Edge; bố cục desktop và tablet; trình duyệt khác được xác nhận theo nhu cầu. | Bộ kiểm thử trình duyệt mục tiêu. |
| NFR-11 | Khả năng tiếp cận | Hướng đến WCAG 2.1 AA: điều hướng bàn phím, nhãn biểu mẫu, độ tương phản, thông báo lỗi dễ hiểu. | Kiểm tra tự động kết hợp đánh giá thủ công. |
| NFR-12 | Bảo trì | Code có lint/test, logging có cấu trúc, tài liệu API, migration có kiểm soát, môi trường dev/staging/prod tách biệt. | Pipeline CI/CD có kiểm thử và quy trình rollback. |
| NFR-13 | Quan sát hệ thống | Có metrics, logs, traces phù hợp; cảnh báo lỗi 5xx, độ trễ, queue backlog, dung lượng và lỗi backup. | Có dashboard và người chịu trách nhiệm xử lý cảnh báo. |
| NFR-14 | Ngôn ngữ/định dạng | Tiếng Việt là mặc định; định dạng ngày giờ, số, tiền tệ và múi giờ thống nhất theo cấu hình cơ sở. | Kiểm thử ngày qua nửa đêm, định dạng và dữ liệu tiếng Việt có dấu. |
| NFR-15 | Toàn vẹn dữ liệu | Dùng transaction, ràng buộc khóa ngoại/unique và cơ chế chống gửi lặp cho thao tác quan trọng. | Kiểm thử đồng thời đặt lịch, thanh toán và cập nhật trạng thái. |
| NFR-16 | Khả năng kiểm thử | Logic nghiệp vụ quan trọng có unit/integration tests; luồng chính có end-to-end tests; dữ liệu test không dùng hồ sơ thật. | Có báo cáo test và ngưỡng chất lượng được thống nhất. |

# **11\. Bảo mật, quyền riêng tư và an toàn lâm sàng**

## **11.1. Bảo vệ dữ liệu sức khỏe**

* Thu thập dữ liệu theo mục đích nghiệp vụ; không yêu cầu trường thông tin không cần thiết nếu chưa có căn cứ.  
* Quy định rõ ai được xem, sửa, xuất, in và chia sẻ hồ sơ; dữ liệu xuất ra ngoài phải được kiểm soát.  
* Mã hóa truyền tải; bảo vệ bản sao lưu và tệp đính kèm; không đặt tệp bệnh án ở đường dẫn công khai.  
* Ghi nhận truy cập và thay đổi dữ liệu nhạy cảm theo chính sách kiểm toán; giới hạn người có quyền xem audit log.  
* Có quy trình xử lý sự cố dữ liệu: phát hiện, cô lập, đánh giá phạm vi, thông báo theo nghĩa vụ áp dụng và khắc phục.  
* Xác định thời hạn lưu trữ, quy trình cung cấp bản sao, đính chính và xử lý yêu cầu của bệnh nhân theo luật áp dụng.

## **11.2. An toàn lâm sàng**

* Hệ thống là công cụ hỗ trợ quy trình, không tự chẩn đoán hay thay thế quyết định của nhân viên y tế có thẩm quyền.  
* Cảnh báo chỉ có ý nghĩa khi nguồn dữ liệu và quy tắc được xác minh; hiển thị nguồn/lý do nếu khả thi.  
* Không coi việc kê đơn là bằng chứng thuốc đã được cấp hoặc đã sử dụng; cần thực thể/trạng thái riêng.  
* Hồ sơ đã ký cần được bảo toàn lịch sử; sửa đổi bằng bản bổ sung/đính chính có người thực hiện, thời gian và lý do.  
* Dùng định danh bệnh nhân rõ ràng trước khi ghi y lệnh, kê đơn hoặc hoàn tất thủ thuật để giảm nhầm bệnh nhân.  
* Các quy tắc bắt buộc và cảnh báo mức nghiêm trọng cần được chuyên gia lâm sàng của cơ sở phê duyệt và kiểm thử.

## **11.3. Pháp lý và tuân thủ**

Trước khi vận hành thật, chủ sản phẩm cần cùng chuyên gia pháp chế/an toàn thông tin xác định quy định hiện hành áp dụng tại quốc gia và địa phương, yêu cầu về hồ sơ bệnh án điện tử, chữ ký/xác nhận, lưu trữ, quyền bệnh nhân, thông báo sự cố, lưu trú dữ liệu và kết nối bên ngoài. Tài liệu này không tự tuyên bố hệ thống đạt HIPAA, ISO 27001 hay bất kỳ chứng nhận nào.

# **12\. Tiêu chí nghiệm thu tổng thể**

| Mã | Khu vực | Tiêu chí nghiệm thu |
| :---- | :---- | :---- |
| AC-01 | Tiếp nhận | Tạo bệnh nhân mới, tìm bệnh nhân cũ, phát hiện khả năng trùng và check-in đều hoạt động theo quyền. |
| AC-02 | Lịch | Không thể lưu hai lịch xung đột cho cùng nguồn lực khi quy tắc không cho phép; đổi/hủy lưu audit. |
| AC-03 | Khám | Bác sĩ xem được thông tin liên quan, tạo ghi chú, chẩn đoán và lưu nháp; thiếu trường bắt buộc thì không thể ký. |
| AC-04 | Kê đơn | Đơn lưu đủ liều, đường dùng, tần suất, thời gian và hướng dẫn theo cấu hình; cảnh báo được hiển thị và ghi nhận override. |
| AC-05 | Thủ thuật | Chỉ định có thể được lên lịch với phòng/nhân sự khả dụng; không đặt được khi có xung đột. |
| AC-06 | Thực hiện | Người được phân công cập nhật trạng thái và kết quả; lịch sử người thực hiện/thời gian được lưu. |
| AC-07 | Kết thúc | Lượt khám có tóm tắt, hướng dẫn và trạng thái kết thúc theo checklist; hồ sơ đã ký không bị ghi đè. |
| AC-08 | Thanh toán | Hóa đơn tính đúng theo bảng giá áp dụng; giao dịch thanh toán/hoàn tiền có lịch sử. |
| AC-09 | Phân quyền | Mọi endpoint nhạy cảm kiểm tra quyền; bệnh nhân chỉ xem tài liệu của chính mình đã được công bố. |
| AC-10 | Audit | Các sự kiện nhạy cảm nằm trong log với actor, timestamp, loại hành động và đối tượng. |
| AC-11 | Báo cáo | Số liệu báo cáo khớp dữ liệu nguồn theo định nghĩa nghiệp vụ đã thống nhất. |
| AC-12 | Khôi phục | Đội vận hành chứng minh khôi phục được backup trong thời gian mục tiêu được duyệt. |
| AC-13 | Hiệu năng | Kiểm thử đạt mục tiêu p95 đã thống nhất với dữ liệu và tải giả lập đại diện. |
| AC-14 | Tính ổn định | Lỗi mạng/timeout không tạo bản ghi trùng hoặc thông báo thành công giả; retry an toàn. |

## **12.1. Bộ kiểm thử tối thiểu**

* Happy path: đặt lịch → check-in → sinh hiệu → khám → chẩn đoán → kê đơn → chỉ định → kết thúc → thanh toán.  
* Phân quyền âm: lễ tân gọi trực tiếp API sửa chẩn đoán; bệnh nhân thử mở URL hồ sơ người khác; kỹ thuật viên truy cập dữ liệu không liên quan.  
* Xung đột đồng thời: hai người cùng đặt slot/phòng; hai người cùng cập nhật trạng thái lượt khám.  
* Sửa đổi sau ký: kiểm tra luồng đính chính, lịch sử phiên bản và quyền phê duyệt.  
* Cảnh báo lâm sàng: dữ liệu dị ứng phù hợp kích hoạt cảnh báo; override yêu cầu lý do theo cấu hình.  
* Sự cố hạ tầng: timeout, retry, gửi thông báo thất bại, backup/restore, hết phiên và mất kết nối giữa lúc nhập.

# **13\. Nguồn tham khảo và cách sử dụng**

1\. SoftwareIdeas – UML Diagram: Hospital Management System. Tham khảo cấu trúc use case, lớp nghiệp vụ và các sequence flow liên quan đến khám, chẩn đoán, kê đơn và đặt thủ thuật. https\://www\.softwareideas.net/uml-diagram-hospital-management-system

2\. GitHub – Smart Hospital Management System – UML. Tham khảo repository và cấu trúc tổ chức dự án; không dùng README như bằng chứng rằng mọi chức năng trong PRD đã được hiện thực. https\://github.com/saipraveen-k/Smart-Hospital-Management-System---UML

Cách sử dụng tài liệu: dùng PRD này làm baseline để workshop với đại diện nghiệp vụ và đội kỹ thuật; đánh dấu từng yêu cầu là Confirmed / Changed / Out of scope; sau đó cập nhật phiên bản, người duyệt, ngày duyệt và liên kết tới thiết kế/issue tương ứng.

*Kết thúc tài liệu.*

