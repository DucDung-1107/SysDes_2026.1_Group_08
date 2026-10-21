# Report Asset Structure

The folders mirror the structure of `doc/report/paper.tex`. The first level is
the report section, the second level is the subsection or artifact type, and
the last level separates editable source from rendered output.

```text
asset/
├── 00-cover/
│   └── branding/{source,figure}/
├── 01-introduction/
│   └── system-overview/{uml_code,figure}/
├── 02-business-analysis/
│   ├── stakeholders/{code,figure}/
│   ├── business-processes/{uml_code,figure}/
│   └── domain-model/{uml_code,figure}/
├── 03-system-requirements/
│   ├── functional-decomposition/{uml_code,figure}/
│   ├── use-case-model/{uml_code,figure}/
│   └── use-case-by-function/
│       ├── patient-management/{uml_code,figure}/
│       ├── appointment/{uml_code,figure}/
│       ├── medical-examination/{uml_code,figure}/
│       ├── prescription/{uml_code,figure}/
│       ├── billing/{uml_code,figure}/
│       └── administration/{uml_code,figure}/
├── 04-system-design/
│   ├── system-architecture/
│   │   ├── logical-architecture/{uml_code,figure}/
│   │   └── deployment-architecture/{uml_code,figure}/
│   ├── layered-design/
│   │   ├── presentation-business-data/{uml_code,figure}/
│   │   └── api-web-service/{uml_code,figure}/
│   ├── database-design/
│   │   ├── entity-relationship/{uml_code,figure}/
│   │   ├── relational-schema/{uml_code,figure}/
│   │   ├── persistent-mapping/{uml_code,figure}/
│   │   └── constraints-multiplicity/{uml_code,figure}/
│   ├── structural-uml/
│   │   ├── class-diagram/
│   │   │   ├── overall/{uml_code,figure}/
│   │   │   └── by-function/{uml_code,figure}/
│   │   ├── package-component/{uml_code,figure}/
│   │   └── deployment-object/{uml_code,figure}/
│   └── use-case-oriented-design/{uml_code,figure}/
├── 05-behavioral-modeling-implementation/
│   ├── behavioral-uml/
│   │   ├── sequence-by-function/{function}/{uml_code,figure}/
│   │   ├── activity-by-workflow/{uml_code,figure}/
│   │   └── state-machine-by-entity/{uml_code,figure}/
│   ├── source-code-organization/{code,figure}/
│   ├── core-functionalities/by-function/{code,figure}/
│   ├── environment/{code,figure}/
│   └── ui-api-evidence/{ui,api}/{code,figure}/
├── 06-testing/
│   ├── test-strategy-environment/{code,figure}/
│   ├── test-scenarios-cases/{code,figure}/
│   ├── coverage-and-evaluation/{code,figure}/
│   └── execution-evidence/{code,figure}/
├── 07-conclusion-future-work/
│   ├── achievements/figure/
│   ├── limitations/figure/
│   └── future-improvements/figure/
├── appendix/
│   ├── detailed-use-cases/{code,figure}/
│   ├── supplementary-uml/{uml_code,figure}/
│   ├── api-data-schemas/{code,figure}/
│   └── detailed-test-cases/{code,figure}/
└── shared/
    ├── data/
    ├── templates/
    └── figure/
```

## Mapping to `paper.tex`

| Report section | Asset root |
| --- | --- |
| Introduction and Problem Definition | `01-introduction/` |
| Business Analysis and Domain Modeling | `02-business-analysis/` |
| System Requirements Analysis | `03-system-requirements/` |
| System Design and Structural Modeling | `04-system-design/` |
| Behavioral Modeling and System Implementation | `05-behavioral-modeling-implementation/` |
| System Testing and Evaluation | `06-testing/` |
| Conclusion and Future Work | `07-conclusion-future-work/` |
| Appendix | `appendix/` |

## File Rules

- Use lowercase `kebab-case` names, for example `create-appointment.puml`.
- Put editable PlantUML, Mermaid, draw.io, or similar sources in `uml_code/`.
- Put code snippets, JSON examples, SQL, and test data in `code/`.
- Put only generated `.pdf`, `.png`, `.svg`, or screenshots in `figure/`.
- Keep the same base name between source and output, for example:
  `uml_code/create-appointment.puml` and `figure/create-appointment.pdf`.
- Use `by-function` only for product capabilities. Use `by-workflow` or
  `by-entity` when the diagram is organized around a process or lifecycle.
- Put an asset in `shared/` only when it is reused by at least two sections.
- Never put LaTeX build output such as `.aux`, `.log`, `.toc`, or `.pdf` in
  `asset/`; keep those beside the report build or in a dedicated build folder.
- Every PlantUML source includes `shared/templates/medora-style.puml` for the report
  palette, fonts and notation. A figure sets only its own `dpi`, font size and wrap
  width after the include. Run PlantUML with `PLANTUML_LIMIT_SIZE=16384` so large
  diagrams are not clipped at 4096 px.
- `03-system-requirements/functional-decomposition/uml_code/` holds a Python script
  instead of a `.puml`, because PlantUML cannot keep a fixed grid there.
