# Section 4 design reference review

## Primary project definition

The MEDORA WorkStation brief is the source for the clinic scope, requirements, entities, and business rules. It describes a conceptual data model and explicitly leaves physical schema choices to design. This section therefore labels architecture and database artifacts as proposed, not implemented.

## Smart Hospital Management System reference

[Repository](https://github.com/saipraveen-k/Smart-Hospital-Management-System---UML)

- Useful: the experiment sequence, functional package grouping, and explicit Boundary -> Control -> Entity responsibility flow provide a traceable way to organize diagrams and controllers.
- Adapted: keep the separation of role-specific screens, workflow controllers, and persistent entities; focus on patient identity, appointments, outpatient encounters, clinical documentation, orders, and billing.
- Excluded: inpatient admission, beds, wards, hospital departments, and other hospital-only structures are not added to MEDORA's clinic model.
- Limitation: the repository is a modeling reference. Its README and diagrams do not prove that MEDORA features or infrastructure have been implemented.

## Local design-class page

The local `http://localhost:3000/design` page is backed by `src/app/design/page.tsx`, the class catalog in `src/data/designClasses.ts`, and the `EXP7-DCD-01` master class view in `src/data/diagrams.ts` in the adjacent Smart Hospital Management System project.

- Useful: the page shows boundary and control classes with operations, domain classes, and a `PaymentProcessor` interface used as an abstraction point.
- Adapted: two focused design-class views use the patient-registration, outpatient encounter, and billing paths, map the hospital `Consultation` role to MEDORA's `Encounter`, and use MEDORA's MRN terminology. The billing controller's `PaymentProcessor` dependency is optional.
- Excluded: admission/bed classes and concrete card, UPI, and SMS adapter classes. The payment port is optional and no provider has been selected.
- Limitation: the page is a design reference for class organization, not evidence that these classes or integrations are implemented in MEDORA.

## Hospital UML example

[Software Ideas Modeler example](https://www.softwareideas.net/uml-diagram-hospital-management-system)

- Useful: presenting related use-case, class, and sequence views for a patient-clinician visit helps readers move from goals to structure and interaction.
- Adapted: the report cross-references its focused class views with the appointment, clinical-note, service-order, and payment sequence diagrams.
- Excluded: discharge and room-management concepts are not imported into MEDORA's outpatient scope.
- Limitation: the example does not specify MEDORA requirements, database schema, API contract, or deployment environment.

## Section 4 design decisions

1. Retain a modular, clinic-focused architecture; do not imply microservices or a hospital-wide platform.
2. Preserve the distinction between Appointment and Encounter, signed note versions, invoice, payment, and refund events.
3. Show only a logical ERD. Candidate key names are included; physical data types, database engine, and DDL remain unselected.
4. Show a client -> application -> relational store deployment as a proposed logical baseline; host topology and operational controls remain undecided.
5. Treat API groupings as interface responsibilities, not endpoint contracts.

