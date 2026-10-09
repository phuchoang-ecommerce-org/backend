package org.phuchoang.ecp.catalog.internal.application.administration.command.product.image.remove;

import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductUpdated;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.service.ProductCommandService;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * `UC-ADM-01` — removes an aggregate-owned image. The product must exist ({@code NOT_FOUND}
 * otherwise); an image already absent is removed idempotently and still announces the product as
 * updated.
 */
@Service
public class RemoveProductImageService {

    private final ProductCommandService products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public RemoveProductImageService(ProductCommandService products, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public void remove(CatalogCommandContext context, UUID productId, UUID imageId) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        Product product = CatalogLookups.requireProduct(products.find(productId));
        Product after = products.removeImage(product, imageId);

        events.publish(new ProductUpdated(after), context);
    }
}
