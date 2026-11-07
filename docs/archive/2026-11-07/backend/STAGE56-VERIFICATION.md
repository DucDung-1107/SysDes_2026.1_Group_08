# Evidence — Stage 5–6, 2026-10-31

AI trực tiếp triển khai và kiểm tra working tree MEDORA trên nhánh `feature`.
Thông điệp commit theo yêu cầu: `api chua co front`. Phần API và quyết định
nghiệp vụ nằm ở [API-STAGE56.md](API-STAGE56.md); frontend chưa triển khai.

## Kết quả thực thi

| Kiểm tra | Actual |
| --- | --- |
| Windows/JDK 21/Maven 3.9.11 | BUILD SUCCESS lúc 10:20:00 +07:00; 8 security + 10 Stage4 + 15 Stage56 tests; không failure/error/skipped |
| Linux container/JDK 21/PostgreSQL 17.6 | BUILD SUCCESS lúc 10:25:32 +07:00; cùng 33 tests qua; test process chạy dưới user 10001 |
| Runtime Docker | Java 21 JRE, API user 10001:10001; PostgreSQL 17.11; `/ready` trả UP |
| Flyway | V1–V7 qua trên database mới trong test; V5–V7 nâng cấp volume demo đang ở V4, giữ dữ liệu cũ |
| HTTP demo đầy đủ | PASS; Encounter Completed, đơn phát hành/cấp thuốc, order Completed/reviewed, file upload/download checksum đúng |
| Ledger demo | Hóa đơn subtotal 150.000, discount 10.000, nhận 140.000, refund 30.000 sau duyệt riêng; balance 30.000, cashflow tăng 110.000 VND |
| Merge demo | Reception yêu cầu, Registrar xác minh/merge; source và evidence giữ nguyên |
| Recreate API + DB | Snapshot SQL trước/sau giống nhau; file private có cùng SHA-256 qua volume attachment_data |
| Python | Hai script demo biên dịch thành công; Stage 6 demo chạy thực tế qua HTTP |

Snapshot persistence có 6 patient, 4 encounter, 5 note, 2 prescription,
1 medication event, 1 order Completed, 1 attachment, 1 invoice, tổng Payment
140.000, Refund Completed 30.000, 107 audit event, 7 migration thành công.
Trong đó có dữ liệu demo Stage 4 cũ và một lần demo dừng đúng ở validation
timestamp khi đồng hồ host chậm hơn Docker; request lỗi không ghi medication
event. Script sau đó dùng timestamp do server trả cho fixture và hoàn tất.

SHA-256 của file demo trước/sau recreate:

```text
4fc3b79f061b0f993d8d3fb203919758af93f24085c6719ab71ae9854ae0ca59
```

## Các ca Stage 5–6 được kiểm tra

- Chặn issue khi chưa có policy/profile; giữ đơn và không tạo sự kiện thuốc.
- Dị ứng chặn cứng; luật có thể override phải đúng quyền, đủ reason/acknowledgement.
- Giữ nội dung đơn cũ khi amend; không đóng lượt còn draft; trigger chặn sửa evidence.
- Chỉ pharmacist được gán ghi sự kiện; retry không tạo thêm; không vượt lượng chỉ định.
- Order Pending không giữ nguồn lực; nhân sự, ca làm/năng lực, slot và trạng thái được kiểm tra.
- Complete thiếu kết quả bị chặn; giữ kết quả đã hoàn tất; review rồi mới đóng lượt.
- Hai order tranh nguồn lực: request lỗi giữ Pending, không booking dở; cancel giải phóng slot.
- Multipart kiểm tra content, giới hạn, scope, checksum, private download và archive.
- Thanh toán một phần; retry/key conflict; không overpay; ledger append-only.
- Hai request thanh toán đồng thời không vượt số dư; giá/invoice lịch sử giữ nguyên.
- Pending refund không đổi số dư; Cashier không refund, requester không tự duyệt.
- Execute retry một lần; Payment gốc còn nguyên; DB chặn xóa refund Completed.
- Hai pending refund không giữ vượt Payment; reject giải phóng phần giữ.
- Kết thúc khoảng giá trong tương lai/thêm giá mới không sửa invoice cũ; auto-refund theo policy rõ ràng.
- Cashflow đối soát; export có audit; Doctor không đọc báo cáo tiền, Auditor không ghi catalog.
- Merge đúng Registrar, giữ source, chặn check-in source, liên kết timeline và lượt cũ.
- In-app retry khi template bị tắt, giữ Appointment, inbox chỉ người nhận.
- Policy ra về có hiệu lực trong checklist; explicit session revocation chặn phiên cũ.

## Evidence ngắn

- [Windows summary](evidence/stage56-2026-10-31/windows-test-summary.txt)
- [Linux summary](evidence/stage56-2026-10-31/linux-test-summary.txt)
- [Security Linux](evidence/stage56-2026-10-31/DockerSecurityFlowTest.txt)
- [Stage4 Linux](evidence/stage56-2026-10-31/DockerStage4IT.txt)
- [Stage56 Linux](evidence/stage56-2026-10-31/DockerStage56IT.txt)
- [HTTP full demo](evidence/stage56-2026-10-31/docker-full-demo.json)
- [SQL trước](evidence/stage56-2026-10-31/persistence-before.json), [sau](evidence/stage56-2026-10-31/persistence-after.json)
- [File trước](evidence/stage56-2026-10-31/attachment-before.txt), [sau](evidence/stage56-2026-10-31/attachment-after.txt)
- [Startup sau recreate](evidence/stage56-2026-10-31/docker-startup.txt)
- [Danh sách file đã dọn](evidence/stage56-2026-10-31/cleanup.txt)

## Dọn và giới hạn

Chỉ dọn trong project: backend target/log, Python cache, LaTeX aux/log/cache và
ảnh debug trong tmp. Giữ source báo cáo, `.bib`, PDF cuối, hình UML, tools và
evidence ngắn. File LaTeX/debug/log được sao lưu ngoài repo tại
`Downloads/tmp/medora-cleanup-20261031`; target là output có thể build lại.
`.env` và `.env.docker-test` giữ local và bị ignore; không đưa credentials,
database hoặc file private vào commit. Không xóa Docker volume cũ hoặc dữ liệu
demo. Container chạy test tạm đã dọn, API/DB demo vẫn chạy.

S3 adapter được biên dịch; kiểm thử tệp ở phiên này dùng filesystem private và
volume Docker, chưa kiểm chứng bucket bên ngoài. Chưa có antivirus/CDR, tải/SLA,
production restore, nhiều node, UAT lâm sàng, portal hay frontend. Không suy ra
chứng nhận hoặc mức sẵn sàng sản xuất từ bộ test này. Các bảng Not Run trong
báo cáo cũ không được âm thầm chuyển thành Passed.
