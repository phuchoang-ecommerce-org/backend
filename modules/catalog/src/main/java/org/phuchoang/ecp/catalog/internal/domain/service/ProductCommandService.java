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
        return product.create(products);
    }

    public Product change(Product product, UUID categoryId, String name, String description, String brand,
            Map<String, Object> attributes) {
        return product.change(categoryId, name, description, brand, attributes, products);
    }

    public Product transitionTo(Product product, PublicationStatus status, Instant now) {
        return product.transitionTo(status, now, products);
    }

    public Product addVariant(Product product, Product.Variant variant) {
        return product.addVariant(variant, products);
    }

    public Product removeVariant(Product product, UUID variantId) {
        return product.removeVariant(variantId, products);
    }

    public Product changePrice(Product product, UUID variantId, BigDecimal amount, String currency) {
        return product.changePrice(variantId, amount, currency, products);
    }

    public Product addImage(Product product, Product.Image image) {
        return product.addImage(image, products);
    }

    public Product removeImage(Product product, UUID imageId) {
        return product.removeImage(imageId, products);
    }

    public void discontinue(Product product) {
        product.discontinue(products);
    }
}
