<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 29 — Release 2 Stabilisation

**Canonical sprint:** [Sprint 29 — Release 2 Stabilisation](../../sprint-backlogs/sprint-29-release-2-stabilisation.md)
**Lane:** Backend · R2 · **Gate:** **`G14` — Contract Sync** · **Backend 21 pts · Frontend 8 pts**

---

## Sprint Goal

> **Release 2 is stabilised and the event backbone is proved under failure.**

**This is the `Must` cut line.** Everything after it is capability the release can ship without, by the Product Owner's own MoSCoW assignment. All 71 `Must` stories are delivered by the end of this sprint — the viable release of SRS §1.5.

No user stories are committed. All 21 backend points are enablers, and each one closes a claim the plan has been carrying: `EN-BENCH-2` proves the outbox guarantee Sprint 08 asserted and IH-2 row 9 could only half-exercise; `EN-OBS-4` turns `NFR-AVAIL-02` from a design property into a harness that stops dependencies and watches the purchase path survive; `EN-CI-2` closes the pipeline at stage 7.

**IH-3 follows immediately.** This sprint's job is to leave it with findings rather than surprises.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `EN-CI-2` | CI stages 5–7; security, dependency and image scanning | 8 |
| BE | `EN-BENCH-2` | L6 event-delivery suite; broker killed mid-relay; read-model rebuild drill | 8 |
| BE | `EN-OBS-4` | NFR-AVAIL-02 dependency-failure harness (stop ES/Mongo, assert purchase path) | 5 |
| | | **Backend total** | **21** |

## Backend Lane

### `EN-BENCH-2` L6 event-delivery suite; broker killed mid-relay; rebuild drill (8 pts)
- [ ] **Kill the broker mid-relay.** Take writes while it is down, bring it back, and confirm **the outbox drains on recovery without manual repair.** Sprint 08 demonstrated this once by hand; this makes it a suite that cannot silently stop being true
- [ ] Kill the **relay** rather than the broker, and confirm the same — a crash between publish and mark republishes, which is correct at-least-once behaviour and must not be mistaken for loss
- [ ] **The read-model rebuild drill**, properly: drop each projection — search (Sprint 10), rating summary (Sprint 24), the reporting models (Sprint 27), **and the payment read model** — and rebuild from the retained outbox alone via `EN-EVENT-4`'s replay path
- [ ] **The payment read model is the carried half of IH-2 row 9.** It is closed here or it is re-logged for IH-3 with a named owner — not passed silently
- [ ] Every rebuild converges and duplicates nothing, because consumers are idempotent (`EN-EVENT-2`). Re-prove it rather than assume it
- [ ] **Measure how long each rebuild takes.** An unmeasured recovery path is one nobody will choose under pressure
- [ ] Confirm the retention policy of [`Backend Architecture.md`](../../../SA-docs/02-backend/Backend%20Architecture.md) §3.4.5 and the pruning obligation of [`ADR-0012`](../../../SA-docs/01-system/ADR/ADR-0012-transactional-outbox-and-kafka.md) §5 are **reconciled**, with the partial index on unpublished rows in place — so the outbox does not become a `P10` problem of its own
- [ ] Testing Strategy §10's fourth coverage rule: **every domain event in [`Integration Contract.md`](../../../SA-docs/04-shared/Integration%20Contract.md) §7 has an L4 test asserting a consumer reacts idempotently.** Audit that here; redelivery is normal under `NFR-REL-06`, not exceptional

### `EN-CI-2` CI stages 5–7 (8 pts)
- [ ] **Stage 5 — security scan**: dependency, secret, and image scanning per [`Security.md`](../../../SA-docs/01-system/Security.md) §12.3. **Fail on critical.** Budget 3 min
- [ ] **Stage 6 — build & tag**: `bootJar`, `next build`, image tagged with the commit SHA. Fail. Budget 4 min
- [ ] **Stage 7 — smoke benchmark**: L7 against **stage 6's image**. **Report only** — [`ADR-0018`](../../../SA-docs/01-system/ADR/ADR-0018-architecture-governance-ci-gate.md) §4 fixes this, and §7.2 rule 3 explains why: *a benchmark that blocks merges on a shared runner's noise gets disabled within a fortnight, and then nothing is measured at all*
- [ ] **Stage 7 runs against the `bootJar`, never a Gradle `bootRun`** — §7.2 rule 4, and IH-3 row 5 checks exactly this
- [ ] Warm-up discarded (rule 5); output is **movement against the last run**, not a certificate against `NFR-PERF-01` (rule 2)
- [ ] Stages 3–6 on every pull request and on `main`; **stage 7 nightly on `main` and on demand**
- [ ] With this, all seven stages of §9 exist. Confirm the budgets hold in practice — the fast suite's 3 minutes especially, since stage 2 is the never-skippable one

### `EN-OBS-4` `NFR-AVAIL-02` dependency-failure harness (5 pts)
- [ ] **Stop Elasticsearch and assert the purchase path continues**: browse by category, product detail, cart, checkout, payment. Search degrades and says so; nothing else does
- [ ] **Stop MongoDB and assert the same.** Reviews collapse to a section empty state, reporting reports its own failure, and **checkout is untouched** — `P13`'s separation running in both directions
- [ ] Stop Redis and confirm the Sprint 06 cache-aside answers identically from the database, and that the Sprint 04 rate limiter's `UC-AUD-04` `E6` fail-open behaviour holds — **requests proceed and the loss of the control is escalated**, because failing closed here turns a protective control into a total outage
- [ ] Stop the payment provider stub and the carrier stub: the platform **degrades rather than breaks** (`NFR-AVAIL-03`). This is IH-3 row 3, exercised first here
- [ ] A harness, not a checklist — runnable on demand, so IH-3 row 2 re-runs it rather than re-improvising it
- [ ] Generalises Sprint 24's review-specific `NFR-AVAIL-02` proof; fold that in rather than leaving two harnesses

---

## Integration Risk & Dependencies


**This sprint commits no user story and therefore produces nothing a gate naturally checks.** `G14`'s scope is the audit trail from Sprint 28 and whatever the regression walk surfaces — the three enablers are verified by their own demonstrations or not at all.

The real exposure is **ordering**: `G14` closes Sprint 29 and IH-3 follows it. Anything `EN-BENCH-2` or `EN-OBS-4` finds late lands in a hardening sprint whose job is to confirm the system, not to repair it. Run both harnesses early in the sprint rather than at the end.

Second: **`EN-CONTRACT-3` landed last sprint and supersedes check 7's manual walk.** `G14` should cite the suite rather than re-walk cells by hand — and if the suite is not yet in stage 4, that is a finding about Sprint 28, not a reason to fall back quietly.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
