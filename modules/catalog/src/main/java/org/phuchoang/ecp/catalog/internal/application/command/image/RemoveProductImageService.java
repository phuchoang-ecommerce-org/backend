package org.phuchoang.ecp.catalog.internal.application.command.image;

import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductUpdated;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
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

    private final ProductRepository products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public RemoveProductImageService(ProductRepository products, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public void remove(CatalogCommandContext context, UUID productId, UUID imageId) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        Product product = CatalogLookups.requireProduct(products, productId);
        Product after = products.save(product.removeImage(imageId));

        events.publish(new ProductUpdated(after), context);
    }
}
