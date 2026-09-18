package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.product;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.model.PublicationStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/** Maps the Product aggregate and its owned children to persistence-only JPA shapes. */
@Component
class ProductJpaMapper {
    private static final TypeReference<Map<String, Object>> OBJECTS = new TypeReference<>() { };
    private static final TypeReference<Map<String, String>> STRINGS = new TypeReference<>() { };

    private final ObjectMapper json;

    ProductJpaMapper(ObjectMapper json) {
        this.json = json;
    }

    CatalogProductEntity create(Product product, Instant now) {
        CatalogProductEntity entity = new CatalogProductEntity(product.id(), product.categoryId(), product.name(), product.slug(),
            product.description(), product.brand(), json(product.attributes()), now);
        product.variants().forEach(variant -> entity.addVariant(variantEntity(variant, now)));
        product.images().forEach(image -> entity.addImage(imageEntity(image, now)));
        return entity;
    }

    Product toDomain(CatalogProductEntity entity) {
        return new Product(entity.id(), entity.categoryId(), entity.name(), entity.slug(), entity.description(), entity.brand(),
            PublicationStatus.from(entity.publicationStatus()), entity.publishedAt(), objects(entity.attributes()),
            entity.variants().stream().map(this::toDomain).toList(), entity.images().stream().map(this::toDomain).toList());
    }

    CatalogVariantEntity variantEntity(Product.Variant variant, Instant now) {
        return new CatalogVariantEntity(variant.id(), variant.sku(), variant.name(), variant.amount(), variant.currency(),
            json(variant.options()), variant.weightGrams(), variant.active(), now);
    }

    CatalogProductImageEntity imageEntity(Product.Image image, Instant now) {
        return new CatalogProductImageEntity(image.id(), image.url(), image.altText(), image.sortOrder(), now);
    }

    private Product.Variant toDomain(CatalogVariantEntity entity) {
        return new Product.Variant(entity.id(), entity.sku(), entity.name(), entity.amount(), entity.currency(),
            strings(entity.options()), entity.weightGrams(), entity.active());
    }

    private Product.Image toDomain(CatalogProductImageEntity entity) {
        return new Product.Image(entity.id(), entity.url(), entity.altText(), entity.sortOrder());
    }

    String json(Object value) {
        try {
            return json.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid catalog JSON", exception);
        }
    }

    private Map<String, Object> objects(String value) {
        try {
            return json.readValue(value, OBJECTS);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid stored catalog attributes", exception);
        }
    }

    private Map<String, String> strings(String value) {
        try {
            return json.readValue(value, STRINGS);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid stored catalog options", exception);
        }
    }
}
