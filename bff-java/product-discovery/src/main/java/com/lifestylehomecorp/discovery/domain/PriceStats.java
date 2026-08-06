package com.lifestylehomecorp.discovery.domain;

/** Aggregate statistics for a numeric (price) facet — minor units for min/max, mean as a double. */
public record PriceStats(long min, long max, double mean) {
}
