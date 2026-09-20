package org.phuchoang.ecp.identity.internal.infrastructure.persistence.address;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.internal.domain.model.CustomerAddress;
import org.phuchoang.ecp.sharedkernel.api.address.Address;

import java.time.Instant;
import java.util.UUID;

/** Maps address-book persistence state while leaving address validation in the shared value object. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface AddressJpaMapper {

    AddressEntity toEntity(AddressState source);

    default AddressEntity toEntity(CustomerAddress address, Instant createdAt) {
        AddressEntity entity = toEntity(AddressState.from(address, createdAt));
        entity.initializeAuditTimestamps(createdAt);
        return entity;
    }

    default CustomerAddress toDomain(AddressEntity entity) {
        Address address = new Address(entity.getLabel(), entity.getRecipientName(), entity.getLine1(), entity.getLine2(),
            entity.getCity(), entity.getRegion(), entity.getPostalCode(), entity.getCountryCode(), entity.getPhone());
        return CustomerAddress.reconstitute(entity.getId(), entity.getAccountId(), address,
            entity.isDefaultShipping(), entity.isDefaultBilling());
    }

    record AddressState(UUID id, UUID accountId, String label, String recipientName, String line1, String line2,
                        String city, String region, String postalCode, String countryCode, String phone,
                        boolean defaultShipping, boolean defaultBilling, Instant createdAt) {
        static AddressState from(CustomerAddress customerAddress, Instant createdAt) {
            Address address = customerAddress.address();
            return new AddressState(customerAddress.id(), customerAddress.accountId(), address.label(), address.recipientName(),
                address.line1(), address.line2(), address.city(), address.region(), address.postalCode(),
                address.countryCode(), address.phone(), customerAddress.defaultShipping(), customerAddress.defaultBilling(), createdAt);
        }
    }
}
