package org.phuchoang.ecp.identity.internal.application.address;

/** A caller-facing snapshot of a {@link CustomerAddress} — plain data only, mirrors {@code AccountSummary}. */
public record AddressSummary(String id, String label, String recipientName, String line1, String line2,
        String city, String region, String postalCode, String countryCode, String phone,
        boolean isDefaultShipping, boolean isDefaultBilling) { }
