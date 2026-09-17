package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.product;

import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Reconciles aggregate-owned Product children with a managed JPA aggregate. */
@Component
class ProductChildSynchronizer {
    private final ProductJpaMapper mapper;
    private final Clock clock;

    ProductChildSynchronizer(ProductJpaMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    boolean synchronize(CatalogProductEntity entity, Product product) {
        Instant now = Instant.now(clock);
        Map<UUID, CatalogVariantEntity> existingVariants = new HashMap<>();
        entity.variants().forEach(variant -> existingVariants.put(variant.id(), variant));
        entity.variants().removeIf(variant -> product.variants().stream().noneMatch(desired -> desired.id().equals(variant.id())));
        boolean addedVariant = false;
        for (Product.Variant desired : product.variants()) {
            CatalogVariantEntity existing = existingVariants.get(desired.id());
            if (existing == null) {
                entity.addVariant(mapper.variantEntity(desired, now));
                addedVariant = true;
            } else if (existing.amount().compareTo(desired.amount()) != 0 || !existing.currency().equals(desired.currency())) {
                existing.changePrice(desired.amount(), desired.currency(), now);
            }
        }
        Map<UUID, CatalogProductImageEntity> existingImages = new HashMap<>();
        entity.images().forEach(image -> existingImages.put(image.id(), image));
        entity.images().removeIf(image -> product.images().stream().noneMatch(desired -> desired.id().equals(image.id())));
        product.images().stream().filter(image -> !existingImages.containsKey(image.id()))
            .forEach(image -> entity.addImage(mapper.imageEntity(image, now)));
        return addedVariant;
    }
}
