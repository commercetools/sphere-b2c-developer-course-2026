package com.lifestylehomecorp.discovery.domain;

/**
 * A product card for a listing/search result — the hydrated projection of one Product Search hit.
 *
 * @param key           the product key (stable, non-localized)
 * @param name          localized name resolved to the requested locale
 * @param slug          localized slug resolved to the requested locale
 * @param price         the shopper's selected price (channel + country + currency); the SALE price
 *                      when a discount applies, else the list price
 * @param originalPrice the pre-discount list price when {@code price} is discounted, else {@code null}
 * @param imageUrl      the master variant's first image, or {@code null}
 */
public record PlpCard(String key, String name, String slug, Money price, Money originalPrice, String imageUrl) {
}
