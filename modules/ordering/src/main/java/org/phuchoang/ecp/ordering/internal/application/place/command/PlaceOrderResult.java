package org.phuchoang.ecp.ordering.internal.application.place.command;

import java.util.UUID;

/** Transport-neutral outcome; duplicate tells the HTTP boundary to return the original resource. */
public record PlaceOrderResult(UUID orderId, String status, boolean duplicate) { }
