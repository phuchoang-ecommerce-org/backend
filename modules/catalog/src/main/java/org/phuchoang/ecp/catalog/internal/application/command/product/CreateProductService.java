package org.phuchoang.ecp.catalog.internal.application.command.product;

import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductCreated;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** `UC-ADM-01` — creates a draft product. */
@Service
public class CreateProductService {

    private final ProductRepository products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public CreateProductService(ProductRepository products, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public ProductSnapshot create(CatalogCommandContext context, CreateProduct command) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        Product product = products.save(Product.create(UUID.randomUUID(), command.categoryId(), command.name(),
            command.description(), command.brand(), command.attributes()));

        events.publish(new ProductCreated(product), context);
        return ProductSnapshot.from(product);
    }
}
