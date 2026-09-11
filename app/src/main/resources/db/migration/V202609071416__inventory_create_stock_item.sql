-- Database.md §4.3 — inventory_stock_item. ADR-0011 optimistic-locking oversell defence.
CREATE TABLE inventory_stock_item (
    id                  UUID        NOT NULL,
    sku                 TEXT        NOT NULL,
    warehouse_id        UUID        NOT NULL,
    owner_id            UUID,
    quantity_on_hand    INTEGER     NOT NULL DEFAULT 0,
    quantity_reserved   INTEGER     NOT NULL DEFAULT 0,
    -- Derived, never stored independently (Domain Model §6, §8.3). A GENERATED
    -- column cannot be written by any statement, so no code path — present or
    -- future, application or manual — can put availability out of step with the
    -- two counters it is defined from. This is BR-INV-01 held by construction
    -- rather than by discipline.
    available_quantity  INTEGER     GENERATED ALWAYS AS (quantity_on_hand - quantity_reserved) STORED,
    reorder_threshold   INTEGER,
    version             BIGINT      NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          UUID,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by          UUID,
    CONSTRAINT pk_inventory_stock_item PRIMARY KEY (id),
    CONSTRAINT fk_inventory_stock_item_warehouse_id
        FOREIGN KEY (warehouse_id) REFERENCES inventory_warehouse (id),
    -- Aggregate identity is (Sku, WarehouseId) — Domain Model §8.3.
    CONSTRAINT ux_inventory_stock_item_sku_warehouse UNIQUE (sku, warehouse_id),
    CONSTRAINT ck_inventory_stock_item_on_hand CHECK (quantity_on_hand >= 0),
    CONSTRAINT ck_inventory_stock_item_reserved CHECK (quantity_reserved >= 0),
    -- BR-INV-01: available stock is never negative. The last line of defence
    -- behind the version check — if optimistic locking were ever bypassed, this
    -- constraint still refuses the oversold row.
    CONSTRAINT ck_inventory_stock_item_reserved_le_on_hand
        CHECK (quantity_reserved <= quantity_on_hand)
);
CREATE INDEX ix_inventory_stock_item_sku ON inventory_stock_item (sku);
CREATE INDEX ix_inventory_stock_item_low_stock
    ON inventory_stock_item (warehouse_id, available_quantity)
    WHERE reorder_threshold IS NOT NULL;
