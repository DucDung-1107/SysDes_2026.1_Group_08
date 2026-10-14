# Reference Review for MEDORA Use-Case Modelling

## 1. Software Ideas Modeler: Hospital Management System (UML Diagrams)

Source: <https://www.softwareideas.net/uml-diagram-hospital-management-system>

| Review point | Outcome for MEDORA |
| --- | --- |
| Useful technique | Separates use-case, class and sequence diagrams; uses patient and doctor interactions to express registration, examination, diagnosis, prescription and procedure scheduling. |
| Relevant elements | Patient registration, examination, diagnosis, prescribing and procedure planning informed MEDORA's patient, encounter, prescription and service-order views. |
| Not copied | Hospital-oriented visit, discharge and procedure confirmation flow was not adopted as a full inpatient workflow. MEDORA remains clinic-focused and does not model wards, inpatient discharge or theatre operations. |
| Missing for MEDORA | The example does not cover clinic check-in/queue coordination, role-based access, audit trails, signed-record amendments, billing boundaries or scoped reporting at the level required by the Medora PRD. |
| MEDORA improvement | Uses canonical IDs, explicit FR traceability, bounded phase-2 portal scope and narrower functional diagrams to keep the overview readable. |

## 2. Smart Hospital Management System --- UML (GitHub)

Source: <https://github.com/saipraveen-k/Smart-Hospital-Management-System---UML>

| Review point | Outcome for MEDORA |
| --- | --- |
| Useful technique | The repository structure highlights the value of organizing a UML set by subsystem and by diagram type rather than keeping one monolithic diagram. |
| Relevant elements | Subsystem grouping informed the separation of Patient Management, Appointment, Examination, Prescription, Billing and Administration assets. |
| Not copied | The repository is treated as an illustrative structure, not as evidence that its listed features are complete or suitable for Medora. No hospital-scale scope, technology architecture or integration is adopted without a Medora requirement. |
| Missing for MEDORA | Public root-level material does not replace a clinic-specific requirements baseline or traceability matrix. Medora therefore uses WorkStation FR identifiers and explicitly marks future portal capability. |
| MEDORA improvement | Keeps editable PlantUML beside rendered outputs; links use cases to FRs; and requires visual review of generated diagrams. |

## Conclusion

The references informed notation, decomposition and diagram-set organization. They do not define Medora's requirements. The WorkStation product requirements remain the source of truth for actors, use cases, constraints and scope boundaries.
