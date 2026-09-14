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
