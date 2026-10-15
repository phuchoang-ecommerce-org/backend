package org.phuchoang.ecp.ordering.api;

/** Result of initiating checkout; the API uses resumed to select 200 or 201. */
public record CheckoutOutcome(CheckoutView checkout, boolean resumed) { }
