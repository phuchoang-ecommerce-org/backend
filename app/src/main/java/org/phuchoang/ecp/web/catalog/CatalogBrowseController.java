package org.phuchoang.ecp.web.catalog;

import jakarta.servlet.http.HttpServletRequest;
import org.phuchoang.ecp.catalog.api.facade.CatalogBrowseFacade;
import org.phuchoang.ecp.catalog.api.query.CatalogListingQuery;
import org.phuchoang.ecp.catalog.api.view.category.CategoryNodeView;
import org.phuchoang.ecp.catalog.api.view.category.CategoryView;
import org.phuchoang.ecp.catalog.api.view.product.ProductPageView;
import org.phuchoang.ecp.catalog.api.view.product.VariantView;
import org.phuchoang.ecp.sharedkernel.api.error.FieldErrorCodes;
import org.phuchoang.ecp.web.error.FieldError;
import org.phuchoang.ecp.web.error.ValidationException;
import org.phuchoang.ecp.web.pagination.Page;
import org.phuchoang.ecp.web.pagination.PageEnvelope;
import org.phuchoang.ecp.web.pagination.Pagination;
import org.phuchoang.ecp.web.request.QueryParams;
import org.phuchoang.ecp.web.request.SortSpec;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Public, guest-readable Sprint 06 catalog endpoints. */
@RestController
class CatalogBrowseController {

  private static final Set<String> TREE_PARAMS = Set.of("rootId", "maxDepth");
  private static final Set<String> LISTING_PARAMS = Set.of("cursor", "size", "sort", "brand", "priceFrom", "priceTo",
      "inStock");
  private static final Set<String> VARIANT_PARAMS = Set.of("options");

  private final CatalogBrowseFacade catalog;

  CatalogBrowseController(CatalogBrowseFacade catalog) {
    this.catalog = catalog;
  }

  @GetMapping("/api/v1/categories")
  List<CategoryNodeView> listCategories(HttpServletRequest request,
      @RequestParam(required = false) UUID rootId, @RequestParam(required = false) Integer maxDepth) {
    QueryParams.rejectUnknown(request, TREE_PARAMS);
    if (maxDepth != null && maxDepth < 1) {
      throw invalid("maxDepth", "Must be at least 1.");
    }
    return catalog.listCategories(rootId, maxDepth);
  }

  @GetMapping("/api/v1/categories/{categoryId}")
  CategoryView getCategory(@PathVariable UUID categoryId) {
    return catalog.getCategory(categoryId);
  }

  @GetMapping("/api/v1/categories/{categoryId}/products")
  PageEnvelope<Object> listCategoryProducts(@PathVariable UUID categoryId, HttpServletRequest request,
      @RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sort, @RequestParam(name = "brand", required = false) List<String> brands,
      @RequestParam(required = false) BigDecimal priceFrom, @RequestParam(required = false) BigDecimal priceTo,
      @RequestParam(required = false) Boolean inStock) {
    QueryParams.rejectUnknown(request, LISTING_PARAMS);
    if (priceFrom != null && priceTo != null && priceFrom.compareTo(priceTo) > 0) {
      throw invalid("priceFrom", "Must not exceed priceTo.");
    }
    String resolvedSort = sort == null || sort.isBlank() ? "default"
        : toSort(SortSpec.parse(sort,
            Set.of("price", "createdAt", "popularity")));
    ProductPageView page = catalog.listCategoryProducts(categoryId,
        new CatalogListingQuery(cursor, Pagination.clampSize(size), resolvedSort,
            brands == null ? List.of() : List.copyOf(brands), priceFrom, priceTo, inStock));
    return new PageEnvelope<>(new ArrayList<>(page.items()),
        new Page(page.items().size(), page.nextCursor(), page.total()));
  }

  @GetMapping("/api/v1/products/{productId}/variants")
  List<VariantView> listProductVariants(@PathVariable UUID productId, HttpServletRequest request,
      @RequestParam(required = false) List<String> options) {
    QueryParams.rejectUnknown(request, VARIANT_PARAMS);
    return catalog.listProductVariants(productId, options(options));
  }

  @GetMapping("/api/v1/products/{productId}/variants/{variantId}")
  VariantView getProductVariant(@PathVariable UUID productId, @PathVariable UUID variantId) {
    return catalog.getProductVariant(productId, variantId);
  }

  private static String toSort(SortSpec sort) {
    return sort.field() + ":" + (sort.descending() ? "desc" : "asc");
  }

  private static Map<String, String> options(List<String> rawOptions) {
    if (rawOptions == null || rawOptions.isEmpty())
      return Map.of();
    Map<String, String> values = new LinkedHashMap<>();
    for (String raw : rawOptions) {
      int separator = raw.indexOf(':');
      if (separator <= 0 || separator == raw.length() - 1 || !raw.substring(0, separator).matches("[a-zA-Z0-9_-]+")) {
        throw invalid("options", "Each option must use name:value format.");
      }
      String previous = values.putIfAbsent(raw.substring(0, separator), raw.substring(separator + 1));
      if (previous != null)
        throw invalid("options", "An option dimension may be selected only once.");
    }
    return Map.copyOf(values);
  }

  private static ValidationException invalid(String field, String detail) {
    return new ValidationException(List.of(new FieldError(field, FieldErrorCodes.CONSTRAINT_VIOLATED, detail)));
  }
}
