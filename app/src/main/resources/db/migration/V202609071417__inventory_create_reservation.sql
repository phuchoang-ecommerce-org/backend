-- Database.md §4.3 — inventory_stock_reservation. BR-INV-02 (resolves exactly once).
CREATE TABLE inventory_stock_reservation (
    id             UUID        NOT NULL,
    stock_item_id  UUID        NOT NULL,
    order_id       UUID        NOT NULL,
    order_line_id  UUID        NOT NULL,
    quantity       INTEGER     NOT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'HELD',
    expires_at     TIMESTAMPTZ NOT NULL,
    resolved_at    TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     UUID,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by     UUID,
    CONSTRAINT pk_inventory_stock_reservation PRIMARY KEY (id),
    CONSTRAINT fk_inventory_stock_reservation_stock_item_id
        FOREIGN KEY (stock_item_id) REFERENCES inventory_stock_item (id) ON DELETE CASCADE,
    CONSTRAINT ck_inventory_stock_reservation_quantity CHECK (quantity > 0),
    CONSTRAINT ck_inventory_stock_reservation_status
        CHECK (status IN ('HELD', 'COMMITTED', 'RELEASED')),
    -- BR-INV-02: a reservation resolves exactly once — committed or released,
    -- never both and never neither. Terminal status and resolution timestamp
    -- are forced to agree, so a half-applied transition cannot be persisted.
    CONSTRAINT ck_inventory_stock_reservation_resolution
        CHECK ((status = 'HELD' AND resolved_at IS NULL)
            OR (status <> 'HELD' AND resolved_at IS NOT NULL))
);
CREATE INDEX ix_inventory_stock_reservation_order_id
    ON inventory_stock_reservation (order_id);
CREATE INDEX ix_inventory_stock_reservation_expiry
    ON inventory_stock_reservation (expires_at) WHERE status = 'HELD';
