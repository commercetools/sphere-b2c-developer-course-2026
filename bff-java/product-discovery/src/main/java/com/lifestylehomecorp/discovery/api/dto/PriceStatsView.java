package com.lifestylehomecorp.discovery.api.dto;

/** Wire model for a numeric (price) facet's statistics. */
public record PriceStatsView(long min, long max, double mean) {
}
