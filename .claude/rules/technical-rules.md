# Coding Conventions — MOJ / CPP Standard (this service)

## Dependency Injection / Component Wiring

- Use the Justice Services Framework component model — `@ApplicationScoped` for framework-managed singletons, `@Inject` (CDI) for collaborator injection
- For command handlers: `@ServiceComponent(COMMAND_HANDLER)` on the class + `@Handles("<command-name>")` on the method
- For event listeners: framework listener base + `@Handles("<event-name>")` on listener methods, `@ServiceComponent(EVENT_LISTENER)` on the class
- For event processors: framework processor base + `@ServiceComponent(EVENT_PROCESSOR)` on the class
- Do NOT use Spring annotations (`@Autowired`, `@Component`, `@Service`) — this is not a Spring service
- Do NOT use Lombok — keep constructors and accessors explicit, consistent with the existing code
- Do NOT roll your own JMS listener / JDBC connection / ObjectMapper

## Envelope / Payload Handling

- Handler/listener method signatures take `Envelope<PayloadType>`, never the raw payload type
- Read the payload via `envelope.payload()`; metadata via `envelope.metadata()`
- Correlation context (correlation id, user id, etc.) lives in `envelope.metadata()` and should be propagated into MDC for SLF4J
- Treat the payload as immutable — do not mutate fields after reading

## Aggregate State Mutation

- All aggregate state mutation goes through the aggregate's `apply(event)` event-replay mechanism (`QueueDocument`, `CPPCase`, `DocumentsExpiration`, `DeleteDocuments` under `documentqueue-domain/documentqueue-domain-aggregate`)
- The handler asks the aggregate to perform a command; the aggregate emits domain events; state is rebuilt by replaying those events via `apply(...)`
- Do NOT write events directly to the event store, and do NOT mutate read-model state from the command side

## Converters (Listener and Processor)

- Listener converters map domain events → viewstore entities (`java:/DS.documentqueue`)
- Processor converters map domain events → public-event payloads, and map inbound public events (bulkscan / PCF / progression / spi) → follow-up commands / queue updates
- Each converter is a single-purpose class (one event → one target shape); composition happens at the listener/processor level

## Error Handling

- Custom exceptions extend `RuntimeException` (or framework-specific bases like `EventStreamException`)
- NEVER swallow exceptions silently — always log or rethrow
- Listener / processor methods can let framework exceptions propagate; the framework handles redelivery and dead-letter routing
- Invalid envelope payloads should fail loudly with a meaningful message — the framework re-delivers, so a silent skip leaks broken state

## Logging

- SLF4J with the framework's logger configuration
- Use `private static final Logger LOGGER = LoggerFactory.getLogger(...)`
- MDC keys: include correlation id and other relevant fields from `envelope.metadata()`
- NEVER use `System.out.println`, `System.err.println`, or `Throwable#printStackTrace()` (Constitution Principle VII)
- NEVER log sensitive data (case/document identifiers in plain text without masking, tokens, passwords, PII)

## Imports

- NEVER use wildcard imports (`import java.util.*`) — always use explicit imports for each class

## Naming Conventions

| Component        | Pattern                  | Example                                  |
|------------------|--------------------------|------------------------------------------|
| Command handler  | `*Handler`               | (under `command.handler`)                |
| Event listener   | `*Listener`              | (under `event.listener`)                 |
| Event processor  | `*Processor`             | (under `event.processor`)                |
| Converter        | `*Converter` or `*To*Converter` | (under `converter/`)              |
| Aggregate        | (singular noun)          | `QueueDocument`, `CPPCase`, `DocumentsExpiration`, `DeleteDocuments` |
| Domain event     | (past tense)             | `outstanding-document-received`, `document-marked-completed`, `document-linked-to-case` |
| Public event     | (past tense)             | `public.documentqueue.document-status-updated` |
| Service / view   | `*Service`               | (query services)                         |
| Test             | `*Test` / `*IT`          | `QueueDocumentTest`, `*IT`               |

## Testing Conventions

- JUnit + Mockito for unit tests (surefire)
- Use `@ExtendWith(MockitoExtension.class)` (or the codebase's preferred Mockito wiring — check existing tests)
- Use `@Nested` classes with `@DisplayName` for grouped scenarios
- Method naming: `{action}_{scenario}_should_{expectation}`
- Use AssertJ where the codebase already does; otherwise plain JUnit assertions are fine
- Integration tests live in `documentqueue-integration-test` (failsafe `*IT.java`; Cucumber) and run via `./runIntegrationTests.sh`
- Test commands:
  - `mvn test` — unit tests only
  - `./runIntegrationTests.sh` — full Dockerised IT run
  - `mvn -pl documentqueue-integration-test test -Dit.test=ClassNameIT` — single IT against running env
- TDD: write the failing test first, see it fail for the right reason, then implement (Constitution Principle VIII)
- Logging in tests: SLF4J only (Constitution Principle VII)

## RAML / JSON Schema

- RAML files: `src/raml/...` per command/query module
- JSON schemas: referenced by `schema_uri`. **Multiple namespaces are in use** — `http://justice.gov.uk/json/schemas/domains/documentqueue/event/...`, `http://justice.gov.uk/json/schemas/documentqueue/...`, `http://moj.gov.uk/cpp/documentqueue/event/...`. Match the namespace the sibling schema is published under
- Every command in RAML has a matching `@Handles` method; every event in a `subscriptions-descriptor.yaml` has a matching listener / processor method; every published event in `public-publications-descriptor.yaml` is actually emitted
- Every event has a JSON schema; every JSON schema is referenced from at least one contract artefact
- When adding a new event:
  1. JSON schema first (correct namespace)
  2. `subscriptions-descriptor.yaml` and/or `public-publications-descriptor.yaml` entry
  3. `event-sources.yaml` if a new topic is involved
  4. Java listener / processor method last
