# Architecture Rules

## AR-01 — Preserve bounded-context ownership

Code belongs to the bounded context that owns the business meaning. Refactoring for reuse must not move domain behavior into a neutral/shared module merely because multiple modules need similar syntax.

## AR-02 — Do not introduce module dependencies for convenience

A business relationship does not automatically imply a Java/Gradle dependency. Cross-context collaboration should use the explicitly permitted contract, port, or asynchronous event pattern.

## AR-03 — Keep the module dependency graph acyclic

The module graph must remain acyclic and verifiable by build-time architecture checks. Never solve a local compilation problem by creating a reverse dependency or cycle.

## AR-04 — Domain remains technology-independent

Domain code must not depend on Spring MVC, JPA annotations used as persistence mechanics, JDBC, Kafka, Redis, Elasticsearch, MongoDB, transport DTOs, or another module's implementation details.

Project-approved domain-oriented annotations such as JMolecules may be used where already part of the architecture governance model.

## AR-05 — Application owns use-case orchestration

The application layer coordinates a business use case, transaction-level workflow, ports, aggregate loading, domain invocation, and output mapping. It must not become a generic SQL/data-access layer or HTTP adapter.

## AR-06 — Infrastructure owns technology mechanics

Infrastructure contains persistence adapters, JDBC query implementations, Kafka consumers/relays, Redis adapters, external clients, serialization, and technology-specific configuration.

## AR-07 — API owns transport adaptation

Controllers and transport adapters translate HTTP/request concerns into application inputs and translate outputs/errors into external contracts. Controllers must not orchestrate multi-step business workflows or access persistence directly.

## AR-08 — Cross-module communication is explicit

Use only the dependency edges and published interfaces/contracts intentionally allowed by the module architecture. Do not import another module's internal implementation package.

## AR-09 — Consumer contracts are locally owned

A consuming module must not depend on a publisher's internal event Java type merely to deserialize an integration event. Preserve contract decoupling and integration-boundary ownership.

## AR-10 — `app` remains the composition/runtime root

The `app` module is responsible for executable composition and runtime wiring. Business modules remain library modules and must not absorb bootstrapping concerns for convenience.

## AR-11 — Architectural rules should be falsifiable

Where a boundary is important, prefer enforcement through compiler dependencies, Spring Modulith verification, ArchUnit, JMolecules rules, or tests rather than comments alone.

## AR-12 — Shared kernel stays intentionally small

Do not move module-specific domain types into `shared-kernel` to reduce duplication unless they are genuinely stable, semantically shared concepts approved by the architecture.
