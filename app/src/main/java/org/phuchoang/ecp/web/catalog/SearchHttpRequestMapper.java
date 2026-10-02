package org.phuchoang.ecp.web.catalog;

import jakarta.servlet.http.HttpServletRequest;
import org.phuchoang.ecp.catalog.api.search.SearchProductsQuery;
import org.phuchoang.ecp.web.common.error.FieldErrorCodes;
import org.phuchoang.ecp.web.common.error.FieldError;
import org.phuchoang.ecp.web.common.error.UnprocessableQueryException;
import org.phuchoang.ecp.web.common.error.ValidationException;
import org.phuchoang.ecp.web.common.pagination.Pagination;
import org.phuchoang.ecp.web.common.request.QueryParams;
import org.phuchoang.ecp.web.common.request.SortSpec;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** HTTP-only search parameter policy and normalization; Catalog receives an already valid query. */
@Component
class SearchHttpRequestMapper {

    private static final Set<String> PARAMETERS = Set.of("q", "cursor", "size", "sort", "categoryId", "brand",
        "attribute", "priceFrom", "priceTo", "inStock");

    SearchProductsQuery query(HttpServletRequest request, String keyword, String cursor, Integer size, String sort,
            UUID categoryId, List<String> brands, List<String> attributes, BigDecimal priceFrom, BigDecimal priceTo,
            Boolean inStock) {
        QueryParams.rejectUnknown(request, PARAMETERS);
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword != null && sort != null && !sort.isBlank()) {
            throw new UnprocessableQueryException("q and sort cannot be supplied together.");
        }
        PriceRange priceRange = PriceRange.normalized(priceFrom, priceTo);
        return new SearchProductsQuery(normalizedKeyword, cursor, Pagination.clampSize(size),
            resolveSort(normalizedKeyword, sort), categoryId, normalizedBrands(brands), attributes(attributes),
            priceRange.from(), priceRange.to(), inStock);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String normalized = keyword.trim();
        if (normalized.isEmpty()) throw invalid("q", "Must not be blank.");
        return normalized;
    }

    private static List<String> normalizedBrands(List<String> brands) {
        if (brands == null) return List.of();
        if (brands.stream().anyMatch(brand -> brand == null || brand.isBlank())) {
            throw invalid("brand", "Must not be blank.");
        }
        return brands.stream().map(String::trim).distinct().sorted().toList();
    }

    private static Map<String, String> attributes(List<String> rawAttributes) {
        if (rawAttributes == null || rawAttributes.isEmpty()) return Map.of();
        Map<String, String> values = new LinkedHashMap<>();
        for (String raw : rawAttributes) {
            int separator = raw.indexOf(':');
            if (separator <= 0 || separator == raw.length() - 1
                    || !raw.substring(0, separator).matches("[a-zA-Z0-9_-]+")) {
                throw invalid("attribute", "Each attribute must use name:value format.");
            }
            if (values.putIfAbsent(raw.substring(0, separator), raw.substring(separator + 1)) != null) {
                throw invalid("attribute", "An attribute may be selected only once.");
            }
        }
        return Map.copyOf(values);
    }

    private static String resolveSort(String keyword, String rawSort) {
        if (rawSort == null || rawSort.isBlank()) return keyword == null ? "createdAt:desc" : "relevance";
        SortSpec parsed = SortSpec.parse(rawSort, Set.of("price", "createdAt", "rating"));
        return parsed.field() + ":" + (parsed.descending() ? "desc" : "asc");
    }

    private static ValidationException invalid(String field, String detail) {
        return new ValidationException(List.of(new FieldError(field, FieldErrorCodes.CONSTRAINT_VIOLATED, detail)));
    }

    private record PriceRange(BigDecimal from, BigDecimal to) {

        private static PriceRange normalized(BigDecimal from, BigDecimal to) {
            return from != null && to != null && from.compareTo(to) > 0 ? new PriceRange(to, from)
                : new PriceRange(from, to);
        }
    }
}
