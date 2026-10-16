package org.phuchoang.ecp.ordering.api;

import java.util.UUID;

/** Deliberately minimal cross-context order observation; customer/address data stays in Ordering. */
public record OrderFulfilmentView(UUID orderId, UUID customerId, String status) { }
