package org.phuchoang.ecp.inventory.internal.domain.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StockAllocationPolicyTest {
    private final StockAllocationPolicy policy = new StockAllocationPolicy();

    @Test
    void returnsNoPartialAllocationWhenAnyLineIsShort() {
        UUID line = UUID.randomUUID();
        var plan = policy.allocate(new StockAllocationPolicy.AllocationRequest(List.of(new StockAllocationPolicy.RequestedLine(line, "SKU-1", 3))),
            new StockAllocationPolicy.StockAvailabilitySnapshot(List.of(availability("SKU-1", 2))),
            StockAllocationPolicy.mostStockFirst());
        assertThat(plan.status()).isEqualTo(StockAllocationPolicy.Status.INSUFFICIENT_STOCK);
        assertThat(plan.allocations()).isEmpty();
        assertThat(plan.shortages()).singleElement().extracting(StockAllocationPolicy.Shortage::availableQuantity).isEqualTo(2);
    }

    @Test
    void requiresAnExplicitWarehouseSelectionStrategy() {
        var plan = policy.allocate(new StockAllocationPolicy.AllocationRequest(List.of(new StockAllocationPolicy.RequestedLine(UUID.randomUUID(), "SKU-1", 1))),
            new StockAllocationPolicy.StockAvailabilitySnapshot(List.of(availability("SKU-1", 1))), null);
        assertThat(plan.status()).isEqualTo(StockAllocationPolicy.Status.ALLOCATION_POLICY_REQUIRED);
    }

    private static StockAllocationPolicy.Availability availability(String sku, int quantity) {
        return new StockAllocationPolicy.Availability(UUID.randomUUID(), UUID.randomUUID(), sku, quantity);
    }
}
