# `references/domain-application-rules.md`

# Domain and Application Rules

## Purpose

This file defines how business behavior must be placed between the domain and application layers.

---

## 1. Domain Owns Business Invariants

The domain layer owns rules that determine whether business state is valid.

Examples:

```text
whether an order may transition state
whether stock may be reserved
whether a promotion may be redeemed
whether a refund would exceed captured amount
whether an entity may perform a particular transition
```

Do not duplicate these decisions in:

```text
controller
application service
repository
event consumer
```

when the aggregate is the rightful consistency owner.

---

## 2. Aggregate Is a Consistency Boundary

An aggregate exists to protect invariants that must remain valid together.

Do not identify aggregates merely from database relationships.

Do not make every table-backed entity an aggregate root.

Do not expand an aggregate only to make navigation convenient.

Use the Domain Model.

---

## 3. Aggregate Behavior

Prefer business operations:

```java
order.place(...);
stockItem.reserve(...);
payment.recordAttempt(...);
promotion.claim(...);
```

over external state manipulation such as:

```java
order.setStatus(...);
stockItem.setReserved(...);
```

The aggregate API should express legal business transitions.

---

## 4. Entities

An entity has identity and lifecycle inside an aggregate or as its own aggregate root according to the Domain Model.

Child entities must not become separately mutable repositories merely because they have identifiers.

If child state and aggregate counters must change atomically, preserve the aggregate boundary.

---

## 5. Value Objects

Use value objects when domain meaning requires constrained value semantics.

Examples may include:

```text
Money
Quantity
Sku
Address
IdempotencyKey
ValidityWindow
```

Do not create value objects mechanically for every primitive.

Their purpose is domain meaning and validity, not class-count inflation.

---

## 6. Domain Services

Use a domain service when business behavior:

```text
is genuinely domain behavior
does not naturally belong to one aggregate/entity/value object
```

Do not create domain services merely as stateless containers for aggregate logic.

---

## 7. Domain Events

A domain event represents a completed business fact.

Prefer:

```text
OrderPlaced
StockReserved
PaymentCaptured
```

over vague technical names:

```text
OrderUpdated
EntityChanged
```

Event publication must not move the actual invariant outside the aggregate.

---

## 8. Repository Abstraction

A domain repository represents access to aggregate persistence where appropriate.

It should speak aggregate vocabulary.

Avoid generic repository APIs that erase business semantics solely for code reuse.

Read-side query repositories are separate concerns and need not mimic aggregate repositories.

---

## 9. Application Layer Responsibility

The application layer orchestrates use cases.

Typical sequence:

```text
accept command
authenticate/obtain actor context where appropriate
load required state
invoke domain behavior
persist changes
record events
return application result
```

It owns orchestration, not core invariant logic.

---

## 10. Transaction Ownership

The application use case normally owns the local transaction boundary for a business command.

Define explicitly:

```text
what must commit together
what happens after commit
what may be eventually consistent
```

Do not allow controller transaction annotations to become accidental business transaction definitions.

---

## 11. Application Policy

Some rules legitimately belong outside an aggregate.

Examples can include:

```text
cross-aggregate orchestration
permission-aware use-case policy
non-authoritative advisory checks
provider coordination
scheduler-driven behavior
```

A rule belongs in application when it describes the use-case workflow rather than the internal validity of one aggregate.

---

## 12. Cross-Aggregate Behavior

Do not make one aggregate directly mutate another.

Use application orchestration or the documented event/integration mechanism.

If atomic consistency across multiple aggregates appears required, verify that the Domain Model actually defines such a consistency requirement before creating a large transaction.

---

## 13. No Framework Leakage

Domain behavior must not depend on:

```text
HTTP
Spring MVC
Kafka producer APIs
RedisTemplate
JdbcTemplate
JSON
ProblemDetail
```

unless an explicit project decision states otherwise.

Infrastructure adapts the domain to technical mechanisms.

---

## 14. Domain Implementation Checklist

Before implementing domain behavior answer:

```text
Which BR-* does this implement?

Which aggregate owns it?

What state can change?

What invalid states are prevented?

What happens on a duplicate invocation?

Does concurrency affect the invariant?

Does the operation emit a business fact?

What tests prove the invariant?
```

