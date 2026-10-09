package org.phuchoang.ecp.inventory.internal.infrastructure.persistence;

import org.phuchoang.ecp.inventory.internal.application.management.query.InventoryManagementQueries;
import org.phuchoang.ecp.inventory.internal.application.management.query.StockAdjustmentView;
import org.phuchoang.ecp.inventory.internal.application.management.query.StockItemView;
import org.phuchoang.ecp.inventory.internal.application.management.query.WarehouseView;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** SQL read adapter for operational inventory views; PostgreSQL remains the source of truth. */
@Repository
class JdbcInventoryManagementQueries implements InventoryManagementQueries {
    private final JdbcClient jdbc;
    JdbcInventoryManagementQueries(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override
    public List<StockItemView> listStockItems(String sku, UUID warehouseId, boolean lowStock, int limit) {
        String sql = "select id, sku, warehouse_id, quantity_on_hand, quantity_reserved, available_quantity, reorder_threshold "
            + "from inventory_stock_item where (:sku is null or sku = :sku) and (:warehouseId is null or warehouse_id = :warehouseId) "
            + "and (:lowStock = false or (reorder_threshold is not null and available_quantity < reorder_threshold)) "
            + "order by sku, warehouse_id, id limit :limit";
        return jdbc.sql(sql).param("sku", sku).param("warehouseId", warehouseId).param("lowStock", lowStock)
            .param("limit", limit).query(JdbcInventoryManagementQueries::stock).list();
    }

    @Override
    public Optional<StockItemView> getStockItem(UUID stockItemId) {
        return jdbc.sql("select id, sku, warehouse_id, quantity_on_hand, quantity_reserved, available_quantity, reorder_threshold "
                + "from inventory_stock_item where id = :id").param("id", stockItemId)
            .query(JdbcInventoryManagementQueries::stock).optional();
    }

    @Override
    public List<StockAdjustmentView> listStockAdjustments(UUID stockItemId, int limit) {
        return jdbc.sql("select id, stock_item_id, delta, reason_code, reason, actor_id, occurred_at "
                + "from inventory_stock_adjustment where stock_item_id = :stockItemId order by occurred_at desc, id desc limit :limit")
            .param("stockItemId", stockItemId).param("limit", limit).query((rs, row) -> new StockAdjustmentView(
                rs.getObject("id", UUID.class), rs.getObject("stock_item_id", UUID.class), rs.getInt("delta"),
                rs.getString("reason_code"), rs.getString("reason"), rs.getObject("actor_id", UUID.class),
                rs.getTimestamp("occurred_at").toInstant())).list();
    }

    @Override
    public List<WarehouseView> listWarehouses(int limit) {
        return jdbc.sql("select id, code, name, country_code, is_active from inventory_warehouse order by code, id limit :limit")
            .param("limit", limit).query((rs, row) -> new WarehouseView(rs.getObject("id", UUID.class),
                rs.getString("code"), rs.getString("name"), rs.getString("country_code"), rs.getBoolean("is_active"))).list();
    }

    private static StockItemView stock(ResultSet rs, int row) throws SQLException {
        int threshold = rs.getInt("reorder_threshold");
        return new StockItemView(rs.getObject("id", UUID.class), rs.getString("sku"), rs.getObject("warehouse_id", UUID.class),
            rs.getInt("quantity_on_hand"), rs.getInt("quantity_reserved"), rs.getInt("available_quantity"),
            rs.wasNull() ? null : threshold);
    }
}
