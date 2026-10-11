package org.phuchoang.ecp.catalog.internal.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.phuchoang.ecp.catalog.internal.domain.event.CatalogDomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductCreated;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductDiscontinued;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPriceChanged;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPublished;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductUpdated;
import org.phuchoang.ecp.catalog.internal.domain.event.VariantAdded;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Catalog's product aggregate. Variants and images are changed only through this root. */
@AggregateRoot
@Getter
@EqualsAndHashCode
public final class Product implements CatalogEventSource {

    @Identity private UUID id;
    private UUID categoryId;
    private String name;
    private String slug;
    private String description;
    private String brand;
    private PublicationStatus publicationStatus;
    private Instant publishedAt;
    private Map<String, Object> attributes;
    private List<Variant> variants;
    private List<Image> images;
    @EqualsAndHashCode.Exclude private final List<CatalogDomainEvent> domainEvents = new ArrayList<>();

    /** Reconstitutes a product without raising an event. */
    public Product(UUID id, UUID categoryId, String name, String slug, String description, String brand,
            PublicationStatus publicationStatus, Instant publishedAt, Map<String, Object> attributes,
            List<Variant> variants, List<Image> images) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.brand = brand;
        this.publicationStatus = publicationStatus;
        this.publishedAt = publishedAt;
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        this.variants = variants == null ? List.of() : List.copyOf(variants);
        this.images = images == null ? List.of() : List.copyOf(images);
    }

    public static Product create(UUID id, UUID categoryId, String name, String description, String brand,
            Map<String, Object> attributes) {
        Product product = new Product(id, categoryId, name, slug(name, id), description, brand,
            PublicationStatus.DRAFT, null, attributes, List.of(), List.of());
        product.domainEvents.add(new ProductCreated(product.snapshot()));
        return product;
    }

    public Product change(UUID updatedCategoryId, String updatedName, String updatedDescription, String updatedBrand,
            Map<String, Object> updatedAttributes) {
        categoryId = updatedCategoryId;
        name = updatedName;
        description = updatedDescription;
        brand = updatedBrand;
        attributes = updatedAttributes == null ? Map.of() : Map.copyOf(updatedAttributes);
        domainEvents.add(new ProductUpdated(snapshot()));
        return this;
    }

    public Product transitionTo(PublicationStatus status, Instant now) {
        if (status == PublicationStatus.PUBLISHED && publishedAt == null) {
            publishedAt = now;
        }
        publicationStatus = status;
        Product snapshot = snapshot();
        domainEvents.add(switch (status) {
            case PUBLISHED -> new ProductPublished(snapshot);
            case DISCONTINUED -> new ProductDiscontinued(id, variantIds(), variantSkus());
            case DRAFT, UNPUBLISHED -> new ProductUpdated(snapshot);
        });
        return this;
    }

    public Product addVariant(Variant variant) {
        List<Variant> updated = new ArrayList<>(variants);
        updated.add(variant);
        variants = List.copyOf(updated);
        domainEvents.add(new VariantAdded(snapshot(), variant));
        return this;
    }

    public Product removeVariant(UUID variantId) {
        variants = variants.stream().filter(variant -> !variant.id().equals(variantId)).toList();
        domainEvents.add(new ProductUpdated(snapshot()));
        return this;
    }

    public Product changePrice(UUID variantId, BigDecimal amount, String currency) {
        variants = variants.stream().map(variant -> variant.id().equals(variantId)
            ? new Variant(variant.id(), variant.sku(), variant.name(), amount, currency, variant.options(),
                variant.weightGrams(), variant.active())
            : variant).toList();
        domainEvents.add(new ProductPriceChanged(id, variant(variantId)));
        return this;
    }

    public Product addImage(Image image) {
        List<Image> updated = new ArrayList<>(images);
        updated.add(image);
        images = List.copyOf(updated);
        domainEvents.add(new ProductUpdated(snapshot()));
        return this;
    }

    public Product removeImage(UUID imageId) {
        images = images.stream().filter(image -> !image.id().equals(imageId)).toList();
        domainEvents.add(new ProductUpdated(snapshot()));
        return this;
    }

    /** Records removal before the application deletes this aggregate's persistent state. */
    public void discontinue() {
        domainEvents.add(new ProductDiscontinued(id, variantIds(), variantSkus()));
    }

    public Variant variant(UUID variantId) {
        return variants.stream().filter(variant -> variant.id().equals(variantId)).findFirst().orElse(null);
    }

    public Image image(UUID imageId) {
        return images.stream().filter(image -> image.id().equals(imageId)).findFirst().orElse(null);
    }

    public List<UUID> variantIds() { return variants.stream().map(Variant::id).toList(); }
    public List<String> variantSkus() { return variants.stream().map(Variant::sku).toList(); }

    public UUID id() { return id; }
    public UUID categoryId() { return categoryId; }
    public String name() { return name; }
    public String slug() { return slug; }
    public String description() { return description; }
    public String brand() { return brand; }
    public PublicationStatus publicationStatus() { return publicationStatus; }
    public Instant publishedAt() { return publishedAt; }
    public Map<String, Object> attributes() { return attributes; }
    public List<Variant> variants() { return variants; }
    public List<Image> images() { return images; }

    @Override
    public List<CatalogDomainEvent> pullDomainEvents() {
        List<CatalogDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private Product snapshot() {
        return new Product(id, categoryId, name, slug, description, brand, publicationStatus, publishedAt,
            attributes, variants, images);
    }

    private static String slug(String name, UUID id) {
        return Slugs.normalize(name) + "-" + id.toString().substring(0, 8);
    }

    @Entity
    public record Variant(@Identity UUID id, String sku, String name, BigDecimal amount, String currency,
            Map<String, String> options, Integer weightGrams, boolean active) {
        public Variant { options = options == null ? Map.of() : Map.copyOf(options); }
    }

    @Entity
    public record Image(@Identity UUID id, String url, String altText, int sortOrder) { }
}
