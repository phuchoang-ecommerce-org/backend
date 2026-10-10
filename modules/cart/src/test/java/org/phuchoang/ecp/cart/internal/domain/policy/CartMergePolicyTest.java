package org.phuchoang.ecp.cart.internal.domain.policy;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.cart.internal.domain.model.CartLine;
import org.phuchoang.ecp.cart.internal.domain.model.CartMergeNotice;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CartMergePolicyTest {
    private final CartMergePolicy policy = new CartMergePolicy();

    @Test
    void combinesLinesCapsPositiveStockRetainsZeroStockAndReportsUnpublishedLines() {
        UUID combined = UUID.randomUUID();
        UUID zeroStock = UUID.randomUUID();
        UUID unpublished = UUID.randomUUID();
        Instant now = Instant.parse("2026-10-09T00:00:00Z");
        CartMergePolicy.MergePlan result = policy.merge(
            List.of(line(combined, "COMBINED", 2, now)),
            List.of(line(combined, "COMBINED", 4, now), line(zeroStock, "ZERO", 1, now), line(unpublished, "OLD", 1, now)),
            Map.of(combined, new CartMergePolicy.VariantAvailability(true, 3, "Combined"),
                zeroStock, new CartMergePolicy.VariantAvailability(true, 0, "Zero")));

        assertThat(result.lines()).extracting(CartLine::variantId, CartLine::quantity)
            .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple(combined, 3), org.assertj.core.groups.Tuple.tuple(zeroStock, 1));
        assertThat(result.notices()).extracting(CartMergeNotice::reason)
            .containsExactlyInAnyOrder(CartMergeNotice.Reason.QUANTITY_REDUCED_TO_AVAILABLE,
                CartMergeNotice.Reason.RETAINED_OUT_OF_STOCK, CartMergeNotice.Reason.DROPPED_UNPUBLISHED);
    }

    private static CartLine line(UUID variantId, String sku, int quantity, Instant now) {
        return new CartLine(UUID.randomUUID(), variantId, sku, quantity, now);
    }
}
