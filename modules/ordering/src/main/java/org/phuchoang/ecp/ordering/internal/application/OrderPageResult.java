package org.phuchoang.ecp.ordering.internal.application;

import java.util.List;

/**
 * Sprint 05 (`US-CUS-10`): {@code ordering} has no read model yet, so {@code items} is always
 * empty and {@code nextCursor} always {@code null} until Sprint 18 populates one. The envelope
 * shape is real — that is what `G2`'s check #8 verifies, not that rows come back.
 */
public record OrderPageResult(List<OrderSummary> items, String nextCursor) {

    public static final OrderPageResult EMPTY = new OrderPageResult(List.of(), null);
}
