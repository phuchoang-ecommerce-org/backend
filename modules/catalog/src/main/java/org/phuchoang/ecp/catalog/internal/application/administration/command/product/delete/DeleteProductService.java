package org.phuchoang.ecp.catalog.internal.application.administration.command.product.delete;

import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductDiscontinued;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.service.ProductCommandService;
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

    private final ProductCommandService products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public DeleteProductService(ProductCommandService products, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public void delete(CatalogCommandContext context, UUID productId) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        Product before = products.find(productId).orElse(null);
        if (before == null) {
            return;
        }
        products.discontinue(before);

        events.publish(new ProductDiscontinued(productId, before.variantIds(), before.variantSkus()), context);
    }
}
