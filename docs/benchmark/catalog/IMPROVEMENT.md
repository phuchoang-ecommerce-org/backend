# Catalog Improvement Suggestions

These recommendations come from the 2026-10-02 first-look benchmark in [README.md](README.md). They are not implemented here.

## Priority summary

| Priority | Improvement | Evidence | Expected result |
|---|---|---|---|
| P0 | Correct PostgreSQL numeric mapping | Rated listings and product detail return HTTP 500. | Restore core catalog correctness for products with ratings. |
| P0 | Make Redis cache reads type-safe | Stored entries deserialize as maps and every nominal hit reloads PostgreSQL. | Make category tree and repeated listing/detail reads actual cache hits. |
| P1 | Apply listing pagination before cover-image lookup | First-page plan ran 10,000 correlated image lookups and hit 31,105 buffers. | Make default/created/popularity listing cost closer to page size. |
| P1 | Stop recomputing exact counts per cursor page | Every page separately aggregates all 10,000 products and 20,000 variants; count alone took 87.856 ms. | Reduce repeated database work, especially on continuation pages. |
| P2 | Add a repeatable benchmark and observable cache acceptance gate | Current metrics require authenticated management access and the cache defect was visible only end to end. | Prevent silent cache regressions and preserve comparable baselines. |

## P0 — Fix non-null rating mapping

`catalog_product.average_rating` is PostgreSQL `NUMERIC(3,2)`, which the driver exposes as `BigDecimal`. `CatalogProductQueries.productDetailRow` and `ProductListingRowMapper` cast `ResultSet.getObject("average_rating")` to `Double`, causing a `ClassCastException` whenever a rating exists.

Recommended change:

- Read the column as `BigDecimal` (or another `Number`) and explicitly convert to the domain/API representation.
- Keep null handling unchanged for unrated products.
- Add PostgreSQL-backed regression coverage for both product detail and listing with a non-null rating. A mocked `ResultSet` is insufficient because the JDBC driver type is the failure boundary.

Acceptance:

- A rated product returns HTTP 200 from both endpoints and retains its decimal rating.
- The benchmark fixture no longer needs the `average_rating = NULL` workaround.

## P0 — Restore real cache hits

The cache writes valid-looking JSON, but the configured generic serializer does not preserve the concrete result type. On read, the value is a generic map rather than `CategoryTree`, `ProductPage`, or `ProductDetail`; `type.isInstance(cached)` fails and `RedisCacheAside` calls the loader again.

Recommended change:

- Use the `Class<T>` already supplied to `CacheAside.getOrLoad` to deserialize into the requested type, or store an explicit, allow-listed type envelope. Do not enable unrestricted polymorphic deserialization.
- Treat incompatible/legacy entries as misses and replace them, preserving the documented fail-open behavior.
- Add a real-serializer test proving that two calls for each catalog result type invoke the loader once and return equal typed values.
- Retain hit/miss/error counters and add a test asserting that a second request increments `hit`, not `miss`.

Acceptance:

- A primed request does not execute catalog SQL.
- Category tree and listing p95 are rerun against the 10,000-product fixture and fall below 300 ms at the same light concurrency.
- Redis failure still falls through to PostgreSQL without failing the catalog request.

## P1 — Reshape the listing query around the page boundary

The current CTE builds price, availability, and cover image for almost every matching product before sorting and limiting. The cover-image scalar subquery executed 10,000 times for a 20-item first page and 9,980 times for the next page.

Recommended change:

- For name, created-at, and popularity sorts, identify the 21 page product IDs first using the existing keyset order and supporting indexes.
- Join/aggregate variants and retrieve the cover image only for those page IDs.
- Replace the per-product image scalar subquery with a page-scoped join or lateral lookup after the limit.
- For price sorting, where variant aggregation determines order, evaluate a catalog-owned PostgreSQL listing projection containing per-product minimum/maximum price, advisory availability, and cover image. Keep PostgreSQL authoritative and update the projection within Catalog's existing write/event boundaries.

Acceptance:

- Default first and continuation plans execute at most one cover-image lookup per returned/look-ahead item, not per catalog product.
- `EXPLAIN (ANALYZE, BUFFERS)` shows work bounded primarily by the category/page selection rather than roughly 31,000 shared-buffer hits.
- Cursor order and published-product filtering remain unchanged.

## P1 — Reuse or avoid repeated exact counts

The API returns an exact total, so every cursor page runs the same expensive count before the page query. Keyset pagination removes `OFFSET` but does not currently remove this repeated full aggregation.

Recommended change:

- Cache the count independently using category plus filter fingerprint, excluding cursor and sort when they do not change membership.
- Invalidate it with the existing category-listing invalidation unit.
- For unfiltered listings, use the cheapest membership-only count and avoid variant/image aggregation.
- If product requirements permit later, consider making exact totals optional for continuation pages; treat that as an explicit API-contract decision rather than an internal optimization.

Acceptance:

- First and continuation requests sharing filters compute the count once.
- Unfiltered count plans do not join images or aggregate variant price/availability.
- Total values remain correct after publication, category, variant, and availability changes covered by existing invalidation events.

## P2 — Keep a small performance regression gate

Turn this first look into an inexpensive, repeatable check after the P0/P1 work:

- Preserve the deterministic 10,000-product fixture in test/benchmark infrastructure rather than production migrations.
- Record cold and confirmed cache-hit paths separately.
- Export or securely scrape `ecp.catalog.cache.accesses` during the run and fail if the warm phase reports misses/errors.
- Keep the default CI gate smaller if needed, but schedule the 10,000-product version periodically and before releases.
- Track p50/p95, failures, SQL execution count, and representative query plans; do not substitute request throughput alone for latency correctness.

Acceptance:

- Zero HTTP failures, typed rating payloads, verified Redis hits, and p95 below the documented threshold for the scoped low-concurrency browse paths.
- Stress/capacity validation remains a separate exercise for `NFR-SCAL-04` and peak-event requirements.
