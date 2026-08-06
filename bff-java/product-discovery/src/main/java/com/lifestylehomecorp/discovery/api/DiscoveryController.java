package com.lifestylehomecorp.discovery.api;

import com.lifestylehomecorp.discovery.api.dto.CardView;
import com.lifestylehomecorp.discovery.api.dto.PlpView;
import com.lifestylehomecorp.discovery.application.PriceSelection;
import com.lifestylehomecorp.discovery.application.SearchService;
import com.lifestylehomecorp.platform.annotations.TaskDescription;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Store-scoped discovery HTTP contract (Session 3). Every endpoint is ONE Product Search call: the
 * search is scoped to a store, filtered / faceted / sorted / paged, and each hit is hydrated
 * (name/slug/price/image) in the same GraphQL response — no second retrieval call, no N+1.
 *
 * <p>Conventions: {@code store} is a store <em>key</em> (resolved to its id for the {@code stores}
 * scope); {@code category} is a category <em>key</em> (resolved to its id for the {@code
 * categoriesSubTree} filter, cached — the storefront nav carries category keys); the five price
 * parameters carry the shopper's price context (currency + country + the store's distribution channel
 * by key, resolved to an id, + customer group after login).
 */
@RestController
public class DiscoveryController {

    private final SearchService searchService;

    public DiscoveryController(SearchService searchService) {
        this.searchService = searchService;
    }

    /** Assemble the price-selection context from the request (see catalog's identical helper). */
    private static PriceSelection price(String currency, String country, String channel, String customerGroup) {
        return new PriceSelection(currency, country, channel, customerGroup);
    }

    @TaskDescription(
            module = "product-discovery", session = "Session 3", taskNumber = 1,
            title = "Store-scoped search", tier = "T1", capability = "search.query",
            description = "Implement one repository method — SearchRepository.search(SearchRequest) in the "
                    + "product-discovery module's infrastructure layer — building ONE Product Search "
                    + "(productsSearch) GraphQL call and returning the raw SDK GraphQLResponse. The search "
                    + "must be scoped to the shopper's store and must hydrate each hit's catalog Product — "
                    + "name/slug/image + the product's price for the shopper's context — in the SAME call, so "
                    + "GET /api/products/search returns ready-to-render cards with no second call and no "
                    + "N+1. SearchService resolves the store and channel keys to ids; the mapper parses the "
                    + "response into cards.",
            hint = "Docs: Product Search — scope by Store and hydrate the Product in one query "
                    + "(docs.commercetools.com/api/projects/product-search).")
    @GetMapping("/api/products/search")
    public List<CardView> search(@RequestParam(required = false) String store,
                                 @RequestParam(required = false) String locale,
                                 @RequestParam(required = false) String priceCurrency,
                                 @RequestParam(required = false) String priceCountry,
                                 @RequestParam(required = false) String priceChannel,
                                 @RequestParam(required = false) String priceCustomerGroup) {
        return searchService.search(store, locale,
                        price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup))
                .stream().map(DiscoveryViewMapper::toView).toList();
    }

    @TaskDescription(
            module = "product-discovery", session = "Session 3", taskNumber = 3,
            title = "Full-text search", tier = "T1", capability = "search.fullText",
            description = "Implement SearchService.fullText(...) in the product-discovery module's "
                    + "application layer — extend the store-scoped Product Search with a full-text "
                    + "(typo-tolerant) clause so a keyword query returns store-scoped, relevance-ranked "
                    + "results. The repository and price hydration are unchanged (reuse the 3.1 search "
                    + "call), so GET /api/search?store=&q= returns relevance-ranked cards for the search "
                    + "box in one call.",
            hint = "Docs: Product Search — full-text and fuzzy expressions "
                    + "(docs.commercetools.com/api/search-query-language#full-text).")
    @GetMapping("/api/search")
    public List<CardView> fullTextSearch(@RequestParam(required = false) String store,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) String locale,
                                         @RequestParam(required = false) String priceCurrency,
                                         @RequestParam(required = false) String priceCountry,
                                         @RequestParam(required = false) String priceChannel,
                                         @RequestParam(required = false) String priceCustomerGroup) {
        return searchService.fullText(store, q, locale,
                        price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup))
                .stream().map(DiscoveryViewMapper::toView).toList();
    }

    @TaskDescription(
            module = "product-discovery", session = "Session 3", taskNumber = 4,
            title = "Browse by category (subtree)", tier = "T1", capability = "search.byCategory",
            description = "Implement SearchService.byCategory(...) in the product-discovery module's "
                    + "application layer — add a category filter to the store-scoped Product Search so a "
                    + "parent category includes its whole subtree (the same 'parent includes descendants' "
                    + "requirement 2.4 met with a query predicate, now in the search engine). GET "
                    + "/api/products/search?store=&category= returns the category's cards in one call.",
            hint = "Docs: Product Search — filter by Category subtree with categoriesSubTree "
                    + "(docs.commercetools.com/api/projects/product-search#filter-by-category-subtree).")
    @GetMapping(value = "/api/products/search", params = "category")
    public List<CardView> searchByCategory(@RequestParam String category,
                                           @RequestParam(required = false) String store,
                                           @RequestParam(required = false) String locale,
                                           @RequestParam(required = false) String priceCurrency,
                                           @RequestParam(required = false) String priceCountry,
                                           @RequestParam(required = false) String priceChannel,
                                           @RequestParam(required = false) String priceCustomerGroup) {
        return searchService.byCategory(store, category, locale,
                        price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup))
                .stream().map(DiscoveryViewMapper::toView).toList();
    }

    @TaskDescription(
            module = "product-discovery", session = "Session 3", taskNumber = 5,
            title = "Faceted search", tier = "T1", capability = "search.facets",
            description = "Implement SearchService.facets(...) in the product-discovery module's "
                    + "application layer — add facets to the store-scoped Product Search (a distinct facet "
                    + "on colour and range/stats facets on price) and return the computed facets alongside "
                    + "the results, so GET /api/products/facets returns BOTH the cards and the facet "
                    + "buckets/stats for the storefront's filter rail from one call.",
            hint = "Docs: Product Search — facets (distinct, ranges, stats) "
                    + "(docs.commercetools.com/api/projects/product-search#facets).")
    @GetMapping("/api/products/facets")
    public PlpView facets(@RequestParam(required = false) String store,
                          @RequestParam(required = false) String locale,
                          @RequestParam(required = false) String priceCurrency,
                          @RequestParam(required = false) String priceCountry,
                          @RequestParam(required = false) String priceChannel,
                          @RequestParam(required = false) String priceCustomerGroup) {
        return DiscoveryViewMapper.toView(searchService.facets(store, locale,
                price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup)));
    }

    @TaskDescription(
            module = "product-discovery", session = "Session 3", taskNumber = 8,
            title = "Configurable facets", tier = "T2", capability = "search.facetsConfig",
            description = "Implement SearchService.facetConfig(...) in the product-discovery module's "
                    + "application layer — instead of 3.5's hardcoded three facets, DERIVE the facet set "
                    + "from the product types' SEARCHABLE attributes under a curation policy, so adding a "
                    + "searchable attribute (e.g. material, rating) in the Merchant Center surfaces a filter "
                    + "with no code change. Read the product types (trainer-provided repository), keep the "
                    + "isSearchable attribute definitions, map each by type (enum/localized-enum → distinct "
                    + "on .key; number/money → stats; skip text/reference/set/boolean), then apply YOUR "
                    + "curation policy and always keep price. GET /api/products/facets?dynamic=true returns "
                    + "the cards plus the derived facet rail from one Product Search.",
            hint = "Docs: ProductType AttributeDefinition (isSearchable) "
                    + "(docs.commercetools.com/api/projects/productTypes#attributedefinition) + Product Search "
                    + "facets (docs.commercetools.com/api/projects/product-search#facets).",
            decisions = {
                    "Curate vs auto-derive — an AI would facet EVERY searchable attribute; decide the "
                            + "policy (allow-list vs deny-list) and the default so the rail stays useful.",
                    "Which searchable attributes become facets, and the per-type facet mapping "
                            + "(enum/lenum → distinct; number/money → stats; skip text/reference/set/boolean).",
                    "How it's configurable — product-type-driven, a config file, or both — and the cap on "
                            + "how many facets the rail may show.",
                    "Ranges need curated boundaries — auto-derived numerics get stats (a slider); the price "
                            + "facet keeps its human-chosen ranges."
            })
    @GetMapping(value = "/api/products/facets", params = "dynamic")
    public PlpView facetsDynamic(@RequestParam(required = false) String store,
                                 @RequestParam(required = false) String locale,
                                 @RequestParam(required = false) String priceCurrency,
                                 @RequestParam(required = false) String priceCountry,
                                 @RequestParam(required = false) String priceChannel,
                                 @RequestParam(required = false) String priceCustomerGroup) {
        return DiscoveryViewMapper.toView(searchService.facetConfig(store, locale,
                price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup)));
    }

    @TaskDescription(
            module = "product-discovery", session = "Session 3", taskNumber = 6,
            title = "Storefront PLP (compose)", tier = "T2", capability = "search.plpV2",
            description = "Implement SearchService.plp(...) in the product-discovery module's application "
                    + "layer — compose store scope + optional full-text + optional category subtree + facets "
                    + "+ sort + pagination into a SINGLE Product Search, and shape the result as "
                    + "{ cards, facets, paging }. This is the one endpoint the storefront PLP calls; the "
                    + "whole page (grid, filter rail, sort, pager) comes from ONE call — the N+1 trap is "
                    + "issuing a search then a per-hit hydration call.",
            hint = "Docs: Product Search — combine query, facets, sort and pagination "
                    + "(docs.commercetools.com/api/projects/product-search).",
            decisions = {
                    "One composed search, not many — store + query + category + facets + sort + paging in a "
                            + "single call; hydrate via product { ... }, never a second retrieval per hit.",
                    "Page shape and paging — expose total/offset/limit so the storefront can page; pick a "
                            + "default page size.",
                    "Sort vocabulary — which sort tokens the storefront may send and how they map to search "
                            + "sort fields (e.g. price asc/desc)."
            })
    @GetMapping("/api/plp")
    public PlpView plp(@RequestParam(required = false) String store,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) String category,
                       @RequestParam(required = false) String sort,
                       @RequestParam(required = false, defaultValue = "0") int page,
                       @RequestParam(required = false, defaultValue = "24") int size,
                       @RequestParam(required = false) String locale,
                       @RequestParam(required = false) String priceCurrency,
                       @RequestParam(required = false) String priceCountry,
                       @RequestParam(required = false) String priceChannel,
                       @RequestParam(required = false) String priceCustomerGroup) {
        return DiscoveryViewMapper.toView(searchService.plp(store, q, category, sort, page, size, List.of(),
                locale, price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup)));
    }

    @TaskDescription(
            module = "product-discovery", session = "Session 3", taskNumber = 7,
            title = "Facet post-filtering", tier = "T2", capability = "search.postFilter",
            description = "Extend SearchService.plp so the shopper's SELECTED facet filters (the filter "
                    + "params, e.g. colour:<key>, price:<from>-<to>) narrow the results WITHOUT collapsing "
                    + "the other facets' counts — applied after the facets are computed, not folded into the "
                    + "main query. GET /api/plp?store=&filter=colour:blue returns the filtered cards with the "
                    + "other facet counts intact, keeping multi-facet navigation stable.",
            hint = "Docs: Product Search — postFilter vs query (facets computed before postFilter) "
                    + "(docs.commercetools.com/api/projects/product-search#filter-examples).",
            decisions = {
                    "query vs postFilter — the store scope (and text/category) belong in query so facet "
                            + "counts reflect them; the shopper's SELECTED facet filters belong in postFilter "
                            + "so they don't collapse the other facets' counts.",
                    "Multi-select semantics — multiple values of the same facet are OR; different facets are "
                            + "AND.",
                    "Filter vocabulary — how the storefront encodes a selected facet (colour:<key>, "
                            + "price:<from>-<to>) and how it maps to a postFilter expression."
            })
    @GetMapping(value = "/api/plp", params = "filter")
    public PlpView plpFiltered(@RequestParam List<String> filter,
                               @RequestParam(required = false) String store,
                               @RequestParam(required = false) String q,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) String sort,
                               @RequestParam(required = false, defaultValue = "0") int page,
                               @RequestParam(required = false, defaultValue = "24") int size,
                               @RequestParam(required = false) String locale,
                               @RequestParam(required = false) String priceCurrency,
                               @RequestParam(required = false) String priceCountry,
                               @RequestParam(required = false) String priceChannel,
                               @RequestParam(required = false) String priceCustomerGroup) {
        return DiscoveryViewMapper.toView(searchService.plp(store, q, category, sort, page, size, filter,
                locale, price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup)));
    }
}
