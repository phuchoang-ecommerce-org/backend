package org.phuchoang.ecp.catalog.internal.application.administration.command.product.publication;

import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.administration.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.ProductSnapshot;
import org.phuchoang.ecp.catalog.internal.application.administration.command.product.ProductSnapshotMapper;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.model.PublicationStatus;
import org.phuchoang.ecp.catalog.internal.domain.service.ProductCommandService;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * `UC-ADM-01` — moves a product between publication states. The business fact emitted follows the
 * target state: {@code PUBLISHED} → {@code ProductPublished}, {@code DISCONTINUED} →
 * {@code ProductDiscontinued}, anything else → {@code ProductUpdated}.
 */
@Service
public class ProductPublicationService {

    private final ProductCommandService products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;
    private final Clock clock;
    private final ProductSnapshotMapper snapshots;

    public ProductPublicationService(ProductCommandService products, IdentityAuthorization authorization,
            CatalogEventPublisher events, Clock clock, ProductSnapshotMapper snapshots) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
        this.clock = clock;
        this.snapshots = snapshots;
    }

    @Transactional
    public ProductSnapshot setPublication(CatalogCommandContext context, UUID productId, SetPublication command) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        PublicationStatus status = parse(command.publicationStatus());
        Product before = CatalogLookups.requireProduct(products.find(productId));
        Product after = products.transitionTo(before, status, clock.instant());

        events.publishFrom(after, context);
        return snapshots.productSnapshot(after);
    }

    private static PublicationStatus parse(String value) {
        try {
            return PublicationStatus.from(value);
        } catch (IllegalArgumentException exception) {
            throw CatalogLookups.invalid("publicationStatus");
        }
    }

}
