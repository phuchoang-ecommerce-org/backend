package org.phuchoang.ecp.identity.internal.application.address;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.internal.domain.model.CustomerAddress;

/** Maps address-book domain state into the application-owned address projection. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface AddressSummaryMapper {

    @Mapping(target = "id", expression = "java(source.id().toString())")
    @Mapping(target = "label", expression = "java(source.address().label())")
    @Mapping(target = "recipientName", expression = "java(source.address().recipientName())")
    @Mapping(target = "line1", expression = "java(source.address().line1())")
    @Mapping(target = "line2", expression = "java(source.address().line2())")
    @Mapping(target = "city", expression = "java(source.address().city())")
    @Mapping(target = "region", expression = "java(source.address().region())")
    @Mapping(target = "postalCode", expression = "java(source.address().postalCode())")
    @Mapping(target = "countryCode", expression = "java(source.address().countryCode())")
    @Mapping(target = "phone", expression = "java(source.address().phone())")
    @Mapping(target = "isDefaultShipping", expression = "java(source.defaultShipping())")
    @Mapping(target = "isDefaultBilling", expression = "java(source.defaultBilling())")
    AddressSummary toSummary(CustomerAddress source);
}
