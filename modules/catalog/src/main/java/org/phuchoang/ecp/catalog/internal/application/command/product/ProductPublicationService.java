package org.phuchoang.ecp.catalog.internal.application.command.product;

import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.CatalogDomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductDiscontinued;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPublished;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductUpdated;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.model.PublicationStatus;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
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

    private final ProductRepository products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;
    private final Clock clock;

    public ProductPublicationService(ProductRepository products, IdentityAuthorization authorization,
            CatalogEventPublisher events, Clock clock) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public ProductSnapshot setPublication(CatalogCommandContext context, UUID productId, SetPublication command) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        PublicationStatus status = parse(command.publicationStatus());
        Product before = CatalogLookups.requireProduct(products, productId);
        Product after = products.save(before.transitionTo(status, clock.instant()));

        events.publish(eventFor(status, after), context);
        return ProductSnapshot.from(after);
    }

    private static PublicationStatus parse(String value) {
        try {
            return PublicationStatus.from(value);
        } catch (IllegalArgumentException exception) {
            throw CatalogLookups.invalid("publicationStatus");
        }
    }

    private static CatalogDomainEvent eventFor(PublicationStatus status, Product product) {
        return switch (status) {
            case PUBLISHED -> new ProductPublished(product);
            case DISCONTINUED -> new ProductDiscontinued(product.id(), product.variantIds(), product.variantSkus());
            case DRAFT, UNPUBLISHED -> new ProductUpdated(product);
        };
    }
}
