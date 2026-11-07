# MEDORA frontend references — design research

Research date: 2026-11-06, Asia/Saigon. Scope: the existing internal outpatient clinic/EMR product, followed by the [design system design system](DESIGN.md). This is research and a design proposal, not an implemented clinical frontend.

## Product and evidence basis

Read the root `AGENT.md`, `README.md`, `src/README.md`, the API documents now preserved under `docs/archive/2026-11-07/backend/`, and backend controllers/security/DTOs. MEDORA serves receptionists, assigned nurses/doctors, technicians, pharmacists, cashiers, finance, managers, registrars, auditors and administrators. Their daily work connects patient identification, appointments/check-in, clinical records, orders, closure and finance. Vietnamese, desktop and tablet use take priority.

The actual application is Java 21 / Spring Boot 4.1.2 / PostgreSQL 17, with Flyway V1–V7 and Docker Compose. No frontend, package manifest, installed Next.js or frontend component conventions exist. Next.js + TypeScript is the repository's intended stack. Preserve that direction; the designs below describe React/CSS implementations rather than importing a competing framework. The architecture and API constraints are recorded in [DESIGN.md](DESIGN.md#architecture-and-integration-basis).

Research methods and limits:

- Web tools retrieved public pages, documentation and source/license text. Public reference images were downloaded to a temporary research folder and visually inspected; none are bundled with this project.
- Visually inspected: Cliniko's calendar and appointment form; Nord's table text, surface hierarchy and navigation color examples. These are published screenshots, not sessions inside either product.
- Motion demos were examined through documentation/source. No animation was visually played. Motion Primitives' hosted pages returned 403; its official GitHub source was accessible.
- The browser tool returned **“No browser is available”**; a second team check reported no browser surfaces. Local animation playback, keyboard behavior and responsive screenshots remain **not browser-verified**. Do not infer those results from source checks.
- Observations below are distinguished from MEDORA proposals. Measurements such as durations, spacing and breakpoints are our choices, not measurements taken from reference sites.

## Four selected references

### 1. Nord Design System — clinical workstation structure

URLs: [overview](https://nordhealth.design/), [layout](https://nordhealth.design/components/layout/), [table](https://nordhealth.design/components/table/), [typography](https://nordhealth.design/design/foundations/typography/), [color](https://nordhealth.design/design/foundations/colors/), [empty states](https://nordhealth.design/components/empty-state/). Discovered through [Lapa's Nordhealth listing](https://www.lapa.ninja/post/nordhealth/), a 2021 marketing reference rather than the current application.

**Observed:** The published [surface image](https://nordhealth.design/img/colors/1.png) separates pale background, white content and lightly tinted table headers using fine borders. The [navigation image](https://nordhealth.design/img/colors/4.png) uses labeled outline icons and persistent selection. The [table text image](https://nordhealth.design/img/table-text.png) distinguishes muted headers from darker row content without heavy decoration. Documentation describes collapsible navigation, a secondary drawer, semantic native tables and recoverable empty states. Its product type is Inter; its documentation specifies a 14px base and a compact hierarchy.

**Why it works / MEDORA adaptation:** Surface hierarchy and aligned data make a workstation calm and scannable. Use MEDORA's own navy/blue palette, a labeled sidebar, one main worklist and optional details panel. Keep patient identity and important warnings immediately visible. Use 16px body text and 14px table text in MEDORA; do not inherit the reference's smallest labels. Use borders and space to group content, with cards reserved for independent sections. Empty states should distinguish no patients, no filter matches and no permission.

**Implementation:** Independently authored React `AppShell`, `WorklistTable`, `PatientContext` and `EmptyState`, with CSS grid, semantic HTML and CSS variables. Avoid a primary button on every row. A sidebar/drawer needs keyboard and focus behavior in addition to visual styling. Complexity: **medium** (shell/responsiveness/focus), **low** (tokens/table presentation).

**Usage restrictions:** [Published terms, available in the search index](https://nordhealth.design/resources/terms/) and [developer guidance](https://nordhealth.design/docs/developer/) restrict Nord components/content to Nordhealth work. The terms page itself returned 404 on direct retrieval; the indexed terms and developer page were readable. Do **not** install Nord packages, copy its component source, fonts, icons, logo or screenshots into MEDORA. Retain only general interface lessons and write original implementations.

### 2. Cliniko — scheduling context and structured booking

URLs: [scheduling features](https://www.cliniko.com/features/appointments/), [booking guide with screenshots](https://help.cliniko.com/en/articles/1024061-booking-an-appointment), [appointment symbol guide](https://help.cliniko.com/en/articles/1023901-a-guide-to-the-symbols-on-appointments).

**Observed:** The booking guide's calendar screenshot has a stable left navigation, location/time context above a day-column/time-row grid, a compact month navigator, and appointment blocks containing names and times. Its appointment form aligns labels and fields, emphasizes patient search, and separates the commit and cancel controls. The screenshots use teal navigation and several vivid appointment colors; the calendar uses compact sans-serif text, while the form title is serif. The symbol guide documents separate arrival, invoice and note states and limits financial symbols by access.

**Why it works / MEDORA adaptation:** Keep date, facility and appointment context visible while booking. Search/select a patient before submitting, and show their code and birth date to help distinguish similar names. Use aligned field groups and one clearly labeled primary action. Adopt the workflow clarity; use MEDORA's consistent sans-serif type and quieter semantic badges. Keep clinical completion and payment as separate states. Visible labels should accompany status icons.

**Implementation:** A day agenda/worklist first, with a date navigator and an availability form; later a day grid only if it adds value. Use MEDORA's actual appointment/availability/check-in APIs. Do not inherit Cliniko's recurring booking, telehealth, online patient booking or SMS features: those are not current MEDORA capabilities. Prefer an explicit reschedule form over drag-only interactions. Complexity: **medium** for daily agenda and booking; **high** for an accessible interactive calendar grid, which is deferred.

**Usage restrictions:** Cliniko is a proprietary reference. No reusable code/asset license was established. Do not redistribute screenshots, brand assets, interface artwork or copy. Reimplement general scheduling conventions using MEDORA's own content and components. No account or trial was created.

### 3. Motion + Motion Primitives — explain state changes quietly

URLs: [layout animation](https://motion.dev/docs/react-layout-animations), [accessibility](https://motion.dev/docs/react-accessibility), [SVG animation](https://motion.dev/docs/react-svg-animation), [accordion source](https://raw.githubusercontent.com/ibelick/motion-primitives/main/components/core/accordion.tsx), [animated background source](https://raw.githubusercontent.com/ibelick/motion-primitives/main/components/core/animated-background.tsx).

**Inspected:** Motion's examples show shared selection through `layoutId`, SVG path drawing and reduced-motion configuration. Motion Primitives' source supplies disclosure/selection animation patterns. Its [hosted accordion](https://motion-primitives.com/docs/accordion) was inaccessible (403); the [shared-layout demo](https://examples.motion.dev/react/shared-layout-animation?utm_source=embed) exposed only a text shell. These are source-supported findings, not observed animation playback.

**Design proposal:** A persistent selected tab, short disclosure-chevron rotation and one-shot confirmed-save check make state changes understandable. Use MEDORA's typography, colors and spacing; this reference supplies behavior, not a palette. Never stagger patient worklist rows, bounce clinical warnings, count up clinical values or animate queue reordering. Important content must be readable immediately.

**Implementation:** Start with CSS color/opacity/transform transitions: 120ms feedback, 160ms selection, 180ms optional panel entry; original SVG path drawing at 220ms. Use static final states under `prefers-reduced-motion`. If shared-layout transitions later justify Motion, isolate `motion/react` in small Next client components and use `MotionConfig reducedMotion="user"` plus explicit hook handling where needed. Motion does not provide tab keyboard semantics or dialog focus management. Complexity: **low** for CSS/disclosure/check; **medium** for interruptible layout/presence animation. The [local specimen](archive/2026-11-07/design-system/index.html) exercises the CSS subset.

**Licenses:** [Motion core](https://raw.githubusercontent.com/motiondivision/motion/main/LICENSE.md) and [Motion Primitives](https://raw.githubusercontent.com/ibelick/motion-primitives/main/LICENCE.md) are MIT; preserve applicable notices if code is copied. Motion+ paid examples are separate. The specimen is independently authored and uses neither library.

### 4. Lucide — one coherent icon vocabulary

URLs: [React guide](https://lucide.dev/guide/react), [stethoscope](https://lucide.dev/icons/stethoscope), [accessibility](https://lucide.dev/guide/react/advanced/accessibility), [license](https://lucide.dev/license).

**Inspected:** React documentation describes inline SVG, typed props, named imports and tree shaking. The icon detail page and access guidance were readable; the raw SVG fetch was unsupported by the web tool. No animated icon demo was played.

**Design proposal:** Consistent outline geometry, a 20px default size, 2px stroke and `currentColor` support a quiet clinical interface. Pair icons with readable Vietnamese labels; patient identity and status must remain understandable without an icon. Use 16px icons only in dense metadata, 24px sparingly. Suggested mappings: `Users` patients, `CalendarDays` appointments, `ListOrdered` queue, `Stethoscope` encounter, `Pill` prescriptions, `Receipt` invoices, `ShieldCheck` administration. Do not combine several unrelated icon packs.

**Implementation:** Named `lucide-react` imports in the future frontend; decorative SVGs hidden from assistive technology, icon-only controls labeled on the button. A wrapper or original SVG check path can animate once after a confirmed API success. Complexity: **low**.

**License:** Lucide uses ISC, with Feather-derived icons carrying MIT notices too. Retain all applicable copyright/permission notices in third-party notices when assets/packages are added. No Lucide package or downloaded icon is included in this phase; the specimen's simple SVG paths are original.

## Exploration log for every requested resource

| Resource | Access / inspection | Decision and usage conditions |
| --- | --- | --- |
| [Awwwards](https://www.awwwards.com/) | Homepage/navigation retrieved; health-category request timed out and guessed Nordhealth detail unavailable | Editorial/marketing discovery only. No award claims or visual findings about inaccessible entries; no asset permission inferred. |
| [Lapa Ninja](https://www.lapa.ninja/) | Homepage, health listings and Nordhealth entry retrieved | Led to clinical-domain research. Its screenshots remain owned by their respective owners. |
| [Siteinspire](https://www.siteinspire.com/) | Homepage, medicine category and [One Medical entry](https://www.siteinspire.com/website/8514-one-medical) retrieved | The One Medical entry dates to 2018 and supplies metadata, not a verified current staff UI. No selected design claim based on that entry. |
| [React Bits](https://reactbits.dev/) | [Animated List](https://reactbits.dev/components/animated-list), [TypeScript source](https://raw.githubusercontent.com/DavidHDev/react-bits/main/src/ts-default/Components/AnimatedList/AnimatedList.tsx) and [license](https://raw.githubusercontent.com/DavidHDev/react-bits/main/LICENSE.md) inspected | Leading label/trailing metadata is useful anatomy; independently implement it. Avoid row reveals, hover selection and fades in clinical queues. Current license is MIT **plus Commons Clause**, restricting standalone component/bundle redistribution. |
| [Motion Primitives](https://motion-primitives.com/) | Hosted pages 403; official GitHub source/license readable | Selected with Motion, with the hosted-demo limitation stated above. |
| [Aceternity](https://ui.aceternity.com/) | [Tabs](https://ui.aceternity.com/components/tabs), [stateful button](https://ui.aceternity.com/components/stateful-button), [licence](https://ui.aceternity.com/licence) and terms inspected | Clear pending/success feedback is useful; layered floating panels are unsuitable. End-product and redistribution restrictions apply; do not assume all snippets are MIT. Independently implement basic async button states. |
| [Motion](https://motion.dev/) | Layout/accessibility/SVG documentation and core license inspected | Selected behavior reference; CSS first, one animation library only when justified. |
| [GSAP](https://gsap.com/) | Docs, [`matchMedia`](https://gsap.com/docs/v3/GSAP/gsap.matchMedia()/) and [license](https://gsap.com/community/standard-license/) inspected | Defer advanced timelines/scroll effects. Current license is custom No Charge, not MIT; competing visual animation builder restrictions exist. |
| [Rive](https://rive.app/) | [React runtime docs](https://rive.app/docs/runtimes/react/react), [runtime MIT license](https://github.com/rive-app/rive-react/blob/main/LICENSE), [pricing](https://rive.app/pricing) inspected | Defer canvas/Wasm/state-machine asset workflow; no task requires it. Runtime rights differ from asset/editor rights; current pricing says free exports include a splash screen. |
| [LottieFiles](https://lottiefiles.com/) | [React docs](https://docs.lottiefiles.com/en/runtimes/distributions/react), [public asset license](https://lottiefiles.com/page/license), [runtime MIT license](https://github.com/LottieFiles/dotlottie-web/blob/main/LICENSE) inspected | Defer player and external animation assets. Verify exact asset/premium terms; public files use the Lottie Simple License, distinct from the player. |
| [Lucide](https://lucide.dev/) | React/icon/accessibility/license pages inspected | Selected, subject to retaining ISC and applicable MIT notices. |
| [SVG Repo](https://www.svgrepo.com/) | [Medical collection](https://www.svgrepo.com/collection/medical-duotone-line-icons/) and [licensing guide](https://www.svgrepo.com/page/licensing/) inspected | Reserve for a specific future illustration. Licenses vary by item; “free” is not a uniform license. Do not mix duotone icons into Lucide navigation. |
| [Flaticon](https://www.flaticon.com/) | Catalogue and [terms](https://www.flaticon.com/legal) inspected | Defer. Free use normally requires platform/contributor attribution; premium and sponsored/editorial assets have different conditions. No assets selected. |

## Coherent selection and validation handoff

Use MEDORA's existing blue identity, neutral surfaces, tabular worklists, readable labels and small state transitions. No video background is proposed. SVG/CSS effects are code-generated interface animation, not real video; no video was sourced or generated. No proprietary asset, external font or animation package was added.

The [design specification](DESIGN.md) turns the four references into tokens and component rules. The standalone [specimen](archive/2026-11-07/design-system/index.html) is a review aid with synthetic labels and simulated feedback, not an API-connected screen. Its manual browser checks are recorded in that specification. Browser validation is still pending because no browser surface was available; implementation must verify the selected effects in a running browser before accepting them.
