package org.phuchoang.ecp.inventory.internal.application.management.command.adjust;

import org.phuchoang.ecp.identity.api.authorization.IdentityActor;

import java.util.Objects;
import java.util.UUID;

/** Application input for UC-INV-04, independent of the published module API. */
public record AdjustStockCommand(IdentityActor caller, UUID correlationId, UUID stockItemId, int delta,
        String reasonCode, String reason) {
    public AdjustStockCommand {
        Objects.requireNonNull(caller, "caller");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(stockItemId, "stockItemId");
        if (delta == 0 || reasonCode == null || reasonCode.isBlank() || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Inventory adjustment requires a non-zero delta and a reason.");
        }
    }
}
