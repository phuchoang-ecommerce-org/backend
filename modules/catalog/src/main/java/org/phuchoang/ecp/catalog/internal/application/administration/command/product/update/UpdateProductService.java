package org.phuchoang.ecp.catalog.internal.application.administration.command.product.update;

import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.ProductSnapshot;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.ProductSnapshotMapper;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductUpdated;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.service.ProductCommandService;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** `UC-ADM-01` — replaces a product's mutable merchandising fields. A missing product is {@code NOT_FOUND}. */
@Service
public class UpdateProductService {

    private final ProductCommandService products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;
    private final ProductSnapshotMapper snapshots;

    public UpdateProductService(ProductCommandService products, IdentityAuthorization authorization,
            CatalogEventPublisher events, ProductSnapshotMapper snapshots) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
        this.snapshots = snapshots;
    }

    @Transactional
    public ProductSnapshot update(CatalogCommandContext context, UUID productId, ProductChange change) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        Product before = CatalogLookups.requireProduct(products.find(productId));
        Product after = products.change(before, change.categoryId(), change.name(), change.description(), change.brand(),
            change.attributes());

        events.publish(new ProductUpdated(after), context);
        return snapshots.productSnapshot(after);
    }
}
