package com.lifestylehomecorp.catalog.domain;

import java.util.List;

/**
 * Frontend-facing category model: key plus a resolved name and slug, and its direct
 * subcategories ({@code children}) so the storefront can render a category → subcategory bar.
 * Top-level categories carry their children; leaf categories have an empty list.
 */
public record CategorySummary(String key, String name, String slug, List<CategorySummary> children) {
}
