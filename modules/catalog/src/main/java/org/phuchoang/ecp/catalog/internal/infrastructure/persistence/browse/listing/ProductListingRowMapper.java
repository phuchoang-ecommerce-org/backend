package org.phuchoang.ecp.catalog.internal.infrastructure.persistence.browse.listing;

import org.phuchoang.ecp.catalog.internal.application.query.model.common.MoneyValue;
import org.phuchoang.ecp.catalog.internal.application.query.model.product.ProductSummary;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/** {@code product_rows} column mapping and its projection into the application's {@link ProductSummary}. */
final class ProductListingRowMapper implements RowMapper<ProductListingRow> {

    @Override
    public ProductListingRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ProductListingRow(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("slug"),
            rs.getString("brand"), rs.getString("publication_status"), rs.getString("image_url"),
            rs.getBigDecimal("price_from"), rs.getBigDecimal("price_to"), rs.getString("currency"),
            (Double) rs.getObject("average_rating"), rs.getInt("review_count"),
            rs.getObject("in_stock", Boolean.class), rs.getObject("created_at", OffsetDateTime.class));
    }

    ProductSummary toSummary(ProductListingRow row) {
        MoneyValue from = row.priceFrom() == null ? null : new MoneyValue(row.priceFrom(), row.currency());
        MoneyValue to = row.priceTo() == null ? null : new MoneyValue(row.priceTo(), row.currency());
        return new ProductSummary(row.id(), row.name(), row.slug(), row.brand(), row.status(), row.imageUrl(), from, to,
            row.averageRating(), row.reviewCount(), row.inStock());
    }
}
