-- Database.md §4.5 — ordering_order_line, ordering_order_line_reservation (child).
CREATE TABLE ordering_order_line (
    id                          UUID          NOT NULL,
    order_id                    UUID          NOT NULL,
    variant_id                  UUID          NOT NULL,
    sku                         TEXT          NOT NULL,
    product_name                TEXT          NOT NULL,
    variant_name                TEXT,
    quantity                    INTEGER       NOT NULL,
    unit_price_at_order_amount  NUMERIC(19,4) NOT NULL,
    unit_price_at_order_currency CHAR(3)      NOT NULL,
    line_discount_amount        NUMERIC(19,4) NOT NULL DEFAULT 0,
    line_total_amount           NUMERIC(19,4) NOT NULL,
    created_at                  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by                  UUID,
    updated_at                  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by                  UUID,
    CONSTRAINT pk_ordering_order_line PRIMARY KEY (id),
    CONSTRAINT fk_ordering_order_line_order_id
        FOREIGN KEY (order_id) REFERENCES ordering_order (id) ON DELETE CASCADE,
    CONSTRAINT ck_ordering_order_line_quantity CHECK (quantity > 0),
    CONSTRAINT ck_ordering_order_line_unit_price CHECK (unit_price_at_order_amount >= 0),
    CONSTRAINT ck_ordering_order_line_total CHECK (
        line_total_amount = (unit_price_at_order_amount * quantity) - line_discount_amount)
);
CREATE INDEX ix_ordering_order_line_order_id ON ordering_order_line (order_id);
CREATE INDEX ix_ordering_order_line_variant_id ON ordering_order_line (variant_id);

-- The plain set of (StockItemId, StockReservationId) references Domain Model
-- §8.3 and §8.5 describe: an order line drawing from three warehouses has three
-- rows here. No FK on either reference — both cross into inventory_.
CREATE TABLE ordering_order_line_reservation (
    id                    UUID        NOT NULL,
    order_line_id         UUID        NOT NULL,
    stock_item_id         UUID        NOT NULL,
    stock_reservation_id  UUID        NOT NULL,
    warehouse_id          UUID        NOT NULL,
    quantity              INTEGER     NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by            UUID,
    CONSTRAINT pk_ordering_order_line_reservation PRIMARY KEY (id),
    CONSTRAINT fk_ordering_order_line_reservation_order_line_id
        FOREIGN KEY (order_line_id) REFERENCES ordering_order_line (id) ON DELETE CASCADE,
    CONSTRAINT ux_ordering_order_line_reservation_reservation
        UNIQUE (stock_reservation_id),
    CONSTRAINT ck_ordering_order_line_reservation_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_ordering_order_line_reservation_order_line_id
    ON ordering_order_line_reservation (order_line_id);
