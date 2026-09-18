package org.phuchoang.ecp.web.catalog;

import org.phuchoang.ecp.catalog.api.facade.CatalogAdministrationFacade;
import org.phuchoang.ecp.catalog.api.request.BulkItem;
import org.phuchoang.ecp.catalog.api.request.CategoryWrite;
import org.phuchoang.ecp.catalog.api.request.ImageWrite;
import org.phuchoang.ecp.catalog.api.request.PriceWrite;
import org.phuchoang.ecp.catalog.api.request.ProductWrite;
import org.phuchoang.ecp.catalog.api.request.PublicationWrite;
import org.phuchoang.ecp.catalog.api.request.VariantWrite;
import org.phuchoang.ecp.catalog.api.result.BulkResult;
import org.phuchoang.ecp.catalog.api.view.category.CategoryView;
import org.phuchoang.ecp.catalog.api.view.product.ProductDetailView;
import org.phuchoang.ecp.catalog.api.view.product.ProductImageView;
import org.phuchoang.ecp.catalog.api.view.product.VariantView;
import org.phuchoang.ecp.web.common.security.RequestContext;
import org.phuchoang.ecp.web.common.security.RequestContextResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Authenticated catalog administration HTTP adapter (`UC-ADM-01`, `UC-ADM-02`). Every method is the
 * same three steps: resolve the request context, call the facade, shape the HTTP response. The
 * correlation id comes from the request context (the {@code X-Correlation-Id} the filter accepted
 * or minted), so the header is optional here like everywhere else.
 */
@RestController
class CatalogAdministrationController {

    private final CatalogAdministrationFacade catalog;
    private final RequestContextResolver requestContext;

    CatalogAdministrationController(CatalogAdministrationFacade catalog, RequestContextResolver requestContext) {
        this.catalog = catalog;
        this.requestContext = requestContext;
    }

    // ---- products ----

    @PostMapping("/api/v1/products")
    ResponseEntity<ProductDetailView> createProduct(@AuthenticationPrincipal Jwt jwt, @RequestBody ProductWrite body) {
        RequestContext context = requestContext.resolve(jwt);

        ProductDetailView product = catalog.createProduct(context.caller(), context.correlationId(), body);

        return ResponseEntity.created(URI.create("/api/v1/products/" + product.id())).body(product);
    }

    @PatchMapping("/api/v1/products/{id}")
    ProductDetailView updateProduct(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestBody ProductWrite body) {
        RequestContext context = requestContext.resolve(jwt);

        return catalog.updateProduct(context.caller(), context.correlationId(), id, body);
    }

    @DeleteMapping("/api/v1/products/{id}")
    ResponseEntity<Void> deleteProduct(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        RequestContext context = requestContext.resolve(jwt);

        catalog.deleteProduct(context.caller(), context.correlationId(), id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/products/{id}/publication")
    ProductDetailView setPublication(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestBody PublicationWrite body) {
        RequestContext context = requestContext.resolve(jwt);

        return catalog.setPublication(context.caller(), context.correlationId(), id, body);
    }

    @PostMapping("/api/v1/products/bulk-amendments")
    BulkResult amendBulk(@AuthenticationPrincipal Jwt jwt, @RequestBody List<BulkItem> body) {
        RequestContext context = requestContext.resolve(jwt);

        return catalog.amendBulk(context.caller(), context.correlationId(), body);
    }

    // ---- variants ----

    @PostMapping("/api/v1/products/{id}/variants")
    ResponseEntity<VariantView> addVariant(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestBody VariantWrite body) {
        RequestContext context = requestContext.resolve(jwt);

        VariantView variant = catalog.addVariant(context.caller(), context.correlationId(), id, body);

        return ResponseEntity.created(URI.create("/api/v1/products/" + id + "/variants/" + variant.id())).body(variant);
    }

    @DeleteMapping("/api/v1/products/{id}/variants/{variantId}")
    ResponseEntity<Void> removeVariant(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @PathVariable UUID variantId) {
        RequestContext context = requestContext.resolve(jwt);

        catalog.removeVariant(context.caller(), context.correlationId(), id, variantId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/v1/products/{id}/variants/{variantId}/price")
    VariantView changePrice(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID variantId,
            @RequestBody PriceWrite body) {
        RequestContext context = requestContext.resolve(jwt);

        return catalog.changePrice(context.caller(), context.correlationId(), id, variantId, body);
    }

    // ---- images ----

    @PostMapping("/api/v1/products/{id}/images")
    ResponseEntity<ProductImageView> addImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestBody ImageWrite body) {
        RequestContext context = requestContext.resolve(jwt);

        ProductImageView image = catalog.addImage(context.caller(), context.correlationId(), id, body);

        return ResponseEntity.created(URI.create("/api/v1/products/" + id + "/images/" + image.id())).body(image);
    }

    @DeleteMapping("/api/v1/products/{id}/images/{imageId}")
    ResponseEntity<Void> removeImage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @PathVariable UUID imageId) {
        RequestContext context = requestContext.resolve(jwt);

        catalog.removeImage(context.caller(), context.correlationId(), id, imageId);

        return ResponseEntity.noContent().build();
    }

    // ---- categories ----

    @PostMapping("/api/v1/categories")
    ResponseEntity<CategoryView> createCategory(@AuthenticationPrincipal Jwt jwt, @RequestBody CategoryWrite body) {
        RequestContext context = requestContext.resolve(jwt);

        CategoryView category = catalog.createCategory(context.caller(), context.correlationId(), body);

        return ResponseEntity.created(URI.create("/api/v1/categories/" + category.id())).body(category);
    }

    @PatchMapping("/api/v1/categories/{id}")
    CategoryView updateCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestBody CategoryWrite body) {
        RequestContext context = requestContext.resolve(jwt);

        return catalog.updateCategory(context.caller(), context.correlationId(), id, body);
    }

    @DeleteMapping("/api/v1/categories/{id}")
    ResponseEntity<Void> deleteCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        RequestContext context = requestContext.resolve(jwt);

        catalog.deleteCategory(context.caller(), context.correlationId(), id);

        return ResponseEntity.noContent().build();
    }
}
