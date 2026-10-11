package org.phuchoang.ecp.catalog.internal.domain.service;

import org.jmolecules.ddd.annotation.Service;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.model.PublicationStatus;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Product command domain service. It invokes aggregate transitions; the aggregate
 * itself reaches the domain repository only to load or persist its own state.
 */
@Service
public final class ProductCommandService {

    private final ProductRepository products;

    public ProductCommandService(ProductRepository products) {
        this.products = products;
    }

    public Optional<Product> find(UUID productId) {
        return Product.findById(productId, products);
    }

    public Product create(UUID id, UUID categoryId, String name, String description, String brand,
            Map<String, Object> attributes) {
        Product product = Product.create(id, categoryId, name, description, brand, attributes);
        product.persist(products);
        return product;
    }

    public Product change(Product product, UUID categoryId, String name, String description, String brand,
            Map<String, Object> attributes) {
        product.change(categoryId, name, description, brand, attributes);
        product.persist(products);
        return product;
    }

    public Product transitionTo(Product product, PublicationStatus status, Instant now) {
        product.transitionTo(status, now);
        product.persist(products);
        return product;
    }

    public Product addVariant(Product product, Product.Variant variant) {
        product.addVariant(variant);
        product.persist(products);
        return product;
    }

    public Product removeVariant(Product product, UUID variantId) {
        product.removeVariant(variantId);
        product.persist(products);
        return product;
    }

    public Product changePrice(Product product, UUID variantId, BigDecimal amount, String currency) {
        product.changePrice(variantId, amount, currency);
        product.persist(products);
        return product;
    }

    public Product addImage(Product product, Product.Image image) {
        product.addImage(image);
        product.persist(products);
        return product;
    }

    public Product removeImage(Product product, UUID imageId) {
        product.removeImage(imageId);
        product.persist(products);
        return product;
    }

    public void discontinue(Product product) {
        product.discontinue(products);
    }
}
