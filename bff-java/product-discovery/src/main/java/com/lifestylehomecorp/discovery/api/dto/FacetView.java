package com.lifestylehomecorp.discovery.api.dto;

import java.util.List;

/** Wire model for a computed facet: buckets for a distinct/ranges facet, or stats for a stats facet. */
public record FacetView(String name, String type, List<FacetBucketView> buckets, PriceStatsView stats) {
}
