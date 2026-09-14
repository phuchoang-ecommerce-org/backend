<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Definition of Ready and Done — ECP

**Document type:** Derived lane quality gate · **Canonical source:** [Definition of Done](../definition-of-done.md)

---

This view makes the backend slice’s acceptance gates directly usable. The canonical document controls where wording differs.

## 2. Definition of Ready

An item may not enter a sprint until all six hold. An item that fails goes back to Refinement — never into the sprint "to be clarified during."

| # | Criterion |
|---|---|
| 1 | Acceptance criteria exist as Given/When/Then in [`../BA-docs/user-stories/`](../../BA-docs/user-stories/README.md), **including one per exception flow** of the source use case |
| 2 | Its OpenAPI `operationId`s are named, and each one exists in `openapi.yaml` |
| 3 | Its `ecp-web` route is named, or the story is explicitly marked as having no route ([`Routing.md`](../../SA-docs/03-frontend/Routing.md) §10.2) |
| 4 | Both lane slices are estimated, `0` included |
| 5 | Its permission-matrix cell is identified for every operation it touches |
| 6 | No unresolved contract question remains. If one exists, it is amended per [`integration-plan.md`](../integration-plan.md) §5 **before** the sprint, not during it |

---

## 3. Definition of Done — Backend Slice

| # | Criterion | Source |
|---|---|---|
| 1 | **Every business rule the story touches has an L1 test naming its `BR-` id** | Testing Strategy §10 |
| 2 | **Every exception flow of a `Must` use case is covered, not only the main scenario.** The exception flows are the requirement | Testing Strategy §10; use-cases README §1 |
| 3 | L2 architecture gate green: `ApplicationModules.verify()`, the `@ApplicationModule(allowedDependencies)` allow-list, and the ArchUnit layer rules | [`ADR-0018`](../../SA-docs/01-system/ADR/ADR-0018-architecture-governance-ci-gate.md) |
| 4 | L3 web-slice test asserts the response shape, the problem+JSON mapping, and the pagination envelope | Testing Strategy §3 |
| 5 | The operation appears in the contract test in **both** directions | Testing Strategy §6.6 |
| 6 | Its permission-matrix cell is asserted — an operation with no cell fails the build | [`Permission Matrix.md`](../../SA-docs/04-shared/Permission%20Matrix.md) |
| 7 | An audit entry is written where `UC-AUD-01` requires one | [`14-audit-access-control.md`](../../BA-docs/use-cases/14-audit-access-control.md) |
| 8 | Any Flyway migration is **backward-compatible** — two application versions run against one schema during a rolling restart | [`ADR-0029`](../../SA-docs/01-system/ADR/ADR-0029-flyway-versioned-schema-migrations.md); Deployment §8 |
| 9 | No foreign key crosses a module table prefix | [`Module Dependency Diagram.md`](../../SA-docs/02-backend/Module%20Dependency%20Diagram.md) §7 |
| 10 | Every domain event the story publishes has an L4 test asserting a consumer reacts **idempotently** — redelivery leaves the document byte-identical | Testing Strategy §10, §6.3 |
| 11 | Structured log lines carry the correlation id; any new Micrometer meter is registered | `NFR-OBS-01`, `-03`, `-04` |
| 12 | No credential, key, or connection string in an image, a log, an audit entry, or the repository | Deployment §5; `NFR-SEC-07` |

**Additionally, for a story with a concurrency guarantee** (`US-INV-01`, `US-ORD-05`, `US-PRM-03`): an **L5 test against Testcontainers PostgreSQL**, not H2. An approximation of a concurrency guarantee is not a guarantee, and this is the rule most likely to be traded away for build speed (Testing Strategy §6.4).

---

## 5. Definition of Done — the Story

A story is Done when **both** slices satisfy their lane's list **and**:

| # | Criterion |
|---|---|
| 1 | The Contract Sync checklist ([`integration-plan.md`](../integration-plan.md) §3) has passed for this story's domain |
| 2 | The story was **demonstrated against the running system** at a Sprint Review — not a screenshot, not a passing test |
| 3 | Every acceptance criterion in its user-story file is satisfied, exception-flow criteria included |
| 4 | Any contract drift it surfaced was amended in `openapi.yaml`, not worked around |
| 5 | Nothing was left as a `TODO` that the acceptance criteria required |

**The `Integrated` column is where this is enforced.** A merged, green slice sits there until the gate. Two lanes each reporting complete against a system nobody has run is the specific failure this column exists to prevent.

---

## 6. Definition of Done — a Release

The `AC-01`–`AC-06` table of [`Testing and Benchmark Strategy.md`](../../SA-docs/01-system/Testing%20and%20Benchmark%20Strategy.md) §11, completed honestly. Its current expected state at the end of Release 2:

| Criterion | Verified by | Expected status |
|---|---|---|
| `AC-01` Core workflows function correctly | L1 + L4 + the thin Playwright suite over `Must` use cases | **Met** |
| `AC-02` Rules enforced regardless of entry point | Each rule exercised through REST, scheduler, and Kafka paths | **Met** |
| `AC-03` New modules added with minimal modification | Worked example, reviewed | **Reviewed, not tested** — it is a design review, not a suite |
| `AC-04` Maintainable as complexity grows | L2 | **Met** once `EN-GATE-1` lands in S01 |
| `AC-05` Reporting does not impact transactions | `NFR-PERF-05` concurrent load | **Unverified — deferred** |
| `AC-06` Production-quality architecture | L5 + L6 + Security §12, **and** peak-load evidence | **Partially met** — the fault-injection half passes; the peak-load half is deferred |

**`AC-05` and `AC-06` are recorded as not-yet-met, not as in progress.** The load rig they depend on is deliberately deferred, with five dated triggers that end the deferral (Testing Strategy §7.9). Restating that here is the point: the plan must not quietly claim what the strategy explicitly says is unverified.

A release is Done when this table is **filled in truthfully**, not when every row says met.

---
