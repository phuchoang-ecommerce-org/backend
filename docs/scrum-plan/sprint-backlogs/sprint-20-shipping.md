<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 20 — Shipping: Quotes, Shipments & Delivery

**Canonical sprint:** [Sprint 20 — Shipping: Quotes, Shipments & Delivery](../../sprint-backlogs/sprint-20-shipping.md)
**Lane:** Backend · R1 · **Gate:** none · **Backend 21 pts · Frontend 8 pts**

---

## Sprint Goal

> **Goods move and the customer can see it.**

`shipping` closes the two provisional edges the last three sprints have been carrying: `getShippingQuotes` has been an `ordering`-side placeholder since Sprint 17, and order-side tracking has had no carrier behind it since Sprint 19. Both become real here.

The theme running through every story is that **the platform never advances on an unconfirmed external fact**. `UC-SHP-03` `E2` and `E3`: a carrier rejection or an unreachable carrier leaves the order in Packed, because an order advanced on an unconfirmed dispatch misstates both fulfilment and revenue. `UC-SHP-06` `E4`: delivery is **never** taken as evidence of payment. And [`Error Codes.md`](../../../SA-docs/04-shared/Error%20Codes.md) §3.8 records that shipping defines **no domain-specific code of its own** — failures resolve to `GEN`, and an order-transition conflict on a shipment-driven change is `ECP-ORD-4091`, owned by `ORD` because the state machine is.

**IH-2 begins after this sprint.**

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-SHP-01` | Calculate Shipping Fee | 5 |
| BE | `US-SHP-03` | Create Shipment | 5 |
| BE | `US-SHP-04` | Record Carrier Tracking Update | 5 |
| BE | `US-SHP-05` | View Shipment Tracking | 3 |
| BE | `US-SHP-06` | Confirm Delivery | 3 |
| | | **Backend total** | **21** |

## Backend Lane

### `US-SHP-01` Calculate Shipping Fee (5 pts) — `getShippingQuotes`
- [ ] Replaces the provisional Sprint 17 implementation. **Confirm the contract shape did not move**; if it did, that is a drift log entry and an `openapi.yaml` amendment, not a silent fix
- [ ] `E1` — an unserved destination is stated and a different address offered. **No fee is quoted that cannot be honoured**
- [ ] `E2` — restricted lines are identified and must be removed or the address changed (`UC-ORD-02` `E5`)
- [ ] **`E3` — a fee that cannot be calculated is reported, with retry permitted. The platform never estimates** — `BR-SHP-01` requires the fee shown to be the fee charged, and a guess either loses money or misquotes
- [ ] `E4` — where a configured fallback rate exists it is used **and identified as a fallback**; otherwise `E3` applies (`NFR-AVAIL-03`)
- [ ] `E5` — unknown weight or dimensions use the category default **and flag the order for review**; an under-declared parcel is a cost the business absorbs on delivery
- [ ] `US-SHP-02` (delivery-date estimate) is **Sprint 31** — the quote carries the shape, not the computation

### `US-SHP-03` Create Shipment (5 pts) — `createShipment`, `listShipments`
- [ ] **`E1` — an order not in Packed is declined.** Dispatching an order whose stock is not committed would ship goods the platform still counts as reserved (`BR-ORD-01`, `P7`)
- [ ] **`E2`/`E3` — a carrier rejection or an unreachable carrier records no shipment and leaves the order in Packed.** The rejection reason is surfaced so the Operator can correct and retry; an alternative carrier is offered where one serves the destination (`NFR-AVAIL-03`). **The order is never advanced on an unconfirmed dispatch**
- [ ] `E4` — a missing tracking reference records the shipment as dispatched, flagged for follow-up, and **notifies the customer of dispatch anyway**. A missing reference does not justify withholding the fact of dispatch
- [ ] **`E5` — a shipment created but a failed order transition retries the transition, never cancels the shipment** — the goods are with the carrier and cannot be recalled (`NFR-REL-04`). Persistent failure is escalated: an order in Packed with goods in transit misstates both fulfilment and revenue (`P7`)
- [ ] Advances the order through `ordering`'s state machine (`US-ORD-10`), never by writing order state directly. ArchUnit asserts it

### `US-SHP-04` Record Carrier Tracking Update (5 pts) — `receiveCarrierEvent`
- [ ] **`E4` — an update failing authenticity verification is rejected and recorded as a security event.** An unverified update could mark an undelivered order delivered, which for Cash On Delivery **moves money** (`UC-PAY-04`). Verify before anything else
- [ ] **`E1` — duplicate delivery is recorded once.** Carriers retry until acknowledged, so duplicates are routine (`NFR-REL-04`)
- [ ] **`E2` — an out-of-order update is retained in the history for completeness but does not move the shipment backwards** (`BR-SHP-02`). An "in transit" arriving after "delivered" must not un-deliver a parcel
- [ ] `E3` — a reference matching no shipment is recorded as **unmatched and escalated**; it may indicate `UC-SHP-03` `E5`
- [ ] **`E5` — a failed recording acknowledges nothing.** The update is retained and retried; it is never acknowledged as processed when it was not (`P6`)
- [ ] System actor — no `CUSTOMER` or `STAFF` path reaches this operation; permission-matrix cell asserted

### `US-SHP-06` Confirm Delivery (3 pts) — `confirmShipmentDelivery`
- [ ] `E1` — an order not in Shipping is declined **and the conflict recorded**. A delivery for an order never dispatched is a mismatch worth investigating
- [ ] `E2` — a duplicate confirmation applies once and **does not restart the return window**, which would otherwise extend it silently (`BR-ORD-05`)
- [ ] `E3` — a disputed delivery leaves the order **Delivered** and records the dispute for Support. The platform does not reverse a carrier's confirmation automatically
- [ ] **`E4` — Cash On Delivery collection not recorded leaves the order Delivered but not Paid**, on the outstanding-collection report. **Delivery is never taken as evidence of payment** (`UC-PAY-04` `E3`). `payment` arrives Sprint 22 — build the state and the report row; carry the settlement to that named sprint
- [ ] `E5` — a failed confirmation leaves the order in Shipping and retries (`NFR-REL-04`)

### `US-SHP-05` View Shipment Tracking (3 pts) — `getShipment`, `listShipmentTrackingEvents`
- [ ] `E1` — no updates yet returns dispatched-and-awaiting, explicitly
- [ ] `E2` — the age of the last update is returned, so the frontend can state it rather than compute it
- [ ] **`E3` — a shipment the caller may not see returns the same response as one that does not exist.** A tracking reference identifies a real address and must not be probeable (`P16`)
- [ ] Cursor pagination on `listShipmentTrackingEvents`

---

## Integration Risk & Dependencies


**No gate closes this sprint, and IH-2 opens immediately after it.** Everything built here — carrier authenticity verification, the unmatched-update path, the Delivered-but-not-Paid state — goes straight into a hardening sprint that will examine the money path around it. That is the right order, but it means Sprint 20's Review is the **last chance to write down known gaps before they become IH-2 findings**.

The specific one to record: `US-SHP-06` `E4` produces a Delivered-but-not-Paid state with **no `payment` module to settle it**, and `US-SHP-03` `E5` escalates to an operational alert channel that does not exist until Sprint 23's notification work. Both are real states with absent drivers.

Second: `getShippingQuotes` replaces a provisional implementation that the frontend has been building against since Sprint 14. If the shape moved, three sprints of `/checkout/shipping` work moved with it — and there is no gate this sprint to catch it. Check it inside the sprint.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
