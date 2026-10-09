package org.phuchoang.ecp.inventory.internal.application.management.query;

import java.util.UUID;

/** Operational read model for a warehouse. */
public record WarehouseView(UUID id, String code, String name, String countryCode, boolean active) { }
