package com.lifestylehomecorp.catalog.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product.ProductProjection;
import com.lifestylehomecorp.catalog.application.PriceSelection;
import com.lifestylehomecorp.catalog.application.ProductRepository;
import com.lifestylehomecorp.platform.errors.TaskNotImplementedException;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SDK gateway for products — the ONE place participants write commercetools SDK code for the catalog
 * tasks. Each method is *only* the SDK fluent call, returning raw SDK types; the mapping to domain
 * happens in {@link com.lifestylehomecorp.catalog.application.CatalogService}. Ground every call on
 * the {@code commercetools-knowledge} MCP.
 *
 * <p>Uses <b>Product Projections</b> — the retrieval API for PLP/PDP (the ready-to-render catalog
 * state). Price selection ({@code priceCurrency}/{@code priceCountry}/{@code priceChannel}/
 * {@code priceCustomerGroup}) populates the selected {@code price} on each variant: the platform picks
 * the best-matching price by precedence (Customer Group &gt; Channel &gt; country) and falls back to
 * the base currency price. Apply <b>every</b> price parameter the request carried — a currency-only
 * selection returns an arbitrary regional/channel price. (Full-text search and faceting use the
 * Product Search API, introduced in the discovery session.)
 */
@Repository
public class CtProductRepository implements ProductRepository {

    private final ProjectApiRoot apiRoot;

    public CtProductRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public List<ProductProjection> findAll(PriceSelection price) {
        // TODO (Task 2.1): implement the Product Projections list SDK call (staged=false, server-side
        // limit) and apply price selection YOURSELF from `price` — add withPriceCurrency, then
        // withPriceCountry / withPriceChannel / withPriceCustomerGroup EACH only when that value is
        // present. Passing only currency returns an arbitrary (non-contextual) price. Return the raw
        // List<ProductProjection>. See the @TaskDescription hint + session-tasks-detailed.md; ground it
        // on the commercetools-knowledge MCP.
        throw new TaskNotImplementedException("2.1");
    }

    @Override
    public List<ProductProjection> findByCategory(List<String> categoryIds, PriceSelection price) {
        // Filter to the given category ids (a category + its subtree) via a query predicate; a
        // reference-array field matches if ANY element's id is in the set. Price selection as above.
        var request = apiRoot.productProjections()
                .get()
                .withStaged(false)
                .withLimit(100)
                .withWhere("categories(id in :ids)")
                .withPredicateVar("ids", categoryIds);
        if (price != null && !isBlank(price.currency())) {
            request = request.withPriceCurrency(price.currency());
            if (!isBlank(price.country())) request = request.withPriceCountry(price.country());
            if (!isBlank(price.channel())) request = request.withPriceChannel(price.channel());
            if (!isBlank(price.customerGroup())) request = request.withPriceCustomerGroup(price.customerGroup());
        }
        return request.executeBlocking().getBody().getResults();
    }

    @Override
    public List<ProductProjection> findBySlug(String locale, String slug, PriceSelection price) {
        // Match the localized slug for ONE locale: `slug(<locale> = :slug)`. The locale names the
        // LocalizedString field to test; the value is bound safely as a predicate var. Validate the
        // locale (it forms part of the predicate text) so it can't be used to inject a predicate.
        if (locale == null || !locale.matches("[a-zA-Z]{2}(-[a-zA-Z0-9]{2,8})?")) {
            return List.of();
        }
        var request = apiRoot.productProjections()
                .get()
                .withStaged(false)
                .withWhere("slug(" + locale + " = :slug)")
                .withPredicateVar("slug", slug);
        if (price != null && !isBlank(price.currency())) {
            request = request.withPriceCurrency(price.currency());
            if (!isBlank(price.country())) request = request.withPriceCountry(price.country());
            if (!isBlank(price.channel())) request = request.withPriceChannel(price.channel());
            if (!isBlank(price.customerGroup())) request = request.withPriceCustomerGroup(price.customerGroup());
        }
        return request.executeBlocking().getBody().getResults();
    }

    @Override
    public List<ProductProjection> findByIds(List<String> ids, PriceSelection price) {
        // Fetch a set of products by id in one query (a bundle's component references). An empty set
        // would make an invalid `id in ()` predicate, so short-circuit.
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        var request = apiRoot.productProjections()
                .get()
                .withStaged(false)
                .withLimit(ids.size())
                .withWhere("id in :ids")
                .withPredicateVar("ids", ids);
        if (price != null && !isBlank(price.currency())) {
            request = request.withPriceCurrency(price.currency());
            if (!isBlank(price.country())) request = request.withPriceCountry(price.country());
            if (!isBlank(price.channel())) request = request.withPriceChannel(price.channel());
            if (!isBlank(price.customerGroup())) request = request.withPriceCustomerGroup(price.customerGroup());
        }
        return request.executeBlocking().getBody().getResults();
    }

    @Override
    public ProductProjection findByKey(String key, PriceSelection price) {
        // TODO (Task 2.2): fetch one product projection BY KEY (not id), current data, and apply price
        // selection YOURSELF (same as 2.1 — each param only when present). Return the raw
        // ProductProjection. A commercetools 404 surfaces as a NotFoundException → clean HTTP 404 via
        // the platform error advice. See the @TaskDescription hint + session-tasks-detailed.md; ground
        // it on the commercetools-knowledge MCP.
        throw new TaskNotImplementedException("2.2");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
