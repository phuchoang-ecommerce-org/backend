<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 06 — Catalog: Categories, Listings & Variants

**Canonical sprint:** [Sprint 06 — Catalog: Categories, Listings & Variants](../../sprint-backlogs/sprint-06-catalog-browse.md)
**Lane:** Backend · R1 · **Gate:** none · **Backend 21 pts · Frontend 17 pts**

---

## Sprint Goal

> **The catalog is browsable.**

`identity` closed at Sprint 05, so `catalog` — the module twelve others read from — starts here. This sprint builds the two browse surfaces and the Redis cache-aside path underneath them; `NFR-PERF-01` is a property of that path, not of the controllers above it, which is why `EN-WIRE-3` is committed in the same sprint as the endpoints it serves rather than retrofitted after them.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-CAT-01` | Browse Category Tree | 5 |
| BE | `US-CAT-02` | Browse Category Product Listing | 5 |
| BE | `US-CAT-04` | Select Product Variant | 3 |
| BE | `EN-WIRE-3` | Catalog read cache-aside + invalidation keys | 8 |
| | | **Backend total** | **21** |

## Backend Lane

### `US-CAT-01` Browse Category Tree (5 pts) — `listCategories`, `getCategory`
- [x] `Category` aggregate and `catalog` schema tables; tree materialised so arbitrary depth is one query, not N
- [ ] `BR-CAT-03` — no category is its own ancestor; enforced on read *and* asserted by an L1 test, because `UC-ADM-02` E1 will rely on the same invariant when writes arrive in Sprint 09
- [ ] `BR-CAT-02` — unpublished products excluded from every count the tree exposes
- [x] Ancestor path returned with a deep-linked category (`A1`), so the frontend can render "move back up" without a second call
- [ ] Exception flows: `E1` unknown/removed category → the contract's `404`, never a `403`; `E2` tree unavailable degrades without closing search or featured (`NFR-AVAIL-02`)
- [ ] Contract test both directions on both operations

### `US-CAT-02` Browse Category Product Listing (5 pts) — `listCategoryProducts`
- [x] Products of the category **and its descendants** (`BR-CAT-02`), published only
- [x] Default ordering, plus price / newest / popularity (`FR-CAT-08`); a changed ordering restarts from the first page — the cursor is not carried across an ordering change
- [x] Cursor pagination on the envelope agreed in Sprint 02 (`EN-WIRE-1`)
- [ ] `A3` — out-of-stock products are **listed and marked**, never hidden
- [x] Exception flows: `E1` empty category returns an explicit empty page, not an error; `E2` page beyond the last returns the last available page; `E3` availability indeterminate is marked unknown rather than suppressing the listing
- [x] Permission-matrix cell asserted: `GUEST` may read

### `US-CAT-04` Select Product Variant (3 pts) — `listProductVariants`, `getProductVariant`
- [ ] `BR-CAT-01` — a selection across every dimension resolves to exactly one stockable unit
- [ ] Partial selection (`A1`) returns the price **range** across matching variants; it never invents a single price
- [ ] Non-existent and zero-stock combinations are marked before selection (`A2`)
- [ ] Exception flows: `E1` combination does not exist; `E2` variant out of stock is still selectable and still returns its detail (`BR-CRT-02`)

### `EN-WIRE-3` Catalog read cache-aside + invalidation keys (8 pts)
- [x] Cache-aside over the `redis-cache` instance stood up by `EN-WIRE-2` in Sprint 03 — read-through, write-around
- [x] **The invalidation key scheme is the deliverable**, not the cache: one key namespace per read surface (`category-tree`, `category-listing:{id}`, `variant:{id}`), documented in the module's `package-info` and reused verbatim by `EN-EVENT-2` in Sprint 09
- [ ] A cache miss and a cache outage produce the same answer as a hit — proved by an L3 test that disables Redis mid-suite
- [ ] Micrometer hit/miss counters wired now so `EN-OBS-2` (Sprint 07) has something to name

---

## Integration Risk & Dependencies


**The invalidation key scheme and the frontend cache-tag scheme are two names for one thing, owned by two people.** Nothing at `G3` will catch a mismatch, because both sides work correctly in isolation — the storefront simply never updates. Agree the exact key strings inside this sprint and write them down in one place, not two.

Secondary: `listCategoryProducts`'s cursor is the first list operation outside `identity`. If its envelope drifts from the Sprint 02 shape, every later list inherits the drift.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
