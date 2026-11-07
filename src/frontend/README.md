# MEDORA frontend

Next.js 16.4.5, React 19.3.1, TypeScript, Lucide and CSS. This preserves the stack recorded in `../README.md`; the older NestJS/Prisma report proposal is not the running backend.

Use Node 24 LTS (24.12.0 was used for verification). From this directory:

```powershell
npm.cmd ci
# Only when creating local configuration for the first time:
Copy-Item -LiteralPath '.env.example' -Destination '.env.local'
npm.cmd run dev
```

Open http://127.0.0.1:3000 and sign in with a staff account from your backend. No demo password, fake patient list or simulated backend response is bundled in this app. The backend default is http://127.0.0.1:8080; configure `MEDORA_API_ORIGIN` in `.env.local` when needed. Restart the server after changing environment configuration. Keep an existing `.env.local` rather than overwriting it.

`NEXT_PUBLIC_FACILITY_ZONE` is explicit deployment configuration. `NEXT_PUBLIC_FACILITY_NAMES` optionally maps authorized facility UUIDs to display names. Public variables must not contain secrets or patient data. Credentials and patient drafts are never persisted to browser storage.

```powershell
npm.cmd run typecheck
npm.cmd run lint
npm.cmd test
npm.cmd run build
npm.cmd start
```

Run `start` only after a successful build; it binds to loopback. Use a supported TLS reverse proxy for an actual deployment and preserve Spring's cookie/session/CSRF behavior. No backend schema or Compose change is required for local frontend use.

The app includes 36 workspaces covering reception, assigned clinical work, prescriptions, procedures, private files, finance, governance, reports and administration. Available pages/actions come from backend permission codes. Missing staff-directory and facility-discovery contracts are documented rather than fabricated.

Read [implementation details](../README.md), [verification evidence](../docs/TESTING.md), [design system](../docs/DESIGN.md), and [reference research](../docs/frontend-references.md). Browser visual/interaction acceptance remains separate from build, contract and HTTP checks.

For a manual test from reception through clinical closure and payment, follow the [Vietnamese role-by-role UAT checklist](../docs/USER_GUIDE.md). It includes setup, exact workspace names, expected states, optional pharmacy/procedure branches and animation checks. The checklist describes tests to perform; it does not claim those browser interactions have already passed.

The read-only local smoke script can verify session/CSRF through the frontend proxy using the existing test environment:

```powershell
python ../scripts/smoke_session.py --base http://127.0.0.1:3000
```

It reads the test credential into memory and prints check names only. It refuses non-loopback targets and does not create business data. Follow the test environment setup in the backend documentation; never repurpose production credentials for this check.
