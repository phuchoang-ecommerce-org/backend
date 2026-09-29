---
name: ecp-backend-refactor
description: Architecture-aware refactoring protocol for the ECP Java/Spring modular monolith. Use when analyzing or refactoring existing backend code while preserving business invariants, module boundaries, CQRS semantics, transaction boundaries, integration contracts, security guarantees, and operational behavior.
version: 1.0.0
---

# Backend Refactor Skill

## Purpose

This skill governs refactoring of the existing ECP backend. It is not a generic Java cleanup guide and it is not a feature-implementation skill. Its purpose is to transform existing code toward the project's intended architecture while preserving observable behavior, business invariants, consistency guarantees, integration contracts, security controls, and operational semantics.

The refactoring sequence is:

`existing code + project specification -> architecture diagnosis -> target design -> incremental migration -> verification`

Never jump directly from a code smell to a code change.

## Mandatory Operating Rule

Before proposing or applying a refactor, classify the affected code by:

1. bounded context and Gradle module;
2. architectural layer;
3. command, query, projection, event, cache, or runtime role;
4. business invariants and external contracts it protects;
5. transaction and consistency semantics;
6. inbound and outbound dependencies;
7. verification evidence currently protecting the behavior.

Only after this classification may a target structure be proposed.

## Refactoring Priorities

When goals conflict, preserve them in this order:

1. business invariants;
2. externally visible API and integration contracts;
3. transaction semantics and atomicity;
4. consistency semantics and concurrency guarantees;
5. bounded-context and module boundaries;
6. security guarantees;
7. observability and auditability;
8. architectural clarity;
9. maintainability and readability;
10. duplication reduction.

Do not trade a higher-priority property for a lower-priority cleanup objective.

## Required Workflow

Execute the workflow defined in `references/refactor-workflow.md`:

`DISCOVER -> CLASSIFY -> CONSTRAINT ANALYSIS -> TARGET DESIGN -> REFACTOR PLAN -> IMPLEMENT -> VERIFY`

For analysis-only requests, stop after `REFACTOR PLAN` unless implementation is explicitly requested.

For implementation requests, work incrementally. Prefer a sequence of small compilable changes over a single large rewrite.

## Architecture Rules

Load and apply:

- `references/architecture-rules.md`
- `references/layer-rules.md`
- `references/cqrs-rules.md`
- `references/persistence-rules.md`
- `references/event-rules.md`
- `references/testing-rules.md`
- `references/refactor-smells.md`

Treat these rules as project constraints, not suggestions.

## Evidence Requirement

Every significant refactor recommendation must identify:

- the current structural problem;
- the violated or weakened architectural rule;
- the invariant or runtime property at risk;
- the target responsibility owner;
- the smallest migration path;
- the verification that proves the migration safe.

Avoid subjective claims such as "cleaner" or "better" unless they are tied to a concrete responsibility, dependency, invariant, or testability improvement.

## Refactor Boundaries

Do not introduce new business behavior during a refactor unless explicitly requested.

Do not silently change:

- REST response semantics;
- error codes;
- event envelope or event meaning;
- database constraints;
- transaction boundaries;
- optimistic locking behavior;
- idempotency behavior;
- cache authority rules;
- consistency classification;
- security decisions;
- module dependency permissions.

If an architectural correction requires one of these changes, report it separately as a behavioral or architectural decision rather than hiding it inside a refactor.

## Preferred Refactor Unit

Refactor by cohesive architectural responsibility rather than by arbitrary file count. Typical units include:

- domain aggregate and invariant enforcement;
- application command use case;
- application query contract;
- JPA write persistence adapter;
- JDBC query service;
- REST adapter;
- event/outbox publishing path;
- Kafka projection consumer;
- Redis cache/rate-limit adapter;
- app/runtime wiring;
- architecture and integration tests.

A whole bounded context may be planned together, but implementation should normally migrate one responsibility slice at a time.

## Output Contract

For a refactor analysis, produce these sections:

1. Current Structure
2. Architectural Findings
3. Refactoring Constraints
4. Target Structure
5. Ordered Migration Steps
6. Regression Risks
7. Verification Plan

For implementation, additionally report:

8. Changes Applied
9. Verification Results
10. Remaining Follow-ups

## Stop Conditions

Stop and surface the conflict when:

- the source code contradicts a normative project rule;
- two normative project rules conflict;
- an invariant cannot be identified from available evidence;
- a migration would require an external contract change;
- a test failure indicates behavior changed unexpectedly.

Do not invent missing business rules to complete a refactor.
