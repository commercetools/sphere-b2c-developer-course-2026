package com.lifestylehomecorp.discovery.api.dto;

/**
 * Response view model for a product card. {@code price} is the shopper's price (the sale price when
 * discounted); {@code originalPrice} is the pre-discount list price when a discount applies, else null.
 */
public record CardView(String key, String name, String slug, MoneyView price, MoneyView originalPrice,
                       String imageUrl) {
}
