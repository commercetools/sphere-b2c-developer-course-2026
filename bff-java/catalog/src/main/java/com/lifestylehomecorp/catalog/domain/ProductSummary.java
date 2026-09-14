package com.lifestylehomecorp.catalog.domain;

/**
 * Frontend-facing product model. A localized commercetools Product is flattened here to a
 * single resolved locale — no SDK types cross into the domain.
 *
 * @param key                    product key (prefer key over id everywhere)
 * @param name                   resolved display name
 * @param slug                   resolved URL slug
 * @param price                  the master variant's EFFECTIVE one-time price (discounted if a Product
 *                               Discount applies), or null if none is set
 * @param originalPrice          the pre-discount one-time price when a discount applies (for a
 *                               strikethrough), else null
 * @param recurringPrice         the effective recurrence-scoped ("Subscribe &amp; Save") price for the
 *                               requested currency, or null if the product has none
 * @param recurringOriginalPrice the pre-discount recurring price when a discount applies, else null
 * @param imageUrl               the master variant's first image URL, or null if none
 */
public record ProductSummary(String key, String name, String slug, Money price, Money originalPrice,
                             Money recurringPrice, Money recurringOriginalPrice, String imageUrl) {
}
