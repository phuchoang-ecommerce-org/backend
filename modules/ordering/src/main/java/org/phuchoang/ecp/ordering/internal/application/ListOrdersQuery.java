package org.phuchoang.ecp.ordering.internal.application;

import java.util.UUID;

/**
 * `listOrders`'s filter/paging parameters (`common.yaml`, `paths/ordering.yaml#/orders`).
 * {@code customerId} is `STAFF`/`WAREHOUSE_OPERATOR`/`CUSTOMER_SUPPORT`/`ADMINISTRATOR`-only —
 * `403` for a `CUSTOMER` (enforced by {@link ListOrdersService}, not this holder).
 */
public record ListOrdersQuery(String cursor, int size, UUID customerId) {
}
