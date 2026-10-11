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
 * Authoritative product write boundary. Each persistence operation is coupled
 * to the aggregate
 * transition that makes it valid, so command use cases cannot persist arbitrary
 * product snapshots.
 */

@Service
public final class ProductCommandService {

  private final ProductRepository products;

  public ProductCommandService(ProductRepository products) {
    this.products = products;
  }

  public Optional<Product> find(UUID productId) {
    return products.findById(productId);
  }

  public Product create(UUID id, UUID categoryId, String name, String description, String brand,
      Map<String, Object> attributes) {
    Product product = Product.create(id, categoryId, name, description, brand, attributes);
    products.save(product);
    return product;
  }

  public Product change(Product product, UUID categoryId, String name, String description, String brand,
      Map<String, Object> attributes) {
    product.change(categoryId, name, description, brand, attributes);
    products.save(product);
    return product;
  }

  public Product transitionTo(Product product, PublicationStatus status, Instant now) {
    product.transitionTo(status, now);
    products.save(product);
    return product;
  }

  public Product addVariant(Product product, Product.Variant variant) {
    product.addVariant(variant);
    products.save(product);
    return product;
  }

  public Product removeVariant(Product product, UUID variantId) {
    product.removeVariant(variantId);
    products.save(product);
    return product;
  }

  public Product changePrice(Product product, UUID variantId, BigDecimal amount, String currency) {
    product.changePrice(variantId, amount, currency);
    products.save(product);
    return product;
  }

  public Product addImage(Product product, Product.Image image) {
    product.addImage(image);
    products.save(product);
    return product;
  }

  public Product removeImage(Product product, UUID imageId) {
    product.removeImage(imageId);
    products.save(product);
    return product;
  }

  public void discontinue(Product product) {
    product.discontinue();
    products.deleteById(product.id());
  }
}
