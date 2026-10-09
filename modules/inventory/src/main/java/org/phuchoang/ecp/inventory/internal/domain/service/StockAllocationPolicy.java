package org.phuchoang.ecp.inventory.internal.domain.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jmolecules.ddd.annotation.Service;

/**
 * Produces a complete warehouse allocation plan from supplied stock facts; it
 * performs no I/O.
 */
@Service
public final class StockAllocationPolicy {

  public StockAllocationPlan allocate(AllocationRequest request, StockAvailabilitySnapshot availability,
      AllocationStrategy strategy) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(availability, "availability");
    if (strategy == null)
      return StockAllocationPlan.policyRequired();
    Map<String, List<Availability>> bySku = availability.items().stream()
        .collect(Collectors.groupingBy(Availability::sku));
    List<Allocation> allocations = new ArrayList<>();
    List<Shortage> shortages = new ArrayList<>();
    for (RequestedLine line : request.lines()) {
      int remaining = line.quantity();
      for (Availability stock : strategy.order(bySku.getOrDefault(line.sku(), List.of()))) {
        int allocated = Math.min(remaining, stock.availableQuantity());
        if (allocated > 0) {
          allocations
              .add(new Allocation(line.orderLineId(), line.sku(), stock.stockItemId(), stock.warehouseId(), allocated));
          remaining -= allocated;
        }
        if (remaining == 0)
          break;
      }
      if (remaining > 0)
        shortages.add(new Shortage(line.orderLineId(), line.sku(), line.quantity(), line.quantity() - remaining));
    }
    return shortages.isEmpty() ? StockAllocationPlan.complete(allocations)
        : StockAllocationPlan.insufficient(shortages);
  }

  public interface AllocationStrategy {
    List<Availability> order(List<Availability> candidates);
  }

  public static AllocationStrategy mostStockFirst() {
    return candidates -> candidates.stream().sorted(
        Comparator.comparingInt(Availability::availableQuantity).reversed().thenComparing(Availability::stockItemId))
        .toList();
  }

  public record AllocationRequest(List<RequestedLine> lines) {
    public AllocationRequest {
      lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
      if (lines.isEmpty())
        throw new IllegalArgumentException("lines are required.");
    }
  }

  public record RequestedLine(UUID orderLineId, String sku, int quantity) {
    public RequestedLine {
      Objects.requireNonNull(orderLineId, "orderLineId");
      if (sku == null || sku.isBlank() || quantity <= 0)
        throw new IllegalArgumentException("valid sku and quantity are required.");
    }
  }

  public record StockAvailabilitySnapshot(List<Availability> items) {
    public StockAvailabilitySnapshot {
      items = List.copyOf(Objects.requireNonNull(items, "items"));
    }
  }

  public record Availability(UUID stockItemId, UUID warehouseId, String sku, int availableQuantity) {
    public Availability {
      Objects.requireNonNull(stockItemId, "stockItemId");
      Objects.requireNonNull(warehouseId, "warehouseId");
      if (sku == null || sku.isBlank() || availableQuantity < 0)
        throw new IllegalArgumentException("availability is invalid.");
    }
  }

  public record Allocation(UUID orderLineId, String sku, UUID stockItemId, UUID warehouseId, int quantity) {
  }

  public record Shortage(UUID orderLineId, String sku, int requestedQuantity, int availableQuantity) {
  }

  public record StockAllocationPlan(Status status, List<Allocation> allocations, List<Shortage> shortages) {
    static StockAllocationPlan complete(List<Allocation> allocations) {
      return new StockAllocationPlan(Status.COMPLETE, List.copyOf(allocations), List.of());
    }

    static StockAllocationPlan insufficient(List<Shortage> shortages) {
      return new StockAllocationPlan(Status.INSUFFICIENT_STOCK, List.of(), List.copyOf(shortages));
    }

    static StockAllocationPlan policyRequired() {
      return new StockAllocationPlan(Status.ALLOCATION_POLICY_REQUIRED, List.of(), List.of());
    }
  }

  public enum Status {
    COMPLETE, INSUFFICIENT_STOCK, ALLOCATION_POLICY_REQUIRED
  }
}
