# Persistence Rules

## Authoritative write path

Preferred shape:

`Application use case -> aggregate/repository port -> JPA persistence adapter -> PostgreSQL`

PostgreSQL remains the source of truth for authoritative business state.

## Read path

Preferred shape:

`API -> application query port/view -> infrastructure query service -> PostgreSQL read SQL or approved derived store`

JDBC is intentionally valid on the read side. A refactor must not replace JDBC with JPA solely for stylistic uniformity.

## Projection path

Preferred shape:

`Kafka event -> infrastructure @EventHandler/consumer -> projection write -> derived store`

Derived stores include approved PostgreSQL projection tables, Elasticsearch indexes, MongoDB read models, and reconstructible Redis cache state as defined by the architecture.

## PERSIST-01 — Preserve database-enforced invariants

Before changing entities, repositories, SQL, migrations, or table ownership, identify constraints, unique indexes, foreign keys, version columns, grants, and triggers that enforce business or security rules.

## PERSIST-02 — Flyway owns schema evolution

Schema changes are migration changes. Do not rely on ORM auto-DDL as the architectural source of schema truth.

## PERSIST-03 — Preserve optimistic-concurrency semantics

Refactoring persistence code must preserve version checks, reservation/claim semantics, and conflict behavior used to protect invariants under concurrency.

## PERSIST-04 — Aggregate persistence is not generic CRUD

Write repositories should reflect aggregate lifecycle and transactional needs. Avoid broad generic data-access APIs that allow callers to bypass aggregate behavior.

## PERSIST-05 — Read SQL may be specialized

CTEs, joins, window functions, cursor predicates, and purpose-built row mapping are acceptable when they serve a query model. Extract SQL when it improves responsibility or testability, not merely to reduce line count.

## PERSIST-06 — Cache is never authoritative by accident

Ask: "If this key vanished, would the platform only become slower, or would behavior change?" If behavior changes, treat it as state with the corresponding availability/durability/security rules rather than ordinary cache.

## PERSIST-07 — Keep zero-lag decisions off cache/projections

Inventory availability, payment state, promotion claims, and other correctness-critical decisions must follow their specified authoritative path.
