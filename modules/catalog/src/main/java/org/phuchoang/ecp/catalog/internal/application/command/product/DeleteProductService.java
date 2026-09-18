package org.phuchoang.ecp.catalog.internal.application.command.product;

import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductDiscontinued;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * `UC-ADM-01` — removes a product and announces its SKUs as discontinued. Idempotent: deleting a
 * product that is already gone succeeds without an event.
 */
@Service
public class DeleteProductService {

    private final ProductRepository products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public DeleteProductService(ProductRepository products, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public void delete(CatalogCommandContext context, UUID productId) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        Product before = products.findById(productId).orElse(null);
        if (before == null) {
            return;
        }
        products.deleteById(productId);

        events.publish(new ProductDiscontinued(productId, before.variantIds(), before.variantSkus()), context);
    }
}
