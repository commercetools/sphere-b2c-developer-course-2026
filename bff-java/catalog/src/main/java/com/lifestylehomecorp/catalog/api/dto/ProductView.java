package com.lifestylehomecorp.catalog.api.dto;

/** Response view model for product endpoints. */
public record ProductView(String key, String name, String slug, MoneyView price, String imageUrl) {
}
