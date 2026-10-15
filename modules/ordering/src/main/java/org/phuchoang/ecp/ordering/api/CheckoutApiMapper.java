package org.phuchoang.ecp.ordering.api;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutAddressInput;
import org.phuchoang.ecp.ordering.internal.application.checkout.command.CheckoutCommandResult;
import org.phuchoang.ecp.ordering.internal.application.checkout.current.query.CheckoutReadView;
import org.phuchoang.ecp.ordering.internal.application.checkout.review.command.ReviewCheckoutSummaryService;
import org.phuchoang.ecp.ordering.internal.application.checkout.shipping.query.ShippingQuoteResult;

import java.util.List;

/** MapStruct boundary mapping keeps API records out of focused application use cases. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CheckoutApiMapper {
    CheckoutAddressInput command(CheckoutAddressRequest request);

    @Mapping(target = "order", source = "checkout")
    @Mapping(target = "ready", source = "checkout.ready")
    @Mapping(target = "selectedShippingOptionId", expression = "java(result.checkout().shippingOption() == null ? null : result.checkout().shippingOption().id())")
    @Mapping(target = "blockers", expression = "java(List.of())")
    CheckoutView checkoutView(CheckoutCommandResult result);

    @Mapping(target = "unitPriceAtOrder", source = "unitPrice")
    CheckoutView.LineView lineView(CheckoutCommandResult.CheckoutLineData value);
    CheckoutView.MoneyView moneyView(CheckoutCommandResult.MoneyData value);
    CheckoutView.AddressView addressView(CheckoutCommandResult.CheckoutAddressData value);
    CheckoutView.ShippingOptionView optionView(CheckoutCommandResult.ShippingOptionData value);
    CheckoutView.OrderView orderView(CheckoutCommandResult.CheckoutData value);

    @Mapping(target = "order", source = ".")
    @Mapping(target = "shippingOptions", expression = "java(List.of())")
    @Mapping(target = "selectedShippingOptionId", expression = "java(value.shippingOption() == null ? null : value.shippingOption().id())")
    @Mapping(target = "blockers", expression = "java(List.of())")
    CheckoutView checkoutView(CheckoutReadView value);
    @Mapping(target = "unitPriceAtOrder", source = "unitPrice")
    CheckoutView.LineView lineView(CheckoutReadView.Line value);
    CheckoutView.MoneyView moneyView(CheckoutReadView.Money value);
    CheckoutView.AddressView addressView(CheckoutReadView.Address value);
    CheckoutView.ShippingOptionView optionView(CheckoutReadView.ShippingOption value);
    CheckoutView.OrderView orderView(CheckoutReadView value);

    @Mapping(target = "options", source = "options")
    ShippingQuoteView shippingQuoteView(ShippingQuoteResult value);
    @Mapping(target = "fee", expression = "java(new CheckoutView.MoneyView(value.feeAmount(), value.currency()))")
    CheckoutView.ShippingOptionView optionView(ShippingQuoteResult.Option value);

    @Mapping(target = "order", source = "checkout.checkout")
    CheckoutSummaryView summaryView(ReviewCheckoutSummaryService.CheckoutSummaryResult value);
}
