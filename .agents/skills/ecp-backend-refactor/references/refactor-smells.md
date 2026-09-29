# Backend Refactor Smells

Use smells as diagnostic signals, not automatic rewrite triggers.

## R01 — Layer Leakage

Symptom: framework/persistence/transport responsibilities appear in the wrong layer.

Risk: dependency inversion weakens and business logic becomes difficult to test or move.

Preferred correction: return the mechanic to its owning adapter/layer and expose the smallest required contract.

## R02 — God Application Service

Symptom: one service handles unrelated commands, queries, mapping, SQL, events, cache, and external calls.

Risk: transaction boundaries and use-case ownership become implicit.

Preferred correction: split by cohesive use case or command/query responsibility.

## R03 — Persistence-Orchestration Mixing

Symptom: application logic contains SQL construction, EntityManager details, or row mapping.

Risk: technology details control use-case design.

Preferred correction: move mechanics into persistence/query adapters while retaining semantic ports where meaningful.

## R04 — Query/Command Mixing

Symptom: the same service loads projections to make write decisions or mutates state while serving queries.

Risk: stale reads become business correctness bugs.

Preferred correction: separate authoritative command path from observational query path.

## R05 — Cross-Module Convenience Dependency

Symptom: a module imports another module's internal class because it is easier than defining the intended contract.

Risk: cycles and bounded-context erosion.

Preferred correction: use allowed port/API/event contract or local translation.

## R06 — Shared Domain Model Across Contexts

Symptom: aggregates/entities are reused directly by multiple bounded contexts.

Risk: model meaning and lifecycle become coupled.

Preferred correction: keep context-local models and translate contracts at boundaries.

## R07 — Anemic Aggregate

Symptom: business invariants are implemented entirely in application services while aggregate methods are setters/data bags.

Risk: callers can bypass invariant enforcement.

Preferred correction: move invariant-bearing state transitions into the aggregate where the domain model assigns ownership.

## R08 — Infrastructure-Aware Domain

Symptom: domain types depend on Spring beans, repositories, HTTP, Kafka, Redis, JDBC, or external SDK types.

Risk: domain logic cannot be reasoned about independently.

Preferred correction: pass domain values and move side effects outward.

## R09 — Controller-Orchestrated Use Case

Symptom: controller executes multiple repositories/services in business order.

Risk: duplicate workflows and inconsistent transaction behavior.

Preferred correction: create/reuse one application use-case boundary.

## R10 — Repository as Generic Data Access

Symptom: repository exposes broad CRUD/query methods unrelated to aggregate persistence.

Risk: aggregate boundaries are bypassed and write/read responsibilities blur.

Preferred correction: keep aggregate repositories narrow; use dedicated query services for read models.

## R11 — Event Contract Coupling

Symptom: consumer imports publisher implementation event class.

Risk: deployment-independent bounded contexts become source-coupled.

Preferred correction: consumer-owned contract type compatible with the integration envelope.

## R12 — Projection Used as Authority

Symptom: Elasticsearch/MongoDB/read table decides whether a command may proceed.

Risk: eventual consistency produces incorrect business outcomes.

Preferred correction: re-check against authoritative state on the command path.

## R13 — Cache Used as Authority

Symptom: missing/evicted cache key changes authorization or business truth unexpectedly.

Risk: cache eviction becomes data loss or security failure.

Preferred correction: classify the key correctly as cache vs state and restore authoritative checks.

## R14 — Transaction Boundary Leakage

Symptom: a use case assumes multiple independent repository operations are atomic without an explicit transaction boundary.

Risk: partial updates and outbox inconsistency.

Preferred correction: establish the project-approved application transaction boundary.

## R15 — Duplicate Infrastructure Policy

Symptom: retry, serialization, correlation, error mapping, cache policy, or broker logic is reimplemented per use case.

Risk: operational semantics drift.

Preferred correction: centralize only the truly cross-cutting infrastructure policy at its approved layer.

## R16 — Premature Shared Abstraction

Symptom: two superficially similar modules are forced behind one generic abstraction.

Risk: bounded contexts become coupled to the wrong common model.

Preferred correction: tolerate local duplication until semantic stability is demonstrated.

## R17 — Hidden Business Invariant

Symptom: critical rule exists only as an incidental SQL predicate, controller condition, or undocumented service branch.

Risk: refactor removes it unknowingly.

Preferred correction: identify its normative owner and add explicit tests/documented enforcement before moving code.

## R18 — Unbounded Application Service

Symptom: application service accumulates unrelated public methods and becomes a module facade for everything.

Risk: change impact and testing scope expand continuously.

Preferred correction: organize around use cases/capabilities with explicit command/query contracts.
