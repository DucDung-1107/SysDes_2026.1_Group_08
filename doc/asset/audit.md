# MEDORA Figure Audit (AGENT.md Sections 9, 11, 13, 15, 16)

Audit date: 22 October 2026. Scope: all 30 figures under `doc/asset` (27 report figures and 3 UI wireframes).
This file supersedes the status table in `figure-audit.md`.

## 1. Method and evidence

- Every figure was re-rendered from its source (PlantUML 1.2026.8, `PLANTUML_LIMIT_SIZE=16384`; one matplotlib script) and opened as a PNG. No figure was scored from source alone.
- Report-scale legibility was computed from the `\includegraphics` limits in `paper.tex` and each figure's pixel size and font size. Effective text size is now 7.0 pt or larger for every figure (smallest: outpatient journey 7.0 pt, functional decomposition 7.0 pt).
- Cross-artifact checks: use-case IDs against `model-inventory.md`; actor names, role tables and the RBAC matrix in `paper.tex`; state names against the report; spelling (US) across figures and report.
- The two course references in `ref/` were checked through rendered slides: `l04` slides 13 to 54 (analysis classes, sequence, state machine notation) and `l05` slides 13 to 18, 25 to 30, 37 to 42 and 49 to 54 (styles, 4+1 views, component and deployment notation), plus extracted text for the rest. Section 4a records the check.
- Not available and therefore not claimed: the project brief, source code or schema (the report states no application code exists), executed tests. The LaTeX report was not compiled, by decision. Nothing here is marked implemented or verified.

Score bands: 9 = submission-ready; 8 = one meaningful defect remains. Overall = mean of seven dimensions. Dimensions in order: Business and scope (B), UML/notation (U), Flow (F), Consistency (C), Architecture/responsibility (A), Visual (V), Natural writing (N). Critical: B, U, F, C, N.

## 2. Result per figure

| ID | Figure | B | U | F | C | A | V | N | Overall | Decision |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F-01 | Outpatient patient journey | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-02 | Appointment and check-in exceptions | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-03 | Clinical and financial domain model | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-04 | Functional decomposition | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-05 | Overall use-case model | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-06 | Patient management use cases | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-07 | Appointment and queue use cases | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-08 | Check-in and queue use cases | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-09 | Screening and consultation use cases | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-10 | Clinical note and closure use cases | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-11 | Service order and execution use cases | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-12 | Prescription and medication safety use cases | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-13 | Billing and payment use cases | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-14 | Administration and governance use cases | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-15 | SSD Book Appointment | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-16 | SSD Check In Patient | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-17 | SSD Payment and Refund | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-18 | SSD Schedule and Execute Service Order | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-19 | SSD Sign or Amend Clinical Note | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-20 | SSD Validate and Issue Prescription | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-21 | Activity: service order allocation and execution | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-22 | Activity: encounter closure and settlement | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-23 | State: appointment | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-24 | State: encounter | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-25 | State: clinical note version | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-26 | State: procedure order | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-27 | State: invoice | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass |
| F-28 | Wireframe: front desk queue | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass (not used in report) |
| F-29 | Wireframe: clinician encounter | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass (not used in report) |
| F-30 | Wireframe: cashier invoice and payment | 9 | 9 | 9 | 9 | 9 | 9 | 9 | 9.0 | Pass (not used in report) |

All 30 figures pass. Seven of them (F-03, F-06, F-23 to F-27) first stopped at B = 8 because they relied on proposals that nobody had approved. The analyst has since decided each point from `paper.tex`; the decisions are recorded in `model-inventory.md` and listed in Section 4. They are design decisions, not implemented behavior.

## 3. Defects found and corrections made

| # | Defect found in the render | Correction | Recheck |
| --- | --- | --- | --- |
| 1 | All six SSDs: guard `[a b]` was parsed as a PlantUML link. The first word was dropped ("no valid allocation" showed as "valid allocation") and guards were blue underlined links. | Removed the brackets in source (PlantUML adds `[ ]` itself). | Guards read correctly; the earlier meaning reversals are gone. |
| 2 | SSD Payment and Refund: the Finance lifeline ran through the first message; activation bars covered guard labels. | Participant order Cashier, System, Finance; removed activation bars; lighter lifelines; wrapped long messages. | Re-rendered, no label collision. |
| 3 | SSD Payment and Refund: pending branch did not say who approves. | Note now points to UC-066 and the approver permitted in the access matrix. | Matches RBAC footnote and UC-066. |
| 4 | Procedure-order state machine clipped at 4096 px ("cancel" cut off). | Raised the size limit and wrapped transition labels. | Full diagram visible. |
| 5 | Invoice lifecycle: terminal state after Paid contradicted refunds; SSD returns `resultingBalance` but no state change existed. | Added `completeRefund` through a choice pseudostate to Partially Paid or Issued (decision D-4); removed the final state. | Consistent with F-17. |
| 6 | Outpatient journey and booking flow used repeated partitions, a decision outside any lane, and an invented "Coordinator" role. | Rewrote as real swimlanes; removed "Coordinator"; allocation moved to the Doctor's create-and-schedule step (UC-039). | Lanes clean; no boundary straddle. |
| 7 | Activity figures were 4.4 to 4.9 pt at page width. | Wrapped labels, enlarged type, narrower lanes. | Now 7.0 pt or larger. |
| 8 | Encounter closure: a "no payment" box straddled two lanes. | Moved both payment outcomes into the Cashier lane. | Clean. |
| 9 | Overall use-case model: abstract actor without real generalization; tangled lines. | Removed the abstract actor; UC-001 kept with a note; actors reordered. `model-inventory.md` updated to match. | No crossings. |
| 10 | Patient management: UC-017 "included" UC-016, which is a precondition, not mandatory behavior. | Removed the include and added a note. | Include/extend checklist satisfied. |
| 11 | `extend` links had no condition; Issue Receipt extended Record Payment without a basis. | Added conditions to UC-047, UC-062, UC-078; made UC-064 a plain Cashier association. | Section 13.1 satisfied. |
| 12 | Service order use cases included two sub-behaviors without IDs. | Assigned UC-056 and UC-057; inventory range updated. | IDs consistent. |
| 13 | Domain model had no Invoice, Payment or Refund although the report keeps them separate. | Added the three entities and relationships (decision D-2); caption and text updated in `paper.tex`. | Matches inventory and BR financial history. |
| 14 | Functional decomposition: staggered, unaligned auto-layout. | Redrawn as a fixed 2 by 3 grid (`functional-decomposition.py`, replaces the `.puml`). | 7.0 pt, aligned. |
| 15 | Authorized Registrar and Authorized Finance Staff were used in figures but missing from the canonical actor table. | Added both to `model-inventory.md`; added Registrar rows and a merge row with footnote to `paper.tex` role and RBAC tables. | Figures, inventory and report agree. |
| 16 | State names appeared in no inventory; the WorkStation draft lists a status field only for Appointment. | Added a status inventory to `model-inventory.md` and a lifecycle-states table to `paper.tex` (decision D-3). | Names match all five figures. |
| 17 | Wireframes: queue states ("Screening", "In service") did not exist in the encounter lifecycle; cashier screen offered Refund review and Request adjustment; clipped button; low resolution. | Used canonical states; removed the refund and adjustment controls from the Cashier screen; payment disabled until invoice is issued; fixed fields; 192 dpi. | Cashier cannot refund, as in the RBAC matrix. |
| 18 | Mixed US and UK spelling ("authorized" and "authorised"). | Normalised to US across all sources and `paper.tex`. | No UK forms left. |
| 19 | Stale SVG and cmapx outputs (8 and 6 files) next to current PNGs. | Deleted; the report uses PNG only. | Only current outputs remain. |
| 20 | Business-rule IDs disagreed: the report table had seven rules with finance as BR-007, while the inventory, the traceability table and the appendices use ten rules with finance as BR-009. | Replaced the report table with the ten canonical rules and remapped the references to BR-009 and BR-010. | All BR references now use one numbering. |
| 21 | Refund approval did not say who approves. | Decision D-5: a Manager or Authorized Finance Staff member who is not the requester; added to the SSD note, inventory and RBAC footnote. | Matches RBAC footnote b and Section 14.1 (no self-approval). |
| 22 | Style check against `ref/l04` (message notation): calls and returns used the same arrowhead, and messages had no sequence number. | Calls keep the solid arrowhead; returns are dashed with an open arrowhead (`-->>`); every SSD message is numbered (`autonumber`), which is the "[sequence number]" part of the message specification. | Payment/Refund SSD re-opened: arrowheads and numbers match the lecture notation. |
| 23 | Style check against the palette (AGENT.md Section 5): actors rendered in PlantUML's default lilac, notes in default yellow, state boxes had an empty second compartment, titles mixed "MEDORA --" and "MEDORA:". | One shared style file, `shared/templates/medora-style.puml`, now defines colors, fonts, actors, notes, states, activities and sequence frames. All 27 PlantUML diagrams include it and keep only their own dpi, font size and wrap width. Titles use "MEDORA: ...". | Use-case, state, activity, domain and sequence figures re-opened: one color identity, no default colors left. |
| 24 | Domain model showed PlantUML's green class icon, which clashes with the ECB class notation of `ref/l04` (stereotype in a compartment, no icon). | `hide circle`; the stereotype `<<entity>>` stays in the header, as on the lecture's analysis-class slides. | Re-opened. |
| 25 | The three salt wireframes could not use the shared file; one had mis-parsed buttons. | Colours set inline; buttons separated with `|`. | Re-opened. |

## 4. Decisions taken from the report

The owner asked the analyst to decide from `paper.tex`, keeping the model consistent. Each decision is recorded in `model-inventory.md` with its basis.

| ID | Decision | Basis in the report | Applied in |
| --- | --- | --- | --- |
| D-1 | Authorized Registrar is a role; only it performs a record merge; a Receptionist may request one. | BR-001, FR-004, TC-002 (registrar and receptionist accounts) | F-06, role tables, RBAC matrix (merge row) |
| D-2 | Refund is its own entity referring to one original Payment; an Encounter has at most one Invoice. | BR-009; "one invoice may have many payment, refund or adjustment transactions" | F-03 |
| D-3 | The listed lifecycle states are the canonical statuses. | Section 2 distinctions; Section 5 models | F-23 to F-27, state table in the report |
| D-4 | A completed refund returns the Invoice to Partially Paid or Issued by net paid amount; a pending refund changes nothing. | BR-009; SSD: balance reported only after completion | F-27, F-17 |
| D-5 | A refund needing approval is approved by a Manager or Authorized Finance Staff member who is not the requester. | RBAC footnote b; Section 14.1 | F-17, inventory, RBAC footnote |

## 4a. Conformance with the course reference (`ref/`)

Checked: `l04-Analysis & Design.pdf` (54 slides) and `l05-Architecture (1).pdf` (56 slides); the slide ranges are listed in Section 1.

| Reference rule | Where it applies | Status |
| --- | --- | --- |
| Sequence diagram: lifelines from the top down, solid call with filled head, dashed return with open head, activation optional, `alt`/`opt`/`loop` with `[guard]`, message spec `[number][guard]: name(params)` | F-15 to F-20 | Met after correction 22 |
| System sequence diagram: actor to system only | F-15 to F-20 | Met: actors and one MEDORA System lifeline |
| State machine: rounded states, black start dot, bullseye end, `event(params) [guard]/action`, stable finite states | F-23 to F-27 | Met; transition labels are split over lines for width but keep the order event, guard, action |
| Use case diagram: actors outside, ellipses inside a named system boundary, dashed include/extend with conditions | F-05 to F-14 | Met after corrections 9 to 11 |
| Package, component and deployment notation (`l05`) | Section 4 of the report | Not applicable yet: see below |
| ECB analysis classes (boundary, control, entity icons), analysis sequence with boundary, control, entity lifelines, unified class model | Section 4, "Use Case-oriented Design" | Not applicable yet: see below |

**Gap that no figure edit can close.** Section 4 of `paper.tex` ("System Design and Structural Modeling") contains only empty headings. Appendix B records that it is reserved for the Chapter 4 contributor, so this audit does not fill it. The headings are: logical and deployment architecture, layered design, API design, ERD and schema, class diagrams, package, component and deployment diagrams, and use-case-oriented design. The report therefore has no ECB analysis, no architecture decision record and none of the structural diagrams that `l04` and `l05` teach. The 30 existing figures conform to the reference; the missing ones are missing, not wrong. AGENT.md Section 16 gates 3 (traceability) and 6 (architecture) cannot pass until this section is written.

## 4b. Prose review of `paper.tex` and `appendices.tex`

Method: the full text was read once for naturalness (AGENT.md Section 9), then every hedge or disclaimer was marked in red with `\weak{...}` (defined in the preamble) so the author can decide whether to keep, soften or remove it. The report was not compiled; brace balance of all red spans was checked by script.

| Change | Count |
| --- | --- |
| Spelling unified to US English across the report, the appendices, all figure sources and the inventory (behavior, center, catalog, canceled, organize, modeled and similar). A side effect, "catalogd", was found and fixed. | whole set |
| Repeated or promotional sentences rewritten: the waiting-time statistics stated twice, "safer today and easier tomorrow", the repeated MVP scope sentence, "is important" filler, claims that cited nothing | 9 |
| Statements that were wrong after earlier corrections: the report said no wireframes existed (three wireframe figures are now in Section 5.4); the state machines were said to formalize WorkStation statuses that WorkStation does not name; the functional decomposition heading promised a package structure it does not show; the business-process table caption promised a baseline it does not contain; the entity glossary omitted Refund | 5 |
| Conclusion and limitation paragraphs recast: results first, then the limit, each in short sentences | 3 |
| Disclaimers marked in red | 68 in `paper.tex`, 22 in `appendices.tex` |

What the red marks cover: "not implemented", "no application code", "Not Run" and "Not measured" status cells, "cannot establish" sentences, "not claims of production performance", "pending approval", "to be decided" and open-decision sentences. They are honest statements required by AGENT.md Section 11, so none was deleted; a few redundant ones were removed instead (for example "are not implementation claims" after use cases that never claimed implementation).

Not changed: the requirement tables use grouped rows, not the per-requirement fields that AGENT.md Sections 12.2 and 12.3 describe. This is structure, not wording, and is the author's call.

## 5. Remaining limits (not defects)

- The sequence diagrams show the dashed lifeline faintly behind a few guard labels. This is the PlantUML layout; the lines are light and do not hide any text.
- The payment idempotency key is a proposed control (NFR-15), not an existing API contract. The report already says so.
- The design decisions D-1 to D-5 are the analyst's, taken on the owner's instruction; a clinic stakeholder has not reviewed them.
- All figures describe specified or designed behavior. None shows implemented or verified behavior.

## 6. Section 16 gate

| Gate | Status |
| --- | --- |
| 1 Scope | Yes: every non-confirmed point is a recorded design decision (D-1 to D-5) |
| 2 Terminology | Yes: actors, UC IDs, BR IDs, states and spelling reconciled with inventory and report |
| 3 Traceability | Partly checked: IDs and ranges match; the report lists UC ranges, not each UC name |
| 4 Authorization and integrity | Yes for refund, merge and cashier rules; the RBAC matrix is now consistent with figures |
| 5 UML semantics | Yes: checklists in 13.1 to 13.5 applied |
| 6 Architecture | No: Section 4 of the report is empty, so no architecture figure exists to check |
| 7 Render | Yes: all 30 opened; effective text 7.0 pt or larger |
| 8 Scores | Yes: all 30 at 9.0 overall, all critical scores at 9 |
| 9 Honesty | Yes: nothing is claimed implemented or verified |

The figure set is submission-ready. The report as a whole is not submission-ready: Section 4 is empty (gate 6 fails, gate 3 is partial), it has not been compiled, and the design decisions above still need a reviewer who owns the clinic scope to confirm them.
