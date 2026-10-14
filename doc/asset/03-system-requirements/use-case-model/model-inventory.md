# MEDORA Use-Case Model Inventory

## Canonical actors

| Actor | Responsibility | Scope status |
| --- | --- | --- |
| Patient | Manages own appointment requests and released documents through a portal. | Phase 2 |
| Authorized Clinic User (abstract) | Authenticates before the system resolves the assigned internal role and facility scope. | MVP |
| Receptionist | Identifies/registers patients, manages appointments, check-in and queues. | MVP |
| Nurse | Records screening, vital signs and assigned care activities. | MVP |
| Doctor | Documents encounter, diagnosis, prescriptions, orders and closure. | MVP |
| Technician / Service Staff | Executes assigned services and records results. | MVP |
| Pharmacist / Medicine Staff | Records dispensing or administration when assigned. | MVP / optional by clinic workflow |
| Cashier | Creates invoices and records payment status. | MVP |
| Clinic Manager | Supervises schedules, operations, dashboards and reports. | MVP |
| System Administrator | Manages accounts, roles, catalogues and configuration. | MVP |
| Auditor | Reviews approved audit evidence in read-only scope. | MVP |

## Use-case traceability

| ID range | Capability | Main FR source | Diagram source |
| --- | --- | --- | --- |
| UC-001 | Authenticate User | FR-001 | `overall-use-case-model.puml` |
| UC-010--UC-019 | Patient Management | FR-003, FR-004, FR-008, FR-029, FR-030, FR-031 | `patient-management.puml` |
| UC-020--UC-026 | Appointment Management | FR-005, FR-017, FR-024, FR-025 | `appointment-and-queue.puml` |
| UC-027--UC-030 | Check-in and Queue Management | FR-006 | `check-in-and-queue.puml` |
| UC-031--UC-035 | Screening and Consultation | FR-007, FR-010 | `medical-examination.puml` |
| UC-036--UC-038, UC-041--UC-042 | Clinical Note Lifecycle and Encounter Closure | FR-009, FR-020, FR-021, FR-032 | `clinical-note-lifecycle.puml` |
| UC-039--UC-040 | Service Order and Execution | FR-014, FR-018 | `service-order-execution.puml` |
| UC-043--UC-052 | Prescription and Medication Safety | FR-011, FR-012, FR-013, FR-028 | `prescription-and-medication-safety.puml` |
| UC-060--UC-069 | Billing and Payment | FR-022, FR-023, FR-027 | `billing-and-payment.puml` |
| UC-070--UC-079 | Administration, Reporting and Governance | FR-002, FR-024, FR-026, FR-027, FR-028, FR-030, FR-033 | `administration-and-governance.puml` |
| Overview | Grouped goals only | Cross-cutting overview; decomposed by the detailed diagrams above. | `overall-use-case-model.puml` |

## Modelling decisions

- `<<include>>` denotes a mandatory reusable step, such as availability validation when booking and required-field validation before signing a note.
- `<<extend>>` denotes conditional behavior, such as overriding a medication-safety warning or applying an authorized discount.
- Patient self-service is labeled as a phase-2 assumption. No external clinical, payment, laboratory or imaging integration is depicted as implemented.
- Detailed specifications for high-risk or stateful cases should be written next for appointment booking, check-in, signing/amending a clinical note, issuing a prescription, scheduling a procedure and recording payment/refund.
