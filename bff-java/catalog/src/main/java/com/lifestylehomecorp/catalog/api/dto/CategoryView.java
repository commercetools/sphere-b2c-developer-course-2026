package com.lifestylehomecorp.catalog.api.dto;

import java.util.List;

/** Response view model for category endpoints; {@code children} are the direct subcategories. */
public record CategoryView(String key, String name, String slug, List<CategoryView> children) {
}
