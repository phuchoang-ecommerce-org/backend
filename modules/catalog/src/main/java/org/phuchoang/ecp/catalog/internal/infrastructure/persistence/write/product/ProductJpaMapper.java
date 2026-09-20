package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.product;

import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;

import java.time.Instant;

/** Maps the Product aggregate and its owned children to persistence-only JPA shapes. */
@Mapper(componentModel = "spring", uses = CatalogJsonMapper.class, injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
interface ProductJpaMapper {

    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "attributes", qualifiedByName = "objectsToJson")
    CatalogProductEntity toEntity(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "attributes", qualifiedByName = "objectsToJson")
    void updateEntity(Product product, @MappingTarget CatalogProductEntity entity);

    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "attributes", qualifiedByName = "jsonToObjects")
    Product toDomain(CatalogProductEntity entity);

    @Mapping(target = "options", qualifiedByName = "stringsToJson")
    CatalogVariantEntity toEntity(Product.Variant variant);

    @Mapping(target = "options", qualifiedByName = "jsonToStrings")
    Product.Variant toDomain(CatalogVariantEntity entity);

    CatalogProductImageEntity toEntity(Product.Image image);

    Product.Image toDomain(CatalogProductImageEntity entity);

    default CatalogProductEntity create(Product product, Instant now) {
        CatalogProductEntity entity = toEntity(product);
        entity.initializeAuditTimestamps(now);
        product.variants().forEach(variant -> entity.addVariant(variantEntity(variant, now)));
        product.images().forEach(image -> entity.addImage(imageEntity(image, now)));
        return entity;
    }

    default CatalogVariantEntity variantEntity(Product.Variant variant, Instant now) {
        CatalogVariantEntity entity = toEntity(variant);
        entity.initializeAuditTimestamps(now);
        return entity;
    }

    default CatalogProductImageEntity imageEntity(Product.Image image, Instant now) {
        CatalogProductImageEntity entity = toEntity(image);
        entity.initializeAuditTimestamps(now);
        return entity;
    }
}
