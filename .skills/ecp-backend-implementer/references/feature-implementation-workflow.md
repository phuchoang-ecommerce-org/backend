
# `references/feature-implementation-workflow.md`

# Backend Feature Implementation Workflow

## Purpose

This document defines the operational workflow an agent should follow when implementing a backend feature.

---

## 1. Phase 1 — Understand the Feature

Translate the request into business intent.

Record:

```text
actor
trigger
business goal
successful outcome
important failure outcomes
```

Avoid translating immediately into classes.

---

## 2. Phase 2 — Trace Requirements

Find the relevant:

```text
FR
BR
NFR
Use Case
```

Extract only requirements relevant to implementation.

Record their identifiers where available.

---

## 3. Phase 3 — Identify Domain Ownership

Determine:

```text
bounded context
aggregate
entities/value objects involved
domain service if any
business invariant
domain event
```

Ask:

```text
Which aggregate must be internally consistent when this operation completes?
```

---

## 4. Phase 4 — Determine Command or Query

Classify the operation.

### Command

Changes authoritative state.

Determine:

```text
command input
aggregate
business behavior
transaction
result
event
```

### Query

Answers a question.

Determine:

```text
read contract
consistency
data source
filter
sort
pagination
view shape
```

A feature may contain both paths.

Analyze them independently.

---

## 5. Phase 5 — Check Module Boundaries

Identify all involved modules.

For every potential direct dependency ask:

```text
Is this edge allowed by Module Dependency Diagram?
```

If not, inspect whether the architecture expects:

```text
event communication
published interface
shared contract
```

Do not introduce forbidden coupling.

---

## 6. Phase 6 — Design the Domain Change

For command features, define the aggregate operation in business language.

Example:

```text
StockItem.reserve(...)

Order.place(...)

Payment.recordSucceededAttempt(...)

Promotion.claimRedemption(...)
```

Prefer behavioral methods over external manipulation of entity state.

Document:

```text
preconditions
invariants
state transition
domain events
result
```

---

## 7. Phase 7 — Design the Application Use Case

The application layer should make orchestration obvious.

Define:

```text
input command
repositories required
services required
aggregate load
aggregate invocation
persistence
outbox/event handling
transaction boundary
returned result
```

Do not put invariant logic here if the invariant belongs to the aggregate.

---

## 8. Phase 8 — Design Persistence

For write-side behavior determine:

```text
aggregate persistence
versioning
constraints
new columns/tables
indexes
Flyway migration
```

For read-side behavior determine:

```text
SQL/read store
joins
aggregation
pagination
index support
row mapping
projection mapping
```

Use the persistence mechanism selected by ECP's architecture rather than choosing technology locally.

---

## 9. Phase 9 — Design Concurrency

If the feature can be invoked concurrently, explicitly answer:

```text
Can two actors modify this aggregate simultaneously?

What prevents lost updates?

Is optimistic locking sufficient?

Does a reservation model participate?

What happens on conflict?

Can the operation be safely retried?
```

Concurrency behavior is part of the feature.

---

## 10. Phase 10 — Design Events

If the command produces a business event, determine:

```text
event name
aggregate id
event payload
event owner
event visibility
outbox storage
downstream consumers
```

For integration-visible events also determine:

```text
envelope
schema compatibility
partition key
ordering
```

Do not publish an event only because an implementation callback is convenient.

---

## 11. Phase 11 — Design Consumers

For each consumer answer:

```text
What business effect occurs?

Can the same message arrive twice?

Can messages arrive out of order?

How is processing made idempotent?

What happens when processing fails?

Can the projection be rebuilt?
```

Never assume exactly-once business execution merely because Kafka is used.

---

## 12. Phase 12 — Design Redis Behavior

If Redis participates, classify its role:

```text
cache
rate limiter
operational coordination
```

For cache behavior specify:

```text
key
authoritative loader
TTL
invalidation trigger
failure policy
```

For rate limiting specify:

```text
bucket
caller identity
algorithm/policy
fail-open or fail-closed behavior
```

Do not introduce Redis without a defined consistency and failure model.

---

## 13. Phase 13 — Design API Contract

If the feature is externally exposed, determine:

```text
HTTP method
path
request
response
validation
authentication
permission
idempotency requirement
error codes
correlation behavior
```

Compare with OpenAPI before implementing the controller.

Do not design the public contract accidentally from Java method signatures.

---

## 14. Phase 14 — Design Failure Semantics

Identify all meaningful failures.

Examples:

```text
business rule violation
not found
authorization denied
optimistic lock conflict
duplicate idempotency key
database unavailable
Redis unavailable
provider timeout
Kafka consumer failure
invalid provider callback
```

For each determine:

```text
business outcome
transaction outcome
retryability
external error
observability
```

---

## 15. Phase 15 — Design Verification

Before production implementation, define evidence.

Map:

```text
business invariant
    → domain test

use-case orchestration
    → application test

database behavior
    → repository integration test

constraint
    → database integration test

API
    → contract/API test

module boundary
    → architecture test

event delivery effect
    → consumer/integration test

concurrency
    → concurrent integration test

security
    → security test
```

---

## 16. Phase 16 — Produce Implementation Contract

Create:

```markdown
## Implementation Contract

### Must Preserve

Existing behavior and contracts that must remain unchanged.

### Must Implement

Required new behavior.

### Must Not Introduce

Forbidden shortcuts or architectural divergence.

### Transaction Boundary

Describe authoritative commit behavior.

### Consistency Model

Describe strong/eventual/advisory data.

### Integration Effects

Events, caches, projections, providers.

### Required Verification

Tests and architecture gates.
```

---

## 17. Phase 17 — Implement in Dependency Order

For command-centric work, prefer:

```text
domain
→ application
→ persistence
→ event/integration
→ API
→ verification
```

For query-centric work, prefer:

```text
query contract
→ persistence/read model
→ application/query entry
→ API
→ verification
```

This is guidance, not a mandatory mechanical ordering.

Keep each step independently testable.

---

## 18. Phase 18 — Review the Completed Flow

Trace the implementation end-to-end.

For commands:

```text
request
→ application
→ aggregate
→ persistence
→ commit
→ outbox
→ event
→ consumer
```

For queries:

```text
request
→ query
→ source
→ projection
→ response
```

Confirm there is no hidden alternate path bypassing intended ownership.
