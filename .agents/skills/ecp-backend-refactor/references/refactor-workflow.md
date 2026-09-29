# Refactor Workflow

## 1. DISCOVER

Inspect the affected files and their immediate dependency neighborhood.

Collect:

- module and package location;
- public entry points;
- inbound callers;
- outbound dependencies;
- database tables/queries involved;
- emitted/consumed events;
- Redis/Elasticsearch/MongoDB usage;
- tests and architecture checks;
- referenced business rules/ADRs/specifications.

Do not modify code during discovery.

## 2. CLASSIFY

Classify every relevant component by:

- bounded context;
- layer: domain/application/api/infrastructure/app;
- path: command/query/projection/event/cache/runtime;
- ownership: authoritative state vs derived state;
- synchronous vs asynchronous contract.

Create a compact responsibility map.

## 3. CONSTRAINT ANALYSIS

Identify what must remain true:

- business invariants;
- DB constraints/versioning;
- transaction boundaries;
- idempotency and ordering;
- external REST/event contracts;
- module dependencies;
- security behavior;
- consistency/read-your-writes assumptions;
- observability/correlation/audit behavior.

Mark unknowns explicitly. Do not infer missing business behavior.

## 4. TARGET DESIGN

Describe the target architecture before editing code.

For each moved responsibility, state:

- current owner;
- target owner;
- dependency direction after the move;
- interface/port boundary if needed;
- behavior that stays unchanged.

Prefer the smallest design that restores correct responsibility ownership.

## 5. REFACTOR PLAN

Create ordered migration steps. Each step should ideally:

- compile independently;
- preserve behavior;
- have a clear verification command/test;
- reduce transitional duplication;
- avoid cross-cutting rewrites.

When a large extraction is needed, use branch-by-abstraction or parallel introduction/migration/removal rather than a flag-day rewrite.

## 6. IMPLEMENT

For each step:

1. make one cohesive change;
2. compile;
3. run focused tests;
4. inspect dependency direction;
5. continue only when the step is stable.

Do not opportunistically refactor unrelated code.

## 7. VERIFY

Verify:

- behavior;
- architecture;
- contracts;
- persistence semantics;
- concurrency/idempotency if relevant;
- full affected-module test suite;
- root `check` when practical/required.

Finally compare the resulting code against the target design and list any intentionally deferred work.
