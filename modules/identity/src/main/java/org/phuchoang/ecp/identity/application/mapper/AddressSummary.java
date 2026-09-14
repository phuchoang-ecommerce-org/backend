package org.phuchoang.ecp.identity.application.mapper;

import org.phuchoang.ecp.identity.domain.CustomerAddress;

/** A caller-facing snapshot of a {@link CustomerAddress} — plain data only, mirrors {@link AccountSummary}. */
public record AddressSummary(String id, String label, String recipientName, String line1, String line2,
        String city, String region, String postalCode, String countryCode, String phone,
        boolean isDefaultShipping, boolean isDefaultBilling) {

    public static AddressSummary of(CustomerAddress address) {
        var a = address.address();
        return new AddressSummary(address.id().toString(), a.label(), a.recipientName(), a.line1(), a.line2(),
            a.city(), a.region(), a.postalCode(), a.countryCode(), a.phone(), address.defaultShipping(),
            address.defaultBilling());
    }
}
