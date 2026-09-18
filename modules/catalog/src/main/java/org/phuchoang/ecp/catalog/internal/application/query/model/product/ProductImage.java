package org.phuchoang.ecp.catalog.internal.application.query.model.product;

import java.util.UUID;

public record ProductImage(UUID id, String url, String altText, int sortOrder) {
}
