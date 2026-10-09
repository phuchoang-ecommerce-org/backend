package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing;

import org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct.ProductListingPort;
import org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct.ProductListingQuery;
import org.phuchoang.ecp.catalog.internal.application.browse.query.categoryproduct.ProductPage;
import org.phuchoang.ecp.catalog.internal.infrastructure.persistence.JdbcQuerySupport;
import org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing.ProductListingCursorCodec.SeekPosition;
import org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing.ProductListingSqlBuilder.ListingSql;
import org.phuchoang.ecp.catalog.internal.application.pagination.CursorCodec;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * The category-listing read adapter: resolves the category path, runs the count and page statements
 * assembled by {@link ProductListingSqlBuilder}, and cuts the page with the look-ahead row and
 * {@link ProductListingCursorCodec}. Keyset pagination throughout — never {@code OFFSET}.
 */
@Component
class CatalogProductListingQueries extends JdbcQuerySupport implements ProductListingPort {

    private final ProductListingCursorCodec cursors;
    private final ProductListingRowMapper rows = new ProductListingRowMapper();

    CatalogProductListingQueries(JdbcClient jdbc, CursorCodec cursorCodec) {
        super(jdbc);
        this.cursors = new ProductListingCursorCodec(cursorCodec);
    }

    @Override
    public long count(UUID categoryId, ProductListingQuery query) {
        String path = required("SELECT path FROM catalog_category WHERE id = ?", String.class, categoryId);
        ListingSql sql = ProductListingSqlBuilder.build(path, query, null);
        return jdbc.sql(sql.countSql()).params(sql.countParameters()).query(Long.class).single();
    }

    @Override
    public ProductPage products(UUID categoryId, ProductListingQuery query, long total) {
        String path = required("SELECT path FROM catalog_category WHERE id = ?", String.class, categoryId);
        SeekPosition cursor = cursors.decode(categoryId, query);
        ListingSql sql = ProductListingSqlBuilder.build(path, query, cursor);
        List<ProductListingRow> page = jdbc.sql(sql.pageSql()).params(sql.pageParameters()).query(rows).list();

        boolean hasMore = page.size() > query.size();
        if (hasMore) {
            page = page.subList(0, query.size());
        }
        String next = hasMore ? cursors.encode(categoryId, query, page.getLast()) : null;
        return new ProductPage(page.stream().map(rows::toSummary).toList(), next, total);
    }
}
