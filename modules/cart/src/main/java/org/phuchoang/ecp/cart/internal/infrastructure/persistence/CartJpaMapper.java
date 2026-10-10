package org.phuchoang.ecp.cart.internal.infrastructure.persistence;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.cart.internal.domain.model.Cart;
import org.phuchoang.ecp.cart.internal.domain.model.CartLine;

import java.time.Instant;
import java.util.List;

/** Maps Cart's framework-free aggregate to JPA-only persistence state. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CartJpaMapper {

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CartEntity toEntity(Cart cart, @Context Instant now);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "guestToken", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lines", ignore = true)
    void updateEntity(Cart cart, @MappingTarget CartEntity entity);

    @Mapping(target = "mergeInto", ignore = true)
    Cart toDomain(CartEntity entity);

    @Mapping(target = "cart", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CartLineEntity toEntity(CartLine line, @Context Instant now);

    List<CartLineEntity> toEntities(List<CartLine> lines, @Context Instant now);

    CartLine toDomain(CartLineEntity entity);

    @AfterMapping
    default void initializeCartEntity(Cart cart, @MappingTarget CartEntity entity, @Context Instant now) {
        entity.initializeAuditTimestamps(now);
        entity.attachLines();
    }

    @AfterMapping
    default void initializeCartLineEntity(CartLine line, @MappingTarget CartLineEntity entity, @Context Instant now) {
        entity.initializeAuditTimestamps(now);
    }
}
