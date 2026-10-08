-- Sprint 11: a retried placement may return its original held reservation, but
-- must never create a second reservation for the same allocated order line.
ALTER TABLE inventory_stock_reservation
    ADD COLUMN orphaned_at TIMESTAMPTZ;

ALTER TABLE inventory_stock_reservation
    ADD CONSTRAINT ux_inventory_stock_reservation_order_line_stock_item
        UNIQUE (order_id, order_line_id, stock_item_id);
