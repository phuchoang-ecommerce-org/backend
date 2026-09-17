package org.phuchoang.ecp.catalog.internal.application.command.model;

import java.util.Map;
import java.util.UUID;

/** Normalized mutable product values, including a requested publication state when applicable. */
public record ProductChange(String name, String description, String brand, UUID categoryId,
        Map<String, Object> attributes, String publicationStatus) { }
