-- Sprint 07 query-plan evidence. Run this after the catalog fixture has been loaded:
-- psql "$JDBC_URL" -f catalog-listing-paths.sql
-- Each statement mirrors CatalogBrowseRepository's keyset path; it is deliberately EXPLAIN-only
-- so fixture data and PostgreSQL version do not turn the evidence into a timing assertion.

EXPLAIN (COSTS OFF)
SELECT p.id FROM catalog_product p JOIN catalog_category c ON c.id = p.category_id
WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE '/catalog/%'
ORDER BY p.name, p.id LIMIT 21;

EXPLAIN (COSTS OFF)
SELECT p.id FROM catalog_product p JOIN catalog_category c ON c.id = p.category_id
WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE '/catalog/%'
ORDER BY p.created_at ASC, p.id ASC LIMIT 21;

EXPLAIN (COSTS OFF)
SELECT p.id FROM catalog_product p JOIN catalog_category c ON c.id = p.category_id
WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE '/catalog/%'
ORDER BY p.created_at DESC, p.id ASC LIMIT 21;

EXPLAIN (COSTS OFF)
SELECT p.id FROM catalog_product p JOIN catalog_category c ON c.id = p.category_id
WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE '/catalog/%'
ORDER BY p.review_count ASC, p.id ASC LIMIT 21;

EXPLAIN (COSTS OFF)
SELECT p.id FROM catalog_product p JOIN catalog_category c ON c.id = p.category_id
WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE '/catalog/%'
ORDER BY p.review_count DESC, p.id ASC LIMIT 21;

EXPLAIN (COSTS OFF)
SELECT p.id, MIN(v.list_price_amount) AS price_from FROM catalog_product p
JOIN catalog_category c ON c.id = p.category_id
LEFT JOIN catalog_variant v ON v.product_id = p.id AND v.is_active
WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE '/catalog/%'
GROUP BY p.id ORDER BY price_from ASC NULLS LAST, p.id ASC LIMIT 21;

EXPLAIN (COSTS OFF)
SELECT p.id, MIN(v.list_price_amount) AS price_from FROM catalog_product p
JOIN catalog_category c ON c.id = p.category_id
LEFT JOIN catalog_variant v ON v.product_id = p.id AND v.is_active
WHERE p.publication_status = 'PUBLISHED' AND c.path LIKE '/catalog/%'
GROUP BY p.id ORDER BY price_from DESC NULLS LAST, p.id ASC LIMIT 21;

EXPLAIN (COSTS OFF)
SELECT v.id FROM catalog_variant v
WHERE v.product_id = '00000000-0000-0000-0000-000000000000' AND v.options @> '{"size":"M"}'::jsonb
ORDER BY v.sku;
