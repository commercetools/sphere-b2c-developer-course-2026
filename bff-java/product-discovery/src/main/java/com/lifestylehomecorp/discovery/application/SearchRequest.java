package com.lifestylehomecorp.discovery.application;

import java.util.List;

/**
 * A surface-agnostic description of one Product Search — everything the {@link SearchRepository}
 * needs to build the GraphQL document, and nothing SDK-specific. The controller/service assemble it
 * per endpoint (store-only for 3.1, +full-text for 3.3, +category for 3.4, +facets for 3.5, the full
 * PLP for 3.6/3.7) via {@link #builder()}; the repository translates it to one {@code productsSearch}
 * call.
 *
 * @param storeIds       resolved store ids for the {@code stores} scope (never empty for a scoped read)
 * @param locale         locale for localized name/slug, full-text language and facet labels
 * @param text           full-text query (also drives a fuzzy clause), or {@code null}
 * @param categoryId     category id for the {@code categoriesSubTree} filter, or {@code null}
 * @param withFacets     compute facets alongside the results
 * @param facetSpecs     the facets to compute (3.8 — derived from the product type); when empty and
 *                       {@code withFacets} is set, the builder falls back to 3.5's hardcoded three
 * @param colourFilters  selected colour keys applied as {@code postFilter} (after facet counts), empty if none
 * @param priceFrom      inclusive lower bound (minor units) for the {@code postFilter} price range, or {@code null}
 * @param priceTo        exclusive upper bound (minor units) for the {@code postFilter} price range, or {@code null}
 * @param sortField      Product Search sort field (e.g. {@code variants.prices.centAmount}), or {@code null}
 * @param sortAscending  sort direction when {@code sortField} is set
 * @param limit          page size
 * @param offset         page offset
 * @param price          resolved price selection (channel/customer-group as ids)
 */
public record SearchRequest(
        List<String> storeIds,
        String locale,
        String text,
        String categoryId,
        boolean withFacets,
        List<FacetSpec> facetSpecs,
        List<String> colourFilters,
        Long priceFrom,
        Long priceTo,
        String sortField,
        boolean sortAscending,
        int limit,
        int offset,
        PriceSelection price) {

    public boolean hasText() {
        return text != null && !text.isBlank();
    }

    /** True when the caller supplied an explicit facet set (3.8) — else the builder uses 3.5's default. */
    public boolean hasFacetSpecs() {
        return facetSpecs != null && !facetSpecs.isEmpty();
    }

    public boolean hasCategory() {
        return categoryId != null && !categoryId.isBlank();
    }

    public boolean hasPostFilter() {
        return (colourFilters != null && !colourFilters.isEmpty()) || priceFrom != null || priceTo != null;
    }

    public boolean hasSort() {
        return sortField != null && !sortField.isBlank();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder — endpoints set only the clauses they need; the rest default to "absent". */
    public static final class Builder {
        private List<String> storeIds = List.of();
        private String locale;
        private String text;
        private String categoryId;
        private boolean withFacets;
        private List<FacetSpec> facetSpecs = List.of();
        private List<String> colourFilters = List.of();
        private Long priceFrom;
        private Long priceTo;
        private String sortField;
        private boolean sortAscending = true;
        private int limit = 24;
        private int offset = 0;
        private PriceSelection price = PriceSelection.none();

        public Builder storeIds(List<String> storeIds) { this.storeIds = storeIds; return this; }
        public Builder locale(String locale) { this.locale = locale; return this; }
        public Builder text(String text) { this.text = text; return this; }
        public Builder categoryId(String categoryId) { this.categoryId = categoryId; return this; }
        public Builder withFacets(boolean withFacets) { this.withFacets = withFacets; return this; }
        public Builder facetSpecs(List<FacetSpec> facetSpecs) {
            this.facetSpecs = facetSpecs == null ? List.of() : facetSpecs;
            return this;
        }
        public Builder colourFilters(List<String> colourFilters) {
            this.colourFilters = colourFilters == null ? List.of() : colourFilters;
            return this;
        }
        public Builder priceRange(Long from, Long to) { this.priceFrom = from; this.priceTo = to; return this; }
        public Builder sort(String field, boolean ascending) {
            this.sortField = field; this.sortAscending = ascending; return this;
        }
        public Builder limit(int limit) { this.limit = limit; return this; }
        public Builder offset(int offset) { this.offset = offset; return this; }
        public Builder price(PriceSelection price) { this.price = price == null ? PriceSelection.none() : price; return this; }

        public SearchRequest build() {
            return new SearchRequest(storeIds, locale, text, categoryId, withFacets, facetSpecs,
                    colourFilters, priceFrom, priceTo, sortField, sortAscending, limit, offset, price);
        }
    }
}
