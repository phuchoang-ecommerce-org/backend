package org.phuchoang.ecp.ordering.api;

import java.util.List;

/** Provisional Sprint-17 contract-shaped quote; Shipping owns real computation from Sprint 20. */
public record ShippingQuoteView(List<CheckoutView.ShippingOptionView> options, int shipmentCount) {
    public ShippingQuoteView { options = List.copyOf(options); }
}
