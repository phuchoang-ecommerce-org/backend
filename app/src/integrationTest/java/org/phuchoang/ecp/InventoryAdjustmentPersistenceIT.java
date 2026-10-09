package org.phuchoang.ecp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.inventory.internal.domain.model.StockAdjustment;
import org.phuchoang.ecp.inventory.internal.domain.model.StockItem;
import org.phuchoang.ecp.inventory.internal.domain.repository.StockItemRepository;
import org.phuchoang.ecp.inventory.internal.domain.service.AdjustStockDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies that a root-owned adjustment persists both aggregate state and its immutable child. */
@SpringBootTest(properties = {
    "ecp.cursor.active-key=integration-test-cursor-secret-0001",
    "spring.flyway.default-schema=inventory_adjustment_l4",
    "spring.flyway.schemas=inventory_adjustment_l4",
    "spring.jpa.properties.hibernate.default_schema=inventory_adjustment_l4",
    "spring.task.scheduling.enabled=false",
    "spring.kafka.listener.auto-startup=false"
})
@Import(TestcontainersConfiguration.class)
class InventoryAdjustmentPersistenceIT {

    @Autowired private AdjustStockDomainService adjustments;
    @Autowired private StockItemRepository stockItems;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    private UUID warehouseId;

    @BeforeEach
    void seedWarehouse() {
        warehouseId = UUID.randomUUID();
        jdbc.update("insert into inventory_adjustment_l4.inventory_warehouse (id, code, name, country_code) values (?, ?, ?, ?)",
            warehouseId, "ADJ-" + warehouseId.toString().substring(0, 8), "Adjustment test warehouse", "VN");
    }

    @Test
    void persistsTheRootAndItsMovementThroughTheStockItemRepository() {
        UUID stockItemId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        transactions.executeWithoutResult(status -> stockItems.saveAll(List.of(
            StockItem.open(stockItemId, "ADJ-1", warehouseId, 5))));

        transactions.executeWithoutResult(status -> adjustments.adjust(stockItemId,
            StockAdjustment.proposed(4, "COUNT", "Cycle count", actorId)));

        assertThat(jdbc.queryForObject("select quantity_on_hand from inventory_adjustment_l4.inventory_stock_item where id = ?",
            Integer.class, stockItemId)).isEqualTo(9);
        assertThat(jdbc.queryForObject("select count(*) from inventory_adjustment_l4.inventory_stock_adjustment "
            + "where stock_item_id = ? and delta = ? and actor_id = ?", Integer.class, stockItemId, 4, actorId)).isEqualTo(1);
    }
}
