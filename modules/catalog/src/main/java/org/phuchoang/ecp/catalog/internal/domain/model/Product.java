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
    PublicationStatus publicationStatus,
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
        return new Product(id, categoryId, name, slug(name, id), description, brand, PublicationStatus.DRAFT, null,
            attributes, List.of(), List.of());
    }

    /** Applies the mutable merchandising fields of an administration command; publication is untouched. */
    public Product change(UUID updatedCategoryId, String updatedName, String updatedDescription, String updatedBrand,
            Map<String, Object> updatedAttributes) {
        return new Product(id, updatedCategoryId, updatedName, slug, updatedDescription, updatedBrand, publicationStatus,
            publishedAt, updatedAttributes, variants, images);
    }

    /**
     * Moves the product to {@code status}. The first transition to {@code PUBLISHED} stamps
     * {@code publishedAt}; later re-publications keep the original timestamp. Every transition is
     * currently permitted — none is forbidden by the SRS — so this is the single place a rule would
     * be added.
     */
    public Product transitionTo(PublicationStatus status, Instant now) {
        Instant firstPublishedAt = status == PublicationStatus.PUBLISHED && publishedAt == null ? now : publishedAt;
        return new Product(id, categoryId, name, slug, description, brand, status, firstPublishedAt, attributes,
            variants, images);
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

    public Image image(UUID imageId) {
        return images.stream().filter(image -> image.id().equals(imageId)).findFirst().orElse(null);
    }

    public List<UUID> variantIds() {
        return variants.stream().map(Variant::id).toList();
    }

    public List<String> variantSkus() {
        return variants.stream().map(Variant::sku).toList();
    }

    private Product withChildren(List<Variant> updatedVariants, List<Image> updatedImages) {
        return new Product(id, categoryId, name, slug, description, brand, publicationStatus, publishedAt, attributes,
            updatedVariants, updatedImages);
    }

    private static String slug(String name, UUID id) {
        return Slugs.normalize(name) + "-" + id.toString().substring(0, 8);
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
