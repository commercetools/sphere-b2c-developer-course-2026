package com.lifestylehomecorp.discovery.api.dto;

/** Wire model for one facet bucket: a value and its product count. */
public record FacetBucketView(String key, long count) {
}
