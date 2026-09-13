<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 14 — Cart: Merge, Expiry & Checkout Funnel (FE)

**Canonical sprint:** [Sprint 14 — Cart: Merge, Expiry & Checkout Funnel (FE)](../../sprint-backlogs/sprint-14-cart-merge-and-checkout-funnel.md)
**Lane:** Backend · R1 · **Gate:** none · **Backend 21 pts · Frontend 11 pts**

---

## Sprint Goal

> **A guest cart survives sign-in, and an abandoned one expires.**

`US-CRT-05` closes the loop Sprint 13 opened: guest identity is only worth having if it converts. `BR-CRT-03` is unusually explicit that a merged line is never silently discarded, and `E3` is unusually explicit that a merge failure must not deny a customer their account — a cart problem is never allowed to become a login problem.

The frontend lane starts the checkout funnel **three sprints before `ordering` exists**. That gap is the largest in the plan (§3.2) and it is deliberate: the funnel is the highest-value surface in the product and the one most worth having built, reviewed and revised before the endpoints behind it arrive.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-CRT-05` | Merge Guest Cart on Login | 5 |
| BE | `US-CRT-06` | Expire Inactive Cart | 3 |
| BE | `EN-EVENT-4` | Consumer obligations: retry, dead-lettering, replay from outbox | 8 |
| BE | `EN-DATA-4` | L5 persistence & concurrency suite scaffolding (schema-per-test-class) | 5 |
| | | **Backend total** | **21** |

## Backend Lane

### `US-CRT-05` Merge Guest Cart on Login (5 pts) — `mergeGuestCart`
- [ ] Runs on the sign-in path, **server-side**, when a guest cart cookie is present — it has no route of its own ([`Routing.md`](../../../SA-docs/03-frontend/Routing.md) §4.2, `FR-CRT-06`)
- [ ] `E1` — a guest line whose variant is unpublished is **not carried over, and the customer is told which product was dropped and why**. Silently discarding it is what `BR-CRT-03` forbids
- [ ] `E2` — a wholly out-of-stock line **is** carried over, marked unpurchasable, so the customer can see it and decide
- [ ] **`E3` — a failed merge leaves login standing.** The stored cart is untouched, the guest cart preserved for retry, and the customer told their items will be recovered. A cart failure must never deny account access (`UC-CUS-03` E4)
- [ ] `E4` — concurrent merges from two sessions apply in sequence, each against the cart as it then stands; neither session's items are lost. L5 concurrency test on the `EN-DATA-4` rig
- [ ] The guest cookie is cleared only after a successful merge, never before

### `US-CRT-06` Expire Inactive Cart (3 pts) — scheduler, no contract surface
- [ ] Scheduled expiry against the `BR-CRT-01` lifetime defined in Sprint 13
- [ ] `E1` — a cart in active checkout **defers** expiry. Expiring mid-checkout strands the customer at the moment of purchase
- [ ] `E2` — a cart holding an order in `Draft` defers to order cancellation (`UC-ORD-08`), which releases any reservation. **The cart itself never releases stock, because it never took any** — this is the boundary between `cart` and `inventory` and it must not blur
- [ ] `E3` — a failed expiry leaves the cart live and retries next run (`NFR-REL-04`). A failed expiry costs storage, not correctness
- [ ] Two schedulers on two instances do not double-expire — same lease pattern as the Sprint 08 relay
- [ ] ArchUnit: `cart` does not depend on `ordering` or `inventory`; the deferral checks go through ports

### `EN-EVENT-4` Consumer obligations: retry, dead-lettering, replay (8 pts)
- [ ] **Retry policy** — bounded, backed off, and applied per consumer rather than globally, so a slow projection does not inherit a fast one's budget
- [ ] **Dead-letter topic** per the Sprint 08 catalogue, with the original envelope and the failure reason preserved. A DLQ entry that has lost its correlation id cannot be investigated
- [ ] **Replay from the outbox** — a range of events can be republished deliberately. This is the mechanism `EN-EVENT-3`'s index rebuild and `EN-BENCH-2`'s Sprint 29 drill both stand on
- [ ] Replay is safe because consumers are idempotent (`EN-EVENT-2`) — re-prove it here rather than assuming it, by replaying a range into the live search projection and asserting no duplication
- [ ] DLQ depth as a Micrometer meter in the `EN-OBS-2` set. **A silently growing DLQ is the failure mode this item exists to prevent**
- [ ] Demonstration, not assertion: a consumer that always fails fills the DLQ, the events are corrected and replayed, and the projection converges

### `EN-DATA-4` L5 persistence & concurrency suite scaffolding (5 pts)
- [ ] Schema-per-test-class against Testcontainers PostgreSQL, so concurrency tests do not contend with each other
- [ ] The Sprint 11 oversell race and the Sprint 12 concurrent-adjustment test **migrate onto this rig** rather than keeping their own setup
- [ ] Fast enough to stay in the default `check` task — [`ADR-0018`](../../../SA-docs/01-system/ADR/ADR-0018-architecture-governance-ci-gate.md) §5's "slow builds create pressure to skip them" applies directly here
- [ ] Documented so `US-CRT-05` `E4`, and every later concurrency story, adds a test rather than a harness

---

## Integration Risk & Dependencies


**`US-ORD-01` and `US-ORD-02` will sit against the mock for three sprints** — the longest unverified stretch in the plan. Everything the funnel assumes about checkout state, shipping-quote shape and payment-method eligibility rests on Prism's generated examples until `G8` at Sprint 17.

The specific exposure is `getShippingQuotes`: `shipping` does not arrive until Sprint 20, so even at `G8` the quotes will be whatever `ordering` returns rather than what `shipping` eventually computes. Record that now so Sprint 17's gate does not mistake it for drift.

Second: `US-CRT-06` has no contract surface, so no gate will ever check it. Its `E1`/`E2` deferrals are the only thing standing between a housekeeping job and a customer stranded mid-checkout — they are verified here or not at all.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
