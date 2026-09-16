package org.phuchoang.ecp.identity.api.request;

/** `components/schemas/identity.yaml#/CustomerAddressWrite`. */
public record AddressWriteRequest(String label, String recipientName, String line1, String line2, String city,
        String region, String postalCode, String countryCode, String phone, Boolean isDefaultShipping,
        Boolean isDefaultBilling) {
}
