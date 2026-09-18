package org.phuchoang.ecp.catalog.internal.application.command.variant;

import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogLookups;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.VariantAdded;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.repository.DuplicateSkuException;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * `UC-ADM-01` — adds a SKU-bearing variant. Catalog-wide SKU uniqueness (`BR-CAT-01`) is decided by
 * the database and surfaced by the repository as {@link DuplicateSkuException}, which becomes the
 * caller's validation failure here.
 */
@Service
public class AddVariantService {

    private final ProductRepository products;
    private final IdentityAuthorization authorization;
    private final CatalogEventPublisher events;

    public AddVariantService(ProductRepository products, IdentityAuthorization authorization,
            CatalogEventPublisher events) {
        this.products = products;
        this.authorization = authorization;
        this.events = events;
    }

    @Transactional
    public VariantSnapshot add(CatalogCommandContext context, UUID productId, AddVariant command) {
        authorization.assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);

        Product before = CatalogLookups.requireProduct(products, productId);
        Product.Variant variant = new Product.Variant(UUID.randomUUID(), command.sku(), command.name(),
            command.amount(), command.currency(), command.options(), command.weightGrams(), command.active());

        Product after;
        try {
            after = products.save(before.addVariant(variant));
        } catch (DuplicateSkuException exception) {
            throw new DomainException(GenErrorCode.VALIDATION_FAILED, exception.getMessage());
        }

        Product.Variant added = after.variant(variant.id());
        events.publish(new VariantAdded(productId, after.categoryId(), added), context);
        return VariantSnapshot.from(added);
    }
}
