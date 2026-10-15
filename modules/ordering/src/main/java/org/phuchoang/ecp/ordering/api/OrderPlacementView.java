package org.phuchoang.ecp.ordering.api;

import java.util.UUID;

/** Stable placement response identifying the one created (or idempotently recovered) order. */
public record OrderPlacementView(UUID orderId, String status, boolean duplicate) { }
