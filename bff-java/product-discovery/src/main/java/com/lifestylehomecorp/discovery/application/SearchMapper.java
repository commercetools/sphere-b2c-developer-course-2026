package com.lifestylehomecorp.discovery.application;

import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.lifestylehomecorp.discovery.domain.Facet;
import com.lifestylehomecorp.discovery.domain.FacetBucket;
import com.lifestylehomecorp.discovery.domain.Money;
import com.lifestylehomecorp.discovery.domain.PlpCard;
import com.lifestylehomecorp.discovery.domain.PlpResponse;
import com.lifestylehomecorp.discovery.domain.PriceStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The boundary where SDK types stop: it reads the raw {@link GraphQLResponse#getData()} (a nested
 * {@code Map}, as the SDK deserializes the GraphQL {@code data} object) and builds the SDK-free
 * domain {@link PlpResponse}. Trainer-provided; participants write only the SDK call in the
 * repository. Every read is null-safe — a missing or partial hit degrades to {@code null}/empty
 * rather than throwing.
 *
 * <p>Card price: the hydrated {@code masterVariant.price(...)} selector returns the shopper's
 * selected price. When it is {@code discounted}, the card shows the discounted (sale) price with
 * the list price as {@code originalPrice}; otherwise the list price and no original.
 */
final class SearchMapper {

    private SearchMapper() {
    }

    static PlpResponse toResponse(GraphQLResponse response) {
        // GraphQL returns HTTP 200 even on errors ({data:null, errors:[…]}), so the SDK does NOT throw.
        // Surface them — otherwise a bad field, a missing scope, or a query error collapses to a silent
        // empty result and the participant can't tell "no matches" from "the query failed".
        if (response != null && response.getErrors() != null && !response.getErrors().isEmpty()) {
            String messages = response.getErrors().stream()
                    .map(e -> e.getMessage())
                    .filter(m -> m != null && !m.isBlank())
                    .collect(java.util.stream.Collectors.joining("; "));
            throw new IllegalStateException("Product Search GraphQL error(s): " + messages);
        }
        Map<String, Object> search = asMap(asMap(dataOf(response)).get("productsSearch"));
        List<PlpCard> cards = new ArrayList<>();
        for (Object result : asList(search.get("results"))) {
            PlpCard card = toCard(asMap(result));
            if (card != null) {
                cards.add(card);
            }
        }
        List<Facet> facets = new ArrayList<>();
        for (Object facet : asList(search.get("facets"))) {
            facets.add(toFacet(asMap(facet)));
        }
        return new PlpResponse(cards, facets, asLong(search.get("total")),
                (int) asLong(search.get("offset")), (int) asLong(search.get("limit")));
    }

    static List<PlpCard> toCards(GraphQLResponse response) {
        return toResponse(response).cards();
    }

    // --- one search hit -> a hydrated product card ------------------------------------------------

    private static PlpCard toCard(Map<String, Object> result) {
        Map<String, Object> product = asMap(result.get("product"));
        if (product.isEmpty()) {
            return null; // a hit whose product could not be hydrated (deleted / not visible)
        }
        Map<String, Object> current = asMap(asMap(product.get("masterData")).get("current"));
        Map<String, Object> masterVariant = asMap(current.get("masterVariant"));
        Money[] prices = priceOf(masterVariant);
        return new PlpCard(
                asString(product.get("key")),
                asString(current.get("name")),
                asString(current.get("slug")),
                prices[0],
                prices[1],
                imageOf(masterVariant));
    }

    /** Returns {price, originalPrice}: the shopper's price and (when discounted) the pre-discount list price. */
    private static Money[] priceOf(Map<String, Object> masterVariant) {
        Map<String, Object> price = asMap(masterVariant.get("price"));
        if (!price.isEmpty()) {
            Money list = money(asMap(price.get("value")));
            Map<String, Object> discounted = asMap(price.get("discounted"));
            if (!discounted.isEmpty()) {
                return new Money[]{money(asMap(discounted.get("value"))), list}; // sale price + original
            }
            return new Money[]{list, null};
        }
        // Fallback selection (no currency in the request): first embedded price, no discount context.
        for (Object p : asList(masterVariant.get("prices"))) {
            Money m = money(asMap(asMap(p).get("value")));
            if (m != null) {
                return new Money[]{m, null};
            }
        }
        return new Money[]{null, null};
    }

    private static Money money(Map<String, Object> value) {
        if (value.isEmpty() || value.get("currencyCode") == null) {
            return null;
        }
        return new Money(asString(value.get("currencyCode")), asLong(value.get("centAmount")));
    }

    private static String imageOf(Map<String, Object> masterVariant) {
        for (Object image : asList(masterVariant.get("images"))) {
            String url = asString(asMap(image).get("url"));
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    // --- facets -----------------------------------------------------------------------------------

    private static Facet toFacet(Map<String, Object> facet) {
        String name = asString(facet.get("name"));
        if (facet.get("min") != null || facet.get("max") != null || facet.get("mean") != null) {
            PriceStats stats = new PriceStats(asLong(facet.get("min")), asLong(facet.get("max")),
                    asDouble(facet.get("mean")));
            return new Facet(name, "stats", null, stats);
        }
        List<FacetBucket> buckets = new ArrayList<>();
        for (Object bucket : asList(facet.get("buckets"))) {
            Map<String, Object> b = asMap(bucket);
            buckets.add(new FacetBucket(asString(b.get("key")), asLong(b.get("count"))));
        }
        return new Facet(name, "bucket", buckets, null);
    }

    // --- defensive JSON-ish readers ---------------------------------------------------------------

    private static Object dataOf(GraphQLResponse response) {
        return response == null ? null : response.getData();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static List<?> asList(Object value) {
        return value instanceof List<?> l ? l : List.of();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private static double asDouble(Object value) {
        return value instanceof Number n ? n.doubleValue() : 0d;
    }
}
