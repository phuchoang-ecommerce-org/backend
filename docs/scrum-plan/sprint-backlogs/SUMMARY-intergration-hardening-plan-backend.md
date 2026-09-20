<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — IH-1 — Integration Hardening: Session & Catalog

**Canonical sprint:** [IH-1 — Integration Hardening: Session & Catalog](../../sprint-backlogs/ih-1-session-and-catalog.md)
**Lane:** Backend · R1 · **Position:** after Sprint 09, before Sprint 10 · **No new stories · no story points**

---

## Goal

> **Session and catalog, end to end.**

This is not a Contract Sync gate and it is not a catch-up sprint. A gate checks the increment just delivered; IH-1 checks the **properties that no single increment owns** — session custody, boundary posture, the invalidation chain, and one correlation id running the length of it.

IH-1 sits first of the three hardening sprints because **session custody is the hardest thing in the plan to retrofit**. Every later sprint assumes it; if it is wrong, it is wrong everywhere at once.

Both developers, both lanes, full sprint. **No new stories are committed and no points are carried** — an IH sprint that takes on delivery work is an IH sprint that reports green because it ran out of time to look.

---

## Backend Verification Checklist

- [ ] Confirm no Server Action or route handler echoes the token into a response body or an error message
- [ ] Drive N concurrent requests through an expired access token and assert **exactly one** refresh call reaches `ecp-api`
- [ ] Sign in as a `CUSTOMER`, navigate to `/admin` and to three `(admin)` detail routes. **The shell renders**, its sections are empty, and every read behind it returned `403` **from the server**
- [ ] Tamper with the callback signature and confirm refusal; confirm the refusal is visible in logs rather than silent
- [ ] Issue one browser request that causes a catalog write, and follow **one** id through the API log, the outbox row, the Kafka envelope, the consumer, and the revalidation callback
- [ ] Walk every identity and catalog operation delivered through Sprint 09 against its matrix row: each role that is granted succeeds, each role that is not is **refused by the server**
- [ ] Hiding a control in the UI counts for nothing here. The check is the API response
- [ ] `UC-AUD-01` remains the Sprint 03/04 stub listener. The **refusal path** (`E1`: audit fails → the change is not applied) is what row 9's adjacent behaviour depends on; confirm the refusal is real even though the persistence is not (`US-AUD-01`, Sprint 12)

## Cross-Lane Milestones

- Complete the shared hardening exit criterion with the other lane.
- Record any unresolved finding as a sized canonical backlog item with a named sprint.
- See the [canonical hardening backlog](../../sprint-backlogs/ih-1-session-and-catalog.md) for the full system checklist.

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — IH-2 — Integration Hardening: The Money Path

**Canonical sprint:** [IH-2 — Integration Hardening: The Money Path](../../sprint-backlogs/ih-2-the-money-path.md)
**Lane:** Backend · R1 · **Position:** after Sprint 20, before Sprint 21 · **No new stories · no story points**

---

## Goal

> **The money path — the sprint that decides whether `P6`, `P7` and `P8` are solved.**

That sentence is from [`../release-plan.md`](../release-plan.md) §4 and it is not rhetoric. Three of the platform's named risks converge here and nowhere else:

| | |
|---|---|
| **`P6`** | An accepted business event is silently lost, and the business operates on an incomplete picture without knowing it |
| **`P7`** | A transaction partially completes — a duplicate order, an unreleasable reservation, a capture with no record, an order marked refunded that was not |
| **`P8`** | Overselling under peak load, then cancelling confirmed orders afterwards — damaging the brand precisely during the event meant to build it |

IH-1 hardened session custody because it is hardest to retrofit. **IH-2 hardens the money path because it is the one whose failures cost money and cannot be apologised away.** Every row below is a property that no single sprint owns: Sprint 18 built placement, Sprint 19 the lifecycle, Sprint 20 shipment — and the guarantees run across all three.

Both developers, both lanes, full sprint. **No new stories, no points.** An IH sprint that takes on delivery work is an IH sprint that reports green because it ran out of time to look.

**One row is about a module that does not exist yet.** `payment` arrives in Sprint 21, so row 8's provider-callback verification and row 9's payment read model are examined against what Sprint 20 built — the carrier authenticity path and the outbox retention — and the payment-specific half is carried to IH-3. Say so in the row rather than passing it on a substitute.

---

## Backend Verification Checklist

- [ ] After the outbox write → the order stands **and the event is eventually delivered** (`UC-ORD-05` `E8`). This one is the opposite of the other three, deliberately: a paid order that no downstream process hears about is `P6`
- [ ] Make `placeOrder` time out. **No automatic retry fires** — assert on the server's request log, not on the UI
- [ ] Confirm the backend's half: `UC-ORD-05` `E7` — a provider timeout leaves the order in **Pending Payment with its reservation held**, not cancelled. A timeout is not a decline
- [ ] Two real accounts, two real orders. Each fetches the other's `getOrder`, `listOrderLines`, `trackOrder`, `cancelOrder`, `getShipment`, `listShipmentTrackingEvents`. **Every one is `404`, never `403`**
- [ ] Confirm the attempt **is** recorded server-side (`P16`). Non-disclosure to the caller is not non-recording
- [ ] **`payment` arrives in Sprint 21.** What exists to examine now is `receiveCarrierEvent` (`UC-SHP-04` `E4`) — verify first, then act; a forged carrier update changes nothing and is recorded as a security event
- [ ] Confirm `/api/internal/revalidate`'s signature check (IH-1 row 6) has not regressed
- [ ] §3.4.5 makes the retained outbox rows **the permanent event history**. Confirm the retention policy in force actually retains them — and that the pruning obligation [`ADR-0012`](../../../SA-docs/01-system/ADR/ADR-0012-transactional-outbox-and-kafka.md) §5 states has been reconciled with it, not applied in ignorance of it
- [ ] Drop a downstream read model and **rebuild it from the outbox alone**, using the Sprint 14 `EN-EVENT-4` replay path. It converges, and idempotency means the replay duplicates nothing
- [ ] Confirm a partial index on unpublished rows exists, so the outbox does not become a `P10` problem of its own
- [ ] **`EN-CONTRACT-1` findings** from Sprint 16 that were logged rather than fixed — confirm each still has a named sprint

## Cross-Lane Milestones

- Complete the shared hardening exit criterion with the other lane.
- Record any unresolved finding as a sized canonical backlog item with a named sprint.
- See the [canonical hardening backlog](../../sprint-backlogs/ih-2-the-money-path.md) for the full system checklist.

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
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
