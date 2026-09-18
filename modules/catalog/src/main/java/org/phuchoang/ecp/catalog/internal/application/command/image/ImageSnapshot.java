package org.phuchoang.ecp.catalog.internal.application.command.image;

import org.phuchoang.ecp.catalog.internal.domain.model.Product;

import java.util.UUID;

/** Application-owned representation of an administratively returned product image. */
public record ImageSnapshot(UUID id, String url, String altText, int sortOrder) {

    public static ImageSnapshot from(Product.Image image) {
        return new ImageSnapshot(image.id(), image.url(), image.altText(), image.sortOrder());
    }
}
