package org.phuchoang.ecp.catalog.internal.application.command.variant;

import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPriceChanged;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** `UC-ADM-01` — replaces a variant's list price; a reason is mandatory and travels with the audit trail. */
@Service
public class ChangeVariantPriceService {

    private final ProductRepository products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public ChangeVariantPriceService(ProductRepository products, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public VariantSnapshot changePrice(CatalogCommandContext context, UUID productId, UUID variantId, Price price) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);
        if (price.reason() == null || price.reason().isBlank()) {
            throw CatalogLookups.invalid("reason");
        }

        Product product = CatalogLookups.requireProduct(products, productId);
        CatalogLookups.requireVariant(product, variantId);
        Product after = products.save(product.changePrice(variantId, price.amount(), price.currency()));

        Product.Variant changed = after.variant(variantId);
        events.publish(new ProductPriceChanged(productId, changed), context);
        return VariantSnapshot.from(changed);
    }
}
