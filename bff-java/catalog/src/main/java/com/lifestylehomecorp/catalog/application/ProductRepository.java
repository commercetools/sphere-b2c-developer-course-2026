package com.lifestylehomecorp.catalog.application;

import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponse;

import java.util.List;

/**
 * SDK gateway for products. Returns raw commercetools SDK types — the repository implementation is
 * the ONE place participants write SDK code (the SDK call only); {@link CatalogService} maps to the
 * domain. Lives in {@code application} (SDK-typed), keeping {@code domain} SDK-free.
 *
 * <p>The PLP/PDP reads use <b>Product Projections</b> (retrieval — the ready-to-render catalog
 * state). The {@link PriceSelection} parameters ({@code priceCurrency}/{@code priceCountry}/
 * {@code priceChannel}/{@code priceCustomerGroup}) populate the selected {@code price} on each
 * variant; the platform applies its precedence and falls back to the base currency price. (Full-text
 * search / faceting is the Product Search API, introduced later in the discovery session.)
 */
public interface ProductRepository {

    /**
     * Task 2.1 (T1) — list products for the PLP via Product Projections, with price selection. Returns
     * the raw SDK <em>paged</em> response: its {@code results} fill the grid and its {@code total} (the
     * count of all matching products, not just this page) drives the storefront's "Total N products".
     */
    ProductProjectionPagedQueryResponse findAll(PriceSelection price);

    /** Task 2.2 (T1) — fetch one product by key for the PDP, with price selection. */
    ProductProjection findByKey(String key, PriceSelection price);

    /**
     * Task 3.2 (T1) — fetch one product by key <em>in the context of a store</em> (in-store PDP),
     * with price selection. Uses the In-Store Product Projection endpoint: a product NOT in the
     * store's assortment surfaces as a commercetools 404 → {@code NotFoundException} → HTTP 404, so
     * an out-of-store PDP is a clean not-found rather than a leak of a product the store doesn't sell.
     */
    ProductProjection findByKeyInStore(String storeKey, String key, PriceSelection price);

    /**
     * Filter products to a set of category ids (a category + its subtree), with price selection.
     * Backs the category-filtered PLP (Task 2.4). Trainer-provided read; the category-subtree logic
     * lives in the service. Returns the paged response so the category PLP carries its {@code total} too.
     */
    ProductProjectionPagedQueryResponse findByCategory(List<String> categoryIds, PriceSelection price);

    /**
     * Find products whose {@code slug} in the given {@code locale} equals {@code slug} (0 or 1 result).
     * Backs slug routing (Task 2.5). Trainer-provided read; the <em>locale fallback chain</em> and the
     * 404-vs-fallback decision live in the service.
     */
    List<ProductProjection> findBySlug(String locale, String slug, PriceSelection price);

    /**
     * Fetch products by their ids (a bundle's component references), with price selection. Backs
     * bundle/composite resolution (Task 2.7); the expand-and-roll-up logic lives in the service.
     */
    List<ProductProjection> findByIds(List<String> ids, PriceSelection price);
}
