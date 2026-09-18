package org.phuchoang.ecp.catalog.internal.application.command.product;

import java.util.Map;
import java.util.UUID;

/** Normalized input for creating a draft Product aggregate; the service assigns the identifier. */
public record CreateProduct(String name, String description, String brand, UUID categoryId,
        Map<String, Object> attributes) {
}
