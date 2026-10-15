# Section 3 Quality Review — System Requirements Analysis

## Evidence and scope review

- **Requirements baseline:** `doc/report/[SYSDES_2026.1] WorkStation.md`.
- **Business baseline:** Section 1 and Section 2 of `doc/report/paper.tex`, including BR-001--BR-010 and the conceptual domain model.
- **UML baseline:** editable PlantUML sources and `model-inventory.md` under `doc/asset/03-system-requirements`.
- **Implementation evidence:** none. `src/` contains only a placeholder README; no source code, database schema, API specification, test suite, test result or deployed environment was available.

## Diagram release checks

| Artifact group | Source count | Semantic parse | Render / visual review | Result |
| --- | ---: | --- | --- | --- |
| Functional decomposition | 1 | PlantUML parse passed. | Re-rendered at 2x PNG. Initial dependency arrows created crossings; they were removed because the artefact answers grouping, not architecture dependency. | Pass |
| Overall use-case model | 1 | PlantUML parse passed. | Rendered 2x PNG; boundary, Phase-2 portal note and grouped actors inspected. | Pass |
| Focused use-case diagrams | 9 | PlantUML parse passed. | Rendered 2x PNG; actor boundaries, `<<include>>`/`<<extend>>` directions, labels and split views inspected. | Pass |

## Consistency checks

- [x] Canonical actor names match the Section 2 role-to-activity map and `model-inventory.md`.
- [x] UC-001, UC-010--UC-079 retain stable identifiers and have FR sources.
- [x] Critical specifications cover trigger, preconditions, main success flow, alternatives, exceptions, authorisation, data changes, postconditions and business rules.
- [x] Specifications preserve the Appointment/Encounter, Prescription/MedicationAdministration, ProcedureOrder/Execution and Invoice/Payment distinctions established in Section 2.
- [x] The functional catalogue covers FR-001--FR-034 and the quality catalogue covers NFR-01--NFR-16 without asserting implementation.
- [x] RBAC applies least privilege and separates administrative, clinical and financial authority.
- [x] Traceability connects business rules, requirements, use cases, domain/design focus and planned verification.
- [x] Report figure paths resolve; `chktex` found no structural error in the Section 3 include after minor wording correction.

## Unresolved decisions and missing evidence

1. Facility policies are needed for retention, consent, clinical-warning source/rules, record-merge approval, refund threshold, portal release and multi-branch scope.
2. FR-019, FR-024, FR-025, FR-033 and FR-034 are phased/configuration-dependent; they must not be presented as current external integrations.
3. NFR-01--NFR-03 and NFR-11 are proposed targets, not measured outcomes.
4. There is no implementation or test evidence with which to mark a requirement, API, database constraint or acceptance criterion as passed.
5. A full LaTeX PDF build remains environment-blocked: the local XeLaTeX process stalled during MiKTeX package initialisation. Individual diagram renders, TeX reference paths and static checks succeeded.

Section 3 is complete as a traceable, report-ready requirements specification. It deliberately does not claim implementation, testing, deployment or legal compliance.
