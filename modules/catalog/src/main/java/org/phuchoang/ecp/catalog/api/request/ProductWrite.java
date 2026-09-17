package org.phuchoang.ecp.catalog.api.request;
import java.util.Map;
import java.util.UUID;

/** Administrator-supplied mutable product information for {@code UC-ADM-01}. */
public record ProductWrite(String name, String description, String brand, UUID categoryId, Map<String, Object> attributes) { }
