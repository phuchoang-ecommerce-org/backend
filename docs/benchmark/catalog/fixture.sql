-- Disposable Catalog browse benchmark fixture: 100 categories, 10,000 published products,
-- two active variants and one cover image per product. Run only against an isolated benchmark DB.
INSERT INTO catalog_category (id, parent_id, name, slug, path, depth, sort_order)
VALUES ('10000000-0000-0000-0000-000000000001', NULL, 'Benchmark root', 'benchmark-root', '/benchmark-root/', 0, 0);

INSERT INTO catalog_category (id, parent_id, name, slug, path, depth, sort_order)
SELECT ('10000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid,
       CASE WHEN n <= 10 THEN '10000000-0000-0000-0000-000000000001'::uuid
            ELSE ('10000000-0000-0000-0000-' || lpad((2 + ((n - 11) % 9))::text, 12, '0'))::uuid END,
       'Benchmark category ' || n, 'benchmark-category-' || n,
       CASE WHEN n <= 10 THEN '/benchmark-root/category-' || n || '/'
            ELSE '/benchmark-root/category-' || (2 + ((n - 11) % 9)) || '/subcategory-' || n || '/' END,
       CASE WHEN n <= 10 THEN 1 ELSE 2 END, n
FROM generate_series(2, 100) AS n;

INSERT INTO catalog_product (id, category_id, name, slug, brand, publication_status, published_at, attributes,
                             average_rating, review_count, created_at)
SELECT ('20000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid,
       ('10000000-0000-0000-0000-' || lpad((2 + ((n - 1) % 99))::text, 12, '0'))::uuid,
       'Product ' || lpad(n::text, 5, '0'), 'product-' || lpad(n::text, 5, '0'),
       'Brand ' || (n % 20), 'PUBLISHED', now() - (n || ' seconds')::interval, '{}'::jsonb,
       1 + ((n % 401)::numeric / 100), n % 500, now() - (n || ' seconds')::interval
FROM generate_series(1, 10000) AS n;

INSERT INTO catalog_variant (id, product_id, sku, name, list_price_amount, list_price_currency, options,
                             is_active, advisory_in_stock)
SELECT ('30000000-0000-0000-0000-' || lpad((n * 2 - 1)::text, 12, '0'))::uuid,
       ('20000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid,
       'BENCH-' || n || '-A', 'Variant A', 10 + (n % 1000), 'USD', '{}'::jsonb, true, (n % 3 <> 0)
FROM generate_series(1, 10000) AS n;

INSERT INTO catalog_variant (id, product_id, sku, name, list_price_amount, list_price_currency, options,
                             is_active, advisory_in_stock)
SELECT ('30000000-0000-0000-0000-' || lpad((n * 2)::text, 12, '0'))::uuid,
       ('20000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid,
       'BENCH-' || n || '-B', 'Variant B', 11 + (n % 1000), 'USD', '{}'::jsonb, true, (n % 4 <> 0)
FROM generate_series(1, 10000) AS n;

INSERT INTO catalog_product_image (id, product_id, url, alt_text, sort_order)
SELECT ('40000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid,
       ('20000000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid,
       'https://example.test/images/' || n || '.jpg', 'Benchmark product ' || n, 0
FROM generate_series(1, 10000) AS n;

ANALYZE catalog_category;
ANALYZE catalog_product;
ANALYZE catalog_variant;
ANALYZE catalog_product_image;
