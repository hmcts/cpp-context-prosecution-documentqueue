# Service Identity

- **Service:** cpp-context-prosecution-documentqueue
- **Description:** Document-queue orchestration context. Tracks documents that must be attached to / distributed for prosecution cases, manages their lifecycle (outstanding → in-progress → completed / deleted), handles document expiration and file-store deletion. Largely event-driven: the event processor reacts to inbound public events from stagingbulkscan, prosecutioncasefile, progression, and stagingprosecutorsspi, and publishes its own `public.documentqueue.*` events.
- **Bounded context:** `documentqueue` (one of many CPP contexts).
- **Programme:** Crime Common Platform (CPP).
- **Organisation:** HMCTS / Ministry of Justice.

## Technology Stack

| Component         | Value                                                                |
|-------------------|----------------------------------------------------------------------|
| Build tool        | Maven (multi-module reactor; root `pom.xml`, `documentqueue-parent`) |
| Language          | Java 17 (CI demand `centos8-j17`)                                   |
| Framework         | Justice Services Framework / CPP `service-parent-pom:17.104.x` (CDI) |
| Packaging         | WAR (`documentqueue-service`) → WildFly via Docker                  |
| Annotations       | `@ServiceComponent`, `@Handles`, `@ApplicationScoped`               |
| DI / boilerplate  | CDI only — no Spring, no Lombok                                      |
| Persistence       | Liquibase changelogs (event-store, aggregate-snapshot, viewstore, event-buffer, event-tracking, file-service) |
| Messaging         | ActiveMQ (Docker for ITs); JMS queue + topics                       |
| Tests             | JUnit + Mockito (unit, surefire); framework's IT harness (`runIntegrationTests.sh`, failsafe); Cucumber |
| CI                | Azure DevOps Pipelines (`azure-pipelines.yaml` + `hmcts/cpp-azure-devops-templates`) |
| Quality gate      | SonarQube in CI (project `uk.gov.moj.cpp.prosecution.documentqueue:documentqueue-parent`) |
| Java packaging    | Root namespace `uk.gov.moj.cpp.prosecution.documentqueue.*`         |

## Constraints

- Maven is the current build tool. Future migration to Gradle is allowed but requires coordinating constitution + rule files + CI pipeline together (see Constitution Principle V).
- Java 17 only — prefer explicit types in public APIs
- Use the framework's `@ServiceComponent` + `@Handles` for command/event handling — NOT hand-rolled JMS listeners
- DI: CDI (`@ApplicationScoped` / `@Inject`); no Spring (`@Autowired` / `@Component` / `@Service`); no Lombok
- Aggregate state mutation must go through the aggregate's `apply(event)` replay (`QueueDocument` / `CPPCase` / `DocumentsExpiration` / `DeleteDocuments`)
- Event listeners and processors must use converter classes in `converter/` packages — NOT inline mapping
- Contracts (RAML, JSON schemas, `subscriptions-descriptor.yaml`, `public-publications-descriptor.yaml`, `event-sources.yaml`) update FIRST, Java second (Constitution Principle I)
- Schema additions / removals / renames update both the subscription/publication descriptor AND the JSON schema in lockstep, in the correct namespace (Constitution Principle VI)
- Logging via SLF4J only — no `System.out` / `System.err` (Constitution Principle VII)
- Test-Driven Development is mandatory (Constitution Principle VIII)

## Build & Test Commands

```bash
# Full build + unit tests
mvn clean install

# Build, no tests
mvn clean install -DskipTests

# Unit tests only
mvn test

# Single module with deps
mvn -pl documentqueue-command/documentqueue-command-handler -am clean install

# Single unit test
mvn -pl <module> test -Dtest=ClassName#methodName

# Liquibase only (all stores)
./runLiquibase.sh

# Integration tests (requires Dockerised env up; CPP_DOCKER_DIR must be set)
./runIntegrationTests.sh

# Single IT against running env
mvn -pl documentqueue-integration-test test -Dit.test=ClassNameIT

# Framework JMX commands
./runSystemCommand.sh           # help / list
./runSystemCommand.sh CATCHUP   # run one
```

## Key version pins (`pom.xml`)

- Parent: `uk.gov.moj.cpp.common:service-parent-pom:17.104.x` (currently 17.104.0); artifact `documentqueue-parent` (currently `17.104.21-SNAPSHOT`), groupId `uk.gov.moj.cpp.prosecution.documentqueue`
- Cross-context / notable pins to keep aligned: `material`, `progression`, `bulkscan`, `referencedata`, `coredomain`, `system.id-mapper`, `system.enterprise-id`, `cpp.structure`
- When bumping any of these, also check the matching schema/RAML classifier dep is on the same version
