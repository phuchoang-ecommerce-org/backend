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
    return products.save(Product.create(id, categoryId, name, description, brand, attributes));
  }

  public Product change(Product product, UUID categoryId, String name, String description, String brand,
      Map<String, Object> attributes) {
    return products.save(product.change(categoryId, name, description, brand, attributes));
  }

  public Product transitionTo(Product product, PublicationStatus status, Instant now) {
    return products.save(product.transitionTo(status, now));
  }

  public Product addVariant(Product product, Product.Variant variant) {
    return products.save(product.addVariant(variant));
  }

  public Product removeVariant(Product product, UUID variantId) {
    return products.save(product.removeVariant(variantId));
  }

  public Product changePrice(Product product, UUID variantId, BigDecimal amount, String currency) {
    return products.save(product.changePrice(variantId, amount, currency));
  }

  public Product addImage(Product product, Product.Image image) {
    return products.save(product.addImage(image));
  }

  public Product removeImage(Product product, UUID imageId) {
    return products.save(product.removeImage(imageId));
  }

  public void discontinue(Product product) {
    products.deleteById(product.id());
  }
}
