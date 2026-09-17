<!-- Generated from the canonical PM-docs plan. Do not edit directly; run `node PM-docs/scripts/generate-lane-plans.mjs`. -->

# Backend Plan — Sprint 09 — Catalog Administration

**Canonical sprint:** [Sprint 09 — Catalog Administration](../../sprint-backlogs/sprint-09-catalog-administration.md)
**Lane:** Backend · R1 · **Gate:** **`G4` — Contract Sync** · **Backend 21 pts · Frontend 15 pts**

---

## Sprint Goal

> **An operator can manage the catalog, and the storefront notices.**

The second half of that sentence is the sprint. Catalog writes without revalidation is a feature; catalog writes *whose effect reaches the storefront through the event backbone* is the architecture [`ADR-0038`](../../../SA-docs/01-system/ADR/ADR-0038-event-driven-catalog-revalidation.md) chose — **one invalidation path, not two**. The tempting shortcut, revalidating directly from the admin write, is precisely what this sprint must not do.

This is also the last sprint before IH-1.

## Committed Backend Work

| Lane | ID | Item | Pts |
|---|---|---:|---:|
| BE | `US-ADM-01` | Manage Products | 8 |
| BE | `US-ADM-02` | Manage Categories | 5 |
| BE | `EN-EVENT-2` | Catalog events published; consumer idempotency and ordering guards | 8 |
| | | **Backend total** | **21** |

## Backend Lane

### `US-ADM-01` Manage Products (8 pts) — `createProduct`, `updateProduct`, `deleteProduct`, `setProductPublication`, `addProductVariant`, `removeProductVariant`, `changeVariantPrice`, `addProductImage`, `removeProductImage`, `amendProductsInBulk`
- [ ] `BR-CAT-01` — SKU uniqueness enforced at the database, not only in the service; `E1` returns the conflict **named**
- [x] `E2` — actor lacking authority is refused by the server via the `identity` `AuthorizationService`, and the attempt recorded. Repricing is `P16`'s worked example
- [ ] `E3` — removal of a product with stock or open orders is declined and unpublishing offered instead. **Neither `inventory` nor `ordering` exists yet**: implement the check against the ports they will provide, return the declined outcome for the cases that are decidable today, and record the gap here rather than marking it done
- [ ] `E4` — validation failure applies nothing; the whole submission is one transaction
- [x] `E5` — **audit entry cannot be written → the change is not applied** (`BR-AUD-01`, `UC-AUD-01` E1). Audit is still the Sprint 03/04 stub listener; wire the *refusal path* now so `US-AUD-01` in Sprint 12 replaces a stub rather than adding a behaviour
- [x] `E6` — search propagation failure lets the change **stand**; retried, not rolled back (`P4`)
- [x] `amendProductsInBulk` is atomic per the contract's documented semantics; a partial bulk result is `P7`
- [ ] Permission-matrix cell asserted per operation

### `US-ADM-02` Manage Categories (5 pts) — `createCategory`, `updateCategory`, `deleteCategory`
- [x] `BR-CAT-03` — `E1` cycle detection on reparent, enforced server-side; the Sprint 06 read-side invariant and this write-side check share one implementation
- [x] `E2` — removal while holding products or children is declined **with the counts**, so the operator knows what to reassign
- [x] `E3` authority; `E4` audit-write failure declines the change
- [ ] Cache invalidation fires on the Sprint 06 `EN-WIRE-3` key namespaces

### `EN-EVENT-2` Catalog events published; consumer idempotency and ordering guards (8 pts)
- [ ] Every catalog write publishes through the Sprint 08 outbox — **never directly to Kafka**, and ArchUnit asserts no module holds a producer
- [x] Event types and payloads registered in the Sprint 08 topic catalogue
- [ ] **Consumer idempotency**: the same event delivered twice produces one effect. Proved by replaying the same envelope, not by inspecting code
- [ ] **Ordering guards**: two events for one aggregate apply in order; an out-of-order arrival is detected and handled rather than silently applied. This is what the Sprint 08 partition key buys, and it is verified here
- [ ] Correlation id survives write → outbox → Kafka → consumer (`NFR-OBS-03`), which IH-1 row 8 then extends to the projection

---

## Integration Risk & Dependencies


**`EN-EVENT-2` → `EN-FE-API-3` is the only cross-lane runtime path in the plan so far**, and it has no contract test — the callback is not in `openapi.yaml`, because it is not an `ecp-api` operation. It will not be caught by checks 2 or 3 at `G4`; it has to be exercised by hand, end to end, at the gate.

Second: ten catalog write operations move from mock to real in one gate. This is the largest single increment of contract surface since Sprint 03.

## Definition of Done

Every item satisfies the [backend Definition of Done](../definition-of-done.md) and the [shared story-level integration criteria](../../definition-of-done.md#5-definition-of-done--the-story).

## Review Notes

Implementation progress recorded 2026-09-16: product/category command paths, RBAC, transactional audit stub,
outbox event publication, topic registration, and the revalidation consumer are implemented. The remaining
unchecked items require the deferred Inventory/Ordering ports or explicit replay, ordering, contract, and
end-to-end verification before they can be claimed complete.

## Retrospective

**Went well:**
**Change one thing:**
**Action (owned, carried to next sprint's board):**
