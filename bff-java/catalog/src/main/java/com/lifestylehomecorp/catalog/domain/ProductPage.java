package com.lifestylehomecorp.catalog.domain;

import java.util.List;

/**
 * A page of PLP product cards plus the {@code total} number of matching products across all pages
 * (Task 2.1 / 2.4). The grid renders {@link #products()} (this page); {@link #total()} drives the
 * storefront's "Total N products" and lets the Canvas verify the full match count — which stays
 * correct even when the server-side page limit is smaller than the total.
 */
public record ProductPage(List<ProductSummary> products, long total) {
}
