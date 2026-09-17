package org.phuchoang.ecp.web.catalog;

import org.phuchoang.ecp.catalog.api.facade.CatalogAdministrationFacade;
import org.phuchoang.ecp.catalog.api.request.*;
import org.phuchoang.ecp.catalog.api.result.BulkResult;
import org.phuchoang.ecp.catalog.api.view.category.CategoryView;
import org.phuchoang.ecp.catalog.api.view.product.ProductDetailView;
import org.phuchoang.ecp.catalog.api.view.product.ProductImageView;
import org.phuchoang.ecp.catalog.api.view.product.VariantView;
import org.phuchoang.ecp.identity.api.authorization.CallerContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Authenticated catalog administration HTTP adapter. */
@RestController
class CatalogAdministrationController {
    private final CatalogAdministrationFacade catalog;
    CatalogAdministrationController(CatalogAdministrationFacade catalog) { this.catalog = catalog; }
    @PostMapping("/api/v1/products") ResponseEntity<ProductDetailView> create(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @RequestBody ProductWrite body) { ProductDetailView value = catalog.createProduct(caller(jwt), c, body); return ResponseEntity.created(URI.create("/api/v1/products/" + value.id())).body(value); }
    @PatchMapping("/api/v1/products/{id}") ProductDetailView update(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @PathVariable UUID id, @RequestBody ProductWrite body) { return catalog.updateProduct(caller(jwt), c, id, body); }
    @DeleteMapping("/api/v1/products/{id}") ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @PathVariable UUID id) { catalog.deleteProduct(caller(jwt), c, id); return ResponseEntity.noContent().build(); }
    @PutMapping("/api/v1/products/{id}/publication") ProductDetailView publication(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @PathVariable UUID id, @RequestBody PublicationWrite body) { return catalog.setPublication(caller(jwt), c, id, body); }
    @PostMapping("/api/v1/products/{id}/variants") ResponseEntity<VariantView> variant(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @PathVariable UUID id, @RequestBody VariantWrite body) { VariantView value = catalog.addVariant(caller(jwt), c, id, body); return ResponseEntity.created(URI.create("/api/v1/products/" + id + "/variants/" + value.id())).body(value); }
    @DeleteMapping("/api/v1/products/{id}/variants/{variantId}") ResponseEntity<Void> deleteVariant(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @PathVariable UUID id, @PathVariable UUID variantId) { catalog.removeVariant(caller(jwt), c, id, variantId); return ResponseEntity.noContent().build(); }
    @PutMapping("/api/v1/products/{id}/variants/{variantId}/price") VariantView price(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @PathVariable UUID id, @PathVariable UUID variantId, @RequestBody PriceWrite body) { return catalog.changePrice(caller(jwt), c, id, variantId, body); }
    @PostMapping("/api/v1/products/{id}/images") ResponseEntity<ProductImageView> image(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @PathVariable UUID id, @RequestBody ImageWrite body) { ProductImageView value = catalog.addImage(caller(jwt), c, id, body); return ResponseEntity.created(URI.create("/api/v1/products/" + id + "/images/" + value.id())).body(value); }
    @DeleteMapping("/api/v1/products/{id}/images/{imageId}") ResponseEntity<Void> deleteImage(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @PathVariable UUID id, @PathVariable UUID imageId) { catalog.removeImage(caller(jwt), c, id, imageId); return ResponseEntity.noContent().build(); }
    @PostMapping("/api/v1/products/bulk-amendments") BulkResult bulk(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @RequestBody List<BulkItem> body) { return catalog.amendBulk(caller(jwt), c, body); }
    @PostMapping("/api/v1/categories") ResponseEntity<CategoryView> category(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @RequestBody CategoryWrite body) { CategoryView value = catalog.createCategory(caller(jwt), c, body); return ResponseEntity.created(URI.create("/api/v1/categories/" + value.id())).body(value); }
    @PatchMapping("/api/v1/categories/{id}") CategoryView categoryUpdate(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @PathVariable UUID id, @RequestBody CategoryWrite body) { return catalog.updateCategory(caller(jwt), c, id, body); }
    @DeleteMapping("/api/v1/categories/{id}") ResponseEntity<Void> categoryDelete(@AuthenticationPrincipal Jwt jwt, @RequestHeader("X-Correlation-Id") UUID c, @PathVariable UUID id) { catalog.deleteCategory(caller(jwt), c, id); return ResponseEntity.noContent().build(); }
    private static CallerContext caller(Jwt jwt) { return new CallerContext(UUID.fromString(jwt.getSubject()), Set.copyOf(jwt.getClaimAsStringList("roles"))); }
}
