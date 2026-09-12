package org.phuchoang.ecp.ordering.api;

import java.util.List;

/** `components/schemas/ordering.yaml#/OrderPage`. Sprint 05: always empty — see {@link OrderFacade}. */
public record OrderPageView(List<Object> items, String nextCursor) {

    public static final OrderPageView EMPTY = new OrderPageView(List.of(), null);
}
