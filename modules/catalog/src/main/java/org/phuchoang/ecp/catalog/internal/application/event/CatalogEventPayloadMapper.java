package org.phuchoang.ecp.catalog.internal.application.event;

import org.phuchoang.ecp.catalog.internal.application.event.payload.CategoryChangedPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.CategoryRemovedPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.MoneyPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.ProductChangedPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.ProductDiscontinuedPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.ProductPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.ProductPriceChangedPayload;
import org.phuchoang.ecp.catalog.internal.application.event.payload.VariantAddedPayload;
import org.phuchoang.ecp.catalog.internal.domain.event.CatalogDomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.event.CategoryChanged;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductCreated;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductDiscontinued;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPriceChanged;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPublished;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductUpdated;
import org.phuchoang.ecp.catalog.internal.domain.event.VariantAdded;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The anti-corruption boundary between Catalog's domain events and the versioned Kafka contract
 * (`04-shared/Event Contract/catalog/*.v1.json`): one method per event, each producing an explicit
 * payload record. A v2 contract is a new mapping here, not an edit to the aggregate.
 */
@Component
public class CatalogEventPayloadMapper {

    public Object payload(CatalogDomainEvent event, AffectedCategories affected) {
        return switch (event) {
            case ProductCreated created -> productChanged(created.product(), affected);
            case ProductUpdated updated -> productChanged(updated.product(), affected);
            case ProductPublished published -> productChanged(published.product(), affected);
            case VariantAdded added -> new VariantAddedPayload(added.productId(), List.of(added.variant().id()),
                List.of(added.variant().sku()), affected.ids(), affected.slugs(), added.variant().id(),
                MoneyPayload.of(added.variant()));
            case ProductPriceChanged changed -> new ProductPriceChangedPayload(changed.productId(),
                List.of(changed.variant().id()), List.of(changed.variant().sku()), changed.variant().id(),
                MoneyPayload.of(changed.variant()));
            case ProductDiscontinued discontinued -> new ProductDiscontinuedPayload(discontinued.productId(),
                discontinued.variantIds(), discontinued.variantSkus());
            case CategoryChanged changed -> changed.removed()
                ? categoryRemoved(changed.category())
                : categoryChanged(changed.category(), affected);
        };
    }

    private static ProductChangedPayload productChanged(Product product, AffectedCategories affected) {
        return new ProductChangedPayload(product.id(), product.variantIds(), product.variantSkus(), affected.ids(),
            affected.slugs(), ProductPayload.of(product));
    }

    private static CategoryChangedPayload categoryChanged(Category category, AffectedCategories affected) {
        return new CategoryChangedPayload(category.id(), category.parentId(), category.name(), category.slug(),
            category.path(), category.depth(), category.sortOrder(), affected.ids(), affected.slugs());
    }

    /** The subtree is gone with the category, so only the category itself remains to be named. */
    private static CategoryRemovedPayload categoryRemoved(Category category) {
        return new CategoryRemovedPayload(category.id(), List.of(category.id()), List.of(category.slug()));
    }
}
