# MEDORA frontend verification

Date: **2026-11-07**, Asia/Saigon. This record distinguishes HTTP integration evidence from browser verification.

## Environment and boundaries

- Production Next.js frontend served at `http://127.0.0.1:3000`, forwarding `/api/*` to the existing MEDORA API at `http://127.0.0.1:8080`.
- Only the pre-existing `medora-stage4-test-db-1` and `medora-stage4-test-api-1` containers were started. Other MEDORA containers were left untouched. No container rebuild/recreation, Compose changes, volume deletion, backend code changes or secret changes were performed for these checks.
- Before starting the existing API image, a read-only query confirmed successful Flyway history **V1–V7** in the existing test database. Schema changes were not requested or performed.
- An interruption had stopped both test containers. The first frontend-proxy check returned HTTP 500 because port 8080 was unavailable. After restarting the same test containers, `/health` and `/ready` both returned **200 `UP`**, and the proxy checks below passed. No frontend routing change was needed to resolve that failure.
- Bootstrap credentials came from the existing `.env.docker-test` file and remained in process memory. No password, cookie, CSRF token, patient record, account value or generated record ID is included in this report or the sanitized smoke output.

## Completed checks

| Check | Observed result | What this establishes |
| --- | --- | --- |
| Full-project typecheck | Passed | TypeScript checks the frontend source without reported errors |
| Full-project lint | Passed | The configured ESLint checks completed without reported errors |
| Unit / contract suite | **34 passed, 0 failed** | 11 transport tests, 17 shared-contract tests, 5 clinical-module tests and 1 all-workspace backend-route mapping test |
| Production dependency audit | **0 advisories** with development dependencies omitted | Audit result for the runtime dependency tree at this checkpoint |
| Full dependency audit | **10 moderate findings**, all in the development tooling tree | Remaining `minimatch` findings propagate through ESLint / typescript-eslint after upgrading ESLint to 10.14.0; no forced downgrade was applied |
| Final production rebuild / proxy rerun | **Passed** after the final UI changes; Next.js 16.4.5 compiled, type-checked and generated the production routes | The final source builds and starts successfully; the 13 session/CSRF checks passed again against the restarted frontend |
| Backend process/database readiness | `/health` and `/ready`: 200 `UP` | Existing backend and database are available |
| Direct backend session/contract smoke | 13 checks passed against port 8080 | Actual Spring session, CSRF and permission contracts |
| Same-origin frontend proxy | The same 13 checks passed against port 3000 | Cookies, form login, CSRF, 204 responses and backend error codes survive the configured rewrite |
| Production HTML routes | Root plus all **36** workspace routes returned 200 with Vietnamese document language | Routes in the served production build deliver HTML; this does not prove client interaction or authorized data rendering |
| Production stylesheet | Final linked stylesheet returned 200, **29,996 bytes**, including motion and reduced-motion rules | The served HTML can retrieve its compiled CSS asset; this does not establish animation playback |
| Additional production HTTP checks | SVG icon 200, `X-Frame-Options: DENY`, backend-health proxy 200 `UP`; an unknown workspace returns the Next `notFound` digest and `noindex` metadata | Asset delivery, configured header, upstream health and missing-workspace handling are present; these are HTTP checks |
| Synthetic reception/clinical workflow through frontend proxy | Existing `demo_stage4.py` completed once; final encounter state `COMPLETED` | Real backend workflow requests work through the frontend origin |

The session/contract smoke checks were:

1. Anonymous `/api/auth/me` returns 401.
2. CSRF bootstrap returns a token contract.
3. Form-urlencoded login with CSRF returns 204.
4. The session ID rotates on login; cookie headers retain HttpOnly and SameSite=Lax.
5. A fresh CSRF token is fetched after login.
6. Authorized `/me` returns the facility-bootstrap contract.
7. Facility permissions are available.
8. The administrator roles response has the expected code/name shape.
9. The accounts response has `items`, `page`, `size`, `totalElements`.
10. An administrator without patient privileges receives 403 on patient search.
11. A deliberately invalid empty account-create request without CSRF returns 403 and creates no account.
12. Logout returns 204.
13. `/me` after logout returns 401.

The read-only smoke script is [src/frontend/scripts/smoke_api.py](../src/frontend/scripts/smoke_api.py). It accepts loopback HTTP(S) origins only and prints check names/statuses rather than credentials or record values. Login/logout and authorized reads can produce normal backend security/audit events.

The all-workspace source-contract test checks the read/detail/view/action methods and paths of all 36 modules against the backend controllers. It complements the HTTP route smoke; neither check establishes that every declared action has been submitted through a browser.

The final HTML checks were repeated after restarting the production server from the final build. The unknown workspace response uses HTTP 200 after streaming has begun, with `NEXT_HTTP_ERROR_FALLBACK;404` and `noindex, nofollow`. This matches the installed Next.js `not-found` documentation; a literal HTTP 404 was not observed for this streamed route. The backend health endpoint returns plain text `UP`, not a JSON object.

After the motion/UAT follow-up, full lint, typecheck, all 34 tests and production build passed again. The production server was restarted; root plus all 36 workspace routes, the compiled stylesheet and backend-health proxy passed HTTP checks, and all 13 session/CSRF checks passed again. The stylesheet contains the login SVG draw, dialog entrance, confirmation draw and reduced-motion rules. Browser playback, focus and interaction are still pending.

The follow-up also exposes reference IDs consistently and adds the assigned-doctor discharge-request form using the existing governance API. Its HTTP method/path is covered by the source-contract suite. Request creation, approval by another account and subsequent closure have **not** been executed through that form. Follow [frontend-uat.md](frontend-uat.md) for the manual test, including the policy-dependent approval branch.

## Synthetic workflow evidence

The existing [demo_stage4.py](../src/scripts/demo_stage4.py) was inspected before execution, then called once through `http://127.0.0.1:3000` with `.env.docker-test`. Its normal output was captured in memory and reduced to PASS/check names, without publishing generated record IDs.

The run created uniquely named synthetic receptionist, nurse and doctor accounts; a synthetic room/service/shift/capability; and one synthetic patient with its appointment and encounter. It then exercised:

- Booking and retry with the same idempotency key, asserting the original appointment is returned.
- Appointment confirmation and identity-verified check-in.
- Assigned-nurse vitals and history entry.
- Assigned-doctor consultation start, diagnosis and SOAP draft.
- Signing the original note, creating a linked amendment, updating the amendment and signing it.
- Closure checklist and encounter completion, asserting `COMPLETED`.

Synthetic fixtures were retained. The run did not delete records, change shared medication/clinic policies, alter schema or operate on real patient records. The broader `demo_full_api.py` was **not** run: it changes the facility medication policy, which was unnecessary for this frontend smoke check.

This is HTTP integration evidence. The script does not click the implemented forms, test their client validation, or establish that every one of the 36 workspaces has completed a real workflow.

## Browser verification still required

Computer Use reported no available application/browser surfaces in this session. Consequently the following checks are **pending**, not passed:

- Visual inspection/screenshots at desktop, tablet and mobile sizes; 320px reflow and 200% zoom.
- Keyboard navigation, modal focus handling, live-region announcements and assistive-technology behavior.
- Actual CSS/SVG animation playback, interruption behavior and reduced-motion behavior.
- Browser console/network inspection and client-side form workflows, including timeout/conflict recovery.
- Visual comparison of the final rendered application against the design system.

HTML/CSS HTTP checks, source review and unit tests do not substitute for those browser checks. The final source has been rebuilt; rebuild again after any subsequent UI changes and complete the pending browser checks when a browser surface is available.

## Reproduction

With the named test backend and frontend already running, run the non-creating contract smoke from `src/frontend`:

```powershell
python scripts/smoke_api.py --base http://127.0.0.1:3000
```

The synthetic workflow can be rerun from `src`, but **each execution adds new synthetic fixtures**. Use only the explicitly selected test environment:

```powershell
python scripts/demo_stage4.py --base-url http://127.0.0.1:3000 --env-file .env.docker-test
```

The command above is the existing script's supported CLI; its normal output includes synthetic record IDs. The recorded run used an in-memory wrapper to redact those values. Never print or commit the environment file.
