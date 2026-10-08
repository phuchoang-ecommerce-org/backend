package org.phuchoang.ecp.identity.api.request;

import jakarta.validation.constraints.NotBlank;

/** `components/schemas/identity.yaml#/CustomerAddressWrite`. */
public record AddressWriteRequest(String label, @NotBlank(message = "recipientName is required.") String recipientName,
        @NotBlank(message = "line1 is required.") String line1, String line2,
        @NotBlank(message = "city is required.") String city, String region,
        @NotBlank(message = "postalCode is required.") String postalCode,
        @NotBlank(message = "countryCode is required.") String countryCode, String phone, Boolean isDefaultShipping,
        Boolean isDefaultBilling) {
}
