package org.phuchoang.ecp.catalog.internal.application.command.product;

import java.util.Map;
import java.util.UUID;

/** Normalized replacement of a product's mutable merchandising fields. */
public record ProductChange(String name, String description, String brand, UUID categoryId,
        Map<String, Object> attributes) {
}
