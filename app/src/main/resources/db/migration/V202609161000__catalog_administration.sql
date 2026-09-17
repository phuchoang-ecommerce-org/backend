-- Sprint 09: administrative category state and permanent SKU retirement.
ALTER TABLE catalog_category ADD COLUMN image_url TEXT;
ALTER TABLE catalog_category ADD COLUMN featured BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE catalog_retired_sku (
    sku TEXT NOT NULL,
    retired_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_catalog_retired_sku PRIMARY KEY (sku)
);

CREATE OR REPLACE FUNCTION catalog_retire_variant_sku() RETURNS trigger AS $$
BEGIN
    INSERT INTO catalog_retired_sku (sku) VALUES (OLD.sku) ON CONFLICT (sku) DO NOTHING;
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_catalog_retire_variant_sku
    BEFORE DELETE ON catalog_variant
    FOR EACH ROW EXECUTE FUNCTION catalog_retire_variant_sku();

CREATE OR REPLACE FUNCTION catalog_reject_retired_sku() RETURNS trigger AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM catalog_retired_sku WHERE sku = NEW.sku) THEN
        RAISE EXCEPTION 'catalog SKU % was previously retired', NEW.sku USING ERRCODE = 'unique_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_catalog_reject_retired_sku
    BEFORE INSERT OR UPDATE OF sku ON catalog_variant
    FOR EACH ROW EXECUTE FUNCTION catalog_reject_retired_sku();

CREATE TABLE catalog_revalidation_cursor (
    aggregate_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_catalog_revalidation_cursor PRIMARY KEY (aggregate_id)
);
