<!--
SYNC IMPACT REPORT
==================
Version change: (uninitialised template) → 1.0.0
Bump rationale: Initial ratification. All principles and sections are new; no
                prior principles to remove or redefine, so MAJOR is the correct
                starting point (1.0.0).

Modified principles: N/A (initial ratification).

Added sections:
  - Core Principles
      I.    RAML / JSON-Schema Contract First
      II.   CQRS Three-Layer Discipline (Command / Listener / Processor)
      III.  CPP Framework Idioms — No Manual Rolling
      IV.   Spec-Driven Build Loop
      V.    HMCTS CPP Standards Compliance
      VI.   Schema-Subscription Symmetry
      VII.  No System.out / System.err — SLF4J Only
      VIII. Test-Driven Development
  - Technology Stack & Deployment
  - Development Workflow & Quality Gates
  - Governance

Removed sections: None.

Templates requiring updates:
  - .specify/templates/plan-template.md       ✅ compatible — the "Constitution
      Check" block is filled per-feature by `/speckit-plan`. Plan authors MUST
      gate on Principles I–VIII.
  - .specify/templates/spec-template.md       ✅ compatible.
  - .specify/templates/tasks-template.md      ✅ compatible — task ordering
      already encodes "tests before implementation", aligning with VIII.
  - .specify/templates/checklist-template.md  ✅ compatible.
  - README.md / CLAUDE.md / docs/*            ✅ aligned — `.claude/rules/*.md`
      encodes these principles informally; this constitution is now the
      authoritative source.

Follow-up TODOs: None. All placeholders resolved.
-->

# cpp-context-prosecution-documentqueue Constitution

## Core Principles

### I. RAML / JSON-Schema Contract First (NON-NEGOTIABLE)

The contracts of this service — commands it accepts, queries it answers, domain
events it emits, public events it publishes, and public events it consumes — are
defined in **RAML files and JSON schemas**. Those artefacts are the source of
truth. Java handler signatures, listener mappings, and processor mappings MUST
follow the contracts; the contracts MUST NOT be inferred from the Java code.

For every command/event change you MUST update:

1. The RAML file — `documentqueue-command-api.raml` (HTTP API),
   `documentqueue-command-handler.messaging.raml` (media-type → command mapping,
   e.g. `application/vnd.documentqueue.command.attach-document+json`),
   `documentqueue-query-api.raml` (read API), and the relevant
   `subscriptions-descriptor.yaml` / `public-publications-descriptor.yaml`.
2. The matching JSON schema referenced by the contract's `schema_uri` —
   **mind the namespace** (see Principle VI; multiple are in use).
3. The `event-sources.yaml` if a new internal/public topic is involved.
4. Then — and only then — the Java handler / listener / processor.

**Rationale**: the framework dispatches commands and events by matching the
RAML/`schema_uri` contract against `@Handles` annotations. A drift between the
contract and the Java code produces a runtime 500 (no matching schema) or, worse,
silent message-loss with no logging. The contracts are also consumed across the
context boundary (stagingbulkscan, prosecutioncasefile, progression,
stagingprosecutorsspi); treating them as documentation rather than
source-of-truth produces cross-context incidents.

### II. CQRS Three-Layer Discipline (NON-NEGOTIABLE)

Every change touching events MUST be reasoned about across **all three
layers**:

```
Command side (handler → aggregate → domain event)
    ↓ writes events to DS.eventstore → topic documentqueue.event
Event listener (projects events → viewstore tables)
    ↓ projects to java:/DS.documentqueue
Event processor (consumes domain events + a large set of inbound public events)
    ↓ publishes public.documentqueue.* events
```

Commands arrive on the `documentqueue.handler.command` queue, are dispatched by
`@Handles` to a handler, which asks the aggregate (`QueueDocument`, `CPPCase`,
`DocumentsExpiration`, `DeleteDocuments`) to perform the command; the aggregate
emits domain events and rebuilds state via `apply(...)`. Adding or modifying a
domain event WITHOUT updating both the listener and the processor (where each
consumes it) is a Principle II violation. Plan authors MUST list which of the
three layers a change touches and confirm the other two are either unaffected
(with reasoning) or carry a paired change in the same PR.

**Rationale**: this service is predominantly a *reactive* context — its event
processor is driven by inbound public events from stagingbulkscan,
prosecutioncasefile, progression, and stagingprosecutorsspi, and it both updates
its own document-queue read model and republishes status to downstream
consumers. A broken layer silently corrupts the queue state that other contexts
poll and react to.

### III. CPP Framework Idioms — No Manual Rolling (NON-NEGOTIABLE)

This service is built on the Justice Services Framework
(`uk.gov.moj.cpp.common:service-parent-pom`). Use the framework's idioms rather
than rolling your own:

- Command handlers: `@ServiceComponent(COMMAND_HANDLER)` + `@Handles(...)` on a
  method taking `Envelope<CommandPayload>`.
- Aggregate state: route mutations through the aggregate's `apply(event)` replay;
  never mutate read-model state from the command side.
- Event listeners: extend the framework's listener bases; map events → viewstore
  entities via dedicated converter classes.
- Event processors: extend the framework's processor bases; map domain events →
  public-event payloads via dedicated converter classes; consume inbound public
  events the same way.
- Persistence: Liquibase changelogs only — never manual DDL.
- Outbound: use the framework's REST/messaging client wiring; publish public
  events via the `public-publications-descriptor.yaml` contract.

**Dependency injection**: use CDI (`@ApplicationScoped` / `@Inject`). This
service does **not** use Spring (`@Autowired`, `@Component`, `@Service` are
forbidden) and does **not** use Lombok — keep constructors and accessors
explicit, consistent with the existing code.

**Forbidden**: hand-rolled JMS listeners, hand-rolled JDBC, ad-hoc ObjectMapper
instances, manual schema validation, Spring DI. The framework already solves
these and rolling your own diverges from the rest of the CPP estate.

**Rationale**: every CPP service follows these idioms, so cross-service
maintenance and operability depend on consistency. A bespoke pattern in one
service makes the next maintainer reach for the wrong mental model.

### IV. Spec-Driven Build Loop (NON-NEGOTIABLE)

Every non-trivial change MUST flow through the cycle:

```
Spec → Write → Code Review → QA → Spec-Validate → Fix → Ship
```

The reviewer agents (`code-reviewer`, `qa`, `spec-validator`) report findings
only; they MUST NOT modify code. The primary agent or a human applies fixes,
then re-runs the loop until all three return PASS / COMPLIANT. The
`spec-validator` here checks that RAML and JSON-schema files are consistent with
both `subscriptions-descriptor.yaml` files, the `public-publications-descriptor.yaml`,
`event-sources.yaml`, and the Java handler / listener / processor / converter
mappings. Changes exempt from the loop: markdown-only edits, whitespace or
import-only edits, `.claude/rules/*` and `CLAUDE.md` rule updates.

**Rationale**: keeps a human (or primary agent) as the decision point; prevents
conflicting auto-fixes; preserves auditable, reproducible review output.

### V. HMCTS CPP Standards Compliance (NON-NEGOTIABLE)

- **Build tool**: Maven (current). Module layout, version management, and CI all
  assume the Maven reactor; a future migration to Gradle is allowed but is
  itself a constitution-amendment-scale change and MUST update this section, the
  rule files, the agent docs, and the CI pipeline in lockstep.
- **Java**: 17.
- **Parent**: `uk.gov.moj.cpp.common:service-parent-pom:17.104.x` — pin updates
  require a coordinated cross-context check against the upstream pins in the root
  `pom.xml` (`material`, `progression`, `bulkscan`, `referencedata`,
  `coredomain`, `system.id-mapper`, `system.enterprise-id`, `cpp.structure`).
- **Packaging**: WAR deployed to WildFly via Docker. The `documentqueue-service`
  module is the packaging WAR; `src/main/descriptors/resource-descriptor.yml`
  wires datasources / the command queue / topics / service mapping (matcher
  `/documentqueue-[^/]+`).
- **Tests**: JUnit + Mockito for unit tests (surefire); integration tests in
  `documentqueue-integration-test` orchestrated by `runIntegrationTests.sh`
  (Docker-based WildFly + Postgres + ActiveMQ + WireMock; Cucumber). ITs require
  `CPP_DOCKER_DIR` pointing at a local checkout of `hmcts/cpp-developers-docker`.
  `runLiquibase.sh` applies changelogs across event log, event-log aggregate
  snapshot, event buffer, viewstore, system, event tracking, and file service.
- **CI/CD**: Azure DevOps (`azure-pipelines.yaml`) using shared
  `hmcts/cpp-azure-devops-templates`: PR builds run `context-verify`; CI builds
  run `context-validation` with `serviceName=documentqueue` and
  `itTestFolder=documentqueue-integration-test`. SonarQube project
  `uk.gov.moj.cpp.prosecution.documentqueue:documentqueue-parent`. `main` is the
  develop branch; `dev/release-*` branches are excluded (jgitflow).
- **Quality gate**: SonarQube — coverage, duplication, smells. No local
  Checkstyle / PMD enforcement at build time.

**Rationale**: aligns this service with the rest of the CPP estate (naming,
build, deploy, test, observability conventions) so cross-team maintenance,
on-call rotation, and platform upgrades work uniformly.

### VI. Schema-Subscription Symmetry (NON-NEGOTIABLE)

When you add, remove, or rename a domain or public event you MUST update **all**
of the relevant contracts in lockstep:

- The listener `subscriptions-descriptor.yaml`
  (`documentqueue-event/documentqueue-event-listener/src/yaml/...`) and/or the
  processor `subscriptions-descriptor.yaml`
  (`.../documentqueue-event-processor/src/yaml/...`) for consumed events (own
  domain events AND inbound public events from bulkscan / PCF / progression / spi).
- The processor `public-publications-descriptor.yaml` for **published** public
  events (e.g. `public.documentqueue.document-status-updated`).
- The matching JSON schema referenced by the `schema_uri`.

**Multiple `schema_uri` namespaces are in use** — at least
`http://justice.gov.uk/json/schemas/domains/documentqueue/event/...`,
`http://justice.gov.uk/json/schemas/documentqueue/...`, and
`http://moj.gov.uk/cpp/documentqueue/event/...`. A new event MUST use the same
namespace its sibling schema file is actually published under; a copy-pasted
`schema_uri` pointing at the wrong namespace fails dispatch.

A subscription or publication without a matching schema produces a runtime 500
on dispatch. A schema without a subscription/publication is dead code that
drifts silently as the event evolves.

**Rationale**: this service both consumes (heavily) and publishes events across
the JMS topics and the cross-context boundary, under several schema namespaces.
A missing or mismatched contract is the most common source of incidents on this
service. Encoding it as a NON-NEGOTIABLE principle makes it a review-blocker.

### VII. No `System.out` / `System.err` — SLF4J Only (NON-NEGOTIABLE)

Code MUST NOT use `System.out.println`, `System.err.println`, or
`Throwable#printStackTrace()`. All diagnostic output goes through SLF4J
(`org.slf4j.Logger` via `LoggerFactory.getLogger(...)`). This applies to
production code AND tests.

**Rationale**: container logs are aggregated and structured; stdout prints
bypass the framework's MDC (correlation id propagation through the `Envelope`
metadata) and the platform log shipping. They vanish from operations and surface
as noise in CI.

### VIII. Test-Driven Development (NON-NEGOTIABLE)

Red → Green → Refactor for every behaviour change.

1. Write the failing test first. It MUST run and fail for the *correct* reason —
   the assertion, not a missing class or compilation error.
2. Write the minimum production code to make it pass.
3. Refactor with the test still green.

PRs MUST show that the test was authored at or before the production code
(commit history or paired-commit are both acceptable). The `qa` reviewer agent
gates on this — production code without an accompanying failing-then-passing
test is FAIL.

Exempt: pure mechanical refactors (rename, move, extract with no behaviour
change), formatting, comment-only edits.

**Rationale**: the regression surface of this service is wide — four aggregates,
a document lifecycle (outstanding → in-progress → completed → deleted/expired),
file-store deletion, and a large fan-in of inbound public events from four other
contexts. Only fail-first tests catch the class of bug where a converter
silently drops a field or a document is left in the wrong queue state.

## Technology Stack & Deployment

- **Java**: 17.
- **Build**: Maven. Multi-module reactor; modules listed in root `pom.xml`
  (`documentqueue-parent`, groupId `uk.gov.moj.cpp.prosecution.documentqueue`).
- **Framework**: Justice Services Framework / CPP `service-parent-pom:17.104.x`.
  CDI; `@ServiceComponent` / `@Handles` annotations. No Spring, no Lombok.
- **Packaging**: WAR (`documentqueue-service`) → WildFly (Docker).
- **Persistence**: Liquibase changelogs (event store, aggregate snapshot, event
  buffer, viewstore, event tracking, file service).
- **Messaging**: ActiveMQ (Docker for ITs); JMS queue `documentqueue.handler.command`
  and topics `documentqueue.event` + `public.event`.
- **Data stores**:
  - `java:/app/documentqueue-service/DS.eventstore` — event store.
  - `java:/DS.documentqueue` — viewstore (read model).
- **Domain**: aggregates `QueueDocument`, `CPPCase`, `DocumentsExpiration`,
  `DeleteDocuments`.
- **Tests**: JUnit + Mockito (unit, surefire); `runIntegrationTests.sh` Dockerised
  IT harness (WildFly + Postgres + ActiveMQ + WireMock; Cucumber).
- **Logging**: SLF4J + the framework's logger configuration; MDC keys carried
  through `Envelope` metadata.
- **CI/CD**: Azure DevOps via `azure-pipelines.yaml` + shared
  `hmcts/cpp-azure-devops-templates`. PR = `context-verify`. CI build =
  `context-validation`. SonarQube project
  `uk.gov.moj.cpp.prosecution.documentqueue:documentqueue-parent`. `dev/release-*`
  branches excluded.
- **Quality gate**: SonarQube — coverage thresholds, duplication, smells
  enforced in CI; no local equivalent at build time.

## Development Workflow & Quality Gates

- **Contract files** (RAML, JSON schemas, both `subscriptions-descriptor.yaml`,
  `public-publications-descriptor.yaml`, `event-sources.yaml`) MUST be updated
  **before** the matching Java change (Principle I + VI).
- The build loop (Principle IV) repeats until `code-reviewer`, `qa`, and
  `spec-validator` each return PASS / COMPLIANT.
- TDD (Principle VIII) MUST be visible in commit history — the failing test
  commit precedes (or is paired with) the production code that satisfies it.
- Every feature built via spec-kit lives under `specs/<JIRA-ID>-slug/`
  (or `specs/NNN-slug/` if not Jira-tracked) containing at least `spec.md`,
  `plan.md`, and `tasks.md`. Flow:
  `/speckit-specify → /speckit-plan → /speckit-tasks → /speckit-implement
  → /speckit-analyze`.
- Required commands run cleanly before merge:
  - `mvn clean install` — full build + unit tests, green.
  - `./runIntegrationTests.sh` — Dockerised IT run, green (when changes touch
    handlers / listeners / processors / converters / schemas).
  - SonarQube quality gate in CI — passing.
- Commit style: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`,
  `refactor:`).
- Pull requests: the description MUST state which principle(s) the change
  touches. Any deviation from a principle requires explicit written
  justification in the PR description and MUST be flagged in the plan's
  "Complexity Tracking" section.
- Branch naming: Jira-prefixed (`DD-XXXXX-feature-slug`) — the speckit
  `before_specify` hook auto-creates these via `/speckit-git-feature`.

## Governance

This constitution supersedes the informal conventions in `.claude/rules/`.
Where this document and those files disagree, this document wins; the rule files
are retained as quick-reference material and MUST be kept in sync.

**Amendment procedure**:

1. Propose the change in a feature spec under `specs/`.
2. Bump `Version` per semantic versioning:
   - **MAJOR** — a breaking principle change, removal, or redefinition that
     invalidates existing practice.
   - **MINOR** — a new principle, new section, or materially expanded guidance.
   - **PATCH** — clarifications, wording, typo fixes, or non-semantic
     refinements.
3. Re-run `/speckit-analyze` on every in-flight feature spec to verify it still
   aligns with the amended principles; update or waive as required.

**Compliance expectations**:

- All PRs MUST honour these principles.
- Deviations MUST be explicitly justified in the PR description and, where
  relevant, in the plan's "Complexity Tracking" table.
- Reviewers MUST block merges that silently violate a NON-NEGOTIABLE principle
  without a written waiver.

**Version**: 1.0.0 | **Ratified**: 2026-06-02 | **Last Amended**: 2026-06-02
