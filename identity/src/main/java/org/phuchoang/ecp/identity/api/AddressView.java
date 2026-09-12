package org.phuchoang.ecp.identity.api;

/** `components/schemas/identity.yaml#/CustomerAddress`. */
public record AddressView(String id, String label, String recipientName, String line1, String line2, String city,
        String region, String postalCode, String countryCode, String phone, boolean isDefaultShipping,
        boolean isDefaultBilling) {
}
