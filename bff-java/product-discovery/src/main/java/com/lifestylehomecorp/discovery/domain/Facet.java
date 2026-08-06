package com.lifestylehomecorp.discovery.domain;

import java.util.List;

/**
 * One computed facet returned alongside the search results. A bucketed facet (distinct or ranges)
 * carries {@code buckets}; a statistics facet carries {@code stats}. The unused side is {@code null}.
 *
 * @param name    the facet name given in the request (e.g. "colour", "price", "priceStats")
 * @param type    "distinct", "ranges" or "stats" — how the storefront should render it
 * @param buckets the buckets for a distinct/ranges facet, else {@code null}
 * @param stats   the statistics for a stats facet, else {@code null}
 */
public record Facet(String name, String type, List<FacetBucket> buckets, PriceStats stats) {
}
