package org.phuchoang.ecp.catalog.internal.domain.model;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Catalog's product aggregate. Variants and images are owned by the product and are changed
 * only through product administration operations.
 *
 * @param id stable product identifier
 * @param categoryId owning category identifier
 * @param name product display name
 * @param slug stable human-readable product identifier
 * @param description optional product description
 * @param brand optional product brand
 * @param publicationStatus product visibility state
 * @param publishedAt first publication timestamp, when published
 * @param attributes product-specific attributes
 * @param variants purchasable configurations owned by this product
 * @param images ordered images owned by this product
 */
@AggregateRoot
public record Product(
    @Identity UUID id,
    UUID categoryId,
    String name,
    String slug,
    String description,
    String brand,
    String publicationStatus,
    Instant publishedAt,
    Map<String, Object> attributes,
    List<Variant> variants,
    List<Image> images) {

    /** Creates an immutable aggregate snapshot. */
    public Product {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        variants = variants == null ? List.of() : List.copyOf(variants);
        images = images == null ? List.of() : List.copyOf(images);
    }

    /** Creates the initial draft aggregate for a catalog administration command. */
    public static Product create(UUID id, UUID categoryId, String name, String description, String brand,
            Map<String, Object> attributes) {
        return new Product(id, categoryId, name, slug(name, id), description, brand, "DRAFT", null, attributes,
            List.of(), List.of());
    }

    /** Applies the mutable product fields represented by an administration command. */
    public Product change(UUID updatedCategoryId, String updatedName, String updatedDescription, String updatedBrand,
            Map<String, Object> updatedAttributes, String requestedPublicationStatus, Instant now) {
        String status = requestedPublicationStatus == null ? publicationStatus : requestedPublicationStatus;
        Instant firstPublishedAt = "PUBLISHED".equals(status) && publishedAt == null ? now : publishedAt;
        return new Product(id, updatedCategoryId, updatedName, slug, updatedDescription, updatedBrand, status,
            firstPublishedAt, updatedAttributes, variants, images);
    }

    /** Adds an aggregate-owned variant. */
    public Product addVariant(Variant variant) {
        List<Variant> updated = new java.util.ArrayList<>(variants);
        updated.add(variant);
        return withChildren(updated, images);
    }

    /** Removes an aggregate-owned variant when present. */
    public Product removeVariant(UUID variantId) {
        return withChildren(variants.stream().filter(variant -> !variant.id().equals(variantId)).toList(), images);
    }

    /** Changes the list price of an aggregate-owned variant. */
    public Product changePrice(UUID variantId, BigDecimal amount, String currency) {
        return withChildren(variants.stream().map(variant -> variant.id().equals(variantId)
            ? new Variant(variant.id(), variant.sku(), variant.name(), amount, currency, variant.options(),
                variant.weightGrams(), variant.active())
            : variant).toList(), images);
    }

    /** Adds an aggregate-owned display image. */
    public Product addImage(Image image) {
        List<Image> updated = new java.util.ArrayList<>(images);
        updated.add(image);
        return withChildren(variants, updated);
    }

    /** Removes an aggregate-owned display image when present. */
    public Product removeImage(UUID imageId) {
        return withChildren(variants, images.stream().filter(image -> !image.id().equals(imageId)).toList());
    }

    public Variant variant(UUID variantId) {
        return variants.stream().filter(variant -> variant.id().equals(variantId)).findFirst().orElse(null);
    }

    private Product withChildren(List<Variant> updatedVariants, List<Image> updatedImages) {
        return new Product(id, categoryId, name, slug, description, brand, publicationStatus, publishedAt, attributes,
            updatedVariants, updatedImages);
    }

    private static String slug(String name, UUID id) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") + "-"
            + id.toString().substring(0, 8);
    }

    /** A purchasable configuration owned by a product. */
    @Entity
    public record Variant(
        @Identity UUID id,
        String sku,
        String name,
        BigDecimal amount,
        String currency,
        Map<String, String> options,
        Integer weightGrams,
        boolean active) {

        public Variant {
            options = options == null ? Map.of() : Map.copyOf(options);
        }
    }

    /** A display image owned by a product. */
    @Entity
    public record Image(@Identity UUID id, String url, String altText, int sortOrder) { }
}
