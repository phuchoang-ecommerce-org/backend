-- Database.md §4.2 — catalog_variant. BR-CAT-01 (SKU unique across the catalog).
CREATE TABLE catalog_variant (
    id                   UUID        NOT NULL,
    product_id           UUID        NOT NULL,
    sku                  TEXT        NOT NULL,
    name                 TEXT        NOT NULL,
    list_price_amount    NUMERIC(19,4) NOT NULL,
    list_price_currency  CHAR(3)     NOT NULL,
    options              JSONB       NOT NULL DEFAULT '{}'::jsonb,
    weight_grams         INTEGER,
    is_active            BOOLEAN     NOT NULL DEFAULT true,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by           UUID,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by           UUID,
    CONSTRAINT pk_catalog_variant PRIMARY KEY (id),
    CONSTRAINT fk_catalog_variant_product_id
        FOREIGN KEY (product_id) REFERENCES catalog_product (id) ON DELETE CASCADE,
    -- BR-CAT-01: a SKU identifies at most one purchasable unit across the
    -- entire catalog. Table-wide, not scoped to product_id — scoping it to the
    -- product would permit the same SKU under two products, which is the exact
    -- collision the rule forbids.
    CONSTRAINT ux_catalog_variant_sku UNIQUE (sku),
    CONSTRAINT ck_catalog_variant_list_price CHECK (list_price_amount >= 0),
    CONSTRAINT ck_catalog_variant_weight CHECK (weight_grams IS NULL OR weight_grams >= 0)
);
CREATE INDEX ix_catalog_variant_product_id ON catalog_variant (product_id);
