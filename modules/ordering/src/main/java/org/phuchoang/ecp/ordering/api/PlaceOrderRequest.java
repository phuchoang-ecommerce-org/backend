package org.phuchoang.ecp.ordering.api;

import java.util.UUID;

/** HTTP-neutral public placement body; the idempotency key is deliberately a request header. */
public record PlaceOrderRequest(UUID checkoutId, String confirmationToken) { }
