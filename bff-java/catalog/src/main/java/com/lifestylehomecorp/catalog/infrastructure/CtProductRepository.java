package com.lifestylehomecorp.catalog.infrastructure;

import com.commercetools.api.client.ByProjectKeyInStoreKeyByStoreKeyProductProjectionsKeyByKeyGet;
import com.commercetools.api.client.ByProjectKeyProductProjectionsGet;
import com.commercetools.api.client.ByProjectKeyProductProjectionsKeyByKeyGet;
import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductProjectionPagedQueryResponse;
import com.lifestylehomecorp.catalog.application.PriceSelection;
import com.lifestylehomecorp.catalog.application.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SDK gateway for products — the ONE place participants write commercetools SDK code for the catalog
 * tasks. Each method is *only* the SDK fluent call, returning raw SDK types; the mapping to domain
 * happens in {@link com.lifestylehomecorp.catalog.application.CatalogService}. Ground every call on
 * the {@code commercetools-knowledge} MCP.
 *
 * <p>Uses <b>Product Projections</b> — the retrieval API for PLP/PDP (the ready-to-render catalog
 * state). {@linkplain PriceSelection Price selection} ({@code priceCurrency}/{@code priceCountry}/
 * {@code priceChannel}/{@code priceCustomerGroup}) populates the selected {@code price} on each
 * variant: the platform picks the best-matching price by precedence (Customer Group &gt; Channel &gt;
 * country) and falls back to the base currency price, so cards/PDP show the shopper's contextual
 * price. (Full-text search and faceting use the Product Search API, introduced in the discovery
 * session.)
 */
@Repository
public class CtProductRepository implements ProductRepository {

    private final ProjectApiRoot apiRoot;

    public CtProductRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public ProductProjectionPagedQueryResponse findAll(PriceSelection price) {
        // Product Projections list: staged=false → current (published) data; limit pages SERVER-SIDE.
        // Return the raw paged response — getBody() carries both results (this page) and total (all
        // matching products), so the PLP can show "Total N products" even when the page is smaller.
        var request = withPrice(apiRoot.productProjections().get().withStaged(false).withLimit(20), price);
        return request.executeBlocking().getBody();
    }

    @Override
    public ProductProjectionPagedQueryResponse findByCategory(List<String> categoryIds, PriceSelection price) {
        // Filter to the given category ids (a category + its subtree) via a query predicate; a
        // reference-array field matches if ANY element's id is in the set. Price selection as above.
        // Paged response so the category PLP carries its total too.
        var request = withPrice(apiRoot.productProjections()
                .get()
                .withStaged(false)
                .withLimit(100)
                .withWhere("categories(id in :ids)")
                .withPredicateVar("ids", categoryIds), price);
        return request.executeBlocking().getBody();
    }

    @Override
    public List<ProductProjection> findBySlug(String locale, String slug, PriceSelection price) {
        // Match the localized slug for ONE locale: `slug(<locale> = :slug)`. The locale names the
        // LocalizedString field to test; the value is bound safely as a predicate var. Validate the
        // locale (it forms part of the predicate text) so it can't be used to inject a predicate.
        if (locale == null || !locale.matches("[a-zA-Z]{2}(-[a-zA-Z0-9]{2,8})?")) {
            return List.of();
        }
        var request = withPrice(apiRoot.productProjections()
                .get()
                .withStaged(false)
                .withWhere("slug(" + locale + " = :slug)")
                .withPredicateVar("slug", slug), price);
        return request.executeBlocking().getBody().getResults();
    }

    @Override
    public List<ProductProjection> findByIds(List<String> ids, PriceSelection price) {
        // Fetch a set of products by id in one query (a bundle's component references). An empty set
        // would make an invalid `id in ()` predicate, so short-circuit.
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        var request = withPrice(apiRoot.productProjections()
                .get()
                .withStaged(false)
                .withLimit(ids.size())
                .withWhere("id in :ids")
                .withPredicateVar("ids", ids), price);
        return request.executeBlocking().getBody().getResults();
    }

    @Override
    public ProductProjection findByKey(String key, PriceSelection price) {
        // Fetch one product projection BY KEY (not id), current data + price selection. A commercetools
        // 404 surfaces as a NotFoundException, which the platform error advice maps to a clean HTTP 404.
        var request = withPrice(apiRoot.productProjections().withKey(key).get().withStaged(false), price);
        return request.executeBlocking().getBody();
    }

    @Override
    public ProductProjection findByKeyInStore(String storeKey, String key, PriceSelection price) {
        // In-Store Product Projection BY KEY: scoped to the store's assortment. A product not in the
        // store surfaces as a NotFoundException → HTTP 404 via the platform error advice. Price
        // selection as above; withStaged(false) → current (published) data.
        var request = withPrice(apiRoot.inStore(storeKey).productProjections().withKey(key).get()
                .withStaged(false), price);
        return request.executeBlocking().getBody();
    }

    // --- price selection helpers (trainer-provided) ------------------------------------------------
    // Apply each price-selection parameter only when present. commercetools then picks the best match
    // by precedence and falls back to the base currency price. Two overloads because the list ("get")
    // and by-key requests are distinct generated SDK builder types.

    private static ByProjectKeyProductProjectionsGet withPrice(
            ByProjectKeyProductProjectionsGet request, PriceSelection price) {
        if (price == null || isBlank(price.currency())) {
            return request; // no currency → no price selection at all
        }
        request = request.withPriceCurrency(price.currency());
        if (!isBlank(price.country())) {
            request = request.withPriceCountry(price.country());
        }
        if (!isBlank(price.channel())) {
            request = request.withPriceChannel(price.channel()); // channel id (resolved upstream)
        }
        if (!isBlank(price.customerGroup())) {
            request = request.withPriceCustomerGroup(price.customerGroup()); // customer-group id
        }
        return request;
    }

    private static ByProjectKeyProductProjectionsKeyByKeyGet withPrice(
            ByProjectKeyProductProjectionsKeyByKeyGet request, PriceSelection price) {
        if (price == null || isBlank(price.currency())) {
            return request;
        }
        request = request.withPriceCurrency(price.currency());
        if (!isBlank(price.country())) {
            request = request.withPriceCountry(price.country());
        }
        if (!isBlank(price.channel())) {
            request = request.withPriceChannel(price.channel());
        }
        if (!isBlank(price.customerGroup())) {
            request = request.withPriceCustomerGroup(price.customerGroup());
        }
        return request;
    }

    private static ByProjectKeyInStoreKeyByStoreKeyProductProjectionsKeyByKeyGet withPrice(
            ByProjectKeyInStoreKeyByStoreKeyProductProjectionsKeyByKeyGet request, PriceSelection price) {
        if (price == null || isBlank(price.currency())) {
            return request;
        }
        request = request.withPriceCurrency(price.currency());
        if (!isBlank(price.country())) {
            request = request.withPriceCountry(price.country());
        }
        if (!isBlank(price.channel())) {
            request = request.withPriceChannel(price.channel());
        }
        if (!isBlank(price.customerGroup())) {
            request = request.withPriceCustomerGroup(price.customerGroup());
        }
        return request;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
