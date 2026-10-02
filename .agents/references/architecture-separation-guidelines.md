# Architecture and Separation of Concerns

## Governing Principle

A datatype or behavior belongs as close as possible to the capability that owns
its semantics. Move it outward or share it only when its semantic ownership is
genuinely broader. Structural similarity is not semantic identity.

Preserve this dependency direction:

```text
Web/API ────────┐
                ▼
           Application
                │
                ▼
             Domain

Infrastructure ──────► Application
Infrastructure ──────► Domain
```

Domain must not depend on Application, Infrastructure, or Web/API.
Application may depend on Domain, but not on a technology implementation.
Infrastructure implements capabilities defined inward. Web/API adapts transport
to Application contracts and must not access Infrastructure directly.

## Ownership and Placement

Before adding or moving a `class`, `record`, `enum`, or interface, identify its
semantic owner. Do not infer ownership from Java syntax, field similarity, or
the package that is most convenient to reuse.

| If it represents… | It belongs in… |
| --- | --- |
| Business meaning, an invariant, aggregate/entity lifecycle, value semantics, domain policy, state transition, domain identifier, or meaningful business fact | Domain |
| A use-case input/output, command/query, orchestration concept, transaction-level capability, application port, or CQRS read model | Application |
| PostgreSQL/JPA/JDBC, Redis, Kafka, Elasticsearch, a provider SDK, serialization, configuration, or another technical representation | Infrastructure |
| HTTP/OpenAPI request or response contract, parameter parsing, transport validation, status/error mapping, or authentication-context extraction | Web/API |

If no independently meaningful owner exists, keep the implementation-only type
private and local to the class that uses it. Do not create `CommonModels`,
`SharedDtos`, `CommonTypes`, `SharedRecords`, or equivalent packages merely to
remove duplication.

## Domain

Domain owns the business model and rules that remain meaningful without HTTP,
PostgreSQL, Redis, Kafka, JSON, Spring, or a provider SDK. Use it for aggregate
roots, entities, value objects, policies, state transitions, domain-specific
identifiers, and domain events that describe completed business facts.

Keep invariants with their rightful aggregate or domain policy. Domain types
must not know table/column shapes, SQL, `ResultSet`, JDBC, Redis/Kafka records,
HTTP DTOs, controller abstractions, or JSON serialization details. Do not
promote a row, projection, API DTO, or integration payload into the Domain
model just because it has similar fields.

## Application

Application owns what the system does: focused business capabilities, use-case
orchestration, transaction boundaries, authorization decisions that belong to a
use case, ports, and query models. Organize it by capability/use case, not by
generic technical categories such as `services`, `dtos`, `commands`, `models`,
or `utils`.

For example, a product capability may contain separate `create`, `detail`, and
`listing` packages. Keep each command, query, result, and read model next to
the use case that owns it. Broaden a type only when its semantics are truly
broader.

Ports express a required capability, not a mechanism. An aggregate repository
can be an Application or Domain port when its language is aggregate-oriented;
a read port can return an Application-owned read model. Application must not
construct SQL, use JPA/JDBC/Redis/Kafka APIs, or depend on an adapter class.
It invokes Domain behavior rather than duplicating domain invariants.

## Infrastructure

Infrastructure owns how a technology fulfills an inward-facing capability:
JPA/JDBC persistence adapters, database queries, Redis adapters, Kafka relay
and consumer mechanics, outbox storage, search/projection adapters, external
HTTP clients, serialization, and technology-specific configuration.

Keep adapter representations local whenever possible. `ProductRow`, a Redis
snapshot, provider response, Kafka payload, or search document is not
automatically a Domain entity, Application model, domain event, or API DTO.
Translate explicitly at each boundary. A type used only by one adapter should
normally be a private nested type rather than a public abstraction.

## Web/API

Web/API owns external HTTP contracts: controllers, request/response DTOs,
parameter parsing, transport validation, HTTP status/error mapping,
authentication-context extraction, pagination parsing, and OpenAPI-facing
representations. Controllers stay thin:

```text
HTTP request → transport parsing/validation → application command or query
→ application result → API response
```

Controllers must not implement business invariants, orchestrate persistence,
or inject JDBC repositories, Redis adapters, Kafka producers, or other
Infrastructure mechanisms. API DTOs and Domain/Application types may resemble
each other, but they are separate contracts unless they genuinely share the
same semantics.

## CQRS and Boundary Mapping

Command paths normally use an Application command, Domain aggregate behavior,
and a persistence port/adapter because authoritative state transitions require
invariants. Read-heavy query paths may use an Application query port and an
optimized Infrastructure query adapter that returns an Application-owned read
model. Do not reconstruct an aggregate solely to serve a projection/listing.

For every cross-layer boundary, state the required mapping explicitly:

```text
Web request ↔ Application input/output ↔ Domain state/values ↔ Infrastructure representation
```

Technology-specific representations must terminate in Infrastructure, and
transport-specific representations must terminate in Web/API. Small deliberate
duplication across contexts, use cases, or layers is preferable to coupling
unrelated concepts behind a shared type.

## Feature and Refactor Gate

Before implementation or a target refactor design, identify:

1. the business capability and bounded-context owner;
2. domain concepts and invariants, if the change has domain behavior;
3. application use cases, input/output models, and required ports;
4. infrastructure adapters and their local representations;
5. Web/API contracts, if externally exposed;
6. every mapping across those boundaries.

Implement conceptually from the inside outward: Domain behavior, Application
use case and ports, Infrastructure adapters, Web/API adapter, then appropriate
tests. For a query-only feature, do not invent a Domain abstraction merely to
follow this sequence.

Before completing, verify that every changed type has a clear semantic owner;
no API or Infrastructure representation leaks inward; shared types are
semantically—not just structurally—shared; read paths reconstruct aggregates
only when needed; and dependencies still point inward.
