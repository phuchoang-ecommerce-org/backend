package org.phuchoang.ecp.catalog.internal.application.command.product;

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

/** `UC-ADM-01` — replaces a product's mutable merchandising fields. A missing product is {@code NOT_FOUND}. */
@Service
public class UpdateProductService {

    private final ProductRepository products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public UpdateProductService(ProductRepository products, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public ProductSnapshot update(CatalogCommandContext context, UUID productId, ProductChange change) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        Product before = CatalogLookups.requireProduct(products, productId);
        Product after = products.save(before.change(change.categoryId(), change.name(), change.description(),
            change.brand(), change.attributes()));

        events.publish(new ProductUpdated(after), context);
        return ProductSnapshot.from(after);
    }
}
