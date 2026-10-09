package org.phuchoang.ecp.catalog.internal.application.administration.command.product.variant.changeprice;

import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.variant.VariantSnapshot;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPriceChanged;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.service.ProductCommandService;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** `UC-ADM-01` — replaces a variant's list price; a reason is mandatory and travels with the audit trail. */
@Service
public class ChangeVariantPriceService {

    private final ProductCommandService products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public ChangeVariantPriceService(ProductCommandService products, IdentityAuthorization authorization,
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

        Product product = CatalogLookups.requireProduct(products.find(productId));
        CatalogLookups.requireVariant(product, variantId);
        Product after = products.changePrice(product, variantId, price.amount(), price.currency());

        Product.Variant changed = after.variant(variantId);
        events.publish(new ProductPriceChanged(productId, changed), context);
        return VariantSnapshot.from(changed);
    }
}
