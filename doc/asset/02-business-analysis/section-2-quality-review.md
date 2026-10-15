# Section 2 Quality Review — Business Analysis and Domain Modeling

## Evidence basis and scope

- **Primary source:** `doc/report/[SYSDES_2026.1] WorkStation.md` (product requirements, workflows, conceptual entities, status rules, FR/NFR catalogue).
- **Supporting source:** the Section 3 use-case inventory and editable PlantUML sources.
- **Implementation evidence:** none is present. `src/README.md` requests that source code be placed in `src/`; no application source, physical schema, test result or deployment evidence was available at review time.
- **Scope decision:** Medora is modelled as a clinic-focused internal web system. The patient portal is Phase 2; external payment, insurance, LIS/RIS/PACS, national exchange and advanced inventory are not represented as confirmed MVP integrations.

## Review findings and corrections

| Severity | Location | Finding | Correction |
| --- | --- | --- | --- |
| Major | Initial conceptual domain diagram | Long cross-package connectors and an overloaded financial/access view made cardinalities difficult to follow. | Replaced it with a focused clinical core model; financial, governance and access concepts remain in report tables with explicit relationships. |
| Major | Initial appointment workflow | The rescheduling branch did not visibly return to the availability-validation activity, which could imply an invalid transition. | Removed the ambiguous loop from the activity diagram. The rule and alternative remain explicitly described in the process and rule tables. |
| Minor | Business context | No site-specific current workflow was evidenced. | Used an evidence-aware As-Is/To-Be table and labelled the model as a target operating model; no claim about paper or spreadsheet usage is made. |
| Minor | Organisation structure | No verified reporting hierarchy was supplied. | Omitted an organisational chart rather than inventing hierarchy; recorded coordination and authority boundaries in the stakeholder table. |

## Diagram release checks

| Diagram | Semantic review | Visual review | Result |
| --- | --- | --- | --- |
| `outpatient-patient-journey.puml` | Start/end, decisions, role lanes and optional service branch agree with FR-003 to FR-023. | Rendered at 2x PNG; labels, branches and lane transitions inspected. | Pass |
| `appointment-and-check-in-exceptions.puml` | Rejects unavailable slots without an appointment/resource booking; separates walk-in acceptance and authorised merge review. | Rendered at 2x PNG; corrected the ambiguous rescheduling route and reinspected. | Pass |
| `conceptual-domain-model.puml` | Distinguishes Patient, Appointment, Encounter, clinical records, prescription items and service orders; multiplicities are documented in the report. | Rendered at 2x PNG; corrected overloaded/crossing layout and reinspected. | Pass |

## Quality checklist

- [x] Clinic context, target boundary, evidence limitations and assumptions are explicit.
- [x] Current and target operating states are distinguished without inventing a legacy workflow.
- [x] Roles, authority boundaries, stakeholder/direct-actor distinction and stable naming align with Section 3.
- [x] Major workflows, decisions, alternatives and exceptions have accountable outcomes.
- [x] Business rules BR-001 to BR-010 are traceable to FR/NFR and use-case identifiers.
- [x] Core domain concepts, cardinalities and non-conflation constraints are documented.
- [x] Business needs map to derived system capabilities without duplicating the Section 3 catalogue.
- [x] All Section 2 diagrams parse, render and have been visually inspected after correction.
- [x] No unsupported implementation, compliance, performance or integration claim is made.

## Remaining decisions requiring project-owner confirmation

1. Facility-specific retention, consent, correction and audit-viewing policies.
2. Formal reporting hierarchy and approval thresholds for record merge, refund and override actions.
3. Exact clinical templates, warning sources/rules and whether each service requires consent.
4. Multi-branch policy, patient-portal release criteria and any external integration contract.
5. Physical schema, implementation status, test evidence and production deployment controls.

The Section 2 analysis is complete as a traceable target business model. These decisions remain intentionally open rather than being represented as confirmed implementation facts.
