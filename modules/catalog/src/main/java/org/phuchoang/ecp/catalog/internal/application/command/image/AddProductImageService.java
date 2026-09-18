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

/** `UC-ADM-01` — adds an ordered display image owned by the product aggregate. */
@Service
public class AddProductImageService {

    private final ProductRepository products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public AddProductImageService(ProductRepository products, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public ImageSnapshot add(CatalogCommandContext context, UUID productId, AddImage command) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        Product product = CatalogLookups.requireProduct(products, productId);
        Product.Image image = new Product.Image(UUID.randomUUID(), command.url(), command.altText(), command.sortOrder());
        Product after = products.save(product.addImage(image));

        events.publish(new ProductUpdated(after), context);
        return ImageSnapshot.from(after.image(image.id()));
    }
}
