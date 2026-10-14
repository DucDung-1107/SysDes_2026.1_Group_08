# MEDORA Use-Case Diagram Review Record

**Review scope:** overall model and all detailed use-case sources under `use-case-by-function`.

**Source of truth:** `[SYSDES_2026.1] WorkStation.md`, functional requirements FR-001 to FR-033.

## Corrections made before release

| Severity | Finding in the previous model | Correction |
| --- | --- | --- |
| Major | The overall model listed UC-001 in the inventory but did not depict authentication. | Added `UC-001 Authenticate User` and the abstract external actor `Authorized Clinic User`; the inventory defines this actor as the pre-authorisation role abstraction. |
| Major | Appointment booking showed availability validation, but rescheduling did not; check-in did not explicitly create the encounter and queue assignment. | Added mandatory `<<include>>` dependencies for both booking and rescheduling to availability validation, and from check-in to encounter/queue creation. |
| Major | The appointment and medical diagrams were too tall and dense for an A4 report figure. | Split appointment management from check-in/queue management, and clinical documentation from service-order execution. |
| Major | Audit filtering was modelled as mandatory whenever an audit log is reviewed. | Corrected it to `Filter Audit Events <<extend>> Review Audit Log`, because filtering is conditional. |
| Major | The medication-warning override extended the warning check rather than the valid prescription-issuance flow. | Corrected it to `Override Safety Warning with Reason <<extend>> Validate and Issue Prescription`; the safety check remains mandatory before issue. |
| Minor | Revenue reporting and price-list administration were linked to unrelated invoice-status/price-application behaviour. | Removed those misleading `<<include>>` dependencies. |
| Minor | Opening an encounter required reviewing history in the diagram, although this is a permitted action rather than a mandatory step for every role. | Removed the invalid mandatory dependency. |

## Review results

| Check | Result |
| --- | --- |
| Scope | Clinic-focused only. Patient self-service is explicitly labelled Phase 2. No hospital wards, external payment gateway, laboratory integration, or compliance claim is depicted. |
| Terminology and IDs | Use-case IDs and actor names are recorded in `model-inventory.md`; diagrams use the same identifiers. |
| UML semantics | Associations use undirected solid lines. Mandatory behavior uses `<<include>>`; conditional behavior uses `<<extend>>`. |
| Traceability | Each detailed diagram maps to WorkStation functional requirements in `model-inventory.md`. |
| Reference review | The two required references and the specific techniques adopted/not adopted are recorded in `reference-review.md`. |
| Rendering | Every `.puml` source was parsed with PlantUML and rendered as a 2x PNG. The rendered figures were visually inspected for clipped labels, overlap and connector ambiguity. |

## Remaining modelling boundary

The diagrams are requirement models, not implementation evidence. Any later claim that a capability is implemented, tested, or compliant must be supported by source code, test results, deployment evidence, or an approved policy.
