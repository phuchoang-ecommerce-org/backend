# Catalog First-Look Benchmark

**Run date:** 2026-10-02 (Asia/Ho_Chi_Minh)  
**Revision:** `b61165bc517b93c2fa24eb791967d375143293dd`  
**Status:** Completed

## Objective and success signal

This is a quick, low-concurrency first look at the Catalog module's conversion-critical browse paths. It is not a stress, capacity, soak, or production certification run.

The directional comparison is `NFR-PERF-01`: catalog and category reads should complete within **300 ms at p95** under expected load. The measurements below include loopback HTTP and client time, while the requirement is defined as server-side latency, so they are diagnostic evidence rather than a formal acceptance result.

Search, catalog administration writes, external network latency, and thousands-of-customer concurrency are out of scope.

## Tools and strategy

| Tool | Purpose |
|---|---|
| ApacheBench 2.3 | A short, repeatable HTTP sample: 100 requests per scenario at concurrency 4, with keep-alive requested. |
| `curl` | One cache-cold request per scenario plus payload/status validation. |
| Redis CLI | Flush only the disposable catalog cache between scenarios and inspect the stored representation. |
| PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)` | Separate HTTP/cache symptoms from query cost and index behavior. |
| Actuator | Confirm application startup. Catalog cache counters were not exported because the management endpoint requires authentication in the normal profile. |

Each scenario used this sequence:

1. Flush `redis-cache`; do not flush correctness-bearing `redis-state`.
2. Send one request and record status, body size, and elapsed time. This request populates the cache.
3. Send 100 requests at concurrency 4 with `ab -k -n 100 -c 4`.
4. Require a valid payload and zero failed requests.

The single cold values are indicative only. The 100-request distribution is the main comparison.

## Environment and fixture

- macOS 15.8.1, x86_64, Intel Core i3-1000NG4 (4 logical CPUs), 8 GB host RAM.
- Colima using Virtualization.Framework with 4 GB configured memory.
- OpenJDK 21.0.12.1, PostgreSQL 16.15, Redis 7.4.11.
- Spring Boot executable JAR, normal security profile, PostgreSQL/Kafka/two Redis instances in the isolated Compose project `ecp-catalog-bench`.
- Elasticsearch was intentionally absent because search was out of scope; its bootstrap degraded as designed.
- Deterministic PostgreSQL fixture generated with `generate_series`: 100 categories (three levels), 10,000 published products, 20,000 active variants, and 10,000 product images. This reaches the `NFR-SCAL-01` catalog-volume floor.

The fixture varied brands, prices, rating counts, timestamps, and advisory availability. IDs, slugs, and SKUs were deterministic. Flyway created the schema before data was inserted, and `ANALYZE` ran before measurement.

### Smoke-test blocker and fixture workaround

The first representative requests used non-null `average_rating` values. Both product listing and product detail returned HTTP 500 because PostgreSQL returned `NUMERIC` as `BigDecimal`, while both row mappers cast it directly to `Double`.

That failure is a benchmark result. To measure the remaining paths without modifying production code, only the disposable fixture was updated to set `average_rating = NULL`. All latency results below therefore carry this limitation; the defect must be fixed before a fully representative rerun.

## Results

| Scenario | Payload validation | Cold sample | Mean | p50 | p95 | p99 | Requests/s | Failed | 300 ms p95 |
|---|---|---:|---:|---:|---:|---:|---:|---:|---|
| Category tree | 1 root, 9 direct children, 41,351 B | 351 ms | 589 ms | 536 ms | **894 ms** | 980 ms | 6.79 | 0 | Miss |
| Default listing, first page | 20 items, total 10,000, next cursor | 212 ms | 872 ms | 832 ms | **1,311 ms** | 1,587 ms | 4.59 | 0 | Miss |
| Default listing, next page | 20 items, begins at product 00021 | 894 ms | 1,090 ms | 963 ms | **1,972 ms** | 2,423 ms | 3.67 | 0 | Miss |
| Price listing, first page | 20 items, minimum price 10.0000 USD | 279 ms | 824 ms | 772 ms | **1,214 ms** | 1,609 ms | 4.85 | 0 | Miss |
| Product detail | Expected product, 2 variants, 1 image, 1,110 B | 30 ms | 43 ms | 39 ms | **79 ms** | 90 ms | 93.46 | 0 | Pass |

All measured requests returned HTTP 200 after the fixture workaround. Product detail was the only path comfortably inside the target. The cold samples should not be compared directly with p95 because they are single observations and benefited from already-warm PostgreSQL pages.

## Diagnostic evidence

### Cache behavior

Redis contained the expected catalog keys and JSON values, but the stored JSON had no concrete type metadata. `RedisCacheAside` receives the value back as a generic map, `type.isInstance(cached)` returns false, and the loader runs again. The benchmark's nominally warm requests therefore repeatedly reached PostgreSQL.

This explains why category tree and listings did not acquire the expected cache-hit latency. It also means this run primarily measures the database-backed behavior at light concurrency, not a healthy warm-cache path.

### Listing query plans

| Statement | Execution time | Shared-buffer hits | Important observation |
|---|---:|---:|---|
| Exact count | 87.856 ms | 1,096 | Aggregates all 10,000 products and 20,000 variants. |
| Default first page | 1,192.285 ms | 31,105 | Cover-image subquery ran 10,000 times before the 21-row limit. |
| Default next page | 187.858 ms | 31,036 | Still processed 9,980 products and ran 9,980 image lookups. |
| Price first page | 196.525 ms | 31,099 | Aggregated and sorted all 10,000 products; image lookup ran 10,000 times. |

The first-page plan includes 30,000 image-index buffer hits. Keyset pagination avoids `OFFSET`, but the current CTE still constructs almost the entire listing projection before applying `LIMIT`; later pages do not become proportional to page size. Each HTTP listing also executes the exact-count statement in addition to the page statement.

## Reproduction outline

```sh
colima start --memory 4
DOCKER_HOST=unix://$HOME/.colima/default/docker.sock \
  docker compose -p ecp-catalog-bench up -d postgres redis-cache redis-state kafka
./gradlew :app:bootJar
ECP_CURSOR_ACTIVE_KEY=benchmark-only-cursor-signing-key-32 \
ECP_REDIS_CACHE_HOST=127.0.0.1 ECP_REDIS_STATE_HOST=127.0.0.1 \
  java -jar app/build/libs/app-0.0.1-SNAPSHOT.jar
```

After Flyway is ready, load the deterministic fixture described above, `ANALYZE` the four catalog tables, flush `redis-cache` before each scenario, and run:

```sh
ab -k -n 100 -c 4 'http://127.0.0.1:8080/api/v1/categories?rootId=<root>&maxDepth=3'
ab -k -n 100 -c 4 'http://127.0.0.1:8080/api/v1/categories/<root>/products?size=20'
ab -k -n 100 -c 4 'http://127.0.0.1:8080/api/v1/categories/<root>/products?size=20&cursor=<cursor>'
ab -k -n 100 -c 4 'http://127.0.0.1:8080/api/v1/categories/<root>/products?size=20&sort=price:asc'
ab -k -n 100 -c 4 'http://127.0.0.1:8080/api/v1/products/<product>'
```

Stop the application and remove only the isolated benchmark project when finished:

```sh
DOCKER_HOST=unix://$HOME/.colima/default/docker.sock \
  docker compose -p ecp-catalog-bench down
```

## Interpretation limits

- This run cannot validate `NFR-SCAL-04` or peak-event behavior.
- ApacheBench reports client-observed loopback time, not the exact server-side measurement named by the SRS.
- The host is a small development machine shared by the application and all containers.
- Rating values were nulled after the functional failure, so rating-bearing payload performance remains unmeasured.
- The ineffective cache makes the warm distribution useful for discovering the defect but not representative of the intended Redis architecture.
