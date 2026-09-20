# Testing and Verification Rules

Refactoring is complete only when structural and behavioral verification passes.

## Minimum verification sequence

Run the narrowest relevant checks during each migration step, then the broader suite before completion.

Typical commands:

```bash
./gradlew compileJava
./gradlew test
./gradlew check
```

Use module-scoped Gradle tasks first when the repository is large, then root-level verification before final completion.

## Architecture verification

Where present, execute and preserve:

- Spring Modulith `ApplicationModules.verify()` tests;
- ArchUnit dependency/layer rules;
- JMolecules architecture verification;
- module dependency checks;
- forbidden-dependency tests.

## Test selection by refactor type

### Domain refactor

Require unit tests for aggregate invariants, state transitions, value-object validation, and edge cases.

### Application refactor

Require use-case tests proving orchestration, failure semantics, transaction assumptions, and port interaction.

### JPA persistence refactor

Require repository/integration tests against the real database behavior where mappings, constraints, locking, or transactions matter.

### JDBC query refactor

Require query integration tests covering filters, ordering, cursor/page boundaries, null handling, aggregation, and row mapping.

### Event/projection refactor

Require duplicate-delivery, ordering, idempotency, transaction, and projection-result tests.

### Cache/rate-limit refactor

Require failure-mode tests including cache miss, cache unavailable behavior, TTL semantics, invalidation, and fail-open/fail-closed behavior where applicable.

### API refactor

Require contract/controller tests for status, error code, validation, headers, and response shape.

## Verification principle

A refactor test should prove preserved behavior or preserved architecture. Do not rewrite tests merely to match a changed implementation if the old expectation represented a normative rule.
