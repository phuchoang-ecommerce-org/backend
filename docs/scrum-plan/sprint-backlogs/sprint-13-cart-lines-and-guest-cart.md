<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 13 — Cart: Lines & Guest Cart

**Canonical sprint:** [Sprint 13 — Cart: Lines & Guest Cart](../../sprint-backlogs/sprint-13-cart-lines-and-guest-cart.md)
**Lane:** Backend · R1 · **Gate:** **`G6` — Contract Sync** · **Backend 20 pts · Frontend 13 pts**

---

## Sprint Goal

> **A guest can build a cart.**

"Guest" is the load-bearing word. A cart that only works for signed-in customers is a shorter sprint and a worse funnel — most carts begin before anyone has an account. So `EN-WIRE-4`'s cookie-based identity resolution is committed alongside the four line operations rather than after them, because retrofitting guest identity onto a customer-only cart means rewriting all four.

One rule threads through every story here: **the platform never silently changes what the customer asked for.** Not a capped quantity, not a dropped line, not an honoured stale price. Each exception flow below is a specific instance of it.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-CRT-01` | Add Item to Cart | 5 |
| BE | `US-CRT-02` | Update Cart Item Quantity | 3 |
| BE | `US-CRT-03` | Remove Item from Cart | 2 |
| BE | `US-CRT-04` | View Cart | 5 |
| BE | `EN-WIRE-4` | Guest-cart cookie handling and cart identity resolution | 5 |
| | | **Backend total** | **20** |

## Backend Lane

### `EN-WIRE-4` Guest-cart cookie handling and cart identity resolution (5 pts)
- [ ] One resolution function: a request resolves to a cart by **customer id when authenticated, by guest-cart cookie otherwise** — never by both, never by a header the client can choose
- [ ] The guest cookie is opaque, unguessable, and carries the same posture rules the Sprint 04 session cookie established
- [ ] A guest cart identifier is not a permission: possession of the cookie grants access to that cart and nothing else. A `CUSTOMER`'s cart is never reachable by a guest cookie
- [ ] `BR-CRT-01` — cart lifetime and expiry semantics defined here, consumed by `US-CRT-06` in Sprint 14
- [ ] The resolution is exercised for both identities on every cart operation below, not only on `getCurrentCart`

### `US-CRT-01` Add Item to Cart (5 pts) — `addCartLine`
- [ ] `E1` — quantity exceeding available stock is **declined**, stating the quantity actually available and offering it. `ECP-CRT-4090`. **Never silently capped** — a shopper who asked for five and receives two discovers it at checkout or on delivery
- [ ] `E2` — a variant unpublished since the page loaded leaves the cart **unchanged** (`BR-CAT-02`)
- [ ] `E3` — a wholly out-of-stock variant is declined and the wishlist offered, so the intent is captured. **`US-CRT-07` is Sprint 31** — return the outcome the contract defines and carry the wishlist offer forward with a named sprint
- [ ] `E4` — a cart expired mid-session creates a new cart and **adds the line anyway**, telling the visitor. The addition is not lost to housekeeping
- [ ] `BR-CRT-04` — the line carries no price of its own; price is read at view time

### `US-CRT-02` Update Cart Item Quantity (3 pts) — `updateCartLineQuantity`
- [ ] `E1` — a new quantity above available stock is declined, the available quantity stated, and **the previous value kept**. `ECP-CRT-4090`
- [ ] `E2` — a line removed in another session reports the line is gone and returns the current cart; it is never resurrected
- [ ] `E3` — a variant unpublished since the line was added declines the increase and marks the line **unpurchasable**

### `US-CRT-03` Remove Item from Cart (2 pts) — `removeCartLine`
- [ ] `E1` — removing an already-removed line reports success. The goal already holds; idempotent, the same shape as Sprint 05's `removeOwnAddress`
- [ ] `E2` — emptying a cart in active checkout **ends the checkout** and returns to the cart (`FR-ORD-01`). `checkout` does not exist until Sprint 17 — implement the cart-side outcome, carry the checkout-side forward with a named sprint

### `US-CRT-04` View Cart (5 pts) — `getCurrentCart`, `getCart`
- [ ] Lines priced at the **current** catalog price, per `BR-CRT-04` — the cart stores intent, not a quotation
- [ ] **`E1` — a risen price is presented at the new, higher value with the change stated explicitly.** The old price is never honoured silently, and the increase is never applied silently either (`BR-ORD-06`)
- [ ] `E2` — a line short of stock is **marked short with the available quantity**; the quantity is not adjusted for the customer
- [ ] `E3` — an unpublished variant's line is marked unpurchasable and **not removed automatically**, so the customer sees what was lost
- [ ] `E4` — an expired cart returns an empty cart stating the previous one expired
- [ ] `getCart` is ownership-scoped: another party's cart returns the same `404` a non-existent one does

---

## Integration Risk & Dependencies


**Guest identity is the first thing in the plan that Prism cannot model.** The mock issues no guest cookie and enforces no ownership scoping, so every frontend cart path has been exercised against a backend that always says yes. `G6` is the first time cookie resolution, ownership scoping and the expired-cart path meet real behaviour.

Second: optimistic updates against a server that legitimately refuses (`ECP-CRT-4090` is a **normal** outcome, not an error) is the combination most likely to produce a UI that ends up out of step with the server. Check the revert, not just the happy path.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
