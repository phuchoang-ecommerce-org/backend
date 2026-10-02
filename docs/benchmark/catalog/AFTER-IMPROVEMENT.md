# Catalog Benchmark — After Improvements

**Run date:** 2026-10-02 (Asia/Ho_Chi_Minh)  
**Base revision:** `4c070d5b0a8bf1ded9c17e5dfe24a83ecdbfa296` (with the working-tree improvements below)  
**Baseline:** [README.md](README.md)  
**Status:** completed; confirmed warm-cache browse paths meet the 300 ms p95 directional target.

## What changed

- PostgreSQL `NUMERIC(3,2)` ratings are read as `BigDecimal` and converted explicitly, so rated listings and product details no longer cast a driver value to `Double`.
- The evictable Redis cache stores plain JSON and deserializes only into the `Class<T>` supplied by the cache-aside caller. Legacy/incompatible JSON is a miss and is replaced; Redis read/write failure remains fail-open.
- Full and scoped category trees are cacheable. A category-tree invalidation clears all tree variants.
- Category listings cache the exact membership total independently of cursor, page size, and sort, while the existing listing invalidation prefix clears both totals and pages.
- Name, created-at, and popularity listing queries select the 21 page/look-ahead products before variant aggregation and cover-image lookup. Aggregate-dependent paths, including price sorting, still use authoritative PostgreSQL aggregation but defer each cover lookup until after the page boundary.
- [fixture.sql](fixture.sql) preserves the disposable deterministic 10,000-product fixture used here.

The identity `RateLimitBuckets` component was also given an explicit bean name. This was an existing duplicate-default-bean-name startup blocker discovered while launching the benchmark; its rate-limit behavior and consumers are unchanged.

## Comparable results

The environment and HTTP procedure matched the baseline: macOS/Colima with 4 GB, PostgreSQL 16.15, Redis 7.4.11, the 10,000-product fixture, ApacheBench `-k -n 100 -c 4`, and one cache-populating request before the warm sample. Values are client-observed loopback times, so they remain diagnostic rather than a formal server-side NFR certification.

| Scenario | Before p95 | After p95 | Change | Failed | 300 ms p95 |
|---|---:|---:|---:|---:|---|
| Category tree, scoped root/depth | 894 ms | **277 ms** | -69% | 0 | Pass |
| Default listing, first page | 1,311 ms | **213 ms** | -84% | 0 | Pass |
| Default listing, next page | 1,972 ms | **235 ms** | -88% | 0 | Pass |
| Price listing, first page | 1,214 ms | **73 ms** | -94% | 0 | Pass |
| Rated product detail | 79 ms | **50 ms** | -37% | 0 | Pass |

The default first-page warm sample was repeated after the initial client/JIT settling pass; the repeated, confirmed-hit result (p50 79 ms, p95 213 ms) is the value reported above. All other rows were confirmed cache hits in their initial warm sample.

Cold samples are single observations only: category 4.564 s during fresh application warm-up, default first listing 603 ms, next listing 167 ms, price listing 606 ms, and rated detail 135 ms. Do not compare those directly with the p95 distribution.

## Correctness and cache evidence

- All benchmark requests returned HTTP 200. The rated listing returned `averageRating: 1.01` and the rated detail returned HTTP 200 with a decimal rating; the baseline had to null ratings to avoid HTTP 500.
- A cold listing created two Redis entries (membership total plus page). Requesting its continuation without flushing increased the cache size only to three entries, proving the continuation reused the first page's total instead of creating a cursor-specific count.
- The individual cold-prime scenarios created one scoped category-tree entry, two listing entries, and one product-detail entry. Each corresponding 100-request warm run completed with zero failures.
- `RedisCacheAsideTest` serializes and reads the category, listing, and detail result shapes through the real JSON mapper, asserting one loader invocation and a warm `hit` counter for each.

## Query-plan evidence

The optimized default first-page plan executed in **19.592 ms** with **364 shared-buffer hits**. Its cover-image lateral lookup executed **21 times**—once per returned/look-ahead page row—rather than the baseline's 10,000 scalar image lookups and 31,105 buffer hits. Variant index scans likewise executed only for the 21 selected products.

The category selection still scans/sorts the 10,000 matching product identities because this benchmark root spans the whole catalog; no new physical projection was introduced. The expensive per-product image and variant expansion is now bounded by page size. Price sorting remains authoritative PostgreSQL aggregation, with its cover-image lookup likewise moved after `LIMIT`.

## Conclusion

The P0 correctness and cache defects are resolved for the benchmarked Catalog browse paths, and the P1 page-boundary/count improvements materially reduce observed warm-read latency. Every scoped low-concurrency warm scenario now satisfies the baseline's directional 300 ms p95 signal with zero HTTP failures.

This is still not stress, capacity, or production certification. The management metrics endpoint remains authenticated in the normal profile, so the run used deterministic Redis entry counts plus the in-process cache regression test as its cache acceptance evidence. A scheduled benchmark job with authenticated metric scraping is the remaining P2 operational gate.

## Verification performed

- `./gradlew :catalog:test --tests org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing.ProductListingSqlBuilderTest`
- Focused Catalog regression build/test run covering cache serialization, cache invalidation, browse services, listing wiring, and listing SQL composition.
- `./gradlew :app:bootJar --rerun-tasks`
- Live PostgreSQL/Redis/Kafka benchmark against [fixture.sql](fixture.sql), including `EXPLAIN (ANALYZE, BUFFERS)` for the default listing.
