package org.phuchoang.ecp.inventory.internal.application.management.command.adjust;

import org.phuchoang.ecp.audit.api.AuditRecord;
import org.phuchoang.ecp.audit.api.AuditTrail;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.internal.domain.model.ReservationStatus;
import org.phuchoang.ecp.inventory.internal.domain.model.StockAdjustment;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.service.AdjustStockDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** One retryable, all-or-nothing adjustment transaction. */
@Service
class AdjustStockAttempt {
    private final AdjustStockDomainService stock;
    private final AuditTrail audit;
    private final AdjustStockMapper mapper;

    AdjustStockAttempt(AdjustStockDomainService stock, AuditTrail audit, AdjustStockMapper mapper) {
        this.stock = stock; this.audit = audit; this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    AdjustStockResult apply(AdjustStockCommand command) {
        AdjustStockDomainService.Adjustment adjustment;
        try {
            adjustment = stock.adjust(command.stockItemId(), StockAdjustment.proposed(command.delta(), command.reasonCode(),
                command.reason(), command.caller().accountId()));
        } catch (AdjustStockDomainService.AdjustmentDeclinedException exception) {
            StockItem item = exception.stockItem();
            throw new StockAdjustmentDeclinedException(item.quantityReserved(), item.reservations().stream()
                .filter(reservation -> reservation.status() == ReservationStatus.HELD)
                .map(reservation -> reservation.orderId()).distinct().toList());
        }
        StockItem before = adjustment.before();
        StockItem after = adjustment.after();
        StockAdjustment stockAdjustment = adjustment.stockAdjustment();
        audit.record(new AuditRecord(stockAdjustment.id(), command.caller().accountId(), role(command.caller()), "adjustStock", "stockItem", after.id(),
            "inventory", quantities(before), quantities(after), command.reason(), command.correlationId(), stockAdjustment.occurredAt(), null, false));
        return mapper.result(after);
    }

    private static Map<String, Object> quantities(StockItem item) {
        return Map.of("sku", item.sku(), "warehouseId", item.warehouseId().toString(), "onHand", item.quantityOnHand(),
            "reserved", item.quantityReserved(), "available", item.availableQuantity());
    }
    private static String role(IdentityActor actor) { return actor.roles().stream().sorted().findFirst().orElse(null); }
}
