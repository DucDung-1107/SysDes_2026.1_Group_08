# MEDORA frontend design system — Phase 3

Date: 2026-11-06, Asia/Saigon. Status: **specified; browser validation pending**. Based on the [Phase 2.5 research](frontend-references.md). This phase adds design documentation and an independent static specimen. Application workflows and API integration are not implemented here.

Implementation update (2026-11-07): the application now lives in `src/frontend`; see [implementation](frontend-implementation.md) and [verification](frontend-verification.md). Statements below describing absent frontend source record the pre-implementation Phase 3 baseline.

## Audience, personality and direction

MEDORA is a Vietnamese staff workstation for an outpatient clinic. Reception needs fast patient matching and intake; clinicians need assigned records and safe drafting/signing; service/pharmacy staff need focused worklists; finance needs precise transaction state; administrators need scoped controls. Design for sustained desktop/tablet use with usable narrow-screen fallbacks.

The personality is calm, precise and approachable. Extend the navy/blue healthcare identity already specified for diagrams in root `AGENT.md` into the application. This extension is a frontend design decision, not an existing app convention. Use white work surfaces, a lightly tinted canvas, compact labeled navigation, strong section headings and consistent tables. Reserve emphasis for the current task and exceptional states.

Research-to-design mapping:

| Reference | MEDORA decision |
| --- | --- |
| Nord clinical workstation examples | Original sidebar/worklist/detail layout, subtle surface hierarchy, legible data, explicit empty states; no Nord package or artwork reuse |
| Cliniko booking screenshots | Persistent facility/date context, patient search within booking, clearly grouped fields, separate clinical/financial statuses |
| Motion + Motion Primitives source | Brief selection/disclosure/confirmed-feedback motion, with static reduced-motion states |
| Lucide | One outline icon family with visible Vietnamese labels |

## Architecture and integration basis

Actual source: `src/backend/pom.xml`, `src/compose.yaml`, `src/backend/src/main/java/vn/medora/api/`, `src/docs/API-STAGE56.md`. Java 21 / Spring Boot 4.1.2 / Maven, PostgreSQL 17, JPA/Hibernate plus JDBC `Store`, Flyway V1–V7, one modular backend. Modules include identity, patients, scheduling, clinical care, prescriptions/services, private attachments, finance, governance and health.

No frontend or installed `node_modules/next/dist/docs/` exists in this checkout. Next.js + TypeScript is the documented intended frontend. Before writing Next application code in Phase 4, install/pin an approved current version and read its locally shipped relevant guides, including any generated `AGENTS.md`; do not substitute remembered Next conventions. The static specimen uses HTML/CSS/JavaScript and does not choose a Next API.

Integration rules derived from current controllers/security:

- Use Spring sessions, not invented JWTs: `GET /api/auth/csrf` → form-urlencoded `POST /api/auth/login` with CSRF → 204 → fresh CSRF → `/api/auth/me` and `/api/auth/permissions?facilityId=…`. Logout is a CSRF-protected POST. Session idle timeout is 30 minutes; `JSESSIONID` is HttpOnly / SameSite=Lax. Never store credentials or bearer tokens in localStorage.
- `/me` returns ID, username, display name and facility IDs; permission codes come from the separate permissions endpoint. Render navigation/actions from permissions and assignment context, with backend authorization authoritative. System administration does not imply clinical access.
- Business routes are facility scoped under `/api/facilities/{facility}`; administration uses `/api/admin/facilities/{facility}`. Propose a same-origin Next proxy/rewrites preserving cookie and CSRF semantics because no CORS/proxy exists yet. This is an implementation proposal requiring validation, not current infrastructure.
- Patients/accounts return `{items,page,size,totalElements}`; many schedules/worklists return arrays. Only show pagination totals that the contract supplies. Date/shift ranges are limited to 31 days; availability requests to one day, 5-minute steps and 100 returned slots.
- Localize `{error: "CODE"}` responses. Handle 400 validation, 401 expiry, 403 access, 404 missing, 409 conflict and 413 upload size. Keep field input on failure. Reconcile `version` conflicts rather than silently overwriting; retain a retry's original body/idempotency key, and use a new key for edited intent.
- Signing, closure, check-in and financial success appear only after server confirmation. A timeout is an uncertain outcome requiring reconciliation, not proof of failure or success. Signed notes are immutable; amendments create successor drafts with reasons. Clinical closure and payment are independent.
- Private attachments are authorized streams, not public URLs: maximum 10 MiB, PDF/PNG/JPEG/UTF-8 text, no SVG/HTML. Money is integer VND. Portal, SMS/email, external payment gateway and external laboratory integration are not implemented.

Current integration gaps to retain in planning:

1. `/me` supplies facility UUIDs, without a name/timezone discovery endpoint. Use explicit approved facility configuration; do not infer clinic timezone from the browser. Backend default is `Asia/Ho_Chi_Minh`.
2. Booking/assignment needs staff UUIDs, but no frontend staff directory endpoint was found. Admin accounts are not a receptionist lookup. An approved directory contract/configuration is a dependency for readable selectors.
3. Current queue and appointment list SQL joins `patientName` and `patientCode`; preserve those authorized labels when opening detail. Encounter detail and other responses often expose IDs without joined names. Technician, pharmacist, cashier and finance roles lack `patient.admin.read` by default, so administrative patient lookup cannot be assumed as a label-hydration path in those workspaces. Use only authorized returned fields until an approved contract resolves the remaining gap.
4. No general refunds-list API was found. Detail-by-ID/request/approval/execution does not establish a complete refund inbox.
5. Reception lacks `report.read`; its home must use queue/appointment data. Actual dashboard fields are `encounters`, `completed`, `waiting`, `awaitingService`, `from`, `to`, `scope`; do not fabricate revenue trends, wait times or percentage changes.

Documentation discrepancies: lower parts of `src/README.md` and Stage 4 docs are explicitly historical. `paper.tex` still proposes NestJS/Prisma and says application/API code is absent; current source is Spring Boot with stages 1–6. This design follows source/current API documentation and does not rewrite the report. Previous backend test evidence was not rerun in this phase.

## Workspaces and proposed pages

Page names below are a frontend proposal traced to existing backend capabilities, not proof of implemented screens. Final visibility is permission based.

| Workspace | Pages / main content | Important boundary |
| --- | --- | --- |
| Session | Login, facility choice, expired session, forbidden | Generic login error; no patient data before authorization |
| Reception | Hôm nay, Lịch hẹn, Hàng đợi, Bệnh nhân, duplicate review, check-in | Explicit identity verification; no fabricated report widgets |
| Nurse | Assigned queue, patient context, screening/vitals/history | Assignment and scope; units/author/time/verification visible |
| Doctor | Assigned encounters, timeline, SOAP, diagnosis, prescriptions, orders/allocation, result review, closure | Allocation requires the assigned doctor; draft/signed/amended distinct; safety warnings and checklist remain static |
| Technician | Assigned services, allocation details, execution/results, permitted files | Execution permission does not grant allocation authority; only authorized tasks and records |
| Pharmacist | Assigned prescription queue, prescription detail, dispensing/administering | Separate recorded events, not automatic prescription completion |
| Cashier | Invoices, invoice detail, payment record, receipt | CASH/BANK_TRANSFER; no assumed gateway or refund authority |
| Finance | Discounts, known refund detail/request/approval/execution, authorized financial reports | Other-person approval; pending/approved is not completed; refund-list gap retained |
| Manager | Scheduling/catalog configuration, prices/policies, reports, authorized refund/discharge approvals | No default discount, refund request/execution or account-management authority |
| Registrar | Authorized patient merge review/detail | Preserve source/history; explicit verification/reason |
| Auditor | Read-only audit and permitted reports/invoices | No editing or implied clinical access |
| Administrator | Accounts/permissions, scheduling/catalog configuration, permitted audit | No default price/policy management; admin does not imply doctor access |
| Authorized staff | Internal notification inbox | No implied SMS/email delivery |

## Color tokens

Canonical values live in [design-system/tokens.css](design-system/tokens.css). All values are MEDORA choices. Preserve existing brand colors while adding darker semantic text colors where needed.

| Token | Value | Purpose |
| --- | --- | --- |
| `--color-brand-navy` | `#163A5F` | Brand, primary headings |
| `--color-action` / `--color-action-hover` | `#2563A6` / `#1D4F85` | Primary action, selected links / hover |
| `--color-canvas` / `--color-surface` | `#F5F9FE` / `#FFFFFF` | Application canvas / work surface |
| `--color-selected` | `#EAF3FB` | Selected navigation and rows |
| `--color-text` / `--color-muted` | `#183247` / `#667085` | Primary / secondary text on white |
| `--color-teal` | `#2A9D9F` | Brand accent only; no small white text on this color |
| `--color-success` / `--color-success-bg` | `#146B5D` / `#EAF6F2` | Confirmed positive state |
| `--color-warning` / `--color-warning-bg` | `#8A4B08` / `#FFF4DF` | Attention required |
| `--color-danger` / `--color-danger-bg` | `#C43D4B` / `#FFF0F1` | Error and destructive action |
| `--color-border` / `--color-control-border` | `#D7E1EB` / `#7B8B9B` | Decorative separation / essential control boundaries |
| `--color-focus` | `#2563A6` | 2px focus outline, 2px offset |

Text-pair contrast is checked numerically in [validation.json](design-system/validation.json). Require 4.5:1 for normal text and 3:1 for essential non-text boundaries/focus. Muted gray is for white surfaces; check each additional tint before using it there. Fine decorative borders are not the only indication of an input. Always pair color with text/shape: “Chờ khám”, “Đã ký”, “Chưa thanh toán”. Success is not a clinical assessment. Do not use green to suggest a clinical value is safe without an approved rule.

## Typography, spacing and geometry

- Typeface: system UI stack (`"Segoe UI", system-ui, -apple-system, BlinkMacSystemFont, Arial, sans-serif`); no font download or license dependency. Consider a licensed self-hosted Vietnamese-capable font only after actual glyph/render checks. Never copy Nord webfonts.
- Text scale: 12px auxiliary labels only; 14px table/metadata; 16px default/forms; 20px section title; 28px page title. Use relative `rem` values. Weights 400/500/600/700. Body line-height 1.5, headings 1.25, Vietnamese multiline fields 1.6. Never clip diacritics with fixed line boxes.
- Patient names remain full, case preserved, wrapping where needed. Use stable patient code and date of birth alongside names. Do not assume an avatar is an identifier.
- Tabular numerals for dates/times/amounts. Format VND with `Intl.NumberFormat('vi-VN', {style:'currency', currency:'VND', maximumFractionDigits:0})`. Display instants in approved facility timezone with `Intl.DateTimeFormat`; keep birth dates as date-only values without UTC conversion. Use `dd/MM/yyyy`, 24-hour time, and a visible timezone label where scheduling needs it.
- Spacing: 4/8/12/16/24/32/48px. Field label gap 8px; related controls 12px; panel padding 24px desktop / 16px narrow; major sections 32px. Prefer one table surface over one card per row.
- Radius: 6px controls, 10px panels, full pill only for statuses. Borders 1px. No persistent card shadows; use one restrained overlay shadow. Minimum control/touch target 44px; dense table rows minimum 56px with wrapping allowed.

## Layout and responsive behavior

Breakpoints are design choices: below 640px narrow; 640–1023px tablet; 1024px+ desktop; 1280px+ wide detail split.

- Desktop: 224px labeled sidebar, 64px contextual header, flexible main area with 24–32px padding. Main grids use `minmax(0,1fr)` to prevent intrinsic overflow. Limit narrative/form reading width to about 72ch; let tables use available width.
- Wide encounter view: patient context above; main record area plus 320–360px secondary panel when space permits. Keep sign/close actions beside the relevant checklist, not floating over content.
- Tablet: two-column forms where labels fit; navigation collapses to a labeled menu; detail panels stack. Staff can reach every action without hover.
- Narrow: single-column forms, persistent patient context without covering content, wrapping action groups. Date agenda becomes a list. Tables may use an explicitly labeled local horizontal scroll region, with a minimum table width; the entire page must not scroll horizontally. Never remove identity/status/action columns merely to fit.
- At 200% zoom, controls and headings reflow. At 320 CSS px, only intrinsically two-dimensional regions such as tables may scroll horizontally. Avoid fixed-height text containers.

## Reusable component conventions

| Component | Required behavior and states |
| --- | --- |
| AppShell / navigation | Semantic header/nav/main; skip link, current-page marker, facility context, permission-filtered links. Mobile modal navigation needs focus containment, Escape and focus return. |
| PatientContext | Available authorized name/code/birth date and encounter context; do not fabricate inaccessible identity fields. Authorized allergy/identity warnings stay visible; never auto-hide. |
| Button | Primary/secondary/quiet/danger. Keep width stable during pending; prevent duplicate submission; expose busy text. Success follows confirmed response; error remains actionable. |
| Field / error summary | Visible label, required indicator explained, units, helper/error linked via `aria-describedby`; `aria-invalid` only on invalid controls. On submit failure focus a summary linking to fields. |
| Patient search | Deliberate selection; keyboard support; no automatic first-match confirmation. Loading, no result, duplicate candidate, forbidden and failure are distinct. Debounce and cancel obsolete requests. |
| WorklistTable | Native table/caption/header scopes, sort buttons if supported, full row labels, explicit detail links. Keep ordering stable while staff act. No hover-only controls or whole-row ambiguous click target. |
| StatusBadge | Text plus optional icon; independent encounter, appointment, note and finance states. Never infer one lifecycle from another. |
| Record/disclosure | Important warnings outside collapsed content. Button with `aria-expanded`/`aria-controls`, or native `details/summary` for secondary content. |
| Confirmation dialog | Specific target and consequence, explicit cancel, correct initial focus, trap focus, Escape/cancel, focus return; require reasons/verification where actual API does. |
| Async state | Stable skeleton or textual loading; clear empty/no-match/no-access/error variants. Retry preserves intent and input. No endless decorative shimmer. |
| Feedback | Polite status for routine completion, assertive alert for critical errors; do not move focus to toasts. Important results also remain inline. |

Vietnamese copy uses sentence case and specific verbs: “Tạo lịch hẹn”, “Xác minh và tiếp nhận”, “Lưu nháp”, “Ký ghi chú”. Avoid ambiguous “OK” for clinical or financial commits. Do not display raw UUIDs as finished labels while concealing a missing directory integration.

## Iconography and motion

Use Lucide after adding the package and required notices in Phase 4. Default 20px / stroke 2 / `currentColor`, consistent alignment, visible text labels. Decorative icons are hidden from assistive technology; icon-only buttons receive accessible names. The specimen uses original generic SVG geometry rather than shipping copied icons.

| Effect | Duration / implementation | Boundary |
| --- | --- | --- |
| Hover/focus/pressed feedback | 120ms CSS color/background; focus outline immediate | No whole-control scale/bounce |
| Selected filter/tab | 160ms CSS color or optional shared indicator | Keyboard semantics independent of animation; content immediate |
| Disclosure chevron | 160ms rotation | Important content never delayed; native content can open instantly |
| Optional detail panel | 180ms opacity + at most 8px translation | No page-wide route animations or moving identity headers |
| Confirmed-save check | 220ms original SVG stroke dash animation | Once, after actual response; static final icon for reduced motion |

Use `cubic-bezier(.2,0,0,1)`. CSS first; `motion/react` only if real shared-layout/presence requirements justify it. Do not add GSAP, Rive and Lottie alongside it without a task-specific need. Respect `prefers-reduced-motion: reduce` at first render and on changes: remove translations, rotations, drawing and loops; show immediate selected/final states. Do not delay API work for a minimum spinner duration. No video backgrounds, particle effects, scroll hijacking, gradient decoration or animated clinical numbers.

## Accessibility and acceptance checks

Target WCAG 2.2 AA as a design/verification goal, not a current compliance claim. Use Vietnamese document language, semantic landmarks, visible keyboard focus, 44px controls, readable contrast and meaningful page titles. Dialogs, tabs and comboboxes require complete keyboard behavior, not just ARIA attributes. Do not preserve private patient drafts in unprotected browser storage. Session expiry must protect data while explaining how the user can continue safely.

The [local specimen](design-system/index.html) and [tokens](design-system/tokens.css) are review aids. Simulated feedback is explicitly labeled and sends no backend request. Open `docs/design-system/index.html` locally, or from repository root run `python -m http.server 4173 --bind 127.0.0.1 --directory docs/design-system` and visit `http://127.0.0.1:4173`. No dependencies or build are required.

Verification record:

| Check | Status / evidence |
| --- | --- |
| Repository contracts/roles/source | Read-only audit of actual controllers/security/DTOs; no backend changes |
| Published reference images | Visually inspected Cliniko calendar/form and Nord table/surface/navigation examples; not redistributed |
| Third-party terms | Checked source links in references; no external assets/packages added |
| Specimen script syntax, local links, token contrast | Machine check results in [validation.json](design-system/validation.json) |
| Running-browser interaction, animation and screenshots | **Pending**: browser surface unavailable, not claimed as passed |
| Backend integration/build/lint/TypeScript | Not applicable to this static phase; no Next application exists yet |

Pending running-browser checks before effects are accepted:

1. At 1440×900, 768×1024 and 390×844, inspect spacing, Vietnamese accents, wrapped controls and local table overflow. Also test 320 CSS px and 200% zoom.
2. Use only keyboard: skip link, navigation, controls, disclosure and feedback. Confirm focus order/visibility, and that filtering never moves focus unexpectedly.
3. Exercise both successful and failed feedback simulations. Confirm busy state, no duplicate activation, error text and stable layout. In Phase 4 repeat against actual APIs and timeout/conflict cases; simulation is insufficient integration evidence.
4. Toggle OS reduced motion and the specimen's override. Confirm selected states/checks remain visible with no animated translation/path drawing; verify actual playback under normal preference too.
5. Inspect console/network, ensure no external requests/assets, and capture screenshots. Add browser/version/date and observed results here; fix defects before claiming visual acceptance.

## Phase 4 handoff

1. Establish Next.js/TypeScript using locally shipped version-specific documentation. Keep these tokens as one source of truth; use small client components for interactions. The available React best-practices skill should be loaded when actual React work begins.
2. Implement same-origin session/CSRF API client and permission/facility bootstrap; verify real 204, expiry, forbidden and stale-version behavior. Resolve staff/facility display contracts before promising booking selectors.
3. Build shell, patient lookup and today's reception worklist; then assigned clinical workflows; then service/pharmacy/finance/admin pages in permission-based slices. Reuse component states throughout.
4. Validate the chosen motion in an actual browser before extending it. Run relevant frontend build/lint/type checks and real workflow tests as application code is added.

Skill scope for this phase: Sites local-only preview guidance was read/applied for the specimen; Computer Use guidance was read during the browser availability attempt. React best practices was inspected for the intended stack, but no React implementation was made. This request focuses on Phase 2.5/3; no third-party skill installation scripts were run and no newly installed skills are claimed active.
