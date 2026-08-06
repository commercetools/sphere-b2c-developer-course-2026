package com.lifestylehomecorp.discovery.api.dto;

import java.util.List;

/** Wire model for a PLP / faceted search response: the cards, the facets, and the paging window. */
public record PlpView(List<CardView> cards, List<FacetView> facets, long total, int offset, int limit) {
}
