<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 07 — Catalog: Product Detail & Read Cache

**Canonical sprint:** [Sprint 07 — Catalog: Product Detail & Read Cache](../../sprint-backlogs/sprint-07-catalog-detail-and-cache.md)
**Lane:** Backend · R1 · **Gate:** **`G3` — Contract Sync** · **Backend 18 pts · Frontend 20 pts**

---

## Sprint Goal

> **The product page is the reference implementation of `NFR-AVAIL-02`.**

[`Routing.md`](../../../SA-docs/03-frontend/Routing.md) §4.1 calls the product page's boundary layout **normative rather than incidental**. Four sections fail independently: product/variants/price takes the page down because it *is* the page; availability, reviews and recommendations must not. Every later page that composes advisory sections copies what is built here, so getting the boundaries wrong now is a pattern that propagates, not a bug that stays local.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-CAT-03` | View Product Details | 5 |
| BE | `EN-DATA-3` | Catalog schema, indexes, and cursor-pagination query design | 8 |
| BE | `EN-OBS-2` | Micrometer meters named in Deployment §8 | 5 |
| | | **Backend total** | **18** |

## Backend Lane

### `US-CAT-03` View Product Details (5 pts) — `getProduct`, `listProductVariants`, `getProductRatingSummary`
- [ ] `BR-CAT-02` checked at step 2 — an unpublished product is `404`, and the same `404` a never-existing id returns
- [ ] Name, description, images, brand, categories, attributes, price, variants assembled in one read path. **Images arrive with `getProduct`** — there is no `listProductImages` operation ([`Routing.md`](../../../SA-docs/03-frontend/Routing.md) §4.1 n.1); `addProductImage`/`removeProductImage` are admin operations and belong to Sprint 09
- [ ] Per-variant availability (`FR-INV-07`) served from the catalog projection, **labelled advisory** — `inventory` does not exist until Sprint 11 and this value is not the binding check
- [ ] `getProductRatingSummary` returns the designed empty summary — `review` does not exist until Sprint 24. The empty shape is the deliverable, the way Sprint 05's `listOrders` empty page was
- [ ] `A2` — a product on promotion returns both the standard and the promotional price plus the period (`FR-PRM-02`); shape only this sprint, since `promotion` arrives in Sprint 15
- [ ] `A3` — every variant out of stock still returns the product in full
- [ ] Exception flows: `E1` unpublished/removed; `E2` reviews unavailable omits the section; `E3` related unavailable omits the rail. **`E2` and `E3` must not propagate into the product response's status code**
- [ ] Contract test both directions on all three operations

### `EN-DATA-3` Catalog schema, indexes, and cursor-pagination query design (8 pts)
- [ ] Flyway migration for the full `catalog` table set under the Sprint 02 prefix convention
- [ ] Indexes for: category descendant lookup, published-product filter, variant resolution by dimension set, and the cursor ordering columns for each sort option `US-CAT-02` exposes
- [ ] **The cursor query design is the deliverable**: a keyset predicate per sort option, each proved stable by an L5 test that inserts a row mid-pagination and asserts no row is skipped or repeated
- [ ] `EXPLAIN` recorded for every listing query in the module's test resources, so a later regression is visible as a plan change rather than as a latency complaint

### `EN-OBS-2` Micrometer meters named in Deployment §8 (5 pts)
- [ ] Every meter named in [`Deployment Diagram.md`](../../../SA-docs/01-system/Deployment%20Diagram.md) §8 registered, with the names exactly as written there — a meter with a plausible but different name is an unmonitored meter
- [ ] The Sprint 06 cache hit/miss counters folded into the named set
- [ ] Management port exposure confirmed against `EN-OBS-1` (Sprint 04); no meter leaks onto the public port

---

## Integration Risk & Dependencies


**`getProductRatingSummary` returns an empty summary against the real API and a populated one against the mock, for seventeen sprints.** Like Sprint 05's `listOrders`, that difference is expected and must be recorded as expected at `G3`, not logged as drift. What is checked is the envelope, not the rows.

Second: `NFR-AVAIL-02` is asserted by *removing* a dependency. If `G3` only exercises the happy path, the four-boundary layout is untested and the sprint's stated goal is unverified.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
