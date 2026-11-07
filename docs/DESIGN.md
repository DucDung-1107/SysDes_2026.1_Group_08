# Thiết kế giao diện MEDORA

Giao diện workstation tiếng Việt cho nhân sự phòng khám, ưu tiên đọc hồ sơ lâu, đối chiếu danh tính và xử lý trạng thái rõ. Source và token hiện tại nằm trong `src/frontend`, không phụ thuộc mẫu HTML ngoài ứng dụng.

## Bố cục và nhận diện

- Navy `#163A5F`, xanh hành động `#2563A6`, canvas nhạt, surface trắng; đỏ chỉ cho lỗi/thao tác rủi ro. Màu không thay nhãn trạng thái.
- Font hệ thống có dấu tiếng Việt; số tiền/ngày/số liệu rõ đơn vị. Không tải font/illustration bên ngoài để render hồ sơ.
- Sidebar nhóm nghiệp vụ, topbar giữ cơ sở/tài khoản, nội dung có tiêu đề/phạm vi, bộ lọc, list và detail. Menu/action lọc theo quyền thực tế.
- Desktop sidebar 224px; tablet/mobile điều hướng thu gọn trong flow. Bảng rộng cuộn trong vùng bảng, không làm toàn trang tràn ngang.
- Khoảng cách 4/8/16/24/32px; control tối thiểu 44px; border/surface phân cấp nhẹ. Token chính thức: [tokens.css](../src/frontend/src/app/tokens.css).

## Component và phản hồi

Form có label, trợ giúp, lỗi gần trường và error summary. Không tự xóa nội dung khi request lỗi. Dialog cần xác nhận khi đóng bản chưa gửi; thao tác rủi ro có xác nhận rõ. Version conflict cho tải bản mới để đối chiếu, không âm thầm ghi đè.

List, detail, nested view, pagination, lookup và kết quả mutation dùng component chung. Các trạng thái đang tải, rỗng, thiếu quyền, request lỗi và server xác nhận được phân biệt. Không dùng số liệu giả, count-up chỉ số lâm sàng hoặc biểu đồ trang trí không có API.

UUID thực xuất hiện ở detail/kết quả để chuyển giữa các nghiệp vụ chưa có directory API. Mã bệnh nhân, ID lượt khám và ID dòng thuốc là các loại khác nhau; không dùng lẫn.

Lucide là bộ icon outline thống nhất, có nhãn tiếng Việt. Icon trang trí ẩn với assistive technology; icon-only button có accessible name. Minh họa login và check SVG do dự án tự viết; license dependency ở [THIRD_PARTY_NOTICES](../src/frontend/THIRD_PARTY_NOTICES.md).

## Chuyển động đang triển khai

| Hoạt động | Hiệu ứng |
| --- | --- |
| Hiện login | SVG connector 900ms và check 220ms, một lần |
| Mở form | Dialog vào nhẹ 180ms, backdrop 120ms |
| Chọn bản ghi | Detail fade ngắn, theo ID bản ghi |
| Mở/thu gọn view | Chevron xoay 160ms, nội dung hiện nhẹ |
| Tải lại/gửi request | Spinner trong thời gian chờ thực tế |
| Server xác nhận thành công | Check SVG vẽ nét 220ms |

Các hiệu ứng là CSS/SVG, không phải video. Không thêm runtime Motion/GSAP/Rive/Lottie khi chưa cần. Không stagger hàng bệnh nhân, đổi thứ tự queue bằng animation, bounce cảnh báo hoặc làm thay đổi chỗ bấm. `prefers-reduced-motion: reduce` bỏ chuyển động, vẫn hiện nội dung và check cuối.

## Nghiệm thu

Cần kiểm tra keyboard/focus/dialog, live announcements, zoom 200%, 320px reflow, tablet, màn desktop, slow network, conflict recovery và reduced motion. Công cụ phiên triển khai không có browser surface nên chưa có ảnh chụp hay bằng chứng phát animation thực tế. Build và HTTP CSS pass không thay thế các bước đó; xem [USER_GUIDE](USER_GUIDE.md) và [TESTING](TESTING.md).

Các nguồn và giới hạn truy cập/license được giữ trong [frontend-references.md](frontend-references.md).
