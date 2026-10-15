package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.ordering.internal.application.checkout.current.query.CheckoutReadView;
import org.phuchoang.ecp.ordering.internal.application.checkout.shipping.query.ShippingQuoteContext;

import java.math.BigDecimal;

/** Maps JPA rows directly to observational application read DTOs. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface CheckoutReadJpaMapper {

    @Mapping(target = "subtotal", expression = "java(money(entity.getSubtotalAmount(), entity.getCurrency()))")
    @Mapping(target = "discount", expression = "java(money(entity.getDiscountAmount(), entity.getCurrency()))")
    @Mapping(target = "shippingFee", expression = "java(money(entity.getShippingFeeAmount(), entity.getCurrency()))")
    @Mapping(target = "total", expression = "java(money(entity.getTotalAmount(), entity.getCurrency()))")
    @Mapping(target = "shippingAddress", expression = "java(address(entity.getShippingRecipientName(), entity.getShippingLine1(), entity.getShippingLine2(), entity.getShippingCity(), entity.getShippingRegion(), entity.getShippingPostalCode(), entity.getShippingCountryCode()))")
    @Mapping(target = "billingAddress", expression = "java(address(entity.getBillingRecipientName(), entity.getBillingLine1(), entity.getBillingLine2(), entity.getBillingCity(), entity.getBillingRegion(), entity.getBillingPostalCode(), entity.getBillingCountryCode()))")
    @Mapping(target = "shippingOption", expression = "java(shippingOption(entity))")
    @Mapping(target = "ready", expression = "java(entity.getShippingRecipientName() != null && entity.getBillingRecipientName() != null && entity.getShippingOptionId() != null)")
    CheckoutReadView toReadView(CheckoutEntity entity);

    @Mapping(target = "unitPrice", expression = "java(money(entity.getUnitPriceAmount(), entity.getUnitPriceCurrency()))")
    @Mapping(target = "lineTotal", expression = "java(money(entity.getLineTotalAmount(), entity.getUnitPriceCurrency()))")
    CheckoutReadView.Line toLine(CheckoutLineEntity entity);

    default ShippingQuoteContext toShippingQuoteContext(CheckoutEntity entity) {
        return new ShippingQuoteContext(entity.getCurrency(), entity.getShippingRecipientName() != null);
    }

    default CheckoutReadView.Money money(BigDecimal amount, String currency) {
        return new CheckoutReadView.Money(amount, currency);
    }

    default CheckoutReadView.Address address(String recipientName, String line1, String line2, String city, String region,
            String postalCode, String countryCode) {
        return recipientName == null ? null : new CheckoutReadView.Address(null, recipientName, line1, line2, city, region, postalCode,
            countryCode, null);
    }

    default CheckoutReadView.ShippingOption shippingOption(CheckoutEntity entity) {
        return entity.getShippingOptionId() == null ? null : new CheckoutReadView.ShippingOption(entity.getShippingOptionId(),
            entity.getShippingCarrier(), entity.getShippingService(), money(entity.getShippingFeeAmount(), entity.getCurrency()),
            entity.getEstimatedDeliveryFrom(), entity.getEstimatedDeliveryTo());
    }
}
