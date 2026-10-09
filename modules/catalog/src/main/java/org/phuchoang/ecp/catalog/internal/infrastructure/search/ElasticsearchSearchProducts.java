package org.phuchoang.ecp.catalog.internal.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortMode;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.phuchoang.ecp.catalog.internal.application.productpricing.MoneyValue;
import org.phuchoang.ecp.catalog.internal.application.search.query.ActiveSearchFilter;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchFacet;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchProduct;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchProductPage;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchProductsPort;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchProductsQuery;
import org.phuchoang.ecp.catalog.internal.application.search.query.SearchStoreUnavailableException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Elasticsearch implementation of Catalog's keyword/filter/search read port. */
@Component
class ElasticsearchSearchProducts implements SearchProductsPort {

    private static final int FACET_SIZE = 50;

    private final ElasticsearchClient client;
    private final SearchProductsCursorCodec cursors;

    ElasticsearchSearchProducts(ElasticsearchClient client, SearchProductsCursorCodec cursors) {
        this.client = client;
        this.cursors = cursors;
    }

    @Override
    public SearchProductPage search(SearchProductsQuery query) {
        // Validation must remain a 400. Only a real Elasticsearch operation is a 503.
        List<FieldValue> searchAfter = cursors.decode(query);
        try {
            SearchResponse<Map> response = client.search(request -> {
                request.index(SearchIndex.ALIAS).size(query.size() + 1)
                    .query(productQuery(query));
                addFacets(request, query);
                addSort(request, query.sort());
                if (!searchAfter.isEmpty()) {
                    request.searchAfter(searchAfter);
                }
                return request;
            }, Map.class);
            List<Hit<Map>> hits = response.hits().hits();
            boolean hasMore = hits.size() > query.size();
            if (hasMore) {
                hits = hits.subList(0, query.size());
            }
            String nextCursor = hasMore ? cursors.encode(query, hits.getLast()) : null;
            return new SearchProductPage(hits.stream().map(ElasticsearchSearchProducts::product).toList(), nextCursor,
                matchCount(response.hits()), facets(response, query), activeFilters(query));
        } catch (Exception exception) {
            throw new SearchStoreUnavailableException("Elasticsearch product search failed.", exception);
        }
    }

    private static Query productQuery(SearchProductsQuery query) {
        BoolQuery.Builder filter = new BoolQuery.Builder();
        // Tombstones retain the event-order high-water mark but are never part of the product
        // read model. This state filter replaces physical deletion; publication is still enforced
        // at projection time rather than by a per-query publication-status predicate.
        filter.filter(term -> term.term(value -> value.field("documentType").value("PRODUCT")));
        if (query.keyword() == null) {
            filter.must(Query.of(match -> match.matchAll(all -> all)));
        } else {
            filter.must(Query.of(match -> match.bool(matches -> matches
                .should(term -> term.multiMatch(multi -> multi.query(query.keyword())
                    .fields("name^4", "brand^3", "description", "attributes.*")))
                .should(term -> term.nested(nested -> nested.path("variants")
                    .query(nestedQuery -> nestedQuery.match(matchSku -> matchSku.field("variants.sku").query(query.keyword())))))
                .minimumShouldMatch("1"))));
        }
        if (query.categoryId() != null) {
            filter.filter(term -> term.term(value -> value.field("categoryId").value(query.categoryId().toString())));
        }
        if (!query.brands().isEmpty()) {
            filter.filter(terms -> terms.terms(values -> values.field("brand")
                .terms(termsValues -> termsValues.value(query.brands().stream().map(FieldValue::of).toList()))));
        }
        query.attributes().forEach((name, value) -> filter.filter(term -> term.term(attribute -> attribute
            .field("attributes." + name).value(value))));
        if (query.priceFrom() != null || query.priceTo() != null || query.inStock() != null) {
            filter.filter(nested -> nested.nested(value -> value.path("variants").query(nestedQuery -> nestedQuery.bool(variant -> {
                if (query.priceFrom() != null || query.priceTo() != null) {
                    variant.filter(range -> range.range(price -> price.number(number -> {
                        number.field("variants.listPrice");
                        if (query.priceFrom() != null) number.gte(query.priceFrom().doubleValue());
                        if (query.priceTo() != null) number.lte(query.priceTo().doubleValue());
                        return number;
                    })));
                }
                if (query.inStock() != null) {
                    variant.filter(term -> term.term(stock -> stock.field("variants.inStock").value(query.inStock())));
                }
                return variant;
            }))));
        }
        return Query.of(value -> value.bool(filter.build()));
    }

    private static void addSort(co.elastic.clients.elasticsearch.core.SearchRequest.Builder request, String sort) {
        if ("relevance".equals(sort)) {
            request.sort(value -> value.score(score -> score.order(SortOrder.Desc)));
        } else if (sort.startsWith("price:")) {
            boolean ascending = sort.endsWith(":asc");
            request.sort(value -> value.field(field -> field.field("variants.listPrice")
                .order(ascending ? SortOrder.Asc : SortOrder.Desc)
                .mode(ascending ? SortMode.Min : SortMode.Max)
                .nested(nested -> nested.path("variants"))));
        } else if (sort.startsWith("rating:")) {
            request.sort(value -> value.field(field -> field.field("averageRating")
                .order(sort.endsWith(":asc") ? SortOrder.Asc : SortOrder.Desc)));
        } else {
            request.sort(value -> value.field(field -> field.field("publishedAt")
                .order(sort.endsWith(":asc") ? SortOrder.Asc : SortOrder.Desc)));
        }
        request.sort(value -> value.field(field -> field.field("productId").order(SortOrder.Asc)));
    }

    private static void addFacets(co.elastic.clients.elasticsearch.core.SearchRequest.Builder request, SearchProductsQuery query) {
        request.aggregations("brand", aggregation -> aggregation.terms(terms -> terms.field("brand").size(FACET_SIZE)));
        request.aggregations("price", aggregation -> aggregation.nested(nested -> nested.path("variants"))
            .aggregations("ranges", inner -> inner.range(range -> range.field("variants.listPrice").keyed(true)
                .ranges(List.of(
                    AggregationRange.of(value -> value.key("under-50").to(50d)),
                    AggregationRange.of(value -> value.key("50-100").from(50d).to(100d)),
                    AggregationRange.of(value -> value.key("100-250").from(100d).to(250d)),
                    AggregationRange.of(value -> value.key("250-plus").from(250d)))))));
        query.attributes().keySet().stream().sorted().forEach(name -> request.aggregations(attributeAggregation(name),
            aggregation -> aggregation.terms(terms -> terms.field("attributes." + name).size(FACET_SIZE))));
    }

    private static Long matchCount(HitsMetadata<Map> hits) {
        return hits.total() == null ? null : hits.total().value();
    }

    private static List<SearchFacet> facets(SearchResponse<Map> response, SearchProductsQuery query) {
        List<SearchFacet> result = new ArrayList<>();
        result.add(termsFacet("brand", response));
        result.add(priceFacet(response));
        query.attributes().keySet().stream().sorted()
            .forEach(name -> result.add(termsFacet("attribute:" + name, attributeAggregation(name), response)));
        return List.copyOf(result);
    }

    private static SearchFacet termsFacet(String field, SearchResponse<Map> response) {
        return termsFacet(field, field, response);
    }

    private static SearchFacet termsFacet(String field, String aggregationName, SearchResponse<Map> response) {
        var aggregate = response.aggregations().get(aggregationName);
        if (aggregate == null || !aggregate.isSterms()) {
            return new SearchFacet(field, List.of());
        }
        return new SearchFacet(field, aggregate.sterms().buckets().array().stream()
            .map(bucket -> new SearchFacet.Value(bucket.key().stringValue(), bucket.docCount())).toList());
    }

    private static SearchFacet priceFacet(SearchResponse<Map> response) {
        var aggregate = response.aggregations().get("price");
        if (aggregate == null || !aggregate.isNested()) {
            return new SearchFacet("price", List.of());
        }
        var ranges = aggregate.nested().aggregations().get("ranges");
        if (ranges == null || !ranges.isRange()) {
            return new SearchFacet("price", List.of());
        }
        return new SearchFacet("price", ranges.range().buckets().array().stream()
            .map(bucket -> new SearchFacet.Value(bucket.key(), bucket.docCount())).toList());
    }

    private static String attributeAggregation(String attributeName) {
        return "attribute_" + attributeName;
    }

    private static List<ActiveSearchFilter> activeFilters(SearchProductsQuery query) {
        List<ActiveSearchFilter> filters = new ArrayList<>();
        if (query.categoryId() != null) filters.add(new ActiveSearchFilter("categoryId", query.categoryId().toString()));
        query.brands().forEach(brand -> filters.add(new ActiveSearchFilter("brand", brand)));
        query.attributes().entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(attribute -> filters.add(new ActiveSearchFilter("attribute", attribute.getKey() + ":" + attribute.getValue())));
        if (query.priceFrom() != null) filters.add(new ActiveSearchFilter("priceFrom", query.priceFrom().toPlainString()));
        if (query.priceTo() != null) filters.add(new ActiveSearchFilter("priceTo", query.priceTo().toPlainString()));
        if (query.inStock() != null) filters.add(new ActiveSearchFilter("inStock", query.inStock().toString()));
        return List.copyOf(filters);
    }

    @SuppressWarnings("unchecked")
    private static SearchProduct product(Hit<Map> hit) {
        Map<String, Object> source = hit.source();
        if (source == null) {
            throw new IllegalStateException("Product search hit contains no source.");
        }
        List<Map<String, Object>> variants = ((List<Map<String, Object>>) source.getOrDefault("variants", List.of()));
        List<BigDecimal> prices = variants.stream().map(variant -> decimal(variant.get("listPrice"))).filter(value -> value != null)
            .sorted().toList();
        Boolean inStock = variants.stream().map(variant -> (Boolean) variant.get("inStock"))
            .filter(value -> value != null).anyMatch(Boolean::booleanValue) ? Boolean.TRUE
            : variants.stream().anyMatch(variant -> variant.containsKey("inStock")) ? Boolean.FALSE : null;
        String currency = variants.stream().map(variant -> (String) variant.get("currency")).filter(value -> value != null)
            .findFirst().orElse(null);
        MoneyValue from = prices.isEmpty() || currency == null ? null : new MoneyValue(prices.getFirst(), currency);
        MoneyValue to = prices.isEmpty() || currency == null ? null : new MoneyValue(prices.getLast(), currency);
        return new SearchProduct(UUID.fromString((String) source.get("productId")), (String) source.get("name"),
            (String) source.get("slug"), (String) source.get("brand"), (String) source.get("primaryImageUrl"), from, to,
            number(source.get("averageRating")), intValue(source.get("reviewCount")), inStock);
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) return null;
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }

    private static Double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
