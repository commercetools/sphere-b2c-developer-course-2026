package com.lifestylehomecorp.catalog.api.dto;

import java.util.List;

/** Wire model for a resolved bundle: components plus the rolled-up total price (Task 2.7). */
public record BundleView(String key, String name, boolean isBundle, MoneyView totalPrice, List<ProductView> components) {
}
