# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

The **documentqueue** context — an HMCTS CPP digital service that **orchestrates a queue of documents** for prosecution cases. It tracks documents that must be attached to / distributed for a case, manages their lifecycle (outstanding → in-progress → completed / deleted), handles document expiration, and removes documents from the underlying file store. It is largely **event-driven from other contexts**: its event processor reacts to public events from stagingbulkscan, prosecutioncasefile, progression, and stagingprosecutorsspi, and it publishes its own `public.documentqueue.*` events.

It is a CQRS + event-sourced microservice built on the `uk.gov.justice` *Justice Services Framework* (parent `uk.gov.moj.cpp.common:service-parent-pom`). Java 17, packaged as a WildFly WAR (`documentqueue-service`). CDI (no Spring, no Lombok).

## Build & test

```bash
mvn clean install                       # full multi-module build + unit tests
mvn clean install -DskipTests           # build, no tests
mvn test                                # unit tests only
mvn -pl documentqueue-command/documentqueue-command-handler -am clean install   # one module with deps
mvn -pl <module> test -Dtest=ClassName#methodName                               # single test
```

Unit tests run under surefire. Integration tests (`documentqueue-integration-test`) require the full Docker stack and do **not** run from a plain `mvn test`.

### Integration tests & Liquibase
`./runIntegrationTests.sh` builds WARs, runs Liquibase, deploys WireMock + WARs, healthchecks, then the IT suite. Requires `CPP_DOCKER_DIR` exported and pointing at a local checkout of `hmcts/cpp-developers-docker`, plus the Docker stack running. `./runLiquibase.sh` runs Liquibase across all stores: event log, event-log aggregate snapshot, event buffer, viewstore, system, event tracking, file service.

### System commands
`./runSystemCommand.sh` wraps `framework-jmx-command-client` to run framework JMX system commands against a running instance (e.g. `./runSystemCommand.sh CATCHUP`). Run with no args to list commands.

## Architecture — three layers (CQRS / event-sourced)

Data flows command → aggregate → events → event store → (listener → viewstore) + (processor → public events). Every change touching events MUST be reasoned about across all three layers.

```
1. Command side: REST/messaging → documentqueue.handler.command → @Handles handler → aggregate → domain event
       ↓ writes to java:/app/documentqueue-service/DS.eventstore → topic documentqueue.event
2. Event listener: projects domain events → viewstore (java:/DS.documentqueue)
3. Event processor: consumes domain events + inbound public events → publishes public.documentqueue.* events
```

- **documentqueue-command** — write side. `*-command-api` (RAML + JSON schemas), `*-command-handler` (`@Handles` handlers). Commands declared in `documentqueue-command-handler.messaging.raml` (media types `application/vnd.documentqueue.command.<name>+json`), e.g. `attach-document`, `link-document-to-case`, `receive-outstanding-document`, `update-document-status`, `delete-documents-of-cases`, `delete-expired-documents`, `mark-*`.
- **documentqueue-domain** — `domain-aggregate` (`QueueDocument`, `CPPCase`, `DocumentsExpiration`, `DeleteDocuments`: turn commands into events, rebuild state via `apply(event)`), `domain-core`, `domain-event`, `domain-value-schema`.
- **documentqueue-event** — `*-event-listener` projects events → viewstore via converters; `*-event-processor` consumes domain events AND a large set of inbound public events, and publishes public events.
- **documentqueue-event-sources** — `src/yaml/event-sources.yaml` declares the `documentqueue` stream (topic `documentqueue.event`, `DS.eventstore`) and the `public` stream (`public.event`).
- **documentqueue-query** — read side. `*-query-api` (RAML) + read views over the viewstore.
- **documentqueue-viewstore** — Liquibase changelogs + persistence for `java:/DS.documentqueue`.
- **documentqueue-service** — the deployable WAR; `src/main/descriptors/resource-descriptor.yml` wires datasources, the `documentqueue.handler.command` queue, and the `documentqueue.event` / `public.event` topics (matcher `/documentqueue-[^/]+`).
- **documentqueue-datatypes-common**, **documentqueue-healthchecks**, **documentqueue-integration-test**.

### Public-event relationships
- **Consumes** (processor `subscriptions-descriptor.yaml`, `public` source): `public.stagingbulkscan.{scan-envelope-registered, document-marked-for-follow-up}`, `public.prosecutioncasefile.{document-review-required, prosecution-submission-succeeded(-with-warnings), material-added}`, `public.progression.{document-review-required, events.case-or-application-ejected}`, `public.stagingprosecutorsspi.event.prosecution-case-filtered`.
- **Publishes** (`public-publications-descriptor.yaml`): `public.documentqueue.document-status-updated`, `public.documentqueue.document-attached`, `public.documentqueue.document-already-attached`, `public.documentqueue.event.document-status-update-failed`.

### Contracts are RAML + JSON schema
- `documentqueue-command-api.raml`, `documentqueue-command-handler.messaging.raml`, `documentqueue-query-api.raml`.
- JSON schemas referenced by `schema_uri`. **Multiple namespaces are in use** — at least `http://justice.gov.uk/json/schemas/domains/documentqueue/event/...`, `http://justice.gov.uk/json/schemas/documentqueue/...`, and `http://moj.gov.uk/cpp/documentqueue/event/...`. Match the namespace the sibling schema is published under; a copy-pasted `schema_uri` in the wrong namespace fails dispatch.
- Listener and processor each have a `subscriptions-descriptor.yaml`; the processor also has a `public-publications-descriptor.yaml`. Add/change an event by editing the schema + descriptor alongside the handler.

## CI / branching

- CI is Azure DevOps (`azure-pipelines.yaml`) using shared `hmcts/cpp-azure-devops-templates`: on PR → `context-verify`, on CI build → `context-validation`. SonarQube project `uk.gov.moj.cpp.prosecution.documentqueue:documentqueue-parent`; `serviceName=documentqueue`; `itTestFolder=documentqueue-integration-test`; pool `MDV-ADO-AGENT-AKS-01` / `centos8-j17`.
- Uses jgitflow; the develop branch is `main`. Release branches are `dev/release-*` (excluded from CI triggers). Parent `service-parent-pom:17.104.0`; module versions managed in the parent `pom.xml`.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
<!-- SPECKIT END -->
