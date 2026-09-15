-- Sprint 07: Catalog-owned, nullable advisory availability. Inventory becomes authoritative later.
ALTER TABLE catalog_variant ADD COLUMN advisory_in_stock BOOLEAN;

-- Published category reads and every keyset ordering path in CatalogBrowseRepository.
CREATE INDEX ix_catalog_category_path_prefix ON catalog_category (path text_pattern_ops);
CREATE INDEX ix_catalog_product_published_category_name ON catalog_product (category_id, name, id)
    WHERE publication_status = 'PUBLISHED';
CREATE INDEX ix_catalog_product_published_category_created_at ON catalog_product (category_id, created_at, id)
    WHERE publication_status = 'PUBLISHED';
CREATE INDEX ix_catalog_product_published_category_popularity ON catalog_product (category_id, review_count, id)
    WHERE publication_status = 'PUBLISHED';
CREATE INDEX ix_catalog_variant_product_active_price ON catalog_variant (product_id, is_active, list_price_amount, id);
CREATE INDEX ix_catalog_variant_options_resolution ON catalog_variant USING GIN (options jsonb_path_ops);
