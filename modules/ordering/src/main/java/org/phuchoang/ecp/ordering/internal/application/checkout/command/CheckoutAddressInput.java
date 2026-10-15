package org.phuchoang.ecp.ordering.internal.application.checkout.command;

/** Command-owned address input; it is translated to the aggregate's snapshot at the command boundary. */
public record CheckoutAddressInput(String label, String recipientName, String line1, String line2, String city,
        String region, String postalCode, String countryCode, String phone) { }
