package org.phuchoang.ecp.catalog.internal.application.administration.command.product.image.add;

import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.image.ImageSnapshot;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.ProductSnapshotMapper;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductUpdated;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.service.ProductCommandService;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** `UC-ADM-01` — adds an ordered display image owned by the product aggregate. */
@Service
public class AddProductImageService {

    private final ProductCommandService products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;
    private final ProductSnapshotMapper snapshots;

    public AddProductImageService(ProductCommandService products, IdentityAuthorization authorization,
            CatalogEventPublisher events, ProductSnapshotMapper snapshots) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
        this.snapshots = snapshots;
    }

    @Transactional
    public ImageSnapshot add(CatalogCommandContext context, UUID productId, AddImage command) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        Product product = CatalogLookups.requireProduct(products.find(productId));
        Product.Image image = new Product.Image(UUID.randomUUID(), command.url(), command.altText(), command.sortOrder());
        Product after = products.addImage(product, image);

        events.publish(new ProductUpdated(after), context);
        return snapshots.imageSnapshot(after.image(image.id()));
    }
}
