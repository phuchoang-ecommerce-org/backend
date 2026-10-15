package org.phuchoang.ecp.ordering.api;

/** Public address shape at the Ordering boundary; it is not Identity's address-book value object. */
public record CheckoutAddressRequest(String label, String recipientName, String line1, String line2, String city, String region,
        String postalCode, String countryCode, String phone) { }
