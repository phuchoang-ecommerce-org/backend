package org.phuchoang.ecp.catalog.internal.application.command.model;

import java.util.Map;
import java.util.UUID;

/** Complete normalized input for creating a draft Product aggregate. */
public record CreateProduct(UUID id, String name, String description, String brand, UUID categoryId,
        Map<String, Object> attributes) { }
