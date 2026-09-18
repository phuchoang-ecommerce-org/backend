package org.phuchoang.ecp.catalog.internal.application.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ImagePayload(UUID id, String url, String altText, int sortOrder) {

    public static ImagePayload of(Product.Image image) {
        return new ImagePayload(image.id(), image.url(), image.altText(), image.sortOrder());
    }
}
