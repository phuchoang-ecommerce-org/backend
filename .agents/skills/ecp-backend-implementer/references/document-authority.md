# `references/document-authority.md`

# Backend Document Authority

## Purpose

This file defines which repository document must be consulted for each category of backend implementation decision.

Do not treat every specification as interchangeable.

---

## 1. Authority Chain

Use the following conceptual hierarchy:

```text
Business Problem
    ↓
SRS / Requirement / Business Rule
    ↓
Use Case
    ↓
Domain Model
    ↓
Solution Architecture / ADR
    ↓
Backend Technical Specification
    ↓
Shared Contract
    ↓
Implementation
    ↓
Verification
```

A downstream implementation must not silently redefine an upstream business rule.

---

## 2. Question Routing

| Question                                     | Primary authority                           |
| -------------------------------------------- | ------------------------------------------- |
| Why does the feature exist?                  | Problem / SRS                               |
| What behavior must occur?                    | FR / BR / Use Case                          |
| What quality constraint applies?             | NFR                                         |
| Which bounded context owns it?               | Domain Model                                |
| Which aggregate owns it?                     | Domain Model                                |
| Which invariant applies?                     | Domain Model + BR                           |
| Why does the architecture use this approach? | ADR                                         |
| What modules may depend on each other?       | Module Dependency Diagram                   |
| How is the command implemented?              | CQRS                                        |
| How is the query implemented?                | CQRS                                        |
| Which store is authoritative?                | CQRS + Database + ADR                       |
| What tables/constraints/indexes exist?       | Database                                    |
| How does Kafka/outbox operate?               | Backend Architecture + Integration Contract |
| How does Redis operate?                      | Backend Architecture                        |
| What HTTP operation exists?                  | OpenAPI                                     |
| Which external error code applies?           | Error Codes                                 |
| Who may perform the operation?               | Permission Matrix                           |
| What security control applies?               | Security                                    |
| What integration envelope applies?           | Integration Contract                        |
| How is behavior tested?                      | Testing and Benchmark Strategy              |
| How does the flow execute end-to-end?        | Sequence Diagrams                           |

---

## 3. Business Documents

Before feature design, establish what the system must do.

Relevant sources include:

```text
SRS
Functional Requirements
Business Rules
Non-Functional Requirements
Use Cases
Traceability Matrix
```

The Traceability Matrix provides scope traceability.

Do not add business behavior only because it appears useful from an implementation perspective.

---

## 4. Domain Model

`Domain Model.md` is authoritative for the logical domain structure.

Use it for:

```text
bounded contexts
aggregates
aggregate roots
entities
value objects
domain services
domain events
repositories
business invariants
context relationships
ubiquitous language
```

It structures behavior already specified by the BA documents.

It must not invent new requirements.

When implementing a feature, identify the relevant aggregate and invariants before choosing persistence or API mechanics.

---

## 5. Solution Architecture and ADR

Solution Architecture records the architectural direction.

ADRs explain why specific architectural choices were selected.

Use ADRs when the implementation depends on choices such as:

```text
modular monolith
Spring Modulith
CQRS
PostgreSQL authority
JPA vs JDBC
optimistic locking
transactional outbox
Kafka
Redis
MongoDB
Elasticsearch
Flyway
security mechanisms
```

Do not reopen an accepted ADR merely because another implementation is easier.

---

## 6. CQRS

Use `CQRS.md` when deciding:

```text
whether the feature is command or query
where command artifacts live
where queries live
which read model is used
projection consistency
projection idempotency
event ordering
projection rebuild
read-after-write behavior
```

Do not assume all reads should traverse repositories for aggregates.

Do not assume all writes may update projections directly.

---

## 7. Database

Use `Database.md` for physical persistence.

It defines:

```text
tables
columns
types
keys
foreign keys
unique constraints
check constraints
indexes
version columns
outbox tables
consumer idempotency tables
derived-store shape
```

Some business rules are intentionally enforced by database constraints.

Those constraints are part of feature correctness.

---

## 8. Module Dependency Diagram

The Domain Model context map describes business relationships.

The Module Dependency Diagram describes legal code dependencies.

They are not equivalent.

Before creating:

```text
module A → module B import
```

verify that the dependency is legal.

If the architecture specifies asynchronous integration, no direct dependency may be required.

---

## 9. Backend Architecture

Use `Backend Architecture.md` for operational mechanisms that implement accepted decisions.

Examples include:

```text
outbox relay
Kafka configuration
serialization
topic operational details
Redis topology
Redis operational parameters
runtime integration mechanics
```

It operationalizes architecture.

It does not override higher-level accepted decisions.

---

## 10. Sequence Diagrams

Use sequence diagrams to understand cross-component runtime flow.

They are especially useful for:

```text
which component initiates the operation
which module participates
sync vs async communication
where external providers participate
where read models are consulted
```

Use them together with normative specifications rather than as the sole authority.

---

## 11. Shared Contracts

### OpenAPI

Defines REST operations and HTTP payloads.

### Error Codes

Defines stable externally visible failure semantics.

### Integration Contract

Defines cross-system conventions such as:

```text
event envelope
topic conventions
correlation
idempotency
consumer obligations
evolution rules
```

### Permission Matrix

Defines authorized operations.

These are system boundaries and must not be silently modified during implementation.

---

## 12. Security

Use `Security.md` for:

```text
trust boundaries
authentication
authorization
PII
audit
rate limiting
provider callbacks
security verification
```

Where Security declares its verification matrix authoritative, use that matrix rather than duplicating alternative security criteria.

---

## 13. Testing and Benchmark Strategy

Use it to determine:

```text
appropriate test layer
ownership
CI gate
test cadence
benchmark expectations
NFR verification
```

Do not choose a test type purely because it is easiest to write.

Test the layer that owns the risk.

---

## 14. Conflict Handling

If documents appear to conflict:

1. identify both requirements explicitly;
2. inspect ADR status and supersession;
3. distinguish business authority from technical elaboration;
4. determine whether one document intentionally operationalizes another;
5. do not resolve ambiguity according to implementation convenience.

If no authoritative resolution exists, report:

```text
SPECIFICATION CONFLICT
```

and identify the affected implementation decision.