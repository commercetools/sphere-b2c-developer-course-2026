package com.lifestylehomecorp.discovery.domain;

/** One bucket of a facet — a value (e.g. a colour) and how many products carry it. */
public record FacetBucket(String key, long count) {
}
