package com.lifestylehomecorp.catalog.api.dto;

import java.util.List;

/**
 * PLP response envelope (Task 2.1 / 2.4): the product cards for this page plus {@code total} — the
 * count of all matching products, which the storefront shows as "Total N products".
 */
public record ProductPageView(List<ProductView> products, long total) {
}
