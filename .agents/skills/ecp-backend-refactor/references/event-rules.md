# Event and Outbox Rules

## EV-01 — Preserve transactional outbox atomicity

A business state change and the outbox record announcing it must commit atomically when the architecture requires publication.

## EV-02 — Do not publish Kafka directly from command business code

Direct broker publication must not replace the transactional outbox merely to simplify code.

## EV-03 — Do not replace durable integration events with in-process events

Spring/application events may serve local concerns where specified, but they are not a substitute for durable cross-module integration when Kafka/outbox semantics are required.

## EV-04 — Consumers own local integration-contract records

Avoid Java-type coupling from consumer to publisher implementation. Deserialize the integration envelope into consumer-owned contract types.

## EV-05 — Consumers are idempotent

Refactoring must preserve duplicate-delivery safety. Prefer database-enforced idempotency/unique guards inside the consuming transaction over check-then-act `SELECT` patterns.

## EV-06 — Preserve ordering guarantees

Do not introduce parallelism, retry routing, partition changes, or relay behavior that breaks per-aggregate ordering assumptions.

## EV-07 — Keep one publication path

Do not create a second event publication mechanism during refactor. Transitional duplication must be explicitly controlled and removed before completion.

## EV-08 — Integration mechanics stay outside domain logic

Domain/application code may produce domain facts or outbox intent according to project design, but Kafka record construction, broker metadata, retries, offsets, serialization, and consumer-group mechanics belong to infrastructure.

## EV-09 — Prefer redelivery over loss where architecture requires it

Preserve the sequence "publish then mark" and "handle then commit" semantics. Refactors must assume duplicate delivery is normal and safe.

## EV-10 — Projection updates include ordering/idempotency guards

Do not separate a projection guard from the write in a way that creates race conditions. Preserve atomic guard-and-update semantics.
