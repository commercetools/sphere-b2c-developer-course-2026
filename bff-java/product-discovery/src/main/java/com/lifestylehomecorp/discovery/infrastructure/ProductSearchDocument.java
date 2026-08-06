package com.lifestylehomecorp.discovery.infrastructure;

import com.commercetools.api.models.graph_ql.GraphQLRequest;
import com.commercetools.api.models.graph_ql.GraphQLVariablesMapBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifestylehomecorp.discovery.application.FacetSpec;
import com.lifestylehomecorp.discovery.application.PriceSelection;
import com.lifestylehomecorp.discovery.application.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles the Product Search ({@code productsSearch}) GraphQL request from a {@link SearchRequest}.
 * Trainer-provided plumbing kept out of the repository so the SDK call there stays a one-liner.
 *
 * <p>The query <em>text</em> is a CONSTANT carrying typed variables ({@code $query}, {@code $postFilter},
 * {@code $sort}, {@code $facets}, the price selector, …); the dynamic search tree is built as an
 * immutable {@link SearchExpr} object graph and passed as the variable <em>values</em>. No caller value
 * is ever concatenated into the query string, so free-text and filters cannot alter its structure. Every
 * shape is grounded on the commercetools GraphQL schema and validated with
 * {@code commercetools-graphql-validate}.
 *
 * <p>{@link #build} also logs the built document and its variables at INFO — a training walkthrough aid.
 */
final class ProductSearchDocument {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchDocument.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String COLOUR_FIELD = "variants.attributes.search-color.key";
    private static final String PRICE_FIELD = "variants.prices.centAmount";
    private static final String DEFAULT_LOCALE = "en-US";

    /** Result envelope + hit hydration, shared by both documents (differs only in the price selection). */
    private static final String RESULTS_HEAD = """
              productsSearch(query: $query, postFilter: $postFilter, sort: $sort, facets: $facets, limit: $limit, offset: $offset) {
                total offset limit
                results { id product { key masterData { current {
                  name(locale: $locale) slug(locale: $locale)
                  masterVariant { sku images { url }
            """;

    private static final String RESULTS_TAIL = """
                  } } } } }
                facets { name
                  ... on ProductSearchFacetResultBucket { buckets { key count } }
                  ... on ProductSearchFacetResultStats { min max mean }
                }
              }
            }""";

    /** With a currency: hydrate the shopper's selected price via the price(...) selector (variables). */
    private static final String DOC_WITH_PRICE =
            "query Search($query: SearchQueryInput, $postFilter: SearchQueryInput, "
                    + "$sort: [SearchSortingInput!], $facets: [ProductSearchFacetExpressionInput!], "
                    + "$limit: Int, $offset: Int, $locale: Locale, "
                    + "$currency: Currency!, $country: Country, $channelId: String, $customerGroupId: String) {\n"
                    + RESULTS_HEAD
                    + "        price(currency: $currency, country: $country, channelId: $channelId, customerGroupId: $customerGroupId) {\n"
                    + "          value { currencyCode centAmount }\n"
                    + "          discounted { value { currencyCode centAmount } }\n"
                    + "        }\n"
                    + RESULTS_TAIL;

    /** Without a currency: fall back to the raw embedded prices (no price-selector variables declared). */
    private static final String DOC_EMBEDDED =
            "query Search($query: SearchQueryInput, $postFilter: SearchQueryInput, "
                    + "$sort: [SearchSortingInput!], $facets: [ProductSearchFacetExpressionInput!], "
                    + "$limit: Int, $offset: Int, $locale: Locale) {\n"
                    + RESULTS_HEAD
                    + "        prices { value { currencyCode centAmount } }\n"
                    + RESULTS_TAIL;

    private ProductSearchDocument() {
    }

    static GraphQLRequest build(SearchRequest r) {
        PriceSelection price = r.price();
        boolean withPrice = price != null && notBlank(price.currency());

        GraphQLVariablesMapBuilder vars = GraphQLVariablesMapBuilder.of()
                .addValue("limit", r.limit())
                .addValue("offset", r.offset())
                .addValue("locale", localeOf(r));

        SearchExpr query = queryExpr(r);
        if (query != null) {
            vars.addValue("query", query.toValue());
        }
        SearchExpr postFilter = postFilterExpr(r);
        if (postFilter != null) {
            vars.addValue("postFilter", postFilter.toValue());
        }
        if (r.withFacets()) {
            vars.addValue("facets", facets(r));
        }
        if (r.hasSort()) {
            vars.addValue("sort", List.of(Map.of(
                    "field", r.sortField(), "order", r.sortAscending() ? "asc" : "desc")));
        }
        if (withPrice) {
            vars.addValue("currency", price.currency());
            if (notBlank(price.country())) {
                vars.addValue("country", price.country());
            }
            if (notBlank(price.channel())) {
                vars.addValue("channelId", price.channel());
            }
            if (notBlank(price.customerGroup())) {
                vars.addValue("customerGroupId", price.customerGroup());
            }
        }
        GraphQLRequest request = GraphQLRequest.builder()
                .query(withPrice ? DOC_WITH_PRICE : DOC_EMBEDDED)
                .variables(vars.build())
                .build();
        logForWalkthrough(request);
        return request;
    }

    /**
     * Logs the exact GraphQL document and the variable values the model produced — a teaching aid so the
     * built query can be walked through in training. Logged at INFO so it surfaces on a normal run; a
     * serialisation hiccup degrades to a placeholder and never breaks the search.
     */
    private static void logForWalkthrough(GraphQLRequest request) {
        if (!log.isInfoEnabled()) {
            return;
        }
        String variables;
        try {
            variables = JSON.writerWithDefaultPrettyPrinter().writeValueAsString(request.getVariables());
        } catch (Exception e) {
            variables = "<unserialisable: " + e.getMessage() + ">";
        }
        log.info("Product Search built via expression model\n--- query ---\n{}\n--- variables ---\n{}",
                request.getQuery(), variables);
    }

    /** store scope + (optional) full-text/fuzzy + (optional) category subtree. */
    private static SearchExpr queryExpr(SearchRequest r) {
        List<SearchExpr> clauses = new ArrayList<>();
        if (r.storeIds() != null && !r.storeIds().isEmpty()) {
            List<SearchExpr> stores = new ArrayList<>();
            for (String id : r.storeIds()) {
                stores.add(new SearchExpr.Exact("stores", id, null, null));
            }
            clauses.add(SearchExpr.or(stores));
        }
        if (r.hasText()) {
            String lang = blankToNull(r.locale());
            clauses.add(SearchExpr.or(List.of(
                    new SearchExpr.FullText("name", r.text(), lang),
                    new SearchExpr.Fuzzy("name", r.text(), 1, lang))));
        }
        if (r.hasCategory()) {
            clauses.add(new SearchExpr.Filter(List.of(
                    new SearchExpr.Exact("categoriesSubTree", r.categoryId(), null, null))));
        }
        return SearchExpr.and(clauses);
    }

    /** selected facet filters (colour + price range), applied after facet counts. */
    private static SearchExpr postFilterExpr(SearchRequest r) {
        if (!r.hasPostFilter()) {
            return null;
        }
        List<SearchExpr> clauses = new ArrayList<>();
        if (r.colourFilters() != null && !r.colourFilters().isEmpty()) {
            List<SearchExpr> colours = new ArrayList<>();
            for (String value : r.colourFilters()) {
                colours.add(new SearchExpr.Exact(COLOUR_FIELD, value, "lenum", null));
            }
            clauses.add(SearchExpr.or(colours));
        }
        if (r.priceFrom() != null || r.priceTo() != null) {
            clauses.add(SearchExpr.longRange(PRICE_FIELD, r.priceFrom(), r.priceTo()));
        }
        return SearchExpr.and(clauses);
    }

    // --- facets -----------------------------------------------------------------------------------

    private static List<Map<String, Object>> facets(SearchRequest r) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (r.hasFacetSpecs()) {
            for (FacetSpec spec : r.facetSpecs()) {
                out.add(facet(spec));
            }
            return out;
        }
        out.add(distinct("colour", COLOUR_FIELD, "lenum", blankToNull(r.locale())));
        out.add(priceRanges("price", PRICE_FIELD));
        out.add(stats("priceStats", PRICE_FIELD));
        return out;
    }

    private static Map<String, Object> facet(FacetSpec spec) {
        return switch (spec.kind()) {
            case DISTINCT -> distinct(spec.name(), spec.field(), blankToNull(spec.fieldType()),
                    blankToNull(spec.language()));
            case RANGES -> priceRanges(spec.name(), spec.field());
            case STATS -> stats(spec.name(), spec.field());
        };
    }

    private static Map<String, Object> distinct(String name, String field, String fieldType, String language) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("name", name);
        d.put("field", field);
        if (fieldType != null) {
            d.put("fieldType", fieldType);
        }
        if (language != null) {
            d.put("language", language);
        }
        return Map.of("distinct", d);
    }

    private static Map<String, Object> priceRanges(String name, String field) {
        List<Map<String, Object>> ranges = List.of(
                Map.of("from", 0L, "to", 5000L),
                Map.of("from", 5000L, "to", 20000L),
                Map.of("from", 20000L, "to", 100000L));
        return Map.of("ranges", Map.of("name", name, "field", field, "ranges", Map.of("long", ranges)));
    }

    private static Map<String, Object> stats(String name, String field) {
        return Map.of("stats", Map.of("name", name, "field", field));
    }

    // --- helpers ----------------------------------------------------------------------------------

    private static String localeOf(SearchRequest r) {
        return blankToNull(r.locale()) == null ? DEFAULT_LOCALE : r.locale();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
