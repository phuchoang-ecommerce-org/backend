package org.phuchoang.ecp.catalog.application.query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Internal read-model values returned by the browse port. The API adapter maps these values to
 * the module's public contract; they must not cross the module boundary directly.
 */
public final class CatalogBrowseModel {
    /** Prevents instantiation of this read-model namespace. */
    private CatalogBrowseModel() { }

    /**
     * Normalized inputs for one category listing read.
     *
     * @param cursor opaque continuation cursor, or {@code null} for the first page
     * @param size clamped page size
     * @param sort normalized sort expression, or {@code default}
     * @param brands matching brands
     * @param priceFrom inclusive minimum price, or {@code null}
     * @param priceTo inclusive maximum price, or {@code null}
     * @param inStock requested advisory stock state, or {@code null}
     */
    public record ListingQuery(String cursor, int size, String sort, List<String> brands,
                               BigDecimal priceFrom, BigDecimal priceTo, Boolean inStock) { }

    /**
     * Compact category reference for an ancestor chain.
     *
     * @param id category identifier
     * @param name display name
     * @param slug human-readable category identifier
     */
    public record CategoryRef(UUID id, String name, String slug) { }

    /**
     * Direct category-read projection.
     *
     * @param id category identifier
     * @param parentId parent identifier, or {@code null} for a root
     * @param name display name
     * @param slug human-readable category identifier
     * @param depth zero-based tree depth
     * @param sortOrder configured sibling order
     * @param imageUrl optional image URL
     * @param featured whether the category is featured
     * @param ancestors root-to-parent breadcrumb chain
     */
    public record Category(UUID id, UUID parentId, String name, String slug, int depth, int sortOrder,
                           String imageUrl, boolean featured, List<CategoryRef> ancestors) { }

    /**
     * Recursive category-navigation projection.
     *
     * @param id category identifier
     * @param parentId parent identifier, or {@code null} for a root
     * @param name display name
     * @param slug human-readable category identifier
     * @param depth zero-based tree depth
     * @param sortOrder configured sibling order
     * @param imageUrl optional image URL
     * @param featured whether the category is featured
     * @param ancestors root-to-parent breadcrumb chain for subtree reads
     * @param children recursively nested children
     */
    public record CategoryNode(UUID id, UUID parentId, String name, String slug, int depth, int sortOrder,
                               String imageUrl, boolean featured, List<CategoryRef> ancestors,
                               List<CategoryNode> children) { }

    /**
     * Exact price paired with its currency.
     *
     * @param amount decimal amount
     * @param currency currency code
     */
    public record Money(BigDecimal amount, String currency) { }

    /**
     * Product projection suitable for a category listing.
     *
     * @param id product identifier
     * @param name display name
     * @param slug human-readable product identifier
     * @param brand optional brand
     * @param publicationStatus visibility state
     * @param primaryImageUrl optional leading image URL
     * @param priceFrom lowest active-variant price, or {@code null}
     * @param priceTo highest active-variant price, or {@code null}
     * @param averageRating denormalized rating, or {@code null}
     * @param reviewCount number of reviews
     * @param inStock advisory availability, or {@code null} when unknown
     */
    public record ProductSummary(UUID id, String name, String slug, String brand, String publicationStatus,
                                 String primaryImageUrl, Money priceFrom, Money priceTo, Double averageRating,
                                 int reviewCount, Boolean inStock) { }

    /**
     * Resolved cursor page of product summaries.
     *
     * @param items page items
     * @param nextCursor cursor for the next page, or {@code null}
     * @param total total matching products
     */
    public record ProductPage(List<ProductSummary> items, String nextCursor, long total) { }

    /**
     * Variant projection for selection and direct lookup.
     *
     * @param id variant identifier
     * @param sku platform-wide stock-keeping unit
     * @param name display name
     * @param listPrice list price
     * @param options identifying option dimension/value pairs
     * @param weightGrams optional shippable weight
     * @param active whether the variant is active
     * @param inStock advisory availability, or {@code null} when unknown
     */
    public record Variant(UUID id, String sku, String name, Money listPrice, Map<String, String> options,
                          Integer weightGrams, boolean active, Boolean inStock) { }
}
