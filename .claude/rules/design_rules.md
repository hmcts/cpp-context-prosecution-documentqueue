# Architecture & Domain Rules

## Three Layers (CQRS / Event-Sourced)

```
1. Command side (handler → aggregate → domain event)
       ↓ writes to event store (java:/app/documentqueue-service/DS.eventstore)
       ↓ published to JMS topic documentqueue.event

2. Event listener (projects events → viewstore tables)
       ↓ projects to java:/DS.documentqueue

3. Event processor (consumes domain events + inbound public events → publishes public events)
       ↓ public.documentqueue.* on public.event
```

Every change touching events MUST be reasoned about across **all three layers**. Breaking one without the others produces silent data drift — and here the queue read-model that other contexts react to goes wrong.

- **Command side** — commands arrive on the `documentqueue.handler.command` queue, dispatched by `@Handles` to handler classes which ask the aggregate to perform the command; the aggregate emits domain events. State is rebuilt by replaying events via `apply(...)`.
- **Event listener** — projects domain events into the viewstore (`java:/DS.documentqueue`). Lives under `documentqueue-event/documentqueue-event-listener`. Converters map events → viewstore entities.
- **Event processor** — consumes domain events AND a large set of inbound public events from other contexts, drives the document queue, and publishes `public.documentqueue.*` events. Lives under `documentqueue-event/documentqueue-event-processor`. Heavy use of converters.

## Domain Concepts

| Concept                 | Description                                                                                                                              |
|-------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| QueueDocument           | Aggregate for a single queued document and its lifecycle (outstanding → in-progress → completed → deleted / file-deleted).               |
| CPPCase                 | Aggregate tracking a case's document-queue state (case status, submission succeeded).                                                    |
| DocumentsExpiration     | Aggregate handling document expiry (delete-expired-documents request lifecycle).                                                         |
| DeleteDocuments         | Aggregate handling bulk deletion of documents for cases / from the file store.                                                           |
| Domain event            | Internal event written to the event store. Examples: `outstanding-document-received`, `document-marked-{outstanding,inprogress,completed,deleted,file-deleted}`, `case-marked-submission-succeeded`, `document-status-updated`, `document-status-update-failed`, `attach-document-requested`, `document-linked-to-case`, `delete-documents-of-cases-requested`, `document-delete-from-file-store-requested`, `delete-expired-documents-request-received`. |
| Public event (out)      | Published on `public.event` via `public-publications-descriptor.yaml`: `public.documentqueue.document-status-updated`, `public.documentqueue.document-attached`, `public.documentqueue.document-already-attached`, `public.documentqueue.event.document-status-update-failed`. |
| Public event (in)       | Consumed from `public.event`: `public.stagingbulkscan.{scan-envelope-registered, document-marked-for-follow-up}`, `public.prosecutioncasefile.{document-review-required, prosecution-submission-succeeded(-with-warnings), material-added}`, `public.progression.{document-review-required, events.case-or-application-ejected}`, `public.stagingprosecutorsspi.event.prosecution-case-filtered`. |
| Command                 | Inbound request via `documentqueue.handler.command`. Declared in RAML, dispatched by `@Handles`. Examples: attach-document, link-document-to-case, receive-outstanding-document, record-case-status, record-document-attached, remove-document-from-queue, update-document-status, delete-documents-of-cases, delete-expired-documents, mark-delete-expired-documents-as-requested, mark-document-deleted-from-file-store, mark-documents-deleted-for-cases. |
| Viewstore               | Read model `java:/DS.documentqueue`, populated by listeners. Schema managed by `documentqueue-viewstore-liquibase`.                      |
| Event store             | Append-only log `DS.eventstore`. Source of truth for aggregate state. Schema managed by `event-repository-liquibase`.                     |

## Authoritative Routing Files (always re-read before reasoning about a flow)

- `documentqueue-event-sources/src/yaml/event-sources.yaml` — event-source streams (`documentqueue` topic + `DS.eventstore`; `public` → `public.event`).
- `documentqueue-event/documentqueue-event-listener/src/yaml/subscriptions-descriptor.yaml` — listener subscriptions (own domain events).
- `documentqueue-event/documentqueue-event-processor/src/yaml/subscriptions-descriptor.yaml` — processor subscriptions (own domain events + inbound public events).
- `documentqueue-event/documentqueue-event-processor/src/yaml/public-publications-descriptor.yaml` — published public events.
- `documentqueue-command/documentqueue-command-handler/src/raml/documentqueue-command-handler.messaging.raml` — command → handler mapping.
- `documentqueue-command/documentqueue-command-api/src/raml/documentqueue-command-api.raml` and `documentqueue-query/documentqueue-query-api/src/raml/documentqueue-query-api.raml` — HTTP APIs.
- `documentqueue-service/src/main/descriptors/resource-descriptor.yml` — datasources, command queue, topics, service mapping.
- Per-command/per-event JSON schemas (multiple namespaces — see Gotchas).

## Module Layout

- `documentqueue-datatypes-common` — shared datatypes/value objects
- `documentqueue-command/documentqueue-command-api` — RAML + JSON schemas
- `documentqueue-command/documentqueue-command-handler` — `@Handles` handlers
- `documentqueue-domain/documentqueue-domain-aggregate` — `QueueDocument`, `CPPCase`, `DocumentsExpiration`, `DeleteDocuments`
- `documentqueue-domain/documentqueue-domain-core` — core domain logic / shared domain types
- `documentqueue-domain/documentqueue-domain-event` — event POJOs / schemas
- `documentqueue-domain/documentqueue-domain-value-schema` — value objects + schema
- `documentqueue-event/documentqueue-event-listener` — listeners + converters → viewstore
- `documentqueue-event/documentqueue-event-processor` — processors + converters → public events; inbound public-event handling
- `documentqueue-event-sources` — `event-sources.yaml`
- `documentqueue-query/documentqueue-query-api` — query RAML + read views over the viewstore
- `documentqueue-viewstore` — Liquibase migrations + persistence
- `documentqueue-service` — packaging WAR; `resource-descriptor.yml` wires datasources / queue / topics
- `documentqueue-healthchecks`, `documentqueue-integration-test` (`*IT.java` via `runIntegrationTests.sh`)

## Adding a New Command

1. **RAML first.** Add the command to `documentqueue-command-handler.messaging.raml` (and the command-api RAML) with the right media type (e.g. `application/vnd.documentqueue.command.<name>+json`).
2. **JSON schema.** Add the command payload schema in the correct namespace.
3. **Handler.** Add `@Handles("<command-name>")` on a `@ServiceComponent(COMMAND_HANDLER)` class; method takes `Envelope<CommandPayload>`.
4. **Aggregate.** If the command mutates state, the handler asks the aggregate to perform it; the aggregate emits a domain event and rebuilds state via `apply(event)`.
5. **Listener.** If the new event updates the viewstore: subscription entry + JSON schema + listener method + converter.
6. **Processor.** If the new event triggers a public event or downstream interaction: subscription (or publication) entry + JSON schema + processor method + converter.
7. **Tests.** Failing unit tests for handler, aggregate, listener (if touched), processor (if touched), converters (if touched). Then production code. Then IT exercising the end-to-end flow.

## Adding a New Domain Event

- Add the event's JSON schema in the correct namespace.
- Update the listener AND/OR processor `subscriptions-descriptor.yaml` (the two subscribe to overlapping but not identical sets — wire it to the component(s) that consume it; document any unaffected).
- For a published public event, add it to `public-publications-descriptor.yaml`.
- Update `event-sources.yaml` if a new topic is introduced.
- Add the listener/processor method + converter, and the failing-then-passing tests.

## Adding a Public-Event Subscription (incoming from another context)

1. **Subscription entry.** Add to the processor (and/or listener) `subscriptions-descriptor.yaml` for the `public` source (e.g. `public.stagingbulkscan.*`, `public.prosecutioncasefile.*`, `public.progression.*`, `public.stagingprosecutorsspi.*`).
2. **JSON schema.** Add the public-event schema (matches the upstream context's contract version).
3. **Processor method.** With `@Handles("<public-event-name>")` and `Envelope<PayloadType>`.
4. **Converter.** Map the public-event payload → a domain command / queue update.
5. **Tests.** Unit tests for the processor + converter. IT simulating the public-event arrival.

## Out-of-Scope (do not add)

- Hand-rolled JMS listeners — use the framework's `@Handles`
- Hand-rolled JDBC — use Liquibase changelogs and the framework persistence
- Ad-hoc `ObjectMapper` instances — use the framework's configured mapper
- Manual JSON schema validation — the framework validates incoming envelopes against subscription-declared schemas
- Spring annotations (`@Autowired`, `@Component`, `@Service`) and Lombok — this service uses plain CDI
- Cross-context coupling beyond declared public events — never call another context's command API directly; publish/consume public events instead

## Common Gotchas

1. **Schema-subscription drift** — adding a `subscriptions-descriptor.yaml` / `public-publications-descriptor.yaml` entry without the matching JSON schema produces a runtime 500 on dispatch. Constitution Principle VI makes this a review-blocker.
2. **Wrong `schema_uri` namespace** — multiple namespaces are in use (`justice.gov.uk/json/schemas/domains/documentqueue/event`, `justice.gov.uk/json/schemas/documentqueue`, `moj.gov.uk/cpp/documentqueue/event`). A copy-pasted `schema_uri` in the wrong namespace fails dispatch.
3. **Three-layer drift** — modifying a domain event without updating the listener AND processor (where each consumes it) is the most common silent-data-drift bug. Constitution Principle II makes this a review-blocker.
4. **Forgetting the inbound public-event side** — this context is primarily reactive; many behaviours start from an inbound `public.*` event from bulkscan / PCF / progression / spi, not from a local command.
5. **Liquibase registration** — adding a changelog file without registering it in the right registry (event-store / aggregate-snapshot / viewstore / event-buffer) means it never applies in CI's IT setup.
6. **Wrong `@ServiceComponent` value** — `COMMAND_HANDLER` vs `EVENT_LISTENER` vs `EVENT_PROCESSOR` are NOT interchangeable; the framework dispatches based on the value.
7. **Cross-context pin drift** — bumping `material` / `progression` / `bulkscan` / `referencedata` / `coredomain` versions in `pom.xml` requires bumping the matching schema/RAML classifier dep to the same version, or inbound/outbound public-event contracts drift.
