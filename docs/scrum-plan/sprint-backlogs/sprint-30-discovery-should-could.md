<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 30 — Release 3: Discovery `Should` and `Could` Stories

**Canonical sprint:** [Sprint 30 — Release 3: Discovery `Should` and `Could` Stories](../../sprint-backlogs/sprint-30-discovery-should-could.md)
**Lane:** Backend · R3 · **Gate:** none · **Backend 18 pts · Frontend 17 pts**

---

## Sprint Goal

> **Discovery gets its `Should` and `Could` capability.**

**Release 3 begins here, past the `Must` cut line.** Everything in this sprint is capability the release could have shipped without — which changes how its failures are treated rather than how carefully it is built.

One rule covers almost every story: **a discovery feature that fails must fail invisibly.** `UC-SCH-05` `E1` — recommendations unavailable omits the section and presents the product page in full. `UC-SCH-06` `E1` — trending unavailable omits trending and still shows new arrivals, which derive from the catalog rather than from behaviour. `UC-CAT-05` `E1` — featured categories unavailable omits the section. [`Routing.md`](../../../SA-docs/03-frontend/Routing.md) §4.1 puts it most directly: **a recommendation outage is not an event a customer should be told about.**

The counterweight is `BR-SCH-01`. `UC-SCH-04` `E2` is unambiguous: when personal history is unavailable the platform **never falls back to another customer's** — *an empty personal section is correct, a wrong one is a breach* (`P16`).

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-CAT-05` | View Featured Categories | 2 |
| BE | `US-SCH-02` | Search Suggestions and Auto-complete | 5 |
| BE | `US-SCH-04` | View Popular and Recent Keywords | 3 |
| BE | `US-SCH-05` | View Related and Frequently-Bought-Together | 5 |
| BE | `US-SCH-06` | View Trending Products and New Arrivals | 3 |
| | | **Backend total** | **18** |

## Backend Lane

### `US-SCH-02` Search Suggestions and Auto-complete (5 pts) — `getSearchSuggestions`
- [ ] Served from the Sprint 10 Elasticsearch read model; **`NFR-PERF-04`'s 150 ms budget is the tightest in the SRS** and regressions show here first (Testing Strategy §7.3 S4)
- [ ] `E1` — no completions returns nothing and **never substitutes a different query for the one being typed**
- [ ] `E2` — an unavailable suggestion service omits suggestions **silently**; the visitor can still type and submit. A failed convenience must not obstruct the underlying goal
- [ ] **`E3` — a response exceeding the latency target is abandoned rather than returned late.** A completion arriving after the visitor has typed past it is a distraction
- [ ] `BR-CAT-02` — unpublished products never appear in suggestions

### `US-SCH-05` View Related and Frequently-Bought-Together (5 pts) — `listRelatedProducts`, `listFrequentlyBoughtTogetherForProduct`, `listFrequentlyBoughtTogetherForCart`
- [ ] Derived from order history through the event backbone, never by querying `ordering` synchronously
- [ ] **`E1` — unavailable omits the section and the product page is presented in full** (`NFR-AVAIL-02`). Sprint 07 built that boundary; this fills it
- [ ] **`E2` — where every candidate is unpublished or out of stock, the section is omitted** rather than presenting products that cannot be bought
- [ ] `BR-CAT-02` throughout

### `US-SCH-04` View Popular and Recent Keywords (3 pts) — `listPopularKeywords`, `listOwnRecentKeywords`, `removeOwnRecentKeyword`, `clearOwnRecentKeywords`
- [ ] `E1` — unavailable popular keywords omits the section (`NFR-AVAIL-02`)
- [ ] **`E2` — where personal history is unavailable, popular keywords are still returned and the platform never falls back to another customer's history.** An empty personal section is correct; a wrong one is a breach (`BR-SCH-01`, `P16`)
- [ ] The personal operations are ownership-scoped absolutely; another customer's history returns the same response as one that does not exist
- [ ] `clearOwnRecentKeywords` and `removeOwnRecentKeyword` are idempotent, the same shape as Sprint 05's `removeOwnAddress`

### `US-SCH-06` View Trending Products and New Arrivals (3 pts) — `listTrendingProducts`, `listNewArrivals`
- [ ] **`E1` — trending data unavailable or too stale to be meaningful omits trending; new arrivals are still presented**, since they derive from the catalog rather than from behaviour
- [ ] **`E2` — where nothing was listed recently enough, the new-arrivals section is omitted rather than widened until it fills**, which would misrepresent old stock as new
- [ ] Both are `R1`-cacheable reads; they join the Sprint 06 cache-aside key namespaces rather than inventing new ones

### `US-CAT-05` View Featured Categories (2 pts) — `listFeaturedCategories`
- [ ] Featured categories in their configured order, with images
- [ ] `A1` — none designated **omits the section rather than returning an empty one**
- [ ] `E1` — unavailable omits the section and the rest of the home page is served; `E2` — **one removed category is omitted from the set and does not suppress the others**
- [ ] `BR-CAT-02`, `BR-CAT-03`

---

## Integration Risk & Dependencies


**No gate closes this sprint, and for the first time both lanes build the same five stories simultaneously.** Every earlier sprint gave the frontend a contract the backend had already implemented, or a mock the backend would later match. Here both sides implement against `openapi.yaml` at the same time, with nothing between them until `G15` two sprints away.

Agree the five response shapes explicitly at Planning — especially `getSearchSuggestions`, where the 150 ms budget may tempt a leaner payload than the contract documents.

Second, and specific to this sprint's nature: **silent failure is very hard to distinguish from a feature that was never wired.** A rail that disappears because the backend errored and a rail that disappears because the frontend forgot to call it look identical to everyone. Log the omission server-side and assert the call client-side, or the sprint can pass its own review while doing nothing.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

<!-- filled at Sprint Review -->

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
