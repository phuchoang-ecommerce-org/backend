# Layer Rules

## Domain

Owns:

- aggregate roots;
- entities;
- value objects;
- domain services;
- domain events;
- invariant enforcement;
- domain-owned abstractions where behavior requires them.

The domain should express business language and decisions, not framework mechanics.

## Application

Owns:

- commands and command handling/application services;
- query ports and application-facing read contracts;
- use-case orchestration;
- aggregate loading and saving through ports;
- use-case transaction boundaries where defined by the project;
- mapping between domain results and application outputs.

Application code must not absorb JDBC query construction, HTTP semantics, Kafka mechanics, or Redis commands.

Package Application code as `application/<use-case>/<command|query>/`, not
module-wide `commands`, `queries`, `services`, `dtos`, or `models`. Command
use cases invoke authoritative Domain Model behavior so its constraints decide
business transitions. Query ports and read models belong in the owning
Application use-case `query` package; Infrastructure supplies the technical
adapter, and queries do not traverse Domain Model constraints.

## API

Owns:

- REST controllers;
- request validation at the transport boundary;
- request/response mapping;
- HTTP headers/status semantics;
- correlation/error contract integration.

API must call application contracts, not infrastructure implementations.

## Infrastructure

Owns:

- JPA persistence adapters;
- JDBC query services;
- Flyway-facing persistence implementation concerns;
- Kafka relay/consumer mechanics;
- projection writers;
- Redis cache/rate-limit/session adapters;
- Elasticsearch/MongoDB read-model adapters;
- external service clients.

## App/Runtime

Owns:

- Spring Boot executable assembly;
- cross-module runtime composition;
- environment/runtime infrastructure wiring;
- deployment-facing configuration that does not belong to a business module.

## Common Layer Smells

### API -> Infrastructure leakage

Symptom: controller injects repository/JdbcTemplate/query implementation directly.

Correction: introduce or reuse the appropriate application command/query contract.

### Persistence leakage into application

Symptom: application service constructs SQL, EntityManager criteria, Redis commands, Kafka records, or persistence-specific pagination.

Correction: move mechanics behind an application/domain-owned port or infrastructure query implementation.

### Framework leakage into domain

Symptom: domain decision requires Spring context, repository bean, HTTP request, or message broker type.

Correction: pass domain data explicitly and move side effects to application/infrastructure.

### Controller-orchestrated use case

Symptom: controller performs load-check-update-publish sequences.

Correction: move orchestration to a single application use case.
