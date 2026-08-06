package com.lifestylehomecorp.discovery.application;

/**
 * A surface-agnostic description of ONE facet to compute — the derived, config-driven counterpart to
 * 3.5's three hardcoded facets. Task 3.8 ({@link SearchService#facetConfig}) turns a product type's
 * searchable {@code AttributeDefinition}s into a list of these, and {@code ProductSearchDocument} emits
 * each one into the {@code productsSearch} {@code facets} argument. Trainer-provided plumbing: the
 * <em>judgement</em> (which attributes, which mapping, the curation policy) lives in the service; the
 * shape here is deliberately dumb.
 *
 * @param name      the facet name echoed back on the result (e.g. "colour", "priceStats")
 * @param field     the search field to facet on (e.g. {@code variants.attributes.search-color.key})
 * @param fieldType the {@code fieldType} enum the API needs for a {@code distinct} facet on an
 *                  attribute keyword field ({@code enum}/{@code lenum}); ignored for {@code stats}/
 *                  {@code ranges}, may be {@code null}. (Carried as a {@code String}; see the deferred
 *                  {@code SearchFieldType}-enum improvement noted on {@code SearchExpr} — it would make
 *                  this compile-time safe for Task 3.8.)
 * @param kind      how to compute it — {@link Kind#DISTINCT} (bucket per value), {@link Kind#RANGES}
 *                  (bucket per numeric range), {@link Kind#STATS} (min/max/mean)
 * @param language  locale for a localized-enum ({@code lenum}) {@code distinct} facet's labels; {@code
 *                  null} for a plain {@code enum} or a numeric facet
 */
public record FacetSpec(String name, String field, String fieldType, Kind kind, String language) {

    /** The three facet shapes the builder knows how to emit (mirrors the Product Search facet types). */
    public enum Kind {DISTINCT, RANGES, STATS}

    /** A {@code distinct} facet (one bucket per value) — enum/localized-enum attribute keyword fields. */
    public static FacetSpec distinct(String name, String field, String fieldType, String language) {
        return new FacetSpec(name, field, fieldType, Kind.DISTINCT, language);
    }

    /** A {@code ranges} facet (one bucket per numeric range) — used for the curated price buckets. */
    public static FacetSpec ranges(String name, String field) {
        return new FacetSpec(name, field, null, Kind.RANGES, null);
    }

    /** A {@code stats} facet (min/max/mean) — numeric attributes and the price. */
    public static FacetSpec stats(String name, String field) {
        return new FacetSpec(name, field, null, Kind.STATS, null);
    }
}
