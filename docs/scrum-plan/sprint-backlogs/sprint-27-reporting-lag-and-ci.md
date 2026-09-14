<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 27 — Reporting: Inventory Report, Lag & CI

**Canonical sprint:** [Sprint 27 — Reporting: Inventory Report, Lag & CI](../../sprint-backlogs/sprint-27-reporting-lag-and-ci.md)
**Lane:** Backend · R2 · **Gate:** **`G13` — Contract Sync** · **Backend 21 pts · Frontend 13 pts**

---

## Sprint Goal

> **Every reporting screen shows its own lag.**

`NFR-PERF-06` permits five minutes. The sprint goal is not that the lag be small — it is that **an operator deciding on a five-minute-old figure must know that is what they are doing.** A figure whose age is unknown will be acted on as though it were current, which is the failure the whole reporting domain is shaped to avoid.

`EN-DATA-5` is what makes the claim checkable: the lag is **measured against the bound**, not asserted to be within it. Sprint 26 built the read models and implemented `E2`'s stale branch against a provisional bound; this sprint gives that branch a real measurement to compare against.

`EN-CI-1` brings stages 1–4 of [`Testing and Benchmark Strategy.md`](../../../SA-docs/01-system/Testing%20and%20Benchmark%20Strategy.md) §9 into a pipeline. Stage 2 is marked **never skippable**, and that word is the point.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-RPT-04` | View Inventory Report | 5 |
| BE | `EN-DATA-5` | MongoDB reporting read models; projection lag measurement against NFR-PERF-06 | 8 |
| BE | `EN-CI-1` | CI stages 1–4 of Testing Strategy §9 | 8 |
| | | **Backend total** | **21** |

## Backend Lane

### `EN-DATA-5` MongoDB read models; projection lag measurement (8 pts)
- [ ] The read-model collections formalised — one per report, projected from the Sprint 08 outbox through the `EN-EVENT-2` idempotency and ordering guards. **No fourth implementation of those guards**
- [ ] **Lag is measured, not assumed**: event-occurred-at to projection-applied-at, exposed as a Micrometer meter in the `EN-OBS-2` set and surfaced through the report responses as the `asAt` the screens display
- [ ] **Asserted against `NFR-PERF-06`'s five minutes under an event burst**, not at rest. Lag at rest is always fine; the bound exists for the burst, and IH-3 row 4 re-runs this assertion
- [ ] Replaces Sprint 26's provisional bound. If the real measurement changes what `E2` considers stale, that is a finding recorded here, not a silent adjustment
- [ ] **Rebuildable from the outbox alone** via the Sprint 14 `EN-EVENT-4` replay path — `EN-BENCH-2` (Sprint 29) drills it and IH-3 row 9 inherited a carried half of exactly this from IH-2
- [ ] `reporting` holds no write-model repository; ArchUnit and `allowedDependencies` assert it, extending Sprint 26's check rather than duplicating it

### `EN-CI-1` CI stages 1–4 of Testing Strategy §9 (8 pts)
- [ ] **Stage 1 — compile & type check**: Java compile, `tsc --noEmit`, ESLint. Fail. Budget 2 min
- [ ] **Stage 2 — fast suite**: L1, L2, L3. **Fail, and never skippable.** [`ADR-0018`](../../../SA-docs/01-system/ADR/ADR-0018-architecture-governance-ci-gate.md) §5 names the failure mode — *slow builds create pressure to skip them, which is exactly how the gate fails* — so the 3-minute budget is part of the requirement, not an aspiration
- [ ] **Stage 3 — slow suite**: L4, L5, L6. Fail. Budget 12 min
- [ ] **Stage 4 — contract**: §6.6 spec ↔ code, **both directions** — `EN-CONTRACT-1` from Sprint 16 and `EN-CONTRACT-2` from Sprint 25, now gated rather than run locally
- [ ] Stages 1–2 on every push; 3–4 on every pull request and on `main`
- [ ] **The CI provider is `Proposed`, not decided** ([`ADR-0018`](../../../SA-docs/01-system/ADR/ADR-0018-architecture-governance-ci-gate.md) §5) and [`../release-plan.md`](../release-plan.md) §7 R5 schedules it to be settled **before this sprint**. Confirm it is settled before starting, or raise it at Planning — the stages are provider-independent by design, but they cannot run on an undecided provider
- [ ] Stages 5–7 are **`EN-CI-2`, Sprint 29**. Do not partially implement them here

### `US-RPT-04` View Inventory Report (5 pts) — `getInventoryReport`
- [ ] **`E1` — the as-at time is stated.** No decision made from this report consumes stock; `UC-INV-01` re-checks at the moment of reserving and `BR-INV-01` holds regardless of what this report showed (`UC-INV-05` `E3`)
- [ ] `E2` — a SKU with no configured reorder threshold appears in the position report but not in low-stock exposure, and is **listed as unconfigured rather than silently omitted**
- [ ] `E3` — authority declined. Stock levels and warehouse structure are commercially sensitive (`P16`, `BR-AUD-02`)
- [ ] **`E4` — a reporting outage leaves the operational inventory view (`US-INV-05`, Sprint 12) available.** Warehouse work cannot stop for a reporting outage (`NFR-AVAIL-02`)

---

## Integration Risk & Dependencies


**`G13` is the first and only gate that looks at reporting**, and four screens built across Sprints 23–24 against the mock arrive at it together. The states that matter — incomplete, stale, zero, partially unavailable — are precisely the ones Prism never generated, so all four are being verified for the first time in one session.

Concretely: if the backend's staleness field is a boolean and the frontend expected a timestamp, or if `E5`'s explicit zero and `E3`'s failure share a shape, four screens are wrong in the same way. That is check 6's job at this gate and it deserves more than a glance.

Second: `EN-CI-1` will make previously-local failures visible. A test that only ever ran on one developer's machine, or a boundary rule nobody re-ran after Sprint 24's `EN-CI-3`, surfaces here. Budget for it inside the sprint.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
