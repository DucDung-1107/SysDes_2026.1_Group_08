# MEDORA Figure and Image Quality Audit Log

> Superseded by `audit.md` (22 October 2026). The status table below is historical.

## Audit contract

Every figure referenced by `doc/report/paper.tex` must have a traceable source,
successful render, visual inspection at its intended report scale, semantic review
against its UML/model type, and a score of at least 9.0/10 in every critical
dimension. `PASS` is prohibited until all evidence is recorded.

Critical dimensions: semantic correctness, business consistency, readability at
report scale, layout/connector clarity, terminology/caption consistency, and
natural visual communication.

## Inventory and status

| ID | Figure family | Source | Output | Status |
| --- | --- | --- | --- | --- |
| F-01 | Outpatient workflow | `02-business-analysis/business-processes/uml_code/outpatient-patient-journey.puml` | `.../figure/outpatient-patient-journey.png` | OPEN - revised; recheck required |
| F-02 | Appointment/check-in workflow | `02-business-analysis/business-processes/uml_code/appointment-and-check-in-exceptions.puml` | `.../figure/appointment-and-check-in-exceptions.png` | OPEN - revised; recheck required |
| F-03 | Domain analysis model | `02-business-analysis/domain-model/uml_code/conceptual-domain-model.puml` | `.../figure/conceptual-domain-model.png` | OPEN - semantic revision; render and report-scale recheck pending |
| F-04 | Functional decomposition | `03-system-requirements/functional-decomposition/uml_code/functional-decomposition.puml` | `.../figure/functional-decomposition.png` | OPEN - revised; recheck required |
| F-05 | Overall use-case model | `03-system-requirements/use-case-model/uml_code/overall-use-case-model.puml` | `.../figure/overall-use-case-model.png` | OPEN - semantic revision; render and report-scale recheck pending |
| F-06--F-14 | Focused use-case models | `03-system-requirements/use-case-by-function/**/uml_code/*.puml` | matching `figure/*.png` | OPEN - semantic revision; render and report-scale recheck pending |
| F-15--F-20 | System sequence models | `05-behavioral-modeling-implementation/behavioral-uml/sequence-by-function/**/uml_code/*.puml` | matching `figure/*.png` | OPEN - semantic revision; render and report-scale recheck pending |
| F-21--F-22 | Activity models | `05-behavioral-modeling-implementation/behavioral-uml/activity-by-workflow/uml_code/*.puml` | matching `figure/*.png` | OPEN - semantic revision; render and report-scale recheck pending |
| F-23--F-27 | State-machine models | `05-behavioral-modeling-implementation/behavioral-uml/state-machine-by-entity/uml_code/*.puml` | matching `figure/*.png` | OPEN - semantic revision; render and report-scale recheck pending |
| F-28--F-30 | Proposed UI wireframes | `05-behavioral-modeling-implementation/ui-api-evidence/ui/code/*.puml` | source/render status to verify | OPEN |

Tables, logos, and non-UML illustrations are audited separately after the diagram
inventory, including their captions, sizing, source status, and report-scale
legibility.

## Source correction round — 22 October 2026

This round corrects editable PUML semantics before any visual calibration. The
domain analysis model no longer exposes design operations; focused use-case
views use a named MEDORA system boundary; refund is no longer merged with an
unspecified adjustment; and prescription issue no longer includes adding a
medicine as if each issue created a new item. The SSDs now use explicit system
operations, bracketed guards, typed outcomes, and state-preservation statements
for failed paths. State machines use stable states and event/guard/effect
transitions; payment and refund history remains non-destructive.

All 30 PUML sources passed PlantUML syntax validation. No figure has been
re-rendered or visually re-scored after this source round, so the affected rows
remain OPEN and no Section 9 score is asserted.

## Notation correction round — 22 October 2026

The activity sources now use UML swimlanes for Doctor, Cashier, Assigned Service
Staff, and MEDORA System instead of embedding role tags in action labels.
Phase-2 status is now an explicit note beside the Patient actor rather than a
label on a UML actor-use-case association. These source changes still require a
fresh render and report-scale inspection before any score or PASS decision.
