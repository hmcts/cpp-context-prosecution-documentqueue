# Workflow: Mandatory Build Loop

Every non-trivial code change MUST follow this cycle:

```
Spec → Write → Code Review (agent) → QA (agent) → Spec Validate (agent) → Fix → Ship
```

- **Spec:** Update RAML / JSON schemas / `subscriptions-descriptor.yaml` / `public-publications-descriptor.yaml` / `event-sources.yaml` BEFORE writing Java (Constitution Principles I + VI)
- **Spec Validate:** Run `spec-validator` agent to check RAML and JSON-schema consistency with both `subscriptions-descriptor.yaml`, the `public-publications-descriptor.yaml`, `event-sources.yaml`, and the Java handler / listener / processor mappings

Loop repeats until ALL agents return PASS / COMPLIANT.

## What Requires the Loop

| Must Go Through Loop                                     | Exempt                         |
|----------------------------------------------------------|--------------------------------|
| New / modified RAML or JSON schema                       | Markdown / docs only           |
| New / modified `subscriptions-descriptor.yaml` entry     | Whitespace / import only       |
| New / modified `public-publications-descriptor.yaml`     | CLAUDE.md and rule updates     |
| New / modified Java handler / listener / processor       | README changes                 |
| New / modified converter class                           |                                |
| Liquibase changelog                                      |                                |
| Aggregate change (`QueueDocument` / `CPPCase` / `DocumentsExpiration` / `DeleteDocuments`) | |
| `event-sources.yaml` change                              |                                |
| `resource-descriptor.yml` change                         |                                |
| Azure DevOps pipeline config (`azure-pipelines.yaml`)    |                                |
| `pom.xml` version pin bump (cross-context)               |                                |

## TDD is Non-Negotiable (Constitution Principle VIII)

- Write the failing test first; confirm it fails for the *correct* reason (assertion failure, not a compilation error)
- Then write the minimum production code to make it pass
- Then refactor with the test still green
- Commit history MUST show the failing test was authored at or before the production code

## Three-Layer Discipline (Constitution Principle II)

For any event-touching change, the plan and PR description MUST list which of the three layers are touched:

1. Command side (handler → aggregate → domain event)
2. Event listener (projects events → viewstore `java:/DS.documentqueue`)
3. Event processor (consumes domain events + inbound public events → publishes `public.documentqueue.*`)

The other two are either changing in lockstep or explicitly out-of-scope with reasoning. A silent "command-only" change to an event shape is the most common source of incidents on this service.

## Schema-Subscription Symmetry (Constitution Principle VI)

Every event addition / removal / rename MUST update both:
- The subscription/publication descriptor (`subscriptions-descriptor.yaml` listener/processor; `public-publications-descriptor.yaml` for published events)
- The matching JSON schema, in the **correct namespace** — multiple are in use (`justice.gov.uk/json/schemas/domains/documentqueue/event`, `justice.gov.uk/json/schemas/documentqueue`, `moj.gov.uk/cpp/documentqueue/event`)

A subscription/publication without a matching schema produces a runtime 500 on dispatch.

## Agent Definitions

### code-reviewer (Read only)
- Spawned as sub-agent with Read-only tools
- Analyses code for: secrets, layering violations, framework idiom misuse, schema-subscription/publication mismatches, wrong-namespace `schema_uri`, three-layer breakage, `System.out` usage, Spring-DI/Lombok usage
- Returns: **PASS** or **NEEDS CHANGES** with severity-rated findings
- NEVER modifies code — reports only

### qa (Read, Write, Bash)
- Spawned as sub-agent
- Generates unit + integration tests (JUnit + Mockito; framework's IT harness; Cucumber)
- Verifies TDD discipline (failing test authored before production code)
- Runs `mvn test` (and `./runIntegrationTests.sh` when feasible)
- Returns: **PASS** or **FAIL** with test results
- NEVER fixes production code — only writes tests

### software-engineer (Full access)
- For full feature implementation tasks
- Follows all rules in `technical-rules.md` and the constitution
- Updates contract files BEFORE Java
- Runs `mvn clean install` after changes; `./runIntegrationTests.sh` when event-touching

### spec-validator (Read only)
- Spawned as sub-agent with Read-only tools
- Reads RAML, JSON schemas, both `subscriptions-descriptor.yaml`, `public-publications-descriptor.yaml`, `event-sources.yaml`, and Java handler / listener / processor / converter files
- Checks: contract↔implementation symmetry, schema-subscription/publication pairing (incl. namespace), three-layer integrity, framework idiom compliance, public-event shape correctness
- Returns: **COMPLIANT** or **DRIFT DETECTED** with severity-rated findings
- NEVER modifies code — reports only

### research (Read, Glob, Grep, WebSearch)
- For deep codebase investigation
- Cross-references contract artefacts with code
- Returns structured findings with citations

## Critical Principle

**Agents are reporters, not fixers.** The parent agent (or developer) reads agent reports and applies all fixes. This prevents conflicting changes and keeps the team in control.
