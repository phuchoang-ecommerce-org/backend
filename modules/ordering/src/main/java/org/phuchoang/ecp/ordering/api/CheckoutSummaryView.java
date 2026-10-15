package org.phuchoang.ecp.ordering.api;

import java.util.List;

/** Public order-summary contract; confirmationToken is state consumed by Sprint 18 placement. */
public record CheckoutSummaryView(CheckoutView.OrderView order, boolean changed, List<Change> changes,
        String confirmationToken) {
    public CheckoutSummaryView { changes = List.copyOf(changes); }
    public record Change(String field, String previous, String current) { }
}
