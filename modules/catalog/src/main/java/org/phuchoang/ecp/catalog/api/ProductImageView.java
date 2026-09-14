package org.phuchoang.ecp.catalog.api;

import java.util.UUID;

/** An image belonging to a product, returned in its configured display order. */
public record ProductImageView(UUID id, String url, String altText, int sortOrder) {
}
