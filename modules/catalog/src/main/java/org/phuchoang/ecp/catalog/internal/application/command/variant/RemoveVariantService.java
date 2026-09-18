package org.phuchoang.ecp.catalog.internal.application.command.variant;

import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
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
 * `UC-ADM-01` — removes a variant. Idempotent: a missing product or a missing variant both succeed
 * without an event; the SKU stays retired at the database (`catalog_retired_sku`).
 */
@Service
public class RemoveVariantService {

    private final ProductRepository products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public RemoveVariantService(ProductRepository products, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public void remove(CatalogCommandContext context, UUID productId, UUID variantId) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        Product product = products.findById(productId).orElse(null);
        if (product == null || product.variant(variantId) == null) {
            return;
        }
        Product after = products.save(product.removeVariant(variantId));

        events.publish(new ProductUpdated(after), context);
    }
}
