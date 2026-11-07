# MEDORA frontend implementation and verification

Updated: 2026-11-07, Asia/Saigon. This record describes the implemented frontend source and the verification actually performed. Session/CSRF checks and a synthetic reception-to-clinical-closure workflow passed over HTTP through the frontend proxy. Browser appearance, interaction and animation playback remain unverified in this environment.

## Architecture

The frontend is in [`src/frontend`](../src/frontend). It uses pinned Next.js **16.4.5**, React / React DOM **19.3.1**, TypeScript and Lucide React **1.70.0**, with a committed npm lockfile. Development and production servers bind to `127.0.0.1` by default. Node **24.12.0** was used for the recorded local tests.

The existing backend remains Java 21 / Spring Boot with PostgreSQL and Flyway. This work does not modify its business logic, migrations, database schema or Compose configuration. The frontend uses the current controllers and DTOs rather than the older NestJS / Prisma proposal in the report.

| Source | Responsibility |
| --- | --- |
| `src/app/` | App Router layouts, home, workspace route, metadata and error / missing-page states |
| `src/components/auth.tsx` | Session bootstrap, login, facility choice, permission loading and logout |
| `src/components/shell.tsx` | Permission-filtered navigation, facility and user context |
| `src/components/home.tsx` | Authorized dashboard fields, active queue and workspace links |
| `src/components/workspace.tsx` | Filtered resource lists, detail views, nested resources and action context |
| `src/components/forms.tsx` | Labeled fields, array editors, validation and explicit mutation confirmation |
| `src/components/ui.tsx` | Tables, details, dialogs, status labels and request results |
| `src/lib/schema.ts` | Typed module / field / action declarations |
| `src/lib/contracts.ts` | Permission checks, path encoding, field conversion and facility-time handling |
| `src/lib/api.ts` | Same-origin session / CSRF transport, errors and authorized downloads |
| `src/lib/use-api.ts` | Request lifecycle and cancellation for reads |
| `src/modules/` | Product-specific endpoints, fields, permissions and lifecycle actions |
| `src/app/globals.css` | MEDORA tokens, application layout, responsive rules and reduced-motion treatment |

The version-specific Next guides shipped in `node_modules/next/dist/docs/` were read before application implementation. The interface is a staff workstation using original MEDORA styles; it does not include copied clinic websites, third-party templates, external illustrations or video assets. Its design basis is documented in [frontend-design.md](frontend-design.md) and [frontend-references.md](frontend-references.md).

The [Vietnamese manual UAT checklist](frontend-uat.md) describes setup, role changes, expected states, the reception-to-payment flow and optional clinical branches. Its steps are pending manual browser execution, separate from the HTTP evidence below.

The motion follow-up uses independently authored CSS and SVG: a one-shot login illustration draw, 180ms dialog entrance, selected-record detail feedback, 160ms disclosure-chevron transitions, a 220ms confirmation-check draw after server success, and rotation during actual refresh requests. The final reduced-motion rule disables animation and transitions while preserving visible content and completed SVG paths. No Motion/GSAP/Rive/Lottie runtime, external animation asset or video background was added. Animation playback and interruption behavior remain unverified without a browser.

## Implemented workspace coverage

There are **36 declared workspace modules**, plus the session screen and home. These are functional schema-backed list / detail / form workspaces, with visibility and actions constrained by permissions. The inventory is not a claim that all 36 modules have passed browser or authenticated end-to-end verification.

| Group | Modules and source IDs |
| --- | --- |
| Tiếp nhận | Bệnh nhân (`patients`), Lịch hẹn (`appointments`), Hàng đợi tiếp nhận (`queue`), Khung giờ trống (`availability`), Thông báo đã gửi (`notifications`) |
| Nguồn lực | Phòng khám (`rooms`), Dịch vụ khám (`services`), Ca làm và nghỉ (`staff-schedules`), Năng lực dịch vụ (`provider-services`) |
| Lâm sàng | Lượt khám được phân công (`encounters`), Chi tiết lượt khám trước (`longitudinal`), Hồ sơ an toàn thuốc (`medication-profile`), Tệp riêng tư (`attachments`) |
| Thuốc & dịch vụ | Đơn thuốc theo lượt khám (`prescriptions`), Công việc dược được phân công (`pharmacy`), Chỉ định theo lượt khám (`procedures`), Dịch vụ được phân công (`procedure-queue`) |
| Danh mục | Danh mục thuốc (`medications`), Quy tắc an toàn thuốc (`medication-rules`), Nguồn chính sách thuốc (`medication-policy`) |
| Quản trị | Tài khoản nhân sự (`accounts`), Danh mục vai trò (`roles`), Bảng giá dịch vụ (`prices`), Chính sách cơ sở (`clinic-policy`), Mẫu thông báo (`notification-templates`), Cấu hình chẩn đoán (`diagnosis-admin`) |
| Tài chính | Hóa đơn (`invoices`), Biên nhận thanh toán (`receipts`), Tra cứu hoàn tiền (`refunds`) |
| Hồ sơ & kiểm soát | Hợp nhất hồ sơ (`merges`), Duyệt ra về (`discharge-approvals`), Nhật ký kiểm toán (`audit`), Lịch sử kiểm toán hồ sơ (`patient-audit`) |
| Báo cáo | Báo cáo vận hành (`operations`), Báo cáo thu chi (`revenue`) |
| Công việc | Hộp thư nội bộ (`inbox`) |

Clinical forms include history and vital corrections, SOAP drafts, signing, amendments, abandonment of amendment drafts, diagnosis selection, discharge-approval requests, closure submission / correction / completion, structured medication profiles, prescription draft arrays, safety review, issue / amendment / cancellation, medicine events, procedure allocation / execution / results / review and private attachment operations. Catalog and operational forms use actual backend fields and versions. No successful backend data is fabricated by the application.

The discharge-request action posts a required reason to the existing `/encounters/{id}/discharge-requests` API. The assigned doctor can give the returned request ID and version to a different authorized approver in `Duyệt ra về`; approval does not itself close the encounter. There is still no request-list API. This newly connected action has source-contract coverage; a real submission/approval round trip through the UI has not been performed.

Lists use contract-supplied counts where present. Array-only endpoints show the number of loaded records and page controls without inventing an overall total. A missing required path input prevents the read. Read failures, empty results, permission denial, pending mutations and server-confirmed results are separate states.

## Integration and integrity rules

- Authentication uses Spring sessions. Login is a form-urlencoded POST with a CSRF token and a valid 204 response; the application obtains a fresh CSRF token after login and bootstraps `/api/auth/me` plus `/api/auth/permissions`. It does not invent JWTs or store credentials in localStorage.
- `next.config.ts` rewrites `/api/:path*` to the configured backend origin. Browser requests remain same-origin, carry session cookies and use the CSRF header named by the backend. The default target is `http://127.0.0.1:8080`.
- Business requests stay under `/api/facilities/{facility}` and administration requests under `/api/admin/facilities/{facility}`. Navigation visibility is a convenience; server-side assignment and permission checks remain authoritative. An administrator does not automatically gain clinical access.
- Encounter access requires the assigned doctor / nurse. Prescription and procedure changes that require a doctor remain subject to the backend's stricter assigned-doctor guard. Pharmacy and technician worklists are assignment scoped.
- Version fields preserve optimistic-concurrency checks. Financial and prescription actions that use idempotency retain a request key for retries of the same submitted body. Mutations are not automatically retried after a rejected or uncertain request.
- SOAP draft text remains non-null even when incomplete. Signing requires complete sections and a diagnosis on the server. Signed notes remain immutable; amendments create a successor with a reason. Clinical closure is independent of payment.
- Medication warnings come from the actual configured source and structured profile. An empty warning list is not a clinical safety guarantee. Non-overridable blocks remain server-enforced; the frontend does not create rule data to bypass them.
- Private uploads use a multipart part named `file`, with an optional related `orderId`. Supported content is PDF, PNG, JPEG or UTF-8 text, up to 10 MiB. Downloads are authorized requests; storage keys and public file URLs are not invented. Technician uploads require the assigned order and an allowed execution state.
- Clinic wall times are converted using `NEXT_PUBLIC_FACILITY_ZONE`; birth dates remain date-only. Money remains integer VND, displayed with Vietnamese formatting.

## Launch locally

Start the existing backend using its documented environment and accounts. Do not recreate production data or change its infrastructure for a frontend preview. From the repository root:

```powershell
Set-Location -LiteralPath 'src\frontend'
npm.cmd ci
Copy-Item -LiteralPath '.env.example' -Destination '.env.local'
npm.cmd run dev
```

Open `http://127.0.0.1:3000`. Use a valid staff account from the selected backend environment. `.env.local` is ignored; do not commit credentials. Copy the example only when creating a new local configuration; preserve an existing `.env.local`.

Configuration in `.env.example`:

```dotenv
MEDORA_API_ORIGIN=http://127.0.0.1:8080
NEXT_PUBLIC_FACILITY_ZONE=Asia/Ho_Chi_Minh
NEXT_PUBLIC_FACILITY_NAMES={}
```

The optional facility-name map contains authorized facility UUIDs and display labels. It is public configuration, never a place for secrets. Restart the frontend after changing these values. Use `npm.cmd` on Windows when the PowerShell execution policy blocks the `npm.ps1` shim; changing the machine execution policy is unnecessary.

Checks and production launch:

```powershell
npm.cmd run typecheck
npm.cmd run lint
npm.cmd test
npm.cmd run build
npm.cmd start
```

The production start command requires a successful build. A successful frontend build does not establish that the backend is reachable or that authenticated workflows have been exercised.

## Recorded verification

| Check | Actual result |
| --- | --- |
| Unit / contract suite | **34 passed, 0 failed**, executed with Node 24.12.0 using `node --experimental-strip-types --test tests/*.test.ts` |
| Transport coverage | 11 tests: login 204 handling; CSRF deduplication / caching and header; cookie credentials; JSON and non-JSON errors; no mutation retries; AbortSignal; 401 behavior; multipart content type |
| Shared contract coverage | 17 tests: permission combinations, facility paths, encoded IDs, missing path values, query values, clinic timezone / day bounds, optional UUIDs, nested prescription serialization, versions, identity confirmation and range validation |
| Clinical module coverage | 5 tests: module methods / routes checked against actual controller source, incomplete SOAP draft DTOs, valid empty profile sets, fractional prescription quantities and empty warning acknowledgement sets |
| All-workspace route coverage | 1 test verifies all 36 module read, detail, view and action routes/methods against actual backend controllers, and checks unique module IDs |
| Full-project TypeScript / lint | **Passed**: `npm.cmd run typecheck` and `npm.cmd run lint` |
| Production dependency audit | **0 advisories** with development dependencies omitted |
| Full dependency audit | **10 moderate findings**, confined to the development tooling dependency tree through `minimatch` in ESLint / typescript-eslint; ESLint was upgraded to 10.14.0. No forced dependency downgrade was applied. |
| Final production rebuild / proxy rerun | **Passed** after the final UI changes: Next.js 16.4.5 production compilation, route generation and startup; all 13 session/CSRF checks passed again through the restarted frontend. |
| Real session / proxy contracts | **13 checks passed** against the backend and again through the frontend origin: login 204, cookie flags/rotation, fresh CSRF, permissions, account/role response shapes, access denial and logout |
| Real synthetic reception / clinical workflow | **Passed once** through the frontend proxy using the inspected `demo_stage4.py`: booking idempotency, confirmation/check-in, assigned screening/consultation, SOAP signing/amendment and closure to `COMPLETED` |
| Production HTML / stylesheet smoke | Final build: root plus all **36** workspace routes returned HTTP 200; compiled CSS (29,996 bytes, including motion rules) returned 200; the backend-health proxy returned 200 `UP`. Vietnamese HTML, SVG delivery and the Next `notFound`/`noindex` response were also verified in the preceding build. |
| Running-browser visual and interaction verification | **Unavailable**: the browser-control surface reported no browsers / apps. No desktop, tablet or mobile screenshot acceptance is claimed. |

The unit suite exercises source contracts and mocked transport. Separate real HTTP checks establish session behavior across the rewrite and the synthetic Stage 4 workflow outcome. They do not click frontend forms or prove browser layout, animation, all-role workflow coverage or persistence after restarting the environment. Detailed methods, scope and retained-fixture boundaries are recorded in [frontend-verification.md](frontend-verification.md).

## Source-backed dependencies and remaining checks

1. `/api/auth/me` returns facility IDs without a facility-name / timezone directory. The frontend uses explicit configuration; confirm it against the chosen backend deployment.
2. Booking, pharmacy assignment and procedure allocation require staff UUIDs. No general receptionist / clinician staff-directory endpoint was found. The UI explains the dependency and permits a supplied, verified ID rather than inventing staff choices.
3. The current queue implementation joins `patientName` and `patientCode`; encounter detail and many other responses expose IDs. Preserve available queue identity labels when opening detail, and resolve additional patient information only through permitted real APIs.
4. Refund detail / request / approval / execution exist, but no global refunds-list endpoint was found. The UI offers detail lookup and invoice-linked history, not a fabricated complete approval inbox. Similar API limits apply to medicine-event listing.
5. Portal access, external laboratory / imaging integration, SMS / email sending and an external payment gateway are not implemented by this frontend. Internal notification and manual clinical result APIs do not establish those integrations.
6. Required running-browser checks remain: login / expiry / logout against real sessions; duplicate-review and appointment conflicts; assigned clinical workflows; confirmed prescription warnings; file upload / download; financial request states; keyboard focus / dialog cancellation; reduced motion; narrow-screen overflow; 200% zoom; console / network inspection.
7. Validate all relevant roles using synthetic records in the approved test backend. Do not use real patient data for screenshots or verification artifacts. Record the browser/version, environment, expected and actual results before describing the interface as visually verified or production-ready.

The static [Phase 3 specimen](design-system/index.html) remains an explicitly labeled demonstration; its simulated feedback is not API integration evidence.
