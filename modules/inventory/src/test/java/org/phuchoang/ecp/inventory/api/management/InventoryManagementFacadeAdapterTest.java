package org.phuchoang.ecp.inventory.api.management;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.inventory.internal.application.management.command.adjust.AdjustStockCommand;
import org.phuchoang.ecp.inventory.internal.application.management.command.adjust.AdjustStockResult;
import org.phuchoang.ecp.inventory.internal.application.management.command.adjust.AdjustStockService;
import org.phuchoang.ecp.inventory.internal.application.management.query.InventoryManagementQueryService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryManagementFacadeAdapterTest {

    private final AdjustStockService commands = mock(AdjustStockService.class);
    private final InventoryManagementQueryService queries = mock(InventoryManagementQueryService.class);
    private final InventoryManagementFacadeAdapter facade = new InventoryManagementFacadeAdapter(commands, queries,
        Mappers.getMapper(InventoryManagementApiMapper.class));

    @Test
    void mapsThePublishedAdjustmentInputAndApplicationResultAtTheApiBoundary() {
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("WAREHOUSE_OPERATOR"));
        UUID correlationId = UUID.randomUUID();
        UUID stockItemId = UUID.randomUUID();
        StockAdjustmentWrite write = new StockAdjustmentWrite(4, "COUNT", "Cycle count");
        when(commands.adjust(any())).thenReturn(new AdjustStockResult(stockItemId, "SKU-1", UUID.randomUUID(), 9, 3, 6));

        StockItemView result = facade.adjustStock(caller, correlationId, stockItemId, write);

        assertThat(result.quantityOnHand()).isEqualTo(9);
        assertThat(result.quantityReserved()).isEqualTo(3);
        assertThat(result.availableQuantity()).isEqualTo(6);
        verify(commands).adjust(new AdjustStockCommand(caller, correlationId, stockItemId, 4, "COUNT", "Cycle count"));
    }

    @Test
    void mapsTheApplicationFailureBackToThePublishedErrorContract() {
        UUID orderId = UUID.randomUUID();
        when(commands.adjust(any())).thenThrow(
            new org.phuchoang.ecp.inventory.internal.application.management.command.adjust.StockAdjustmentDeclinedException(3,
                List.of(orderId)));

        assertThatThrownBy(() -> facade.adjustStock(new IdentityActor(UUID.randomUUID(), Set.of("WAREHOUSE_OPERATOR")),
            UUID.randomUUID(), UUID.randomUUID(), new StockAdjustmentWrite(-1, "COUNT", "Cycle count")))
            .isInstanceOf(StockAdjustmentDeclinedException.class)
            .satisfies(exception -> assertThat(((StockAdjustmentDeclinedException) exception).orderIds()).containsExactly(orderId));
    }
}
