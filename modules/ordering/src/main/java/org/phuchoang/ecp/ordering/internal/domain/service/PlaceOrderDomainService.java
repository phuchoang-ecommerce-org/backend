package org.phuchoang.ecp.ordering.internal.domain.service;

import org.phuchoang.ecp.ordering.internal.domain.model.Checkout;
import org.phuchoang.ecp.ordering.internal.domain.model.Money;
import org.phuchoang.ecp.ordering.internal.domain.model.Order;
import org.phuchoang.ecp.ordering.internal.domain.model.OrderReservation;
import org.phuchoang.ecp.ordering.internal.domain.repository.OrderRepository;

import java.util.List;

/** Domain coordinator that persists the completed Order aggregate only after its transition succeeds. */
public final class PlaceOrderDomainService {
    private final OrderRepository orders;

    public PlaceOrderDomainService(OrderRepository orders) { this.orders = orders; }

    public Order place(Checkout checkout, List<OrderReservation> reservations, Money bindingDiscount) {
        return Order.place(checkout, reservations, bindingDiscount).save(orders);
    }
}
