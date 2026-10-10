package org.phuchoang.ecp.cart.internal.application.cart;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.cart.internal.application.cart.command.CartMergeNotice;
import org.phuchoang.ecp.cart.internal.application.cart.command.CartMergeResult;
import org.phuchoang.ecp.cart.internal.application.cart.query.CartReadQuery;
import org.phuchoang.ecp.cart.internal.domain.model.Cart;
import org.phuchoang.ecp.cart.internal.domain.service.CartCommandService;

/** Maps Cart domain results to application-owned command and query representations. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CartApplicationMapper {

    CartReadQuery.CartReadModel readModel(Cart cart);

    CartMergeResult mergeResult(CartCommandService.MergeResult result);

    CartMergeNotice mergeNotice(org.phuchoang.ecp.cart.internal.domain.model.CartMergeNotice notice);
}
