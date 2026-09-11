-- Database.md §4.3 — inventory_warehouse.
CREATE TABLE inventory_warehouse (
    id           UUID        NOT NULL,
    code         VARCHAR(32) NOT NULL,
    name         TEXT        NOT NULL,
    country_code CHAR(2)     NOT NULL,
    is_active    BOOLEAN     NOT NULL DEFAULT true,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by   UUID,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by   UUID,
    CONSTRAINT pk_inventory_warehouse PRIMARY KEY (id),
    CONSTRAINT ux_inventory_warehouse_code UNIQUE (code)
);
