package org.phuchoang.ecp.cart.api;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.cart.internal.application.cart.command.AddCartLineCommand;

/** Maps Cart's public consumer contract to and from its application use-case contract. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CartApiMapper {

    AddCartLineCommand addCartLineCommand(CartLineWrite write);

    CartView cartView(org.phuchoang.ecp.cart.internal.application.cart.query.CartView cart);

    CartLineView cartLineView(org.phuchoang.ecp.cart.internal.application.cart.query.CartLineView line);

    MoneyView moneyView(org.phuchoang.ecp.cart.internal.application.cart.query.MoneyView money);

    CartMergeResult mergeResult(org.phuchoang.ecp.cart.internal.application.cart.command.CartMergeResult result);

    CartMergeNotice mergeNotice(org.phuchoang.ecp.cart.internal.application.cart.command.CartMergeNotice notice);
}
