package org.phuchoang.ecp.inventory.api.management;

import java.util.UUID;

public record WarehouseView(UUID id, String code, String name, String countryCode, boolean active) { }
