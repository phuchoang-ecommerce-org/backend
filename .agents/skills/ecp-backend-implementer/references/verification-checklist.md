# `references/verification-checklist.md`

# Backend Feature Verification Checklist

## Purpose

This checklist determines whether a backend feature has sufficient evidence of correctness.

Apply only the sections relevant to the feature, but do not skip a section merely because verification is inconvenient.

---

## 1. Requirement Verification

```text
[ ] Feature traces to the intended FR / BR / NFR / Use Case.

[ ] No undocumented business behavior was added.

[ ] All relevant acceptance/failure flows are implemented.

[ ] Non-goals remain outside the implementation.
```

---

## 2. Domain Verification

When aggregate behavior changes:

```text
[ ] Relevant aggregate is correct according to Domain Model.

[ ] Business invariants are enforced by the owning aggregate or designated mechanism.

[ ] Valid transition succeeds.

[ ] Invalid transition fails.

[ ] Duplicate invocation behavior is understood.

[ ] Entity identity remains correct.

[ ] Value-object validation remains correct.

[ ] Domain events represent completed business facts.

[ ] Domain tests operate without infrastructure where practical.
```

---

## 3. Application Verification

```text
[ ] Use-case orchestration follows the specified flow.

[ ] Transaction boundary is explicit.

[ ] Correct repositories/services are used.

[ ] Domain behavior is invoked rather than duplicated.

[ ] Cross-aggregate coordination is intentional.

[ ] Failure behavior matches the use case.

[ ] Application tests cover orchestration where necessary.
```

---

## 4. Database Verification

```text
[ ] Flyway migration exists when schema changes.

[ ] Migration is valid against the actual database.

[ ] Required primary/foreign/unique/check constraints exist.

[ ] Required version column exists.

[ ] Indexes support material query patterns.

[ ] Existing data remains compatible with the migration.

[ ] ORM mappings agree with schema.

[ ] Database-enforced BR-* rules have integration tests.
```

---

## 5. Persistence Verification

```text
[ ] Write repository preserves aggregate semantics.

[ ] Read query returns the required view.

[ ] Row mapping is correct.

[ ] Nullability semantics are correct.

[ ] Monetary values preserve exact precision.

[ ] Query filtering is correct.

[ ] Sort ordering is deterministic.

[ ] Pagination does not skip or duplicate records.

[ ] Repository tests use the database engine when behavior depends on database-specific semantics.
```

---

## 6. Concurrency Verification

When concurrent writes are possible:

```text
[ ] Lost updates are prevented.

[ ] Optimistic locking conflict is tested.

[ ] Reservation semantics remain atomic where required.

[ ] Duplicate requests do not produce duplicate commercial effects.

[ ] Idempotency semantics remain correct.

[ ] Retry after conflict is intentionally defined.

[ ] High-contention invariant has a concurrency test where required.
```

---

## 7. CQRS Verification

```text
[ ] Commands change authoritative state through the intended path.

[ ] Queries use the intended read source.

[ ] Write side does not depend on projection state as authority unless explicitly specified.

[ ] Projection consistency classification is correct.

[ ] Read-after-write behavior is correct.

[ ] Projection remains rebuildable.

[ ] New read model has an explicitly documented store.
```

---

## 8. Outbox Verification

When a command emits integration events:

```text
[ ] Aggregate state and outbox record commit atomically.

[ ] No event is lost between DB commit and Kafka publication under the intended failure model.

[ ] Relay can retry publication.

[ ] Event identity remains stable across retries.

[ ] Publishing does not cause duplicate business effects downstream.
```

---

## 9. Kafka Consumer Verification

```text
[ ] Duplicate event delivery is safe.

[ ] Ordering assumptions are explicit.

[ ] Consumer transaction behavior is correct.

[ ] Failure produces retry/redelivery behavior as specified.

[ ] Consumer idempotency mechanism is tested.

[ ] Projection event guards are tested where applicable.

[ ] Poison-message handling follows platform policy.

[ ] Consumer failure is observable.
```

---

## 10. Projection Verification

```text
[ ] Projection source events are known.

[ ] Duplicate processing does not corrupt projection state.

[ ] Out-of-order handling follows the projection policy.

[ ] Projection can be rebuilt according to CQRS rules.

[ ] Rebuild does not require a coordinated system outage if architecture forbids one.

[ ] Projection is never the only copy of authoritative business information.
```

---

## 11. Redis Verification

When Redis is involved:

```text
[ ] Redis role is explicitly classified.

[ ] Key format follows the platform specification.

[ ] TTL is correct.

[ ] Cache miss reaches authoritative data.

[ ] Mutation triggers required invalidation.

[ ] Redis failure follows documented fail-open/fail-closed semantics.

[ ] Stale cache cannot authorize an invalid authoritative business transition.

[ ] Rate-limit retry-after behavior is correct where applicable.
```

---

## 12. Module Architecture Verification

```text
[ ] No forbidden module edge was introduced.

[ ] No implementation/internal package is imported across a forbidden boundary.

[ ] Shared-kernel use is justified.

[ ] Published interface is used where required.

[ ] Event type lives in the specified location.

[ ] Dependency graph remains acyclic.

[ ] Spring Modulith verification passes.

[ ] Applicable ArchUnit rules pass.
```

---

## 13. REST API Verification

When the feature exposes HTTP behavior:

```text
[ ] Operation agrees with OpenAPI.

[ ] Request shape is correct.

[ ] Validation constraints are correct.

[ ] Response shape is correct.

[ ] HTTP status is correct.

[ ] Problem response follows the shared contract.

[ ] Stable error code is used.

[ ] Correlation id is preserved.

[ ] Breaking compatibility was not introduced accidentally.
```

---

## 14. Security Verification

```text
[ ] Authentication requirement is correct.

[ ] Backend permission check is enforced.

[ ] Object-level authorization is enforced where applicable.

[ ] Permission Matrix is satisfied.

[ ] Rate-limit bucket is correct where applicable.

[ ] Sensitive information is absent from logs.

[ ] Sensitive information is absent from unauthorized responses.

[ ] Audit behavior is implemented where required.

[ ] Provider callback authenticity is verified where applicable.

[ ] Security.md verification requirements are satisfied.
```

---

## 15. Idempotency Verification

For idempotent operations:

```text
[ ] Same logical request uses the same idempotency identity.

[ ] Duplicate request does not repeat the commercial effect.

[ ] Previously completed result can be resolved correctly where required.

[ ] Concurrent requests with the same key are safe.

[ ] Idempotency storage lifecycle follows the specification.
```

---

## 16. External Provider Verification

```text
[ ] Provider timeout behavior is tested.

[ ] Provider duplicate callback is safe.

[ ] Invalid callback authentication is rejected.

[ ] Provider failure cannot create an impossible domain state.

[ ] Unknown outcome is represented distinctly from confirmed failure where required.

[ ] Provider reference is persisted correctly.

[ ] Retry behavior is safe.
```

---

## 17. Error Verification

```text
[ ] Business failure maps to the intended error code.

[ ] Validation failure is distinguishable from domain failure.

[ ] Infrastructure failure does not leak implementation detail.

[ ] Conflict/error retryability is represented correctly.

[ ] Error responses preserve correlation information.
```

---

## 18. Observability Verification

```text
[ ] Correlation id survives relevant flow boundaries.

[ ] Important failures are logged at the appropriate layer.

[ ] Logs include sufficient diagnostic context.

[ ] Logs do not contain secrets or prohibited PII.

[ ] Outbox relay failure is observable.

[ ] Kafka consumer failure is observable.

[ ] Projection lag is observable where required.

[ ] Provider failure is observable where required.
```

---

## 19. Performance Verification

When the feature may materially affect performance:

```text
[ ] Query access pattern is supported by indexes.

[ ] N+1 queries were not introduced.

[ ] Large aggregates are not loaded unnecessarily for query behavior.

[ ] Query plan is examined when appropriate.

[ ] Cache does not weaken correctness.

[ ] Required NFR load/latency benchmark is performed.

[ ] Event throughput implications are considered.
```

---

## 20. Regression Verification

```text
[ ] Existing domain tests pass.

[ ] Existing module tests pass.

[ ] Existing integration tests pass.

[ ] Existing architecture gates pass.

[ ] Existing OpenAPI contract checks pass.

[ ] No unrelated public contract changed.

[ ] No unintended event schema change occurred.
```

---

## 21. Final End-to-End Trace

For a command feature verify the entire path:

```text
caller
→ API
→ application
→ aggregate
→ invariant
→ repository
→ database
→ outbox
→ commit
→ relay
→ Kafka
→ consumer
→ projection/downstream behavior
```

Mark any irrelevant stage as not applicable.

For a query feature:

```text
caller
→ API
→ query service
→ read source
→ SQL/projection
→ mapping
→ response
```

---

## 22. Completion Questions

Before declaring the feature complete, answer:

```text
Which requirement did we implement?

Which bounded context owns it?

Which aggregate protects its invariants?

What state is authoritative?

What commits atomically?

What becomes eventually consistent?

What happens if the same command arrives twice?

What happens if two commands race?

What happens if Kafka delivers twice?

What happens if Redis is unavailable?

What happens if a provider times out?

Which external contract changed?

Which permission protects the operation?

Which test proves each critical guarantee?
```

---

## 23. Verification Summary Format

For substantial features produce:

```markdown
# Backend Verification Summary

## Requirements
PASS / BLOCKED / NOT APPLICABLE

Evidence:
- ...

## Domain
PASS / BLOCKED / NOT APPLICABLE

Evidence:
- ...

## Persistence
PASS / BLOCKED / NOT APPLICABLE

Evidence:
- ...

## CQRS
PASS / BLOCKED / NOT APPLICABLE

Evidence:
- ...

## Integration
PASS / BLOCKED / NOT APPLICABLE

Evidence:
- ...

## Architecture
PASS / BLOCKED / NOT APPLICABLE

Evidence:
- ...

## Security
PASS / BLOCKED / NOT APPLICABLE

Evidence:
- ...

## Performance
PASS / BLOCKED / NOT APPLICABLE

Evidence:
- ...

## Remaining Risks
- ...
```

Never report `PASS` when required verification was not actually executed.

---

## 24. Definition of Verified

A backend feature is verified when there is sufficient evidence that:

```text
the specified business behavior works,
domain invariants remain true,
database guarantees remain valid,
concurrent execution remains safe,
module boundaries remain valid,
integration is retry-safe and idempotent,
external contracts remain coherent,
security controls are enforced,
and applicable NFRs remain satisfied.
```
