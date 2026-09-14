<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — IH-3 — Integration Hardening: Whole System

**Canonical sprint:** [IH-3 — Integration Hardening: Whole System](../../sprint-backlogs/ih-3-whole-system.md)
**Lane:** Backend · R2 · **Position:** after Sprint 29, before Sprint 30 · **No new stories · no story points**

---

## Goal

> **The whole system, and an honest statement of what is still unverified.**

The second half of that sentence carries as much weight as the first. IH-1 hardened session custody because it is hardest to retrofit. IH-2 hardened the money path because its failures cost money. **IH-3 audits the whole system and then states plainly what it could not verify** — which is a deliverable, not a shortfall.

The `Must` cut line was Sprint 29. Every `Must` story is delivered and every enabler has landed. What remains is to check the properties that span all of them, and to write down, without softening, that **`AC-05` and `AC-06` are unverified** because the load rig they depend on is deliberately deferred ([`Testing and Benchmark Strategy.md`](../../../SA-docs/01-system/Testing%20and%20Benchmark%20Strategy.md) §7.9, with five dated triggers that end the deferral).

**Row 9 is the one row that passes by recording a failure.** A row 9 reported as "in progress" fails; a row 9 reported as "unverified" passes. That inversion is the entire point of this sprint, and it is the failure mode `P15` describes arriving through the last available door.

Both developers, both lanes, full sprint. **No new stories, no points.**

---

## Backend Verification Checklist

- [ ] Confirm the rows §12.1 owns that structure cannot: [`ADR-0018`](../../../SA-docs/01-system/ADR/ADR-0018-architecture-governance-ci-gate.md) §5 concedes that *calling* `AuthorizationService` is structurally checkable but calling it **with the right permission** is not. `EN-CONTRACT-3` (Sprint 28) is that test — confirm it is in the pipeline and covers all 155 operations
- [ ] A row whose evidence is "it was checked at an earlier gate" is **not passed**. Re-demonstrate or log it
- [ ] Confirm a figure past the bound is **labelled stale** end to end, from the projection through the API to the rendered screen
- [ ] Include the meters added since `EN-OBS-2` (Sprint 07): cache hit/miss, outbox backlog and relay lag, DLQ depth, projection lag per read model
- [ ] All **40 rules**. Grep the suites for each `BR-` id and confirm a test names it — this is mechanical and should be a script, so it can be re-run rather than re-audited
- [ ] Confirm the rules that hold "whatever entry point the request arrives through" are tested through **more than one** path: REST, scheduler, and Kafka (`AC-02`). `BR-AUD-02`, `BR-ORD-01`, `BR-REV-01` and `BR-INV-01` are the ones that say so explicitly
- [ ] A rule covered only incidentally, by a test that exercises it without naming it, **does not count** — the naming is what makes the coverage auditable
- [ ] Log every gap as a sized backlog item with a named sprint
- [ ] Confirm the **audit-branch pairs** are both covered, since they are opposite answers to the same trigger: `UC-INV-04` `E4` and `UC-ADM-06` `E5` refuse the action; `UC-PAY-06` `E7` and `UC-PRM-05` `E5` let it stand and escalate
- [ ] Record the audit as a list, per use case, with gaps named. **Coverage that is assumed rather than recorded is what this row exists to replace**
- [ ] Line coverage is measured and reported but **is not a gate** (§10) — do not substitute a percentage for this audit
- [ ] **IH-2 row 8, carried half** — the **real payment provider callback**, including that it terminates at `nginx` and never passes through `ecp-web`. IH-2 passed the row on the carrier substitute and deferred this deliberately. Close it under row 1 or re-log it with a named sprint
- [ ] **IH-2 row 9, carried half** — the **payment read-model replay**. Sprint 29's `EN-BENCH-2` was to close it; confirm under row 4, or re-log
- [ ] **`EN-FE-E2E-2` findings** from Sprint 27's accessibility sweep — all were to be logged as sized items with named sprints. Confirm none was fixed-if-easy and left unrecorded otherwise

## Cross-Lane Milestones

- Complete the shared hardening exit criterion with the other lane.
- Record any unresolved finding as a sized canonical backlog item with a named sprint.
- See the [canonical hardening backlog](../../sprint-backlogs/ih-3-whole-system.md) for the full system checklist.

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
