package org.phuchoang.ecp.catalog.internal.application.command;

import tools.jackson.databind.ObjectMapper;
import org.phuchoang.ecp.catalog.internal.application.command.model.*;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.application.port.CatalogAuditRecorder;
import org.phuchoang.ecp.catalog.internal.domain.event.*;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.model.Product.Image;
import org.phuchoang.ecp.catalog.internal.domain.model.Product.Variant;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.phuchoang.ecp.identity.api.authorization.AuthorizationService;
import org.phuchoang.ecp.identity.api.authorization.CallerContext;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.phuchoang.ecp.sharedkernel.api.event.EventActor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Transactional catalog command application service for all Sprint-09 administration operations. */
@Service
public class CatalogAdministrationService {
    public static final String MANAGE_PRODUCTS = "manageProducts";
    public static final String MANAGE_CATEGORIES = "manageCategories";

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final AuthorizationService authorization;
    private final CatalogAuditRecorder audit;
    private final CatalogEventPublisher events;
    private final ObjectMapper json;
    private final Clock clock;

    public CatalogAdministrationService(ProductRepository products, CategoryRepository categories,
            AuthorizationService authorization, CatalogAuditRecorder audit, CatalogEventPublisher events, ObjectMapper json,
            Clock clock) {
        this.products = products;
        this.categories = categories;
        this.authorization = authorization;
        this.audit = audit;
        this.events = events;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public ProductSnapshot createProduct(CallerContext caller, UUID correlationId, CreateProduct command) {
        authorize(caller, MANAGE_PRODUCTS, correlationId, "createProduct", "Product", command.id(), null);
        Product product = products.save(Product.create(command.id(), command.categoryId(), command.name(), command.description(),
            command.brand(), command.attributes()));
        audit(correlationId, caller, "createProduct", "Product", product.id(), null, product, null);
        publish(new ProductCreated(product), correlationId, caller, product.categoryId());
        return ProductSnapshot.from(product);
    }

    @Transactional
    public ProductSnapshot updateProduct(CallerContext caller, UUID correlationId, UUID productId, ProductChange change) {
        authorize(caller, MANAGE_PRODUCTS, correlationId, "updateProduct", "Product", productId, null);
        Product before = product(productId);
        Product after = products.save(change(before, change));
        audit(correlationId, caller, "updateProduct", "Product", productId, before, after, null);
        publish(new ProductUpdated(after), correlationId, caller, after.categoryId());
        return ProductSnapshot.from(after);
    }

    @Transactional
    public void deleteProduct(CallerContext caller, UUID correlationId, UUID productId) {
        authorize(caller, MANAGE_PRODUCTS, correlationId, "deleteProduct", "Product", productId, null);
        Product before = products.findById(productId).orElse(null);
        if (before == null) return;
        audit(correlationId, caller, "deleteProduct", "Product", productId, before, null, null);
        products.deleteById(productId);
        events.publish(new ProductDiscontinued(productId, before.variants().stream().map(Variant::sku).toList()),
            correlationId, actor(caller), List.of(), Map.of());
    }

    @Transactional
    public ProductSnapshot setPublication(CallerContext caller, UUID correlationId, UUID productId, String status, String reason) {
        authorize(caller, MANAGE_PRODUCTS, correlationId, "setProductPublication", "Product", productId, reason);
        if (!List.of("DRAFT", "PUBLISHED", "UNPUBLISHED", "DISCONTINUED").contains(status)) invalid("publicationStatus");
        Product before = product(productId);
        Product after = products.save(before.change(before.categoryId(), before.name(), before.description(), before.brand(),
            before.attributes(), status, clock.instant()));
        audit(correlationId, caller, "setProductPublication", "Product", productId, before, after, reason);
        if (status.equals("PUBLISHED")) publish(new ProductPublished(after), correlationId, caller, after.categoryId());
        else if (status.equals("DISCONTINUED")) events.publish(new ProductDiscontinued(productId,
            after.variants().stream().map(Variant::sku).toList()), correlationId, actor(caller), List.of(), Map.of());
        else publish(new ProductUpdated(after), correlationId, caller, after.categoryId());
        return ProductSnapshot.from(after);
    }

    @Transactional
    public VariantSnapshot addVariant(CallerContext caller, UUID correlationId, UUID productId, AddVariant command) {
        authorize(caller, MANAGE_PRODUCTS, correlationId, "addProductVariant", "Product", productId, null);
        Product before = product(productId);
        try {
            Product after = products.save(before.addVariant(variant(command)));
            Variant added = after.variant(command.id());
            audit(correlationId, caller, "addProductVariant", "Variant", added.id(), null, added, null);
            publish(new VariantAdded(productId, added), correlationId, caller, after.categoryId());
            return VariantSnapshot.from(added);
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException(GenErrorCode.VALIDATION_FAILED, "SKU '" + command.sku() + "' is already in use or retired.");
        }
    }

    @Transactional
    public void removeVariant(CallerContext caller, UUID correlationId, UUID productId, UUID variantId) {
        authorize(caller, MANAGE_PRODUCTS, correlationId, "removeProductVariant", "Variant", variantId, null);
        Product product = products.findById(productId).orElse(null);
        if (product == null) return;
        Variant before = product.variant(variantId);
        if (before == null) return;
        Product after = products.save(product.removeVariant(variantId));
        audit(correlationId, caller, "removeProductVariant", "Variant", variantId, before, null, null);
        publish(new ProductUpdated(after), correlationId, caller, after.categoryId());
    }

    @Transactional
    public VariantSnapshot changePrice(CallerContext caller, UUID correlationId, UUID productId, UUID variantId, Price price,
            String reason) {
        authorize(caller, MANAGE_PRODUCTS, correlationId, "changeVariantPrice", "Variant", variantId, reason);
        if (reason == null || reason.isBlank()) invalid("reason");
        Product product = product(productId);
        Variant before = variant(product, variantId);
        Product after = products.save(product.changePrice(variantId, price.amount(), price.currency()));
        Variant changed = after.variant(variantId);
        audit(correlationId, caller, "changeVariantPrice", "Variant", variantId, before, changed, reason);
        publish(new ProductPriceChanged(productId, changed), correlationId, caller, after.categoryId());
        return VariantSnapshot.from(changed);
    }

    @Transactional
    public ImageSnapshot addImage(CallerContext caller, UUID correlationId, UUID productId, AddImage command) {
        authorize(caller, MANAGE_PRODUCTS, correlationId, "addProductImage", "Product", productId, null);
        Product product = product(productId);
        Product after = products.save(product.addImage(new Image(command.id(), command.url(), command.altText(), command.sortOrder())));
        Image added = after.images().stream().filter(image -> image.id().equals(command.id())).findFirst().orElseThrow();
        audit(correlationId, caller, "addProductImage", "ProductImage", added.id(), null, added, null);
        publish(new ProductUpdated(after), correlationId, caller, after.categoryId());
        return ImageSnapshot.from(added);
    }

    @Transactional
    public void removeImage(CallerContext caller, UUID correlationId, UUID productId, UUID imageId) {
        authorize(caller, MANAGE_PRODUCTS, correlationId, "removeProductImage", "ProductImage", imageId, null);
        Product after = products.save(product(productId).removeImage(imageId));
        audit(correlationId, caller, "removeProductImage", "ProductImage", imageId, null, null, null);
        publish(new ProductUpdated(after), correlationId, caller, after.categoryId());
    }

    @Transactional
    public CategorySnapshot createCategory(CallerContext caller, UUID correlationId, CreateCategory command) {
        authorize(caller, MANAGE_CATEGORIES, correlationId, "createCategory", "Category", command.id(), null);
        Category category = categories.save(Category.create(command.id(), command.parentId(), command.name(), command.slug(),
            command.imageUrl(), command.sortOrder(), command.featured(), parent(command.parentId())));
        CategorySnapshot snapshot = snapshot(category);
        audit(correlationId, caller, "createCategory", "Category", category.id(), null, snapshot, null);
        publishCategory(category, correlationId, caller);
        return snapshot;
    }

    @Transactional
    public CategorySnapshot updateCategory(CallerContext caller, UUID correlationId, UUID categoryId, CategoryChange change) {
        authorize(caller, MANAGE_CATEGORIES, correlationId, "updateCategory", "Category", categoryId, null);
        Category before = category(categoryId);
        Category parent = parent(change.parentId());
        if (!before.mayMoveBelow(parent)) invalid("parentId");
        Category after = categories.save(before.change(change.parentId(), change.name(), change.imageUrl(), change.sortOrder(),
            change.featured(), parent));
        audit(correlationId, caller, "updateCategory", "Category", categoryId, snapshot(before), snapshot(after), null);
        publishCategory(after, correlationId, caller);
        return snapshot(after);
    }

    @Transactional
    public void deleteCategory(CallerContext caller, UUID correlationId, UUID categoryId) {
        authorize(caller, MANAGE_CATEGORIES, correlationId, "deleteCategory", "Category", categoryId, null);
        Category before = categories.findById(categoryId).orElse(null);
        if (before == null) return;
        long productCount = products.countByCategoryId(categoryId);
        long childCategoryCount = categories.countByParentId(categoryId);
        if (productCount > 0 || childCategoryCount > 0) {
            throw new DomainException(GenErrorCode.VALIDATION_FAILED, "Category cannot be deleted: productCount=" + productCount
                + ", childCategoryCount=" + childCategoryCount);
        }
        audit(correlationId, caller, "deleteCategory", "Category", categoryId, snapshot(before), null, null);
        categories.deleteById(categoryId);
        events.publish(new CategoryChanged(categoryId), correlationId, actor(caller), List.of(),
            Map.of("id", categoryId, "affectedCategorySlugs", List.of(before.slug())));
    }

    private Product change(Product product, ProductChange change) {
        return product.change(change.categoryId(), change.name(), change.description(), change.brand(), change.attributes(),
            change.publicationStatus(), clock.instant());
    }
    private void authorize(CallerContext caller, String operation, UUID correlationId, String action, String type, UUID id, String reason) {
        try { authorization.assertAuthorized(caller, operation); }
        catch (DomainException exception) { audit.record(correlationId, actor(caller), action + "Denied", type, id, null, null, reason); throw exception; }
    }
    private Product product(UUID id) { return products.findById(id).orElseThrow(() -> new DomainException(GenErrorCode.NOT_FOUND, "Product not found.")); }
    private Variant variant(Product product, UUID id) { Variant variant = product.variant(id); if (variant == null) throw new DomainException(GenErrorCode.NOT_FOUND, "Variant not found."); return variant; }
    private Category category(UUID id) { return categories.findById(id).orElseThrow(() -> new DomainException(GenErrorCode.NOT_FOUND, "Category not found.")); }
    private Category parent(UUID id) { return id == null ? null : category(id); }
    private static Variant variant(AddVariant command) { return new Variant(command.id(), command.sku(), command.name(), command.amount(), command.currency(), command.options(), command.weightGrams(), command.active()); }
    private static CategorySnapshot snapshot(Category category) { return new CategorySnapshot(category.id(), category.parentId(), category.name(), category.slug(), category.path(), category.depth(), category.imageUrl(), category.sortOrder(), category.featured()); }
    private void invalid(String field) { throw new DomainException(GenErrorCode.VALIDATION_FAILED, "Invalid " + field + "."); }
    private EventActor actor(CallerContext caller) { return caller == null || caller.accountId() == null ? null : new EventActor(caller.accountId(), caller.roleNames().stream().findFirst().orElse(null)); }
    private void audit(UUID c, CallerContext caller, String a, String t, UUID id, Object before, Object after, String reason) { audit.record(c, actor(caller), a, t, id, value(before), value(after), reason); }
    private String value(Object value) { try { return value == null ? null : json.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("Cannot serialize audit value", e); } }
    private void publish(CatalogDomainEvent event, UUID correlationId, CallerContext caller, UUID categoryId) { events.publish(event, correlationId, actor(caller), categories.findSlugsInSubtree(categoryId), Map.of()); }
    private void publishCategory(Category category, UUID correlationId, CallerContext caller) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("id", category.id()); payload.put("parentId", category.parentId()); payload.put("name", category.name());
        payload.put("slug", category.slug()); payload.put("path", category.path()); payload.put("depth", category.depth());
        payload.put("sortOrder", category.sortOrder()); payload.put("affectedCategorySlugs", categories.findSlugsInSubtree(category.id()));
        events.publish(new CategoryChanged(category.id()), correlationId, actor(caller), List.of(), payload);
    }
}
