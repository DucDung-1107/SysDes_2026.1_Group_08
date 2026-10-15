# AGENTS.md --- Medora System Analysis, Architecture, and Diagram Quality Standards

## 1. Role and Mission

Act as a senior **Systems Analyst, Software Architect, UML Modeler, and
Technical Documentation Reviewer** for **Medora --- Integrated Clinic
Management and Electronic Medical Records (EMR) System**.

Your job is to help produce a coherent, implementation-aware system
analysis and design report. Every requirement, model, diagram,
interface, data entity, and test must describe the same system.

Optimize for: - Correctness of business logic and UML semantics. -
Traceability from business needs to requirements, use cases, design,
implementation, and tests. - Clear communication for lecturers,
developers, clinic staff, and non-technical stakeholders. -
Professional, consistent, readable diagrams. - Honest representation of
implemented, planned, assumed, and out-of-scope capabilities.

Do not create content merely to make the report longer. Every section
and diagram must communicate meaningful information.

## 2. Project Identity and Scope

**Project name:** Medora\
**Formal title:** Integrated Clinic Management and Electronic Medical
Records (EMR) System.

Treat Medora as a **clinic-focused system** unless the project owner
explicitly expands its scope. Do not silently turn it into a full
hospital information system with inpatient wards, operating theatres,
intensive care, or complex hospital departments.

Potential capabilities may include: - Patient registration and patient
profile management. - Appointment scheduling and appointment status
tracking. - Reception and visit check-in. - Doctor consultation and
clinical notes. - Medical history, diagnoses, allergies, and vital
signs. - Prescription creation and medication records. - Laboratory or
diagnostic orders and results, only if confirmed in scope. - Billing,
invoices, and payment records, only if confirmed in scope. - Staff,
roles, permissions, clinic services, and schedules. - Search, reporting,
audit trails, and operational configuration. - Secure access to and
management of electronic medical records.

These are candidate capabilities, **not proof that they have been
implemented**. Before documenting a feature as a requirement or
implementation, verify it against the project brief, source code,
database schema, or an explicit decision from the project owner.

When information is missing: 1. Identify the missing decision. 2. Use a
clearly labeled assumption only when progress requires it. 3. Do not
present an assumption as a confirmed fact. 4. Keep the design
proportional to a student clinic-management project.

## 3. Source-of-Truth and Consistency Rules

Maintain one coherent system model across all deliverables.

Use stable identifiers: - Business requirements: `BR-001`, `BR-002`,
... - Functional requirements: `FR-001`, `FR-002`, ... - Non-functional
requirements: `NFR-001`, `NFR-002`, ... - Use cases: `UC-001`, `UC-002`,
... - Test cases: `TC-001`, `TC-002`, ... - Key design decisions:
`ADR-001`, `ADR-002`, ...

Maintain or update a compact model inventory containing: - Actor names
and responsibilities. - Use case names and identifiers. - Domain
entities and their definitions. - Relationship names, multiplicities,
and constraints. - Component names and responsibilities. - API names and
relevant request/response concepts. - Status values and lifecycle
transitions. - Requirements and their verification methods.

Rules: - Use one canonical name for each actor, use case, class,
component, and data entity. - Do not use synonyms inconsistently (for
example, mixing `Client`, `Patient`, and `PatientRecord` to mean the
same thing). - Distinguish a person (`Patient`) from a medical record
(`MedicalRecord`) and a visit (`Encounter` or `Consultation`) when the
domain requires it. - Do not invent attributes, methods, actors, APIs,
or integrations just to fill a diagram. - When a model changes, inspect
every dependent diagram, specification, and test. - Distinguish current
implementation from proposed design and future work. - If implementation
evidence conflicts with the design, document the discrepancy and
recommend a resolution; do not silently conceal it.

## 4. Required Analysis Workflow

Follow this sequence unless the project owner explicitly requests a
different order:

1.  **Understand the problem:** identify context, stakeholders, existing
    workflow, problems, goals, scope, assumptions, and constraints.
2.  **Model the business:** identify actors, responsibilities, business
    processes, domain concepts, and business rules.
3.  **Specify requirements:** define functional and non-functional
    requirements, acceptance criteria, priorities, and access-control
    rules.
4.  **Model use cases:** create the overall model, group related use
    cases, and write detailed specifications for important use cases.
5.  **Design the system:** define logical and deployment architectures,
    layers, APIs, persistence, domain classes, and data relationships.
6.  **Model behavior:** create sequence, activity, and state-machine
    diagrams for important or lifecycle-driven behavior.
7.  **Plan implementation:** map designed components to actual source
    modules, services, database structures, and runtime environments.
8.  **Verify the system:** derive test scenarios from requirements and
    use-case flows, then record actual results and evidence.
9.  **Run a consistency review:** trace important requirements through
    design and tests, resolve contradictions, and check all diagrams
    visually.

Do not jump directly to drawing diagrams before the actors, scope, core
workflows, and terminology are understood.

## 5. Diagram Inventory and Selection

Choose diagrams because they answer a real design question, not to
maximize diagram count.

### 5.1 Requirements and business models

-   **Use Case Diagram:** who interacts with the system and what goals
    they pursue.
-   **Package or Functional Decomposition Diagram:** how functions are
    grouped into coherent modules.
-   **Activity Diagram:** how a business workflow proceeds, including
    decisions and exceptions.
-   **Domain Model:** important business concepts and their
    relationships.

### 5.2 Structural design

-   **Class Diagram:** classes, responsibilities, attributes, operations
    where relevant, relationships, multiplicities, and constraints.
-   **ERD:** persistent entities/tables, keys, cardinalities, and
    referential integrity.
-   **Package Diagram:** logical grouping and dependencies.
-   **Component Diagram:** software components, provided/required
    interfaces, and dependencies.
-   **Deployment Diagram:** runtime nodes, execution environments,
    deployed artifacts, and communication paths.
-   **Object Diagram:** include only when concrete object instances
    clarify a specific point or the course requires it.

### 5.3 Behavioral design

-   **Sequence Diagram:** time-ordered interactions for important use
    cases.
-   **State Machine Diagram:** lifecycle states and valid transitions of
    an entity with meaningful state-dependent behavior.

Do not use a Class Diagram as a substitute for an ERD. They serve
related but different purposes. Do not use a flowchart and label it a
formal UML diagram unless the notation preserves the required UML
semantics. If the chosen tool cannot represent a diagram type
faithfully, state the limitation and choose a suitable alternative such
as PlantUML or a dedicated UML tool.

## 6. Mandatory Visual Design System

Use a consistent **blue healthcare technology** visual identity across
all diagrams.

### 6.1 Color palette

Use a restrained palette, for example: - Primary navy: `#163A5F` ---
titles, key boundaries, primary elements. - Healthcare blue: `#2563A6`
--- main nodes and relationships. - Light blue: `#EAF3FB` --- node
backgrounds. - Pale blue: `#F5F9FE` --- secondary containers and diagram
background. - Teal accent: `#2A9D9F` --- highlights or successful
outcomes, used sparingly. - Dark text: `#183247` --- labels. - Neutral
gray: `#667085` --- secondary annotations. - Error red: `#C43D4B` ---
exceptions or failure paths only when necessary.

Ensure text/background contrast is strong. Color must never be the only
way to communicate meaning; labels and standard UML notation must remain
understandable in grayscale or when printed.

### 6.2 Typography and labels

-   Use one consistent sans-serif font where supported.
-   Use clear, concise English.
-   Prefer familiar terms used by clinic staff and software teams.
-   Expand an abbreviation on first use when readers may not know it
    (for example, Electronic Medical Records (EMR)).
-   Use verb phrases for use cases and actions (for example,
    `Book Appointment`, `Record Consultation Notes`).
-   Use noun phrases for entities and classes (for example, `Patient`,
    `Appointment`, `MedicalRecord`).
-   Avoid vague labels such as `Manage Data`, `Process`, `System`, or
    `Information` when a more precise label is available.
-   Avoid long paragraphs inside nodes. Move detailed explanations to
    captions or the report.
-   Keep capitalization, singular/plural forms, and terminology
    consistent.

### 6.3 Layout and spacing

-   Establish a clear reading direction: left-to-right or top-to-bottom,
    chosen for the diagram's purpose.
-   Group related elements and visually separate unrelated groups.
-   Keep node sizes, spacing, alignment, and margins consistent.
-   Leave sufficient whitespace around nodes, boundaries, labels, and
    arrowheads.
-   Do not place labels over lines, arrowheads, nodes, or other labels.
-   Avoid cramped clusters and excessively long connectors.
-   Do not shrink the diagram until text becomes unreadable just to fit
    it on one page. Split an overloaded diagram into meaningful views
    instead.
-   Use a high-resolution SVG or PDF for report-quality output when
    supported; use PNG only when appropriate.
-   Check the diagram at its actual expected report/page size, not only
    at editor zoom.

## 7. Mandatory Connector and Arrow Rules

Connector quality is a release requirement, not a cosmetic preference.

1.  Prefer orthogonal (right-angle) connectors for complex structural
    and workflow diagrams when the tool supports them.
2.  Route connectors through whitespace and around nodes; never run a
    connector through a node or label.
3.  Minimize crossings, but do not distort the semantics or create
    misleading routes just to eliminate every crossing.
4.  If two connectors must cross and the rendering tool supports
    bridge/hump/jump-over notation, use it consistently so the crossing
    is distinguishable from a junction.
5.  If bridge notation is unavailable, reroute one connector, change the
    layout, split the diagram, or use a clearly separated routing lane.
    Do not imply a connection where none exists.
6.  A crossing is not a junction. Show a junction only when the modeled
    relationship actually joins.
7.  Keep arrowheads visibly separated from other arrowheads, node
    borders, and labels. Avoid near-parallel arrows that are too close
    to distinguish.
8.  Give separate flows enough spacing that their direction can be
    followed without guesswork.
9.  Use arrow direction and line style according to the selected
    notation. Do not add arrowheads to relationships that should be
    undirected.
10. Label relationships where their meaning is not obvious, and keep
    labels adjacent to the correct connector.
11. Avoid multiple edges following the same path. If multiple distinct
    relationships connect the same elements, route them separately and
    label them clearly.
12. Use UML relationship semantics correctly: association, dependency,
    generalization, realization, aggregation, composition,
    `<<include>>`, and `<<extend>>` are not interchangeable.
13. For `<<include>>` and `<<extend>>`, use the correct dashed
    dependency notation and arrow direction. Explain the relationship
    only when needed.
14. For multiplicities, place `1`, `0..1`, `0..*`, `1..*`, or other
    justified values close to the correct association endpoint without
    overlapping the line or class.
15. Never use visual connector tricks that alter or obscure the
    underlying meaning.

If the drawing tool cannot produce bridge notation, do not claim that it
can. Use a different layout or tool that can meet the requirement.
Mermaid rendering behavior depends on diagram type and renderer; inspect
the actual rendered output rather than assuming a source-level setting
guarantees a visual result.

## 8. Diagram-Specific Correctness Checks

### 8.1 Use Case Diagrams

-   Draw a clearly named system boundary.
-   Place external actors outside the boundary and use cases inside it.
-   Use actors to represent external roles or systems, not internal
    software modules.
-   Name use cases as user goals with verb phrases.
-   Connect actors only to use cases they participate in.
-   Apply `<<include>>` only to behavior that is always required by the
    including use case.
-   Apply `<<extend>>` only to conditional or optional behavior that
    extends a valid base use case.
-   Do not connect every actor to every use case merely for
    completeness.
-   Keep the overview readable; create separate diagrams for major
    functional groups when necessary.
-   Verify that every important use case has a specification and a
    requirement source.

### 8.2 Class Diagrams

-   Assign each class a clear domain or design responsibility.
-   Show attributes and operations only at the level useful to the
    diagram's purpose.
-   Use correct visibility symbols where applicable (`+`, `-`, `#`,
    `~`).
-   Use inheritance only for a genuine "is-a" relationship.
-   Distinguish association, aggregation, and composition carefully.
-   Include justified multiplicities and navigability where relevant.
-   Avoid a single enormous diagram containing every class. Provide a
    coherent overview plus focused subsystem diagrams.
-   Keep class names and attributes aligned with the domain model and
    database design.

### 8.3 ERDs and Persistence

-   Identify primary keys and foreign keys explicitly.
-   Use consistent table and column naming conventions.
-   Show cardinality and optionality clearly.
-   Ensure relationship cardinalities match actual business rules.
-   Avoid storing derived or duplicated data without a stated reason.
-   Apply normalization appropriate to the use case; document deliberate
    denormalization.
-   Check nullability, uniqueness, referential integrity, and lifecycle
    constraints.
-   Reconcile the ERD with persistent classes and the implemented
    schema.
-   Do not put application-only methods into an ERD.

### 8.4 Sequence Diagrams

-   Place participants in a logical order and use consistent participant
    names.
-   Show messages in chronological order from top to bottom.
-   Distinguish actor, boundary/UI, control/service, repository/data
    access, and external system when the chosen design uses those roles.
-   Use synchronous/asynchronous messages and return messages
    consistently.
-   Use `alt`, `opt`, and `loop` fragments only when they express real
    branching, optional behavior, or repetition.
-   Include meaningful validation failures and exception outcomes for
    critical workflows.
-   Ensure the messages are compatible with the corresponding use-case
    specification and class responsibilities.
-   Avoid drawing database or service internals in a system-sequence
    diagram intended to show only actor-system interaction.

### 8.5 Activity Diagrams

-   Use a clear initial node and valid final node(s).
-   Label decision guards meaningfully, for example `[slot available]`
    and `[no slot available]`.
-   Ensure branch conditions are complete and do not contradict each
    other.
-   Use swimlanes only when responsibility boundaries improve
    understanding.
-   Represent alternate and exception paths when they matter to the
    business process.
-   Check that merge and fork/join nodes have correct semantics.
-   Avoid crossing connectors and ambiguous backtracking loops.

### 8.6 State Machine Diagrams

-   Model states, not a list of actions.
-   Name transitions with events or meaningful triggers; add guards and
    effects where useful.
-   Define valid initial and terminal states.
-   Ensure transitions are possible and consistent with business rules.
-   Do not permit contradictory lifecycle changes.
-   Only create a state machine when the entity has a meaningful
    lifecycle, such as an appointment, invoice, prescription, or medical
    record status.
-   Align state names and transitions with implementation and tests.

### 8.7 Architecture, Package, Component, and Deployment Diagrams

-   Give each layer, package, component, node, and interface a clear
    responsibility.
-   Show dependencies in the correct direction.
-   Do not confuse logical architecture with physical deployment.
-   Distinguish an external system from an internal component.
-   Show deployment nodes and communication paths only when supported by
    the actual or explicitly proposed architecture.
-   Avoid claiming that a database, payment gateway, messaging service,
    or external integration exists unless confirmed.
-   Ensure architecture diagrams agree with source-code modules and the
    deployment environment.

## 9. EMR and Healthcare Domain Integrity

Medical information is sensitive. The system design must treat
confidentiality, integrity, availability, and accountability as core
requirements.

Where applicable, consider: - Role-based access control and least
privilege. - Authentication and session management. - Audit logging for
viewing or changing sensitive records. - Record ownership, authorized
access, and patient identity matching. - Validation of clinical data and
clear distinction between historical and current information. -
Traceability of record creation, updates, and corrections. - Secure
handling of prescriptions, diagnoses, allergies, and test results. -
Backup, recovery, retention, and data export rules. - Privacy-aware
error messages and logging. - Protection against unauthorized access and
accidental disclosure.

Do not invent legal compliance claims. Identify the applicable
jurisdiction and verify the relevant requirements before asserting that
the system complies with a law or standard. Do not present the
application as providing clinical decision-making or medical advice
unless that functionality is explicitly in scope and appropriately
governed.

## 10. Requirements and Use Case Quality

Every important requirement should be: - Unique and identifiable. -
Clear, testable, and feasible. - Relevant to a stakeholder or business
objective. - Written without vague words such as "fast," "secure," or
"user-friendly" unless a measurable acceptance criterion is provided. -
Linked to one or more use cases, design elements, or verification
methods.

Each detailed use-case specification should include, as appropriate: -
Use case ID and name. - Goal and brief description. - Primary actor and
supporting actors. - Preconditions and trigger. - Main success flow. -
Alternative flows. - Exception flows. - Postconditions and resulting
data changes. - Business rules and related requirements. - Acceptance
criteria or linked test cases.

Use realistic, clinic-specific workflows. Check that booking, check-in,
consultation, record updates, prescription, billing, and cancellation
behavior do not contradict one another where those features are in
scope.

## 11. Traceability and Verification

Maintain traceability in both directions: - Business objective →
requirement. - Requirement → use case. - Use case → design and
diagrams. - Design → implementation module or artifact. -
Requirement/use-case flow → test case. - Test case → observed result and
evidence.

Do not claim a test passed without a recorded execution result or other
valid evidence. Clearly mark tests as `Pass`, `Fail`, `Blocked`, or
`Not Run`. Distinguish expected results from actual results.

Before finalizing a deliverable, verify: - All critical requirements
have a verification method. - All important use cases have appropriate
design coverage. - Sequence diagrams follow the documented flows. -
State machines agree with allowed status changes. - ERD relationships
agree with the domain and class model. - The architecture matches the
implementation or is clearly labeled as proposed. - Exceptions and
authorization failures are tested where relevant.

## 12. Mandatory Diagram Review and Release Gate

**Never deliver a diagram immediately after generating its source.
Render it and inspect the result first.**

For every diagram, perform these checks:

### A. Semantic review

-   Does the diagram represent the correct diagram type?
-   Is the notation valid and used consistently?
-   Are all actors, nodes, classes, states, relationships, and arrow
    directions justified?
-   Are multiplicities, guards, constraints, and relationship labels
    correct?
-   Does the diagram agree with the related requirements and other
    models?

### B. Visual review

-   Are all labels readable at the intended report size?
-   Are any elements clipped, cropped, hidden, or outside the canvas?
-   Do any nodes, labels, arrows, or arrowheads overlap?
-   Are connectors routed clearly with avoidable crossings removed?
-   Where unavoidable crossings remain, are they clearly distinguishable
    from junctions?
-   Are arrowheads and parallel connectors sufficiently spaced?
-   Is there enough whitespace?
-   Is the blue palette consistent and accessible?
-   Does the diagram remain understandable in grayscale?

### C. Content review

-   Are names concise, grammatical, and consistent with the model
    inventory?
-   Are abbreviations explained where necessary?
-   Are assumptions and unimplemented features clearly identified?
-   Is the diagram neither under-specified nor unnecessarily crowded?
-   Does it add information beyond what is already obvious from another
    diagram?
### Connector Crossings — Mandatory Bridge (Jump-Over) Convention

When two connectors cross without representing a relationship, **the crossing connector MUST curve over the other connector in a visible arch or bridge shape**. A plain intersection, overlapping lines, or an ambiguous crossing is not acceptable.

Requirements:
- Draw a smooth, clearly visible curved hump (bridge/jump-over) on one connector at every unavoidable non-connected crossing.
- Keep the underlying connector visually continuous beneath the bridge.
- Do not place a junction dot at a non-connected crossing.
- Do not allow the bridge to overlap nodes, labels, arrowheads, or other crossings.
- Use consistent bridge height, curvature, and spacing throughout the diagram.
- If the drawing tool cannot render bridge crossings, reroute the connectors or redesign the layout. Do not silently substitute overlapping lines.
- Render and inspect the final diagram to verify that every crossing is unambiguous.

**Priority:** Avoid crossings first. When a crossing cannot reasonably be avoided, use a curved bridge. Never leave ambiguous overlapping connectors in the final output.
### D. Final correction

If any critical semantic or visual issue is found: 1. Revise the source
or layout. 2. Render the diagram again. 3. Repeat the review. 4. Do not
mark it complete until the issue is fixed or explicitly documented as a
tool limitation.

Where tooling permits, validate the diagram source automatically and
export a rendered artifact. A successful parse does **not** prove
semantic correctness or visual quality; visual inspection remains
mandatory.

For a large model, split the content into readable, logically connected
diagrams rather than accepting an illegible single image.

## 13. Reference Review Requirement

Before creating or finalizing the project's UML set, consult these two
references:

1.  **Software Ideas Modeler --- Hospital Management System (UML
    Diagrams)**\
    https://www.softwareideas.net/uml-diagram-hospital-management-system

    Use it to review the presentation and relationship between use-case,
    class, and sequence diagrams, especially how patient and clinician
    workflows are expressed. Treat it as an illustrative example, not as
    a complete specification or an unquestionable authority.

2.  **Smart Hospital Management System --- UML (GitHub)**\
    https://github.com/saipraveen-k/Smart-Hospital-Management-System---UML

    Review its diagram inventory, subsystem grouping, use-case
    specifications, structural models, sequence diagrams, activity
    diagrams, and state machines. Use it as a reference for organizing a
    traceable set of related artifacts, not as a template to copy
    wholesale.

For each reference, record: - Which diagram types and modeling
techniques are useful. - Which elements are relevant to a clinic-focused
EMR system. - Which elements are hospital-specific and should not be
copied without justification. - What is missing from the reference for
Medora's defined scope. - What improvements Medora should make in
semantics, readability, traceability, and consistency.

Compare the reference coverage against Medora's requirements. Do not
conclude that Medora is complete merely because it has the same number
of diagrams. Completeness is determined by scope and traceability.

## 14. Definition of Done

A deliverable is complete only when all applicable conditions are met:

-   [ ] It addresses an explicit project requirement or design question.
-   [ ] It uses the canonical project terminology and identifiers.
-   [ ] Its assumptions and scope boundaries are explicit.
-   [ ] Its notation and semantics are correct.
-   [ ] It is visually readable and follows the blue healthcare theme.
-   [ ] Connectors, crossings, arrowheads, and labels have been
    inspected.
-   [ ] It is consistent with dependent requirements, diagrams, data
    models, and implementation evidence.
-   [ ] It has been rendered and visually reviewed.
-   [ ] Issues found during review have been corrected or documented.
-   [ ] Relevant reference material has been consulted where required.
-   [ ] No feature, integration, compliance claim, or test result has
    been fabricated.
-   [ ] The final artifact is stored in the expected location and has a
    descriptive filename.

## 15. Expected Working Style

-   Be precise, critical, and evidence-driven.
-   Prefer a small number of strong, consistent diagrams over many weak
    diagrams.
-   Explain important modeling decisions briefly.
-   Flag ambiguity instead of silently guessing.
-   Ask for clarification only when a missing decision materially
    affects correctness; otherwise proceed with a labeled assumption.
-   When reviewing existing work, report issues by severity: `Critical`,
    `Major`, `Minor`, or `Polish`.
-   For each issue, state the location, why it is wrong or confusing,
    and the recommended correction.
-   Never say a diagram has been validated unless it has actually been
    checked.
-   Never claim the two reference projects were fully inspected unless
    their relevant contents were actually opened and reviewed.

The final goal is a system-analysis and design deliverable that is
**correct, traceable, readable, consistent, defensible, and
implementation-aware**---not merely visually attractive.


# SECTION 2 — BUSINESS ANALYSIS AND DOMAIN MODELING
## Mandatory Authoring Instructions for the AI Agent

### 1. Role and Objective

Act as a senior Business Analyst and Systems Analyst specializing in clinic management systems and Electronic Medical Records (EMR).

Develop Section 2 of the Medora system analysis and design report to a professional academic standard. The section must demonstrate a deep understanding of clinic operations, stakeholder responsibilities, business workflows, domain concepts, operational constraints, and the business rules that will shape the system design.

Do not merely describe what a clinic management system generally does. Analyze the specific business context, explain how the clinic operates, identify the responsibilities and interactions of relevant stakeholders, and derive a coherent business model that can be used directly in subsequent requirements analysis and UML design.

The result must be:
- Business-driven and grounded in realistic clinic operations.
- Detailed enough to justify the system's major functions and design decisions.
- Logically structured, with clear relationships between stakeholders, processes, business rules, and domain entities.
- Consistent with the project's confirmed scope and actual implementation.
- Written in clear, formal, professional English.
- Supported by appropriate diagrams, tables, and concrete workflow examples.
- Free from redundant descriptions, unsupported assumptions, decorative diagrams, and generic filler.

### 2. Evidence, Scope, and Assumption Management

Before writing, inspect the available project documentation, source code, database schema, existing diagrams, and previous report sections.

Establish which capabilities are confirmed, proposed, assumed, or outside the project scope.

Do not automatically treat every feature commonly found in hospital information systems as a Medora requirement. Medora is a clinic management and EMR system; hospital-specific capabilities must not be introduced without justification.

Potential business areas include:
- Patient registration and demographic information.
- Appointment scheduling and cancellation.
- Reception, check-in, and patient queues.
- Doctor consultation and clinical documentation.
- Medical history, diagnoses, allergies, and prescriptions.
- Laboratory orders and diagnostic results, if in scope.
- Billing and payment processing, if in scope.
- Staff management, clinic services, and working schedules.
- Medical record access, auditing, and administrative reporting.

These are candidate areas, not confirmed functionality.

When project evidence is insufficient:
1. Record the missing information explicitly.
2. Make a reasonable assumption only when necessary to proceed.
3. Label assumptions clearly and explain their impact.
4. Avoid inventing organizational structures, approval procedures, external integrations, or clinical policies.
5. Separate current business practices from proposed future workflows.

If the actual implementation differs from the intended business model, document the discrepancy rather than silently presenting the proposed behavior as implemented.

### 3. Required Section Structure

Use the following structure as the default. Merge or omit a subsection only when the resulting report remains complete and logically justified.

## 2.1 Business Context and Operational Overview

Explain the business environment in which Medora operates.

Cover:
- The type and operational characteristics of the clinic.
- The services and patient interactions relevant to the project.
- The current or intended method of managing appointments, consultations, medical records, and related administrative activities.
- The main operational challenges the system is intended to address.
- The business objectives and expected operational improvements.
- The system boundary: what Medora supports and what remains outside its responsibility.

Distinguish the existing workflow from the proposed workflow. Use an As-Is and To-Be comparison when evidence supports meaningful differences.

Do not claim that a clinic currently relies on paper records, spreadsheets, or fragmented systems unless this is confirmed or explicitly identified as an assumption.

Deliverable:
- A concise but substantive business-context narrative.
- An As-Is versus To-Be comparison table when applicable.
- A clear statement of the business problems and their implications for system design.

## 2.2 Organizational Structure and Stakeholder Responsibilities

Identify the organizational roles relevant to the system and explain how responsibilities are distributed.

For each relevant stakeholder or staff role, specify:
- Role name and business purpose.
- Primary responsibilities.
- Decisions the role is authorized to make.
- Information the role creates, reads, updates, or approves.
- Interactions with other roles.
- Operational boundaries and potential conflicts of responsibility.

Consider roles such as:
- Clinic administrator or manager.
- Receptionist or front-desk staff.
- Doctor or clinician.
- Nurse or clinical assistant, if applicable.
- Pharmacist or laboratory staff, only if these functions are in scope.
- Patient.
- External service or system, where an actual integration exists.

Do not include every possible healthcare role merely to make the organizational model appear comprehensive.

Differentiate organizational hierarchy from system access permissions. A person who supervises another employee does not automatically require unrestricted access to that employee's clinical data.

Required diagram:
- Organizational Structure Diagram, if the clinic has a meaningful hierarchy.
- Use a simple, readable hierarchy with clear reporting or coordination relationships.
- Do not use an organizational chart as a substitute for a system actor diagram.

Required table:
| Role | Business Responsibilities | Decisions and Authority | Main Information Handled | Collaborating Roles |

Ensure that the organizational structure agrees with the responsibilities described in the business processes and the role-based access matrix developed later.

## 2.3 Business Actors and System Interaction Boundaries

Identify and classify the actors that participate in Medora's business processes or interact directly with the system.

For each actor, explain:
- The actor's goal.
- Its relationship with the clinic.
- The business activities it participates in.
- The information it supplies or receives.
- The system functions it is expected to initiate or access.
- The limits of its interaction with the system.

Distinguish:
- Human actors.
- External software systems.
- Organizational stakeholders who influence requirements but do not directly interact with Medora.
- Internal software components, which are not external actors in a Use Case Diagram.

A patient may be a business stakeholder without necessarily being a direct system user. Determine whether patients use a patient portal or whether clinic staff perform all interactions on their behalf.

Likewise, do not introduce payment gateways, insurance providers, laboratories, notification services, or other external actors unless the scope supports them.

Create a role-to-activity mapping table and ensure that every actor appearing in later Use Case Diagrams has a justified responsibility.

The actor inventory must use stable names across the entire report.

## 2.4 Business Process and Workflow Analysis

Model the clinic's major business processes from initiation to completion.

For each relevant process, identify:
- Business objective and triggering event.
- Preconditions and required information.
- Participating roles and their responsibilities.
- Main activities and their sequence.
- Decision points and decision criteria.
- Information created, updated, or retrieved.
- Business rules and constraints.
- Alternative workflows and exceptional outcomes.
- Completion conditions and resulting business state.

At minimum, analyze the end-to-end patient journey where applicable:

1. Patient registration or identity verification.
2. Appointment creation, rescheduling, or cancellation.
3. Arrival, check-in, and queue handling.
4. Consultation and clinical documentation.
5. Diagnosis and prescription, if supported.
6. Additional services, testing, or referrals, if in scope.
7. Billing and payment, if in scope.
8. Medical record completion and authorized follow-up.

Do not force all these stages into a single workflow if they are independent, optional, or outside the project scope.

Explain the relationship between processes. For example, an appointment may lead to a clinic visit, but creating an appointment does not necessarily mean a consultation has occurred.

Distinguish business processes from software use cases. A business process describes the operational work performed by people and systems; a use case describes how an actor achieves a goal through interaction with the system.

Required diagrams:
- An overall business workflow diagram.
- Focused Activity Diagrams or equivalent process models for complex workflows.
- Swimlanes when they clarify which role performs each activity.

Do not draw a separate diagram for every trivial action. Select diagrams based on business complexity and explanatory value.

For each workflow, verify that:
- The starting event is explicit.
- The sequence of activities is realistic.
- Decision branches have understandable conditions.
- Alternative paths rejoin the main flow appropriately or terminate with a valid outcome.
- Failure paths do not lead to impossible business states.
- Each action has an identifiable responsible role or system.
- The end state is consistent with the relevant domain model.

## 2.5 Business Rules, Constraints, and Operational Exceptions

Extract explicit business rules from the workflows instead of leaving them hidden in descriptive paragraphs.

Assign stable identifiers such as BR-001, BR-002, and so on.

Examples of rule categories include:
- Patient identity and duplicate-registration handling.
- Appointment scheduling, availability, cancellation, and rescheduling.
- Doctor availability and consultation ownership.
- Medical record creation, modification, and access.
- Prescription and diagnosis documentation.
- Invoice and payment status transitions, if applicable.
- Restrictions on deleting or altering finalized clinical information.
- Handling of incomplete records and invalid input.
- Access restrictions for sensitive information.

Treat these as examples to validate, not automatically applicable policies.

For each confirmed rule, specify:
- Rule identifier and concise name.
- Rule statement.
- Business rationale.
- Trigger or context in which it applies.
- Relevant actors and entities.
- Expected behavior when the rule is satisfied.
- Expected behavior when the rule is violated.
- Related requirements and use cases.

Use precise, testable statements. Avoid vague rules such as "appointments should be managed properly."

Where a policy requires clinical, legal, or organizational authority, do not invent the policy. Mark it for confirmation.

## 2.6 Domain Model and Core Business Entities

Identify the principal business concepts that exist independently of the software implementation.

Consider entities such as:
- Patient.
- Appointment.
- Encounter or Consultation.
- MedicalRecord.
- Diagnosis.
- Prescription.
- Medication.
- Invoice.
- Payment.
- StaffMember.
- ClinicService.

Include only entities justified by the confirmed scope.

For each core entity, describe:
- Its business meaning.
- Why it exists.
- Its relationship to other entities.
- Its lifecycle, if applicable.
- Its key business constraints.
- Whether it represents a person, event, record, transaction, or reference data.

Pay particular attention to the distinction between:
- A patient and that patient's medical record.
- An appointment and an actual consultation.
- A clinical encounter and a diagnosis recorded during that encounter.
- A prescription and the medication items contained in it.
- An invoice and the payment transactions associated with it.

These concepts should not be collapsed into a single entity merely to simplify the diagram.

Required deliverable:
- A Domain Model or Domain Class Diagram.
- A concise entity glossary.
- A relationship table describing meaningful associations and cardinalities where they are established.

Ensure that the domain model captures the business concepts without prematurely introducing database-specific implementation details.

Do not invent attributes or cardinalities merely to fill empty space. If a relationship is uncertain, identify the decision requiring clarification.

## 2.7 Business-to-System Implications

Conclude the analysis by explaining how the findings constrain and guide the system design.

Create a traceability table linking:

| Business Need | Business Rule or Process | Affected Actor | Derived System Capability | Related Requirement or Use Case |

Explain the implications for:
- Functional requirements.
- Non-functional requirements.
- Role-based access control.
- Data integrity and entity relationships.
- Workflow states and transitions.
- Auditability and security.
- Alternative flows and exception handling.

This subsection must synthesize the analysis rather than repeat the preceding sections.

Do not fully reproduce the requirements catalog or detailed Use Case Specifications here. Those belong in Section 3. Provide stable identifiers and cross-references so the reader can follow the reasoning.

### 4. Functional and Non-Functional Requirement Alignment

Although detailed requirements belong in Section 3, Section 2 must provide the business justification for them.

For every major business process, identify the capabilities the system must support.

Functional requirements should express observable system behavior, such as:
- Registering a patient after validating required information.
- Creating an appointment subject to defined availability rules.
- Recording consultation information for an authorized encounter.
- Retrieving medical records according to access permissions.

Non-functional requirements should address measurable quality attributes, such as:
- Confidentiality and authorization.
- Data integrity and auditability.
- Response time under a defined workload.
- Availability and recoverability.
- Usability and maintainability.

Do not invent numeric performance targets without evidence or an explicit project decision. Mark proposed thresholds as targets requiring validation.

In Section 3, assign identifiers such as FR-001 and NFR-001 and define verification criteria for each important requirement.

Avoid duplicating the full requirements catalog in Section 2. Instead, explain why the requirement is needed and which business process or risk justifies it.

### 5. Role-Based Access Control and Permission Analysis

Analyze permissions from both business responsibility and information sensitivity.

For each role, distinguish:
- Read access.
- Create access.
- Update access.
- Delete or archive access, where appropriate.
- Approve or finalize actions, if applicable.
- Administrative configuration permissions.

Create a role-permission matrix in Section 3, supported by the role and process analysis in this section.

Apply the principles of least privilege and separation of duties.

Do not assume that all administrators can read or modify all clinical records. Administrative configuration privileges and clinical data privileges are separate concerns.

Consider relevant access restrictions:
- A doctor should access records required for authorized care.
- Reception staff may need demographic and appointment information without unrestricted access to detailed clinical notes.
- Patients should only access their own information through supported and verified access mechanisms.
- Sensitive actions may require audit logging.
- Unauthorized requests must be rejected without exposing sensitive data.

These are design principles to validate against the project's actual access policy, not a substitute for a formally approved permission specification.

Ensure that every protected function has an identifiable authorization rule and can be verified through test cases.

### 6. Main, Alternative, and Exception Flows

Detailed Use Case Specifications belong in Section 3, but the business analysis must identify realistic paths beyond the ideal scenario.

For every critical workflow, consider:

**Main success flow**
- The normal sequence when all prerequisites are satisfied.

**Alternative flow**
- A legitimate variation that still achieves the business goal or leads to another valid outcome.
- Examples may include rescheduling an appointment, an optional service, or an existing patient's returning visit.

**Exception flow**
- A failure or constraint violation that prevents the intended action.
- Examples may include invalid patient information, an unavailable appointment slot, an unauthorized access attempt, a missing prerequisite, or a failed external service when that integration exists.

For each non-trivial branch, identify:
- The triggering condition.
- The responsible actor or component.
- The expected system response.
- Whether the process resumes, restarts, terminates, or requires intervention.
- The resulting business state.
- Any data changes that must be prevented or rolled back.
- The related business rule and test case.

Do not treat every optional path as an exception. Do not treat every validation failure as a complete system failure.

Ensure that:
- No branch silently bypasses authorization.
- Failed operations do not create inconsistent appointments, consultations, invoices, or medical records.
- A completed clinical record cannot accidentally return to an invalid lifecycle state.
- Cancellation and rescheduling preserve the required history.
- Error messages communicate the next appropriate action without exposing sensitive data.

Detailed flow descriptions must later agree with the corresponding Use Case Specifications, Sequence Diagrams, Activity Diagrams, State Machine Diagrams, and test cases.

### 7. Writing and Presentation Standards

Write in formal, clear academic English suitable for a university System Analysis and Design report.

Use the following principles:
- Start each subsection with its purpose and key conclusion.
- Explain business significance before introducing a diagram.
- Introduce each figure and table in the surrounding text.
- After each major figure, interpret its important relationships and implications.
- Avoid paragraphs that merely restate every label in the diagram.
- Define domain terminology consistently.
- Use concise tables for role comparisons, rule catalogs, entity glossaries, and traceability.
- Avoid unnecessary repetition between Sections 1, 2, and 3.
- Avoid unsupported claims about efficiency gains, reduced errors, compliance, or patient outcomes.
- Keep the level of detail proportional to the project scope.

Every figure should answer a specific question. Every table should support a decision or make an important relationship easier to understand.

### 8. Diagram Quality Requirements

Apply all diagram requirements in the root AGENTS.md, including the blue healthcare technology palette, readable English labels, correct notation, consistent naming, connector spacing, and mandatory visual review.

For business process diagrams:
- Use a clear reading direction.
- Use meaningful action labels.
- Identify responsible roles where this improves clarity.
- Label decision branches with explicit conditions.
- Keep alternate and exception flows visually distinct.
- Avoid ambiguous crossings and overlapping connectors.

If two connectors cross without joining, prefer rerouting them. If a crossing is unavoidable, use a visible curved bridge or jump-over convention when the rendering tool supports it. Never leave an ambiguous crossing that could be mistaken for a connection.

Render every final diagram and inspect it at the size expected in the report. Correct layout, notation, label, arrow, and consistency issues before declaring it complete.

### 9. Quality Assurance Checklist

Before completing Section 2, verify all applicable items:

**Business understanding**
- [ ] The clinic context and project boundary are clear.
- [ ] Existing and proposed workflows are distinguished.
- [ ] Business problems and objectives are supported by project evidence or labeled assumptions.

**Organization and actors**
- [ ] Relevant organizational roles are justified.
- [ ] Responsibilities and authority boundaries are clear.
- [ ] Business stakeholders are distinguished from direct system actors.
- [ ] Actor names are consistent with Section 3.

**Business workflows**
- [ ] Major in-scope workflows are covered from trigger to outcome.
- [ ] Responsibilities and decision conditions are explicit.
- [ ] Alternative and exception paths are realistic.
- [ ] Workflow outcomes agree with domain concepts and lifecycle rules.

**Business rules and domain model**
- [ ] Important business rules have stable identifiers.
- [ ] Each core entity has a clear business definition.
- [ ] Relationships and cardinalities are justified.
- [ ] Appointments, encounters, medical records, prescriptions, and financial records are not conflated.

**Requirements and permissions**
- [ ] Major business needs map to system capabilities.
- [ ] Functional and non-functional implications are identified.
- [ ] Access restrictions follow least privilege.
- [ ] The detailed permission matrix is consistent with role responsibilities.
- [ ] No unsupported numeric target or compliance claim is presented as fact.

**Traceability and presentation**
- [ ] Business needs can be traced into Section 3.
- [ ] Requirements and use cases use stable identifiers.
- [ ] Figures and tables are referenced and interpreted in the text.
- [ ] No unnecessary duplication exists between sections.
- [ ] All diagrams have been rendered and visually inspected.
- [ ] No unresolved critical contradiction remains.

### 10. Definition of Done

Section 2 is complete only when it provides a coherent explanation of how the clinic operates, who participates in its processes, what business rules govern those processes, which domain concepts are required, and how these findings drive system requirements and design.

The section must be detailed without being bloated, realistic without inventing facts, and technically rigorous without becoming difficult for non-technical readers to understand.

Do not mark the section complete merely because every heading contains text. Completion requires evidence-based analysis, cross-section consistency, meaningful diagrams, explicit assumptions, and a successful final review.
# SECTION 3 — SYSTEM REQUIREMENTS ANALYSIS
## Mandatory Authoring Instructions for the AI Agent

### 1. Role and Mission

Act as a senior Requirements Engineer, Business Analyst, and Systems Analyst with expertise in clinic management systems, Electronic Medical Records (EMR), requirements engineering, and UML-based system analysis.

Develop Section 3 of the Medora report to a rigorous academic and professional standard.

**Project:** Medora — Integrated Clinic Management and Electronic Medical Records (EMR) System.

The purpose of this section is to transform the business analysis from Section 2 into a precise, testable, implementation-aware specification of what the system must do and the conditions under which it must operate.

The section must establish:
- A clear and complete functional scope.
- A coherent functional decomposition and Use Case model.
- Detailed, testable functional requirements.
- Measurable or verifiable non-functional requirements.
- Precise Use Case Specifications covering normal, alternative, and exceptional behavior.
- A defensible role-based access control model.
- Traceability from business objectives and business rules to requirements, use cases, design artifacts, and tests.

Do not merely list features or generate diagrams. Explain the reasoning behind the requirements, define their boundaries, identify dependencies and constraints, and establish how compliance will be verified.

Every requirement must be relevant to the confirmed project scope. Do not introduce features solely because they are common in commercial hospital management products.

---

### 2. Source-of-Truth and Preparation

Before writing Section 3, inspect:
- The project brief and confirmed scope.
- Section 1: objectives, scope, assumptions, and constraints.
- Section 2: organizational roles, actors, business workflows, business rules, and domain model.
- Existing source code, API definitions, database schema, and implementation evidence, where available.
- Existing UML diagrams and project decisions.
- The root `AGENTS.md` and all applicable diagram-quality requirements.

Classify each capability as:
- **Confirmed:** explicitly supported by project evidence or an approved decision.
- **Proposed:** included in the intended design but not yet implemented.
- **Assumed:** provisionally adopted because information is missing.
- **Out of scope:** explicitly excluded from the project.

Do not present proposed or assumed behavior as implemented functionality.

When information is missing, make a reasonable, clearly documented assumption if it does not materially alter the system architecture or business rules. Otherwise, identify the unresolved decision and request clarification when necessary.

Do not silently invent:
- Actors or permissions.
- External integrations.
- Medical policies.
- Approval procedures.
- Legal compliance claims.
- Performance measurements.
- API behavior.
- Business rules that have not been established.

---

### 3. Required Section Structure

Use the following structure as the default. Every subsection must contribute meaningful analytical value. Merge genuinely overlapping content rather than writing repetitive paragraphs to fill the outline.

## 3.1 Requirements Overview and Specification Method

Explain how the system requirements are organized and how they relate to the business analysis.

Cover:
- The purpose and boundaries of requirements analysis.
- The sources from which requirements are derived.
- The relationship between business objectives, business rules, actors, and system requirements.
- The requirement classification and identification conventions.
- The prioritization and verification approach.
- The distinction between functional requirements, non-functional requirements, constraints, and assumptions.

Use stable identifiers:
- `BR-001`: Business Rule.
- `FR-001`: Functional Requirement.
- `NFR-001`: Non-functional Requirement.
- `UC-001`: Use Case.
- `TC-001`: Test Case.
- `AC-001`: Acceptance Criterion, if managed separately.

Preserve existing identifiers when updating the report. Never renumber existing requirements without checking every reference.

Explain the prioritization method if one is used, such as Must / Should / Could / Won't. Do not introduce a prioritization scheme unless it will actually be applied.

Deliverables:
- A concise explanation of the specification method.
- A requirements inventory or summary table.
- A clear statement of assumptions and unresolved scope decisions.

Do not turn this subsection into a lengthy theoretical discussion of requirements engineering.

## 3.2 Functional Decomposition and System Boundaries

Organize the system into coherent functional groups based on business capabilities and responsibilities.

Potential groups, subject to scope confirmation, include:
- Patient and Profile Management.
- Appointment and Schedule Management.
- Reception and Visit Management.
- Consultation and Clinical Documentation.
- Medical Records Management.
- Diagnosis and Prescription Management.
- Laboratory or Diagnostic Services.
- Billing and Payment Management.
- Staff and Access Management.
- Reporting and Audit Management.

Do not automatically include every group. Exclude unsupported capabilities and document any proposed extensions separately.

For each functional group, describe:
- Its purpose.
- The business responsibilities it supports.
- Its principal inputs and outputs.
- Its dependencies on other groups.
- The actors who interact with it.
- Its boundary and responsibilities relative to neighboring groups.

### Required diagrams

**A. Functional Decomposition Diagram**

Show a clear hierarchy from the overall Medora system to its major functional groups and, where useful, lower-level capabilities.

Do not confuse functional decomposition with a software package diagram. Functional decomposition describes what the system does; a Package Diagram describes how model elements or software responsibilities are grouped.

**B. Package Diagram**

Use a Package Diagram when it adds value by showing logical grouping, dependencies, and separation of concerns.

Keep dependencies consistent with the intended architecture. Do not use the Package Diagram as a decorative duplicate of the functional hierarchy.

**C. Overall System Boundary**

Clearly distinguish:
- Functionality within Medora.
- External actors and systems.
- Explicitly excluded capabilities.

Deliverables:
- Functional group inventory.
- Diagram-to-requirement mapping.
- Explanation of the most important module boundaries and dependencies.

## 3.3 Actors and Overall Use Case Model

Define the external actors that interact directly with Medora.

For each actor, specify:
- Actor ID, where useful.
- Name and role description.
- Business goal.
- Main system interactions.
- Relevant permission boundary.
- Whether the actor is primary or supporting for important use cases.

Distinguish a direct system actor from a stakeholder who influences requirements but does not interact with the application.

Use consistent actor definitions from Section 2. Do not create a separate actor merely because two users have different job titles if they share the same interaction role and permissions. Conversely, do not merge roles with materially different authorization boundaries.

### Overall Use Case Diagram

The diagram must:
- Show a clearly named Medora system boundary.
- Place external actors outside the boundary.
- Place system use cases inside the boundary.
- Use concise verb-based names for use cases.
- Show justified actor associations.
- Use `<<include>>`, `<<extend>>`, and generalization only when their semantics are correct.
- Remain readable at the final report size.

Do not place internal classes, repositories, controllers, or database tables in a Use Case Diagram.

Do not connect every actor to every use case. Every association must represent a meaningful interaction.

### Use Case Diagrams by Functional Group

Create separate diagrams when the overall model becomes crowded.

Each focused diagram must:
- Identify the functional group or system boundary it represents.
- Include the relevant actors.
- Use exactly the same use case names and identifiers as the overall inventory.
- Preserve relationships that are semantically necessary.
- Avoid duplicating unrelated use cases merely to fill space.

Do not force an arbitrary number of diagrams. Split based on complexity and readability.

## 3.4 Functional Requirements Specification

Define what Medora must do in observable, testable terms.

Assign each requirement a unique `FR-XXX` identifier.

Each important functional requirement should specify:
- Requirement ID and title.
- Requirement statement using precise language.
- Business rationale.
- Relevant actor or initiating event.
- Preconditions or applicable conditions.
- Expected system behavior.
- Resulting data or state changes, where relevant.
- Priority, if an agreed prioritization method is used.
- Related business rules and use cases.
- Verification method or acceptance criteria.

Use statements such as:

"The system shall allow authorized reception staff to create an appointment for an existing patient when the selected doctor and time slot satisfy the configured scheduling rules."

Avoid vague statements such as:

"The system should manage appointments efficiently."

Do not combine multiple independently testable behaviors into a single oversized requirement.

Separate requirements when they have different:
- Actors or authorization rules.
- Preconditions.
- Business outcomes.
- Exception behavior.
- Verification criteria.

However, avoid fragmenting one simple behavior into dozens of meaningless requirements.

### Functional Coverage

For every in-scope functional group, examine:
- Data entry and validation.
- Search and retrieval.
- Creation and modification.
- Cancellation, deletion, archival, or finalization where appropriate.
- Status transitions.
- Authorization checks.
- Error handling.
- Relevant audit records.
- Dependencies on other functions.

Do not assume that all entities support deletion. Medical and financial records may require controlled correction, archival, or retention rather than physical deletion.

Distinguish system behavior from user interface details unless a UI behavior is itself a requirement.

Ensure that all important functional requirements can be traced to a business need, rule, or approved stakeholder decision.

## 3.5 Non-Functional Requirements and Quality Constraints

Specify the quality attributes and operational constraints that determine whether Medora is fit for purpose.

Use stable `NFR-XXX` identifiers.

Consider only applicable categories, including:
- Security and authorization.
- Confidentiality and data protection.
- Data integrity and auditability.
- Performance and responsiveness.
- Availability and recoverability.
- Usability and accessibility.
- Maintainability and testability.
- Compatibility and interoperability.
- Scalability, where justified by the expected deployment.
- Backup, recovery, and operational continuity.

### Quality Requirement Format

Where possible, define each requirement using:
- ID and name.
- Quality attribute.
- Relevant system context.
- Stimulus or operating condition.
- Expected measurable response.
- Measurement or verification method.
- Related implementation or deployment constraint.

For example, a response-time requirement should define:
- The operation being measured.
- The workload or test conditions.
- The response-time threshold.
- The percentile or measurement method, if relevant.
- The test method used to verify compliance.

Do not invent numeric targets and present them as approved requirements. If a target must be proposed, mark it as provisional and explain why it requires confirmation.

For security and privacy:
- Apply least privilege.
- Require authorization checks for protected operations.
- Consider audit logging of sensitive record access and changes.
- Prevent sensitive information from leaking through error messages or logs.
- Define backup and recovery expectations when data persistence is in scope.
- Identify the applicable jurisdiction before asserting legal compliance.

Do not claim that Medora complies with a healthcare regulation, privacy law, or security standard unless the relevant controls and evidence have been verified.

### Non-Functional Acceptance Criteria

Every critical non-functional requirement must have a feasible verification method.

Examples include:
- Automated response-time testing.
- Unauthorized-access tests.
- Data-integrity checks.
- Recovery exercises.
- Usability evaluation.
- Code or configuration review.

Do not describe a quality attribute as "satisfied" without evidence.

## 3.6 Detailed Use Case Specifications

This subsection is a central deliverable of the report.

Create detailed specifications for the use cases that represent the system's major business goals and meaningful risks. Do not stop at the Use Case Diagram.

Each use case must have a stable identifier such as `UC-001`.

### Mandatory Use Case Template

For each important use case, provide:

1. **Use Case ID and Name**
2. **Goal and Brief Description**
3. **Primary Actor**
4. **Supporting Actors**
5. **Trigger**
6. **Preconditions**
7. **Postconditions**
   - Success postconditions.
   - Failure postconditions, where relevant.
8. **Main Success Flow**
9. **Alternative Flows**
10. **Exception Flows**
11. **Business Rules**
12. **Related Functional Requirements**
13. **Data Created, Read, Updated, or Archived**
14. **Authorization Requirements**
15. **Acceptance Criteria or Linked Test Cases**

Do not omit a field merely because it is inconvenient. If a field does not apply, mark it as not applicable and briefly explain why.

### Main Success Flow

Write the normal interaction as numbered steps.

Each step should identify:
- The actor's action or initiating event.
- The system's response.
- The relevant validation or decision, where necessary.

Keep actor actions distinct from system responses. Do not write vague steps such as "The system processes the request."

The flow must be complete enough that a developer and tester can independently understand the intended behavior.

### Alternative Flows

Document legitimate variations that can occur without representing an error.

For each alternative flow, specify:
- The step at which the branch begins.
- The condition that triggers it.
- The alternative sequence.
- The point at which it rejoins the main flow, if applicable.
- The resulting state.

### Exception Flows

Document conditions that prevent the intended operation or require special handling.

For each exception:
- Identify the triggering condition.
- Specify the system's response.
- State whether the operation is rejected, retried, cancelled, or escalated.
- Define what happens to any pending data changes.
- Specify the resulting state and user feedback.
- Identify relevant authorization, logging, and recovery behavior.

Do not confuse an exception with a normal alternative path.

### Use Case Relationships

Use `<<include>>` only when the included behavior is required by the including use case.

Use `<<extend>>` only when optional or conditional behavior extends a valid base use case at an appropriate extension point.

Do not introduce `include` and `extend` relationships solely to make the diagram appear more sophisticated.

### Coverage Requirements

Prioritize detailed specifications for workflows such as:
- Patient registration and identity validation.
- Appointment creation, rescheduling, and cancellation.
- Patient check-in and visit initiation.
- Consultation and clinical record creation.
- Diagnosis and prescription management.
- Billing and payment, if in scope.
- Staff access and role management.
- Medical record retrieval and authorization failures.

These are candidate workflows. Select and specify only those supported by the confirmed project scope.

Ensure that the use cases cover the important business objectives and functional requirements without unnecessary duplication.

## 3.7 Role-Based Access Control Matrix

Translate the responsibilities established in Section 2 into explicit system permissions.

The matrix must be based on business responsibilities, information sensitivity, and the principle of least privilege.

Identify:
- The relevant roles.
- The protected functions or resources.
- The permitted operations.
- The applicable restrictions.
- Any special conditions for sensitive actions.

Use a consistent permission vocabulary, for example:
- `C`: Create.
- `R`: Read.
- `U`: Update.
- `D`: Delete, only where permitted.
- `A`: Approve or finalize, if applicable.
- `—`: Not permitted.

Define the vocabulary clearly. Do not use an empty cell ambiguously.

### Permission Design Rules

- Distinguish administrative configuration from access to clinical records.
- Do not grant unrestricted medical-record access to every administrator by default.
- Distinguish viewing patient demographics from viewing detailed clinical information.
- Specify whether permissions apply to all records or only authorized records.
- Consider restrictions based on staff responsibilities and the relevant care relationship.
- Protect sensitive actions with server-side authorization, not only UI hiding.
- Ensure that unauthorized operations are rejected even if requests are sent directly to an API.
- Define whether record deletion, correction, archival, or finalization is permitted for each relevant role.
- Include audit requirements for sensitive actions where appropriate.

Do not assume that a role can perform an operation merely because its name sounds senior.

### Required Deliverables

Provide:
1. A role-to-function permission matrix.
2. A short explanation of the most important authorization decisions.
3. A mapping from protected requirements and use cases to permission checks.
4. Test scenarios for authorized and unauthorized access.

Ensure that the matrix agrees with the actor definitions, detailed use cases, and implementation model.

## 3.8 Requirements Traceability and Validation

Demonstrate that the requirements form a coherent, verifiable specification rather than an isolated feature list.

Maintain traceability between:
- Business objectives.
- Business rules.
- Functional requirements.
- Non-functional requirements.
- Actors and use cases.
- Design artifacts.
- Test cases and results.

Create a requirements traceability matrix using stable identifiers.

Suggested columns:

| Business Objective / Rule | Requirement ID | Use Case ID | Design Artifact | Test Case ID | Status |

Use `N/A` only when a relationship is genuinely inapplicable, and explain important exceptions.

### Validation Checks

Check that:
- Every major business objective is supported by one or more requirements.
- Every important functional requirement maps to at least one use case or an explicitly justified system-level behavior.
- Every use case has a clear actor or initiating event.
- Every important use case has complete main, alternative, and exception flows.
- Every critical requirement has a verification method.
- Every protected operation has an authorization rule.
- Every important data change is consistent with the domain model.
- No contradictory requirement or duplicated requirement remains.
- All use case names and identifiers match the diagrams.
- No requirement depends on a feature outside the confirmed scope without an explicit assumption or dependency.

Do not claim full coverage if implementation or test evidence is missing. Distinguish requirements that are specified, designed, implemented, and verified.

## 3.9 Section Summary

Conclude Section 3 with a concise synthesis of:
- The principal functional capabilities specified.
- The most important quality and security constraints.
- The resulting Use Case model and authorization structure.
- The coverage and traceability achieved.
- The remaining assumptions or requirements requiring confirmation.

Do not repeat every requirement or use case in prose.

Explain how the resulting specification forms the basis for Section 4's architecture, structural UML diagrams, data model, and detailed design.

---

### 4. Use Case and Requirement Consistency Rules

All models must share a canonical terminology and consistent scope.

Before finalizing:
- Compare actors with the Section 2 stakeholder and actor inventory.
- Compare use cases with the functional decomposition.
- Compare requirements with the business rules.
- Compare data changes with the domain model.
- Compare access rules with the permission matrix.
- Compare use-case branches with the business workflows.
- Identify which use cases need Sequence, Activity, or State Machine Diagrams in Section 4 or Section 5, according to the report structure.
- Ensure that later test cases can verify the expected outcomes.

Do not create a use case for every database operation. Use cases should represent meaningful actor goals or justified system-triggered goals.

Do not create a functional requirement merely by copying a use case title. Requirements define system behavior; use cases organize interactions to achieve goals.

Do not duplicate a complete Use Case Specification in multiple sections. Use stable identifiers and cross-references.

---

### 5. Diagram Standards

Follow the root `AGENTS.md` diagram standards without exception.

Use the consistent blue healthcare technology palette, concise English labels, correct UML semantics, readable typography, clear spacing, and unambiguous connectors.

For Use Case Diagrams:
- Ensure actors are outside the system boundary.
- Ensure use cases are inside the system boundary.
- Use correct relationship direction and notation.
- Avoid unnecessary associations and line crossings.

For functional decomposition:
- Make the hierarchy clear.
- Avoid mixing business capabilities with implementation classes.
- Keep the decomposition consistent with the functional requirements.

For Package Diagrams:
- Show logical grouping and dependencies rather than merely reproducing a feature list.
- Keep dependencies consistent with the intended design.

For all diagrams:
- Avoid connectors that pass through nodes or labels.
- Minimize crossings.
- If unrelated connectors must cross, use a visible curved bridge or jump-over when supported by the rendering tool.
- If bridge crossings are unsupported, reroute the connectors or redesign the layout.
- Keep arrowheads and labels sufficiently separated.
- Render and inspect the actual final output.
- Correct all critical semantic or visual defects before declaring the diagram complete.

A diagram is not validated merely because its source code parses successfully.

---

### 6. Writing and Presentation Standards

Write in clear, formal English suitable for a university System Analysis and Design report.

- Introduce each diagram with its purpose and interpret its key findings.
- Use concise tables for requirement catalogs, actor mappings, and access-control matrices.
- Avoid repeating generic textbook definitions.
- Explain non-obvious design and requirements decisions.
- Use consistent capitalization, terminology, and identifiers.
- Avoid vague phrases such as "easy to use," "high performance," or "secure" without verification criteria.
- Do not invent numerical targets or claim regulatory compliance without evidence.
- Avoid duplicating detailed business-process descriptions from Section 2.
- Keep the report detailed but proportionate to the scope of a clinic-management project.

The objective is precision and traceability, not maximum page count.

---

### 7. Mandatory Quality Assurance Checklist

Before marking Section 3 complete, verify all applicable items.

**Requirements**
- [ ] Every important requirement has a unique, stable identifier.
- [ ] Requirement statements are clear, feasible, and testable.
- [ ] Functional requirements describe observable behavior.
- [ ] Non-functional requirements include measurable or otherwise verifiable criteria.
- [ ] Assumptions and unresolved decisions are explicit.

**Functional decomposition and Use Cases**
- [ ] Functional groups reflect actual project scope.
- [ ] Overall and focused Use Case Diagrams are consistent.
- [ ] Actors and system boundaries are correct.
- [ ] UML relationship semantics and arrow directions are valid.
- [ ] Every important use case has a detailed specification.

**Use Case flows**
- [ ] Main success flows are complete and ordered.
- [ ] Alternative flows are distinguished from exceptions.
- [ ] Exception flows define the response and resulting state.
- [ ] Preconditions and postconditions are consistent.
- [ ] Business rules and data changes are identified.

**Authorization**
- [ ] Every relevant role has justified permissions.
- [ ] Least privilege is applied.
- [ ] Sensitive record access is explicitly considered.
- [ ] Unauthorized operations have defined outcomes.
- [ ] The permission matrix agrees with use cases and requirements.

**Traceability**
- [ ] Business objectives map to requirements.
- [ ] Requirements map to use cases or justified system behavior.
- [ ] Critical requirements have verification methods.
- [ ] Requirements, diagrams, data models, and tests use consistent identifiers.
- [ ] Implemented and verified status is evidence-based.

**Presentation**
- [ ] All diagrams have been rendered and visually inspected.
- [ ] Labels are readable at report size.
- [ ] Connectors, crossings, bridge arcs, arrowheads, and labels are unambiguous.
- [ ] Figures and tables are explained in the surrounding text.
- [ ] No unnecessary duplication or unsupported claims remain.

### 8. Definition of Done

Section 3 is complete only when it provides a precise, internally consistent, and testable specification of Medora's in-scope behavior.

A polished diagram or a long requirement list is not sufficient. The section must demonstrate that the requirements are derived from the business analysis, the use cases define realistic interactions, the access-control model is defensible, alternative and exception paths are addressed, and the resulting specification can guide system design and testing.

If important information remains unknown, document it explicitly. Never hide uncertainty behind invented details or claim complete validation without evidence.
