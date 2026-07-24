package com.lifestylehomecorp.catalog.domain;

/**
 * Frontend-facing product model. A localized commercetools Product is flattened here to a
 * single resolved locale — no SDK types cross into the domain.
 *
 * @param key      product key (prefer key over id everywhere)
 * @param name     resolved display name
 * @param slug     resolved URL slug
 * @param price    the master variant's price, or null if none is set
 * @param imageUrl the master variant's first image URL, or null if none
 */
public record ProductSummary(String key, String name, String slug, Money price, String imageUrl) {
}
