package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.write.category;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;

/** Maps the Category aggregate to the JPA persistence shape. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CategoryJpaMapper {

    CatalogCategoryEntity toEntity(Category category);

    @Mapping(target = "create", ignore = true)
    @Mapping(target = "domainEvents", ignore = true)
    Category toDomain(CatalogCategoryEntity entity);
}
