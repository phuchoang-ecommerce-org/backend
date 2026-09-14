<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 21 — Payment: Authorise & Provider Callback

**Canonical sprint:** [Sprint 21 — Payment: Authorise & Provider Callback](../../sprint-backlogs/sprint-21-payment-authorise.md)
**Lane:** Backend · R1 · **Gate:** **`G10` — Contract Sync** · **Backend 19 pts · Frontend 23 pts**

---

## Sprint Goal

> **Money can be taken.**

Every exception flow in `UC-PAY-02` and `UC-PAY-03` reduces to one discipline: **the platform never guesses what the provider did.** A timeout is not a decline (`E2`). A result that cannot be attributed is recorded as unmatched, never discarded (`E4`, `E2`). A result contradicting one already applied is escalated, never overwritten (`E4`) — deciding automatically which of two contradictory financial statements is true is precisely what human reconciliation is for.

Two structural facts shape the sprint. The **provider callback terminates at `nginx` and never passes through `ecp-web`** — it is a server-to-server path, not a browser one, and IH-2 row 8 deferred its verification to IH-3. And `/checkout/payment/processing` **polls**; the provider return is a route handler, not a page ([`Routing.md`](../../../SA-docs/03-frontend/Routing.md) §4.3 rule 6).

The lane balance inverts this sprint — 23 frontend points against 19 backend, the first time the frontend carries more. It spends the surplus on `review` and `notification` screens whose backends are three sprints out.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-PAY-01` | Select Payment Method | 3 |
| BE | `US-PAY-02` | Authorise Online Payment | 8 |
| BE | `US-PAY-03` | Handle Payment Gateway Result | 8 |
| | | **Backend total** | **19** |

## Backend Lane

### `US-PAY-01` Select Payment Method (3 pts) — `listEligiblePaymentMethods`, `selectCheckoutPaymentMethod`
- [ ] Replaces the mock the frontend has used since Sprint 20. **Confirm the contract shape did not move**; a change is a drift entry and an `openapi.yaml` amendment
- [ ] **`E1` — Cash On Delivery outside its configured conditions is not offered**, and the reason is stated if asked (`BR-PAY-03`). Offering it and declining later wastes the customer's time at the worst moment
- [ ] `E2` — where no method is eligible, the platform says so and directs to Support. **It never places an unpayable order**
- [ ] `E3` — a method unavailable by the time of the summary requires a new selection (`UC-ORD-04`)

### `US-PAY-02` Authorise Online Payment (8 pts) — `initiatePayment`, `getOrderPayment`
- [ ] Provider **anti-corruption layer**: the provider's vocabulary stops at the adapter and `payment.domain` never sees it
- [ ] **Card details are never logged** (`NFR-SEC-07`) — assert it, including in error paths and in the correlation-id trace
- [ ] `E1` — a decline moves the order to **Payment Failed with the reservation held** for the retry window (**[A-07]**), the reason surfaced only as far as the provider permits, and a retry offered
- [ ] **`E2` — no answer, or an answer after the timeout, is not a failure.** The order stays in **Pending Payment** with its reservation held and the attempt marked **unresolved**, settled when the provider's result arrives or on reconciliation. Treating a timeout as a decline is how a customer is charged for an order the platform then cancels — the partial failure `P7` names
- [ ] `E3` — an unreachable provider makes no attempt; the order stays Pending Payment. **Platform state is not corrupted by a provider outage** (`NFR-AVAIL-03`)
- [ ] **`E4` — a provider success for an attempt the platform has no record of is recorded as an unmatched payment and escalated. Never discarded** — an unrecorded capture is money taken from a customer that the business cannot see (`P7`)
- [ ] `E5` — a duplicate authorisation for one attempt applies the result once (`BR-PAY-01`). **The customer is charged once.** `ECP-PAY-4090` when an attempt is already in flight
- [ ] `E6` — an amount differing from the order total records the discrepancy and **does not transition to Paid**; escalated rather than accepted
- [ ] `Idempotency-Key` required on `initiatePayment` — `ECP-ORD-4001`/`4090` apply here as they do to `placeOrder`

### `US-PAY-03` Handle Payment Gateway Result (8 pts) — `receivePaymentProviderNotification`
- [ ] **`E3` — verify authenticity first, before anything else.** An unverified result is an instruction to move money from an unknown party; rejected and recorded as a security event
- [ ] The endpoint **terminates at `nginx` and never passes through `ecp-web`** — confirm the routing, and that no browser-reachable path exposes it
- [ ] **`E1` — duplicate delivery is applied once and the duplicates acknowledged.** Providers retry until acknowledged, so duplicates are routine; applying a success twice would move an order twice or refund twice (`BR-PAY-01`, `NFR-REL-04`)
- [ ] `E2` — an unattributable result is recorded as **unmatched and escalated**, never discarded (`UC-PAY-02` `E4`)
- [ ] **`E4` — a result contradicting one already applied is not overwritten.** The conflict is recorded and escalated
- [ ] `E5` — a result for an order that was cancelled in flight is **recorded and escalated**: a capture against a cancelled order is money to be refunded (`UC-PAY-06`, Sprint 22), not a result to drop
- [ ] **`E6` — a failed application acknowledges nothing.** Retained and retried; never acknowledged as processed when it was not (`P6`)
- [ ] Payment events onto the Sprint 08 outbox, partitioned per the `EN-EVENT-5` scheme — this is the read model IH-3 will replay

---

## Integration Risk & Dependencies


**`G10` verifies the customer-facing half of payment and cannot verify the provider half.** The callback terminates at `nginx` from a real provider; the gate exercises `initiatePayment` and `getOrderPayment` and a simulated notification. **IH-2 row 8 already carried the real callback verification to IH-3** — `G10` must not be read as having closed it.

Second, and larger: **the unresolved-payment state is the hardest thing on the screen to test and the most expensive to get wrong.** `E2` produces an order that is neither paid nor failed, holding stock, waiting on a provider. Both lanes must exercise it deliberately — the backend by timing the provider out, the frontend by polling a payment that never resolves — because nothing in the happy path will produce it.

Third: three of the five frontend stories are mock-only for three more sprints (`review` Sprint 24, `notification` Sprint 23). That is the contract-first dividend, but `ECP-REV-4030`'s eventual-consistency wording is the kind of detail Prism cannot validate.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
