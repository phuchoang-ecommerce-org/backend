package org.phuchoang.ecp.cart.internal.domain.policy;

import org.phuchoang.ecp.cart.internal.domain.model.CartLine;
import org.phuchoang.ecp.cart.internal.domain.model.CartMergeNotice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure BR-CRT-03 policy. It decides the merged lines and every customer-visible adjustment;
 * catalog facts are supplied by the application boundary and aggregate persistence is handled
 * by the domain command service.
 */
public final class CartMergePolicy {

    public MergePlan merge(List<CartLine> customerLines, List<CartLine> guestLines,
            Map<UUID, VariantAvailability> availability) {
        Map<UUID, CartLine> merged = new LinkedHashMap<>();
        customerLines.forEach(line -> merged.put(line.variantId(), line));
        List<CartMergeNotice> notices = new ArrayList<>();

        for (CartLine guestLine : guestLines) {
            Optional<VariantAvailability> fact = Optional.ofNullable(availability.get(guestLine.variantId()));
            if (fact.isEmpty() || !fact.get().published()) {
                notices.add(new CartMergeNotice(guestLine.variantId(), guestLine.sku(),
                    fact.map(VariantAvailability::productName).orElse(null),
                    CartMergeNotice.Reason.DROPPED_UNPUBLISHED, null));
                continue;
            }

            CartLine existing = merged.get(guestLine.variantId());
            int requested = Math.addExact(existing == null ? 0 : existing.quantity(), guestLine.quantity());
            VariantAvailability variant = fact.get();
            if (variant.availableQuantity() != null && variant.availableQuantity() == 0) {
                // E2: a zero-stock line remains visible rather than disappearing during merge.
                merged.put(guestLine.variantId(), withQuantity(existing, guestLine, requested));
                notices.add(new CartMergeNotice(guestLine.variantId(), guestLine.sku(), variant.productName(),
                    CartMergeNotice.Reason.RETAINED_OUT_OF_STOCK, requested));
            } else if (variant.availableQuantity() != null && requested > variant.availableQuantity()) {
                merged.put(guestLine.variantId(), withQuantity(existing, guestLine, variant.availableQuantity()));
                notices.add(new CartMergeNotice(guestLine.variantId(), guestLine.sku(), variant.productName(),
                    CartMergeNotice.Reason.QUANTITY_REDUCED_TO_AVAILABLE, variant.availableQuantity()));
            } else {
                merged.put(guestLine.variantId(), withQuantity(existing, guestLine, requested));
            }
        }
        return new MergePlan(List.copyOf(merged.values()), List.copyOf(notices));
    }

    private static CartLine withQuantity(CartLine existing, CartLine incoming, int quantity) {
        CartLine source = existing == null ? incoming : existing;
        return new CartLine(source.id(), source.variantId(), source.sku(), quantity, source.addedAt());
    }

    public record VariantAvailability(boolean published, Integer availableQuantity, String productName) { }
    public record MergePlan(List<CartLine> lines, List<CartMergeNotice> notices) { }
}
