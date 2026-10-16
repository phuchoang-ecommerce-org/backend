package org.phuchoang.ecp.ordering.internal.infrastructure.persistence;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutAddress;
import org.phuchoang.ecp.ordering.internal.domain.model.CheckoutLine;
import org.phuchoang.ecp.ordering.internal.domain.model.Money;
import org.phuchoang.ecp.ordering.internal.domain.model.ShippingOption;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Maps the complete Checkout aggregate to the JPA write representation and back. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, imports = BigDecimal.class)
interface CheckoutJpaMapper {

    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "currency", expression = "java(checkout.subtotal().currency())")
    @Mapping(target = "subtotalAmount", expression = "java(checkout.subtotal().amount())")
    @Mapping(target = "discountAmount", expression = "java(checkout.discount().amount())")
    @Mapping(target = "shippingFeeAmount", expression = "java(checkout.shippingFee().amount())")
    @Mapping(target = "taxAmount", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "totalAmount", expression = "java(checkout.total().amount())")
    @Mapping(target = "shippingRecipientName", source = "shippingAddress.recipientName")
    @Mapping(target = "shippingLine1", source = "shippingAddress.line1")
    @Mapping(target = "shippingLine2", source = "shippingAddress.line2")
    @Mapping(target = "shippingCity", source = "shippingAddress.city")
    @Mapping(target = "shippingRegion", source = "shippingAddress.region")
    @Mapping(target = "shippingPostalCode", source = "shippingAddress.postalCode")
    @Mapping(target = "shippingCountryCode", source = "shippingAddress.countryCode")
    @Mapping(target = "billingRecipientName", source = "billingAddress.recipientName")
    @Mapping(target = "billingLine1", source = "billingAddress.line1")
    @Mapping(target = "billingLine2", source = "billingAddress.line2")
    @Mapping(target = "billingCity", source = "billingAddress.city")
    @Mapping(target = "billingRegion", source = "billingAddress.region")
    @Mapping(target = "billingPostalCode", source = "billingAddress.postalCode")
    @Mapping(target = "billingCountryCode", source = "billingAddress.countryCode")
    @Mapping(target = "shippingOptionId", source = "shippingOption.id")
    @Mapping(target = "shippingCarrier", source = "shippingOption.carrier")
    @Mapping(target = "shippingService", source = "shippingOption.serviceName")
    @Mapping(target = "estimatedDeliveryFrom", source = "shippingOption.estimatedDeliveryFrom")
    @Mapping(target = "estimatedDeliveryTo", source = "shippingOption.estimatedDeliveryTo")
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CheckoutEntity toEntity(Checkout checkout, @Context Instant persistedAt);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "currency", expression = "java(checkout.subtotal().currency())")
    @Mapping(target = "subtotalAmount", expression = "java(checkout.subtotal().amount())")
    @Mapping(target = "discountAmount", expression = "java(checkout.discount().amount())")
    @Mapping(target = "shippingFeeAmount", expression = "java(checkout.shippingFee().amount())")
    @Mapping(target = "taxAmount", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "totalAmount", expression = "java(checkout.total().amount())")
    @Mapping(target = "shippingRecipientName", source = "shippingAddress.recipientName")
    @Mapping(target = "shippingLine1", source = "shippingAddress.line1")
    @Mapping(target = "shippingLine2", source = "shippingAddress.line2")
    @Mapping(target = "shippingCity", source = "shippingAddress.city")
    @Mapping(target = "shippingRegion", source = "shippingAddress.region")
    @Mapping(target = "shippingPostalCode", source = "shippingAddress.postalCode")
    @Mapping(target = "shippingCountryCode", source = "shippingAddress.countryCode")
    @Mapping(target = "billingRecipientName", source = "billingAddress.recipientName")
    @Mapping(target = "billingLine1", source = "billingAddress.line1")
    @Mapping(target = "billingLine2", source = "billingAddress.line2")
    @Mapping(target = "billingCity", source = "billingAddress.city")
    @Mapping(target = "billingRegion", source = "billingAddress.region")
    @Mapping(target = "billingPostalCode", source = "billingAddress.postalCode")
    @Mapping(target = "billingCountryCode", source = "billingAddress.countryCode")
    @Mapping(target = "shippingOptionId", source = "shippingOption.id")
    @Mapping(target = "shippingCarrier", source = "shippingOption.carrier")
    @Mapping(target = "shippingService", source = "shippingOption.serviceName")
    @Mapping(target = "estimatedDeliveryFrom", source = "shippingOption.estimatedDeliveryFrom")
    @Mapping(target = "estimatedDeliveryTo", source = "shippingOption.estimatedDeliveryTo")
    void updateEntity(Checkout checkout, @MappingTarget CheckoutEntity entity, @Context Instant persistedAt);

    @Mapping(target = "shippingAddress", expression = "java(address(entity.getShippingRecipientName(), entity.getShippingLine1(), entity.getShippingLine2(), entity.getShippingCity(), entity.getShippingRegion(), entity.getShippingPostalCode(), entity.getShippingCountryCode()))")
    @Mapping(target = "billingAddress", expression = "java(address(entity.getBillingRecipientName(), entity.getBillingLine1(), entity.getBillingLine2(), entity.getBillingCity(), entity.getBillingRegion(), entity.getBillingPostalCode(), entity.getBillingCountryCode()))")
    @Mapping(target = "shippingOption", expression = "java(shippingOption(entity))")
    @Mapping(target = "discount", expression = "java(new Money(entity.getDiscountAmount(), entity.getCurrency()))")
    @Mapping(target = "save", ignore = true)
    @Mapping(target = "selectShippingOption", ignore = true)
    @Mapping(target = "removeVoucher", ignore = true)
    Checkout toDomain(CheckoutEntity entity);

    @Mapping(target = "checkout", ignore = true)
    @Mapping(target = "unitPriceAmount", source = "unitPrice.amount")
    @Mapping(target = "unitPriceCurrency", source = "unitPrice.currency")
    @Mapping(target = "lineDiscountAmount", expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "lineTotalAmount", expression = "java(line.lineTotal().amount())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CheckoutLineEntity toEntity(CheckoutLine line, @Context Instant persistedAt);

    List<CheckoutLineEntity> toEntities(List<CheckoutLine> lines, @Context Instant persistedAt);

    @Mapping(target = "unitPrice", expression = "java(new Money(entity.getUnitPriceAmount(), entity.getUnitPriceCurrency()))")
    CheckoutLine toDomain(CheckoutLineEntity entity);

    @AfterMapping
    default void synchronizeAggregate(Checkout checkout, @MappingTarget CheckoutEntity entity, @Context Instant persistedAt) {
        if (entity.getCreatedAt() == null) entity.setCreatedAt(persistedAt);
        entity.setUpdatedAt(persistedAt);
        entity.replaceLines(toEntities(checkout.lines(), persistedAt));
    }

    @AfterMapping
    default void initializeLine(CheckoutLine line, @MappingTarget CheckoutLineEntity entity, @Context Instant persistedAt) {
        entity.setCreatedAt(persistedAt);
        entity.setUpdatedAt(persistedAt);
    }

    default CheckoutAddress address(String recipient, String line1, String line2, String city, String region, String postal,
            String country) {
        return recipient == null ? null : new CheckoutAddress(null, recipient, line1, line2, city, region, postal, country, null);
    }

    default ShippingOption shippingOption(CheckoutEntity entity) {
        return entity.getShippingOptionId() == null ? null : new ShippingOption(entity.getShippingOptionId(),
            entity.getShippingCarrier(), entity.getShippingService(), new Money(entity.getShippingFeeAmount(), entity.getCurrency()),
            entity.getEstimatedDeliveryFrom(), entity.getEstimatedDeliveryTo());
    }
}
