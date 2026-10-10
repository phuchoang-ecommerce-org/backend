package org.phuchoang.ecp.catalog.internal.application.administration.command.product.image;

import java.util.UUID;

/** Application-owned representation of an administratively returned product image. */
public record ImageSnapshot(UUID id, String url, String altText, int sortOrder) {
}
