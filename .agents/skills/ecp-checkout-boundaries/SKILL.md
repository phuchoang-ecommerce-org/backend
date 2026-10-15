---
name: ecp-checkout-boundaries
description: Refactor or implement ECP backend checkout and order workflows with Facade adapters, domain-service command paths, query ports, and locally owned records.
---

# ECP Checkout Boundaries

Use this skill for backend checkout/order work that changes an API boundary, command path, read path, or a checkout-owned datatype. It complements the project refactor/implementer skills; load their referenced architecture rules first.

## Required Shape

- Publish a small `*Facade` interface in `<module>.api` and implement it with a Spring `*FacadeAdapter`. The adapter maps API records to application commands and maps application results/errors back to public contracts. Do not let module-internal code import its own `api` package.
- Put each application capability under `application/<use-case>/<command|query>/`. Keep command inputs/results with their use case; do not recreate a module-wide service or model package.
- A command application service authorizes/orchestrates and calls a Spring-wired domain service. It never calls a domain repository or invokes an aggregate transition itself.
- A domain service loads the aggregate and invokes its command method. The aggregate owns persistence of its complete state through its domain repository; child records have no independent write operation.
- A query is observational: define its port and view in the owning application query package, and let infrastructure map persistence data directly to that view. It need not construct or validate an aggregate.

## Ownership and Mapping

Declare a record beside the layer and capability that owns its meaning. Similar field layouts do not justify a shared module: Identity's address-book address and Ordering's immutable checkout address are separate values. Map them explicitly at the module/API boundary.

Use Lombok for dependency-injection boilerplate and MapStruct for repeatable boundary mappings. Preserve REST contracts, authorization operation names, transaction semantics, Flyway constraints, and public error codes while moving code.

## Persistence implementation

Keep JPA entities and persistence-only row shapes local to `infrastructure/persistence`. A local
MapStruct mapper owns aggregate/domain-value ↔ JPA entity conversion, including owned children,
timestamps, and bidirectional associations; repository adapters delegate to it rather than
hand-writing field copies. Map JPA read rows to application query DTOs with a separate local
mapper. Spring `@Configuration` classes that register persistence-backed beans live under
`infrastructure/configuration/`, separate from the persistence adapter implementation.

## Verification

Add or retain architecture tests proving application code does not depend on its own API/infrastructure or a domain repository, and API code does not depend on domain/infrastructure. Run the focused module tests plus the app architecture and Modulith tests.
