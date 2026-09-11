-- Database.md §4.3 — inventory_stock_adjustment. BR-INV-03 (reason/actor mandatory).
CREATE TABLE inventory_stock_adjustment (
    id             UUID        NOT NULL,
    stock_item_id  UUID        NOT NULL,
    delta          INTEGER     NOT NULL,
    reason_code    VARCHAR(32) NOT NULL,
    reason         TEXT        NOT NULL,
    actor_id       UUID        NOT NULL,
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     UUID,
    CONSTRAINT pk_inventory_stock_adjustment PRIMARY KEY (id),
    CONSTRAINT fk_inventory_stock_adjustment_stock_item_id
        FOREIGN KEY (stock_item_id) REFERENCES inventory_stock_item (id),
    CONSTRAINT ck_inventory_stock_adjustment_delta CHECK (delta <> 0)
);
CREATE INDEX ix_inventory_stock_adjustment_stock_item_id
    ON inventory_stock_adjustment (stock_item_id, occurred_at DESC);
