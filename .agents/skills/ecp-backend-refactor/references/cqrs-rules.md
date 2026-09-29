# CQRS Rules

## CQRS-01 — Commands operate on authoritative state

Command decisions must be based on the authoritative write model/source of truth required by the domain invariant.

## CQRS-02 — Commands never decide against a read model

A projection, cache, Elasticsearch index, MongoDB document, or read-optimized table must not authorize or reject a business state transition when it may be stale.

## CQRS-03 — Queries do not mutate authoritative state

Query execution is observational. It must not cause domain state transitions or write authoritative business data.

## CQRS-04 — Query models are purpose-built

A read model may be shaped for listing, search, reporting, or API projection needs. Do not force aggregates or write entities to become generic DTOs for read scenarios.

## CQRS-05 — Derived stores have one writer

A derived projection store must have one intentional projection-writing path. Prevent controllers, command services, and unrelated infrastructure from writing it directly.

## CQRS-06 — Cache invalidation is not projection authorship

Redis cache invalidation may be triggered by events, but cache entries remain reconstructed from their authoritative/read source. Do not turn invalidation handlers into a second source of projected business truth.

## CQRS-07 — Do not force JPA onto read paths

If a query is naturally expressed as SQL and is part of the read model, direct JDBC/JdbcClient-style infrastructure is valid. Uniform persistence technology is not a refactor objective.

## CQRS-08 — Do not create repository abstractions without semantic value

A repository abstraction is appropriate when it represents aggregate persistence or a meaningful port. Do not wrap every read SQL query in a generic repository merely to hide JDBC.

## CQRS-09 — Read-your-writes is not implemented by waiting for projections

Do not refactor command completion into polling/waiting for Kafka projections. Return command-owned results or use an explicitly defined authoritative follow-up path.

## CQRS-10 — Preserve consistency classification

Do not move a zero-lag or correctness-critical decision onto an eventually consistent projection as a performance optimization.
