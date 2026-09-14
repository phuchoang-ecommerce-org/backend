<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 17 — Ordering: Checkout Funnel

**Canonical sprint:** [Sprint 17 — Ordering: Checkout Funnel](../../sprint-backlogs/sprint-17-ordering-checkout-funnel.md)
**Lane:** Backend · R1 · **Gate:** **`G8` — Contract Sync** · **Backend 18 pts · Frontend 10 pts**

---

## Sprint Goal

> **Checkout collects everything an order needs.**

`ordering` is the module with three cross-context edges, and every one of them now exists: `inventory` since Sprint 11, `cart` since Sprint 13, `promotion` since Sprint 16. This sprint builds the collection phase — nothing here reserves stock, redeems a voucher or takes money. Placement is Sprint 18, deliberately separated, because the partnership transaction deserves a sprint that is about nothing else.

`G8` is the gate the frontend has been waiting three sprints for: `/checkout` and `/checkout/shipping` were built in Sprint 14 and `/checkout/review` in Sprint 15, all against Prism. This is the **longest mock-only stretch in the plan**, and the gate is where it ends.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-ORD-01` | Initiate Checkout | 5 |
| BE | `US-ORD-02` | Provide Shipping and Billing Information | 5 |
| BE | `US-ORD-03` | Apply Voucher at Checkout | 3 |
| BE | `US-ORD-04` | Review Order Summary | 5 |
| | | **Backend total** | **18** |

## Backend Lane

### `US-ORD-01` Initiate Checkout (5 pts) — `initiateCheckout`, `getCurrentCheckout`
- [ ] `Checkout` aggregate and the `ordering` Flyway migration; the module scaffold has existed since Sprint 05's `listOrders`
- [ ] `E1` — an empty cart is declined. `ECP-ORD-4220`, documented as **never retry**
- [ ] `E2` — no purchasable line is declined with each unpurchasable line identified; `ECP-ORD-4220`
- [ ] **`E3` — some lines unpurchasable: checkout proceeds only after the customer explicitly removes or reduces them.** The platform never silently drops a line from an order about to be paid for
- [ ] `E4` — an unverified account is declined with a verification resend offered, and **the cart is preserved** (`BR-CUS-02`)
- [ ] `E5` — a guest is required to log in or register, guest cart preserved for merge (`UC-CRT-05`, Sprint 14)
- [ ] Cross-module reads go through ports; ArchUnit confirms `ordering` reaches `cart` and `catalog` only by the edges its `allowedDependencies` grants

### `US-ORD-02` Provide Shipping and Billing Information (5 pts) — `setCheckoutShippingAddress`, `setCheckoutBillingInformation`, `selectCheckoutShippingOption`
- [ ] `Address` comes from `shared-kernel`, the same value object Sprint 05 added — not an `ordering`-local copy
- [ ] `E1` — validation names **which field** failed and does not proceed
- [ ] `E2` — an unserved destination is stated plainly with a different address offered; **no fee is quoted that cannot be honoured**
- [ ] **`E3` — a fee that cannot be calculated blocks the order and permits retry. The platform never guesses** (`BR-SHP-01`: the fee shown at confirmation is the fee charged)
- [ ] `E4` — an address changed after quoting **re-calculates** fee and estimate before the summary
- [ ] `E5` — lines that cannot ship to the destination are identified and must be removed or the address changed
- [ ] **`shipping` does not exist until Sprint 20.** `getShippingQuotes` is served here by a provisional `ordering`-side implementation against the contract's shape. Record it as provisional in the Review Notes and carry the real computation to Sprint 20 — do not mark `US-SHP-01` done from here

### `US-ORD-03` Apply Voucher at Checkout (3 pts) — `applyCheckoutVoucher`, `removeCheckoutVoucher`
- [ ] Calls `promotion`'s validation through a port; **no voucher is redeemed at this stage** — redemption happens inside the placement transaction in Sprint 18
- [ ] `E1`/`E2` — non-disclosive failure copy, `ECP-PRM-4220`, exactly as Sprint 15 established. The checkout path must not become a second, more helpful oracle
- [ ] `E3` — an unmet condition **is** named, because it is actionable; `E4` — a reached usage limit reports unavailable
- [ ] `E5` — a discount exceeding the order value is **capped**; the total is never negative (`BR-PRM-02`). `ECP-PRM-4221`
- [ ] `E6` — re-validation at placement is `US-ORD-05`'s `E3`, Sprint 18. Carried forward with that named sprint

### `US-ORD-04` Review Order Summary (5 pts) — `getOrderSummary`
- [ ] The summary is assembled from lines, discounts and shipping fee, with **every amount server-computed**. `E6` of `UC-PRM-03` applies: line amounts must sum to the recorded total, or Finance cannot reconcile it (`FR-DAT-01`, `P7`)
- [ ] `E1` — a changed price shows the current value with the change stated and **requires explicit re-confirmation** (`BR-ORD-06`)
- [ ] `E2` — a line short of stock reports the shortfall and requires reduction or removal. Reserving happens at placement, so this check exists here **and again** at `UC-ORD-05`
- [ ] `E3` — a voucher gone invalid is removed, the total change stated, re-confirmation required
- [ ] `E4` — a changed shipping fee is shown and re-confirmed (`BR-SHP-01`)
- [ ] `E5` — every line unpurchasable ends the checkout and returns the customer to the cart
- [ ] The re-confirmation token is real state, not a UI convention — `US-ORD-05` will refuse a placement whose summary was never re-confirmed

---

## Integration Risk & Dependencies


**Three sprints of frontend checkout work meet a real backend for the first time at `G8`.** `/checkout`, `/checkout/shipping`, `/checkout/payment` and `/checkout/review` were all built against Prism's generated examples. Everything they assume about checkout state shape, re-confirmation semantics and summary composition is unverified until the gate.

The specific trap: **`getShippingQuotes` is provisional this sprint** and `listEligiblePaymentMethods` has no `payment` module behind it until Sprint 21. Both will return contract-shaped placeholders. Record them as expected at `G8` rather than logging them as drift — and record equally that the *shapes* are what the gate checks, not the values.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
