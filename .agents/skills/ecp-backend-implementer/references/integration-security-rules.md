# `references/integration-security-rules.md`

# Integration and Security Rules

## Purpose

This file governs events, Kafka, outbox, module integration, external providers, API security, errors, and observability.

---

## 1. Transactional Outbox

When an authoritative state transition must publish an integration event, persist:

```text
business state
+
outbox event
```

within the required local transaction.

The relay publishes later.

Do not directly publish to Kafka as the only record of the event before or after an unrelated database commit when the architecture requires transactional outbox.

---

## 2. Event Envelope

External/integration-visible events must follow the Integration Contract.

Inspect:

```text
event id
event type
aggregate id
timestamp
correlation metadata
version/schema
```

Use the exact contract rather than inventing module-specific envelopes.

---

## 3. Partitioning and Ordering

Use the partition key defined by the integration architecture.

When ordering is relevant determine:

```text
ordering scope
aggregate identity
consumer ordering assumptions
```

Do not assume global event ordering.

---

## 4. Consumer Idempotency

Design every durable consumer assuming duplicate delivery is possible.

A consumer must ensure repeated delivery does not duplicate the business effect.

Possible mechanisms must follow the repository specification, such as:

```text
consumer idempotency table
projection version/event guard
terminal entity state
idempotency key
```

Choose the mechanism appropriate to that consumer.

---

## 5. Consumer Failure

Define what occurs when processing fails.

Determine:

```text
retry behavior
transaction rollback
message redelivery
poison message behavior
observability
```

Do not catch and discard failures simply to keep the consumer running.

---

## 6. Projection Consumer

For projection consumers also determine:

```text
deduplication
ordering
version guard
rebuild behavior
```

Projection updates must remain deterministic enough to reconstruct derived state.

---

## 7. Module Integration

Before synchronous cross-module calls determine whether the dependency is allowed.

If the relationship is intentionally asynchronous, communicate through the documented event mechanism.

Do not create hidden coupling by importing another module's persistence adapter.

---

## 8. External Provider Integration

For payment, shipping, or other providers determine:

```text
request idempotency
timeout
provider retry
callback authenticity
duplicate callbacks
out-of-order callbacks
provider reference
partial failure
```

Provider success must be translated into domain behavior rather than directly modifying persistence records.

---

## 9. Authentication

Authentication establishes caller identity.

Do not confuse it with permission to perform a business action.

Use the project's authentication model rather than feature-specific authentication schemes.

---

## 10. Authorization

Every protected operation must enforce authorization on the backend.

Determine:

```text
required role/permission
resource ownership
administrative scope
```

Use the Permission Matrix.

Do not rely on callers to self-enforce permissions.

---

## 11. Object-Level Authorization

A valid role may still lack access to a specific resource.

For customer-owned resources determine:

```text
does the authenticated principal own this object?
```

Do not expose resources merely because the actor has a generic customer role.

---

## 12. Rate Limiting

When the feature belongs to a documented rate-limit bucket, preserve:

```text
bucket identity
caller identity
allowed behavior
retry-after semantics
fail-open/fail-closed policy
```

Do not add ad-hoc in-memory limits.

---

## 13. API Errors

Map failures to the stable error registry.

The layers should retain semantic separation:

```text
domain/application failure
        ↓
API error mapping
        ↓
stable error code / HTTP problem
```

Do not throw arbitrary HTTP-oriented exceptions throughout the domain.

---

## 14. Correlation

Preserve the platform correlation identifier through:

```text
HTTP
application logs
events
provider interaction where specified
```

Do not generate unrelated identifiers at every internal layer when one end-to-end operation should remain traceable.

---

## 15. Sensitive Data

Before adding data to:

```text
event
log
projection
cache
API response
```

determine its classification.

Do not propagate PII merely because the source aggregate contains it.

---

## 16. Audit

When an operation requires auditing, record the audit behavior according to the architecture.

Audit is not ordinary application logging.

Do not make audit correctness depend on mutable debug logs.

---

## 17. Integration Checklist

For every new boundary answer:

```text
Who owns the contract?

Is communication sync or async?

What happens on timeout?

What happens on duplicate delivery?

What happens out of order?

What can be retried?

What is idempotent?

What is authoritative?

How is correlation preserved?

How is failure observed?

What sensitive data crosses the boundary?
```