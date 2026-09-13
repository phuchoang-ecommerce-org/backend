<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 19 — Ordering: Lifecycle & Admin Orders

**Canonical sprint:** [Sprint 19 — Ordering: Lifecycle & Admin Orders](../../sprint-backlogs/sprint-19-ordering-lifecycle-and-admin.md)
**Lane:** Backend · R1 · **Gate:** **`G9` — Contract Sync** · **Backend 19 pts · Frontend 7 pts**

---

## Sprint Goal

> **An order has a life after placement.**

`US-ORD-10` at 8 points is the order state machine, and it is the sprint's centre of gravity. `BR-ORD-01` makes it **a property of the platform rather than a convention of one interface** — an `ADMINISTRATOR` cannot move an order from Draft to Delivered, and an administrative interface that could skip transitions would be exactly the loophole `P5` describes.

Two exception flows encode the whole discipline and pull in opposite directions: **`E3` — a failed side effect means the transition does not occur**, because a Processing → Packed that fails to commit the reservation leaves stock the platform thinks it still holds. **`E6` — a failed event leaves the transition standing**, because a dropped event means a downstream process silently misses a shipment. State and side effect move together; state and *notification of state* do not.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-ORD-06` | View Order Details | 3 |
| BE | `US-ORD-07` | Track Order | 3 |
| BE | `US-ORD-08` | Cancel Order | 5 |
| BE | `US-ORD-10` | Advance Order Status | 8 |
| | | **Backend total** | **19** |

## Backend Lane

### `US-ORD-10` Advance Order Status (8 pts) — `advanceOrderStatus`, `advanceOrderStatusesInBulk`, `setOrderInvestigationFlag`
- [ ] The state machine as a first-class domain object, not a set of `if` statements per endpoint. The legal transitions are enumerated once and every caller goes through them
- [ ] **`E1` — an illegal transition is declined whatever the actor's role and whatever entry point the request arrived through**, stating the current state and the transitions available. `ECP-ORD-4091` (`BR-ORD-01`, `P5`)
- [ ] `E2` — authority is checked **per transition**, not per endpoint. Approving a refund and packing a box are different authorities (`P16`); permission-matrix cell asserted for each
- [ ] **`E3` — a failed side effect means the transition does not occur** (`NFR-REL-01`). The worked case: Processing → Packed commits the reservation through `inventory`; if the commit fails the order stays in Processing
- [ ] `E4` — a failed audit write means the transition is not applied. The `UC-AUD-01` `E1` branch from Sprint 12, selected — not reimplemented
- [ ] `E5` — concurrent transitions: exactly one succeeds, the other is re-evaluated against the new state and declines as `E1` if no longer legal. L5 concurrency test on the `EN-DATA-4` rig
- [ ] **`E6` — a business event that cannot be raised leaves the transition standing** and is retried until delivered (`NFR-REL-06`, `P6`). The Sprint 08 outbox already guarantees it; assert it here
- [ ] `advanceOrderStatusesInBulk` reports **per-order** outcomes. An aggregate result hides the `E1` failures inside it
- [ ] `setOrderInvestigationFlag` is audited like any other command

### `US-ORD-08` Cancel Order (5 pts) — `cancelOrder`
- [ ] **`E1` — Packed or beyond is declined**; once packed the goods are committed and the remedy is a return (`BR-ORD-04`). `ECP-ORD-4091`. `US-ORD-09` is Sprint 31 — the decline is real now, the return path is carried forward with that named sprint
- [ ] `E2` — an already-cancelled order reports success without acting. **Releasing the reservation twice would inflate available stock** (`UC-INV-02` `E1`)
- [ ] **`E3` — a failed reservation release leaves the order Cancelled** — the customer's request is honoured — and the release is retried and escalated. Held-but-unreleasable stock is invisible loss (`P7`)
- [ ] **`E4` — a failed refund leaves the order Cancelled but not Refunded**, visibly awaiting refund and retried. **The discrepancy is never closed by marking it refunded** (`BR-PAY-02`, `P7`). `payment` arrives Sprint 22 — build the awaiting-refund state; carry the refund call to that named sprint
- [ ] `E5` — a cancellation racing a state advance: exactly one transition applies; if the advance wins to Packed, cancellation fails as `E1`

### `US-ORD-06` View Order Details (3 pts) — `getOrder`, `listOrderLines`
- [ ] **`E1` — an order the caller does not own returns the same response as one that does not exist**, and the attempt is recorded (`P16`). Same non-disclosure rule as Sprint 05's `getOwnAddress`
- [ ] `E2` — an order referencing a deleted product presents **in full from the values recorded at placement** (`FR-DAT-04`). A catalog change never rewrites a purchase record
- [ ] **`E3` — the placement price is returned, not the current one** (`BR-ORD-06`, `FR-DAT-03`)
- [ ] This replaces the empty `listOrders` Sprint 05 built — confirm `/account/orders` now returns real rows

### `US-ORD-07` Track Order (3 pts) — `trackOrder`
- [ ] `E1` — when carrier updates are unavailable, the **last recorded state and when it was received** are returned. A stale state is never presented as current
- [ ] `E2` — an update older than the latest recorded **does not move the shipment backwards** (`BR-SHP-02`)
- [ ] `E3` — ownership as `UC-ORD-06` `E1`
- [ ] **`shipping` does not exist until Sprint 20** — this returns the order-side tracking state; real carrier events arrive next sprint. Record the boundary

---

## Integration Risk & Dependencies


**`placeOrder` and the whole order lifecycle meet the frontend at `G9`, one sprint after the partnership was built and one sprint before IH-2 examines it properly.** The gate will exercise the happy path and the documented error codes; it will not exercise fault injection, concurrency, or idempotency under load. Those are IH-2 rows 1, 2 and 3, and `G9` must not be read as having covered them.

Second: `US-ORD-08` `E4` and `US-ORD-10` `E3` both depend on modules that are partly absent — `payment` until Sprint 22, `shipping` until Sprint 20. Both produce **real states with no real driver** this sprint. Record precisely which half is built, or Sprint 20–22 will re-litigate it.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
