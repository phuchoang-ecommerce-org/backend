<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 15 — Promotion: Vouchers & Redemption

**Canonical sprint:** [Sprint 15 — Promotion: Vouchers & Redemption](../../sprint-backlogs/sprint-15-promotion-vouchers.md)
**Lane:** Backend · R1 · **Gate:** **`G7` — Contract Sync** · **Backend 21 pts · Frontend 13 pts**

---

## Sprint Goal

> **A voucher is validated and redeemed without over-redemption.**

`UC-PRM-02` `E7` is structurally the same problem as `BR-INV-01` and gets the same treatment: two customers redeem the last available use at once, **exactly one succeeds**, and the limit holds under concurrency. Over-redemption is unbudgeted spend, which is why `BR-PRM-01` is stated as an absolute rather than a target.

The second theme is **non-disclosure**. `UC-PRM-02` `E1` requires that "never existed", "expired" and "exhausted" be indistinguishable on the standalone validation endpoint, because that endpoint is an enumeration surface — the same reasoning behind Sprint 03's sign-in response and Sprint 05's password-reset response, arriving through a third door.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-PRM-01` | Create Promotion | 8 |
| BE | `US-PRM-02` | Validate Voucher Code | 5 |
| BE | `US-PRM-03` | Apply Promotion to Order | 8 |
| | | **Backend total** | **21** |

## Backend Lane

### `US-PRM-01` Create Promotion (8 pts) — `createPromotion`, `listPromotions`, `generatePromotionVouchers`
- [ ] `Promotion` aggregate, voucher codes, conditions, and the `promotion` Flyway migration under the Sprint 02 prefix convention
- [ ] **`E2` — a promotion with no usage limit is declined.** An unbounded promotion is unbounded discount exposure, and `BR-PRM-01` exists to force that decision before launch rather than after
- [ ] `E1` — a configuration that could produce a negative total is declined, or an explicit cap required (`BR-PRM-02`). **The business never pays a customer to order**
- [ ] `E3` — an end before its start, or an already-elapsed period, is declined **with the problem named**
- [ ] `E4` — mutually exclusive conditions are declined; a campaign that can never redeem is reported as a platform fault by every customer who tries it
- [ ] `E5` — authority declined **and recorded**. Creating a promotion is creating a licence to reduce prices (`P16`)
- [ ] `E6` — audit write failure means the promotion is **not created**, through the Sprint 12 `US-AUD-01` `E1` path
- [ ] `generatePromotionVouchers` produces unguessable codes — a sequential or derivable code makes `E1`'s non-disclosure pointless
- [ ] Permission-matrix cell asserted per operation; cursor pagination on `listPromotions`

### `US-PRM-02` Validate Voucher Code (5 pts) — `validateVoucher`
- [ ] **`E1` — an unrecognised code is reported as "not valid", with "never existed", "expired" and "exhausted" indistinguishable.** `ECP-PRM-4220`, described in [`Error Codes.md`](../../../SA-docs/04-shared/Error%20Codes.md) as *deliberately* non-disclosive on this endpoint
- [ ] `E2` — where the campaign intends it, the validity period may be stated; otherwise `E1` applies. **The default is `E1`**
- [ ] `E3` — ineligibility is stated **without disclosing the criteria**, which would otherwise be gameable
- [ ] `E4` — per-customer limit reached is stated plainly: actionable and not disclosive
- [ ] `E5` — an unmet order condition **is** named — a minimum value, a qualifying category — because the customer may choose to meet it, which is what the condition is for
- [ ] `E6` — total usage exhausted reports the promotion is no longer available
- [ ] `E8` — rate limiting applies (`UC-AUD-04`). Repeated failed codes from one caller is code-guessing, and `NFR-SEC-05` says so
- [ ] The four-way split between what is disclosed (`E4`, `E5`) and what is not (`E1`, `E2`, `E3`) is asserted by test, per branch — this is the story most likely to be broken by someone being helpful

### `US-PRM-03` Apply Promotion to Order (8 pts) — `PromotionRedemptionPort` (internal), `listPromotionRedemptions`
- [ ] **`E7` of `UC-PRM-02` — the concurrency case.** An L5 race on the `EN-DATA-4` rig: N customers redeem the last available use; exactly one succeeds, the rest are told it is exhausted, and **the total redeemed never exceeds the limit** (`BR-PRM-01`). Same rig, same shape, same seriousness as Sprint 11's oversell race
- [ ] `ECP-PRM-4090` is the code for losing that race — **expected under peak load**, like `ECP-INV-4091`
- [ ] `E1` — a discount exceeding the discountable value is **capped**; the total is never negative (`BR-PRM-02`). `ECP-PRM-4221`
- [ ] `E2` — conflicting promotions resolve to the **single most favourable to the customer**, deterministically, and the others are recorded as not applied (`BR-PRM-03`). A non-deterministic resolution prices the same order two ways on two attempts
- [ ] `E3` — an order changing after a discount is applied **re-evaluates every applied promotion**
- [ ] `E6` — the configured rounding rule is applied consistently so line amounts sum to the recorded total. **An order whose parts do not add up cannot be reconciled by Finance** (`FR-DAT-01`, `P7`)
- [ ] `E4` (granted item out of stock) and `E5` (deactivated between application and placement) depend on `inventory` and on placement: `inventory` exists from Sprint 11, so `E4` is implementable; **`E5`'s re-validation at placement belongs to `US-ORD-05` in Sprint 18** — carry it forward with that named sprint rather than marking it done
- [ ] The redemption port is internal; code→spec confirms it exposes no endpoint

---

## Integration Risk & Dependencies


**The disclosure boundary is the risk, and it is a two-sided one.** The backend can leak by distinguishing causes; the frontend can leak by explaining a cause the backend withheld. Neither side's tests catch the other's leak. `G7` check 6 has to compare the **rendered copy** for expired, exhausted and unknown codes and confirm they are byte-identical — the same check Sprint 05 ran on password-reset messaging.

Second: `getOrderSummary` is mock-only for two more sprints, and it is the screen carrying every `Money` value in the funnel. Whatever Prism generates for discount and total shapes is unverified until `G8`.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
