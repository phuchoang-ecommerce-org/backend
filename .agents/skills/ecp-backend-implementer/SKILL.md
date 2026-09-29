---
name: ecp-backend-implementer
description: Architecture-aware implementing protocol for the ECP Java/Spring modular monolith. Use when analyzing or implementing existing backend code while preserving business invariants, module boundaries, CQRS semantics, transaction boundaries, integration contracts, security guarantees, and operational behavior.
version: 1.0.0
---
# ECP Backend Feature Implementation Skill

## Purpose

This skill defines how an engineering agent must analyze, design, implement, and verify a backend feature in the Enterprise Commerce Platform (ECP).

It applies to backend work involving:

* business capabilities;
* domain behavior;
* application use cases;
* commands and queries;
* PostgreSQL persistence;
* read models;
* Redis;
* Kafka and transactional outbox;
* module integration;
* REST APIs;
* security and authorization;
* database migrations;
* backend tests.

This skill does not define frontend implementation.

The frontend is a separate project and consumes the contracts exposed by the backend.

---

## 1. Core Principle

ECP is specification-driven rather than code-driven.

The current implementation is not sufficient evidence of the intended architecture.

Before implementing a feature, trace the requested behavior through the repository specification.

The default reasoning chain is:

```text
Business Problem
    ↓
Requirement / Business Rule
    ↓
Use Case
    ↓
Bounded Context
    ↓
Aggregate / Invariant
    ↓
Architecture Decision
    ↓
Module Ownership
    ↓
Command or Query Path
    ↓
Persistence / Integration
    ↓
External Contract
    ↓
Implementation
    ↓
Verification
```

Never begin feature design from:

```text
Which service class should I add this method to?
```

Begin from:

```text
What business behavior is being implemented,
and which architectural boundary owns it?
```

---

## 2. Repository Authorities

Use the references in this skill together with the repository documentation.

Load the following files as required:

```text
references/document-authority.md
references/backend-architecture-map.md
references/feature-implementation-workflow.md
references/domain-application-rules.md
references/cqrs-persistence-rules.md
references/integration-security-rules.md
references/verification-checklist.md
```

Do not load every document mechanically.

Determine which architectural dimensions are relevant to the requested feature and inspect those sources.

---

## 3. Mandatory Feature Reasoning Pipeline

Every non-trivial backend feature follows:

```text
CLASSIFY
    ↓
TRACE
    ↓
LOCATE
    ↓
MODEL
    ↓
DESIGN
    ↓
IMPACT
    ↓
IMPLEMENT
    ↓
VERIFY
```

---

## 4. CLASSIFY

Determine what kind of backend change is requested.

Possible categories include:

```text
new business capability
new command
aggregate behavior
new query
new read model
database schema change
REST endpoint
cross-module interaction
domain event
integration event
Kafka consumer
cache behavior
security rule
authorization
scheduled/background process
external provider integration
```

A feature may belong to several categories.

Classification determines which repository documents must be inspected.

---

## 5. TRACE

Trace the feature back to business authority.

Find where applicable:

```text
Problem
→ FR / NFR / BR
→ Use Case
```

Determine:

```text
actor
preconditions
trigger
main flow
alternative flows
failure flows
postconditions
business rules
non-functional constraints
```

Do not introduce undocumented business behavior while implementing a feature.

If the requested feature requires behavior absent from the specification, report:

```text
SPECIFICATION GAP
```

before inventing policy.

---

## 6. LOCATE

Identify architectural ownership.

Determine:

```text
bounded context
Gradle module
aggregate root
application use case
query owner
database owner
event owner
API owner
```

Do not choose ownership because one existing class already has useful dependencies.

Ownership follows business boundaries and the module dependency specification.

---

## 7. MODEL

Before coding, model the feature.

For a command-side feature identify:

```text
command
aggregate loaded
aggregate behavior invoked
invariants checked
state changed
domain events produced
repository operations
transaction boundary
outbox effects
result returned
```

For a query-side feature identify:

```text
query intent
read contract
source of truth or projection
consistency requirement
filtering
sorting
pagination
SQL/read model
returned view
```

For integration features identify:

```text
producer
event
consumer
delivery semantics
idempotency
ordering
retry
failure behavior
```

---

## 8. DESIGN

Design the smallest feature that satisfies the specification.

Do not introduce:

```text
generic abstractions
framework wrappers
shared utility modules
additional events
new read stores
new module dependencies
```

unless the feature actually requires them.

Prefer explicit domain and architectural vocabulary.

---

## 9. IMPACT

Before implementation, determine whether the feature affects:

```text
Business Rule
Aggregate Invariant
Module Boundary
Transaction Boundary
Database Schema
Database Constraint
Command Contract
Query Contract
HTTP Contract
Error Code
Permission
Domain Event
Integration Event
Read Model
Kafka
Redis
Security
Observability
Performance
Testing
```

An unaffected category does not need work, but affected categories must not be overlooked.

---

## 10. IMPLEMENT

Implementation must follow responsibility ownership.

Conceptually:

```text
api
    transport adaptation

application
    use-case orchestration

domain
    business behavior and invariants

persistence/infrastructure
    technical storage and integration
```

Do not allow implementation convenience to collapse these responsibilities.

---

## 11. Command Implementation Default

For authoritative business mutation:

```text
HTTP / caller
    ↓
API adapter
    ↓
Command / application use case
    ↓
Load aggregate
    ↓
Execute aggregate behavior
    ↓
Check invariant
    ↓
Persist aggregate
    +
Persist outbox event
    ↓
Commit
```

The exact path may vary according to the documented feature, but authoritative state must preserve the intended consistency boundary.

---

## 12. Query Implementation Default

For reads:

```text
HTTP / caller
    ↓
Query entry point
    ↓
Query service
    ↓
JDBC / projection / approved read store
    ↓
Read model
    ↓
Response
```

Do not reconstruct aggregates merely to answer read-only questions unless the architecture explicitly requires authoritative aggregate reads.

---

## 13. Event Integration Default

For asynchronous integration:

```text
Aggregate Mutation
    ↓
Transactional Outbox
    ↓
Commit
    ↓
Outbox Relay
    ↓
Kafka
    ↓
Consumer
    ↓
Projection / downstream reaction
```

Never replace an intentionally asynchronous module relationship with a direct synchronous call simply because it is easier to implement.

---

## 14. Database Authority

PostgreSQL is the authoritative persistent store unless a repository specification explicitly states otherwise.

Derived stores must not become alternative sources of truth.

The documented architecture distinguishes:

```text
PostgreSQL
    authoritative business state

MongoDB
    scoped read projections

Elasticsearch
    search read model

Redis
    cache / rate limiting / documented operational coordination
```

A projection must remain conceptually rebuildable from authoritative state or the event backbone according to the CQRS architecture.

---

## 15. Module Boundary Rule

Before creating an import between modules, inspect the Module Dependency Diagram.

Do not infer legal dependencies from the DDD context map.

A business relationship may intentionally be implemented through asynchronous events and therefore create no compile-time dependency.

Internal packages of another module are not integration APIs.

---

## 16. Security Rule

Every protected backend operation must enforce authorization server-side.

UI behavior is irrelevant to backend authorization correctness.

For a secured feature determine:

```text
authentication
permission
resource ownership
rate limit
data classification
audit requirement
provider verification
```

Use the Permission Matrix and Security specification.

---

## 17. Contract Rule

For features exposed outside the owning module, identify applicable contracts.

Possible contracts include:

```text
OpenAPI
Integration Contract
Error Codes
Permission Matrix
event schema
published module interface
```

Internal implementation refactoring must not accidentally modify these contracts.

---

## 18. Verification Rule

Implementation is not complete when code compiles.

Verification must demonstrate the risks introduced by the feature.

Possible evidence includes:

```text
domain unit test
application test
repository integration test
database constraint test
Flyway migration test
API contract test
module architecture test
event idempotency test
concurrency test
security test
load/benchmark test
```

Use `references/verification-checklist.md`.

---

## 19. Required Pre-Implementation Output

For substantial features, produce:

```markdown
# Backend Feature Analysis

## 1. Business Intent

## 2. Authoritative Requirements

## 3. Domain Ownership

## 4. Architecture Constraints

## 5. Command / Query Model

## 6. Data Model Impact

## 7. Integration Impact

## 8. API and Security Impact

## 9. Implementation Design

## 10. Implementation Sequence

## 11. Verification

## 12. Risks

## 13. Non-Goals

## Implementation Contract

### Must Preserve
### Must Implement
### Must Not Introduce
### Required Verification
```

The implementation phase should not need to rediscover architecture decisions already resolved here.

---

## 20. Definition of a Correct Backend Feature

A backend feature is correct only when it:

```text
implements the specified business behavior,
preserves domain invariants,
belongs to the correct bounded context,
respects module boundaries,
uses the intended command/query path,
preserves transaction semantics,
uses the correct source of truth,
maintains contract compatibility,
handles failure explicitly,
enforces backend security,
and provides appropriate verification evidence.
```

Code correctness and architecture correctness are both part of feature correctness.
