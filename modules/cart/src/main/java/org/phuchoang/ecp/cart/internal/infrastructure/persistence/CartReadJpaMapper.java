package org.phuchoang.ecp.cart.internal.infrastructure.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.cart.internal.application.cart.query.CartReadQuery;

/** Maps JPA query results directly to Cart's application-owned read model. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CartReadJpaMapper {

    CartReadQuery.CartReadModel toReadModel(CartEntity entity);

    CartReadQuery.CartLineReadModel toReadModel(CartLineEntity entity);
}
