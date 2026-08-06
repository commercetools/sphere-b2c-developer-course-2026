package com.lifestylehomecorp.catalog.api;

import com.lifestylehomecorp.catalog.api.dto.BundleView;
import com.lifestylehomecorp.catalog.api.dto.CategoryView;
import com.lifestylehomecorp.catalog.api.dto.ProductPageView;
import com.lifestylehomecorp.catalog.api.dto.ProductView;
import com.lifestylehomecorp.catalog.api.dto.VariantMatrixView;
import com.lifestylehomecorp.catalog.application.CatalogService;
import com.lifestylehomecorp.catalog.application.PriceSelection;
import com.lifestylehomecorp.platform.annotations.TaskDescription;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalog HTTP contract. Fully wired to the application layer — these endpoints return real
 * data as soon as the corresponding infrastructure adapter method is implemented. Until then
 * they surface HTTP 501 from the stubbed adapter, via the platform error advice.
 */
@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Assemble the price-selection context from the request. All parameters are optional: the
     * storefront sends the shopper's currency + country, the active store's distribution channel
     * (by key — the service resolves it to an id), and later the customer group after login. Whatever
     * is present is applied; commercetools picks the best-matching price and falls back.
     */
    private static PriceSelection price(String currency, String country, String channel, String customerGroup) {
        return new PriceSelection(currency, country, channel, customerGroup);
    }

    @TaskDescription(
            module = "catalog", session = "Session 2", taskNumber = 1,
            title = "List products (PLP)", tier = "T1", capability = "catalog.plp",
            description = "Implement one repository method — CtProductRepository.findAll(PriceSelection) in "
                    + "the catalog module's infrastructure layer — returning the raw SDK "
                    + "ProductProjectionPagedQueryResponse (published projections, with a sensible limit). "
                    + "CatalogService maps its results to product cards so GET /api/products fills the PLP "
                    + "grid and Featured rail — each card showing the product's price for the shopper's "
                    + "context — and surfaces the response's total as 'Total N products'.",
            hint = "Docs: Product price selection + fallback precedence "
                    + "(docs.commercetools.com/api/pricing-and-discounts-overview#price-selection). The "
                    + "query response wraps results in a paged envelope that also carries total.")
    @GetMapping("/api/products")
    public ProductPageView listProducts(@RequestParam(required = false) String locale,
                                        @RequestParam(required = false) String priceCurrency,
                                        @RequestParam(required = false) String priceCountry,
                                        @RequestParam(required = false) String priceChannel,
                                        @RequestParam(required = false) String priceCustomerGroup) {
        return CatalogViewMapper.toView(catalogService.listProducts(
                locale, price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup)));
    }

    @TaskDescription(
            module = "catalog", session = "Session 2", taskNumber = 2,
            title = "Get product by key", tier = "T1", capability = "catalog.pdp",
            description = "Implement one repository method — CtProductRepository.findByKey(key, "
                    + "PriceSelection) in the catalog module's infrastructure layer — returning the raw SDK "
                    + "ProductProjection for GET /api/products/{key} so the PDP renders with the product's "
                    + "price for the shopper's context. A commercetools 404 surfaces as a NotFoundException "
                    + "→ HTTP 404 via the shared error advice, so an unknown key returns a clean 404.",
            hint = "Docs: Product Projections — get a published projection by key "
                    + "(docs.commercetools.com/api/projects/productProjections).")
    @GetMapping("/api/products/{key}")
    public ProductView getProduct(@PathVariable String key,
                                  @RequestParam(required = false) String locale,
                                  @RequestParam(required = false) String priceCurrency,
                                  @RequestParam(required = false) String priceCountry,
                                  @RequestParam(required = false) String priceChannel,
                                  @RequestParam(required = false) String priceCustomerGroup) {
        return CatalogViewMapper.toView(catalogService.getProduct(
                key, locale, price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup)));
    }

    @TaskDescription(
            module = "catalog", session = "Session 3", taskNumber = 2,
            title = "In-store PDP", tier = "T1", capability = "catalog.pdpInStore",
            description = "Implement one repository method — CtProductRepository.findByKeyInStore(store, "
                    + "key, PriceSelection) in the catalog module's infrastructure layer — using the "
                    + "In-Store Product Projection endpoint so GET /api/products/{key}?store= returns the "
                    + "PDP only when the product is in that store's assortment, with the product's price for "
                    + "the shopper's context. A product not carried by the store surfaces as a commercetools "
                    + "404 → NotFoundException → HTTP 404 via the shared error advice — the store boundary is "
                    + "enforced by commercetools, so an out-of-store key is a clean not-found. When no store "
                    + "is present the plain 2.2 handler still serves the global PDP.",
            hint = "Docs: In-Store Product Projections — get a projection by key in a store "
                    + "(docs.commercetools.com/api/projects/productProjections#get-productprojection-in-store).")
    @GetMapping(value = "/api/products/{key}", params = "store")
    public ProductView getProductInStore(@PathVariable String key,
                                         @RequestParam String store,
                                         @RequestParam(required = false) String locale,
                                         @RequestParam(required = false) String priceCurrency,
                                         @RequestParam(required = false) String priceCountry,
                                         @RequestParam(required = false) String priceChannel,
                                         @RequestParam(required = false) String priceCustomerGroup) {
        return CatalogViewMapper.toView(catalogService.getProductInStore(
                store, key, locale, price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup)));
    }

    @TaskDescription(
            module = "catalog", session = "Session 2", taskNumber = 3,
            title = "List categories", tier = "T1", capability = "catalog.categories",
            description = "Implement one repository method — CtCategoryRepository.findAll() in the catalog "
                    + "module's infrastructure layer — returning the raw SDK List<Category>. CatalogService "
                    + "maps each to a {key, name, slug} summary so GET /api/categories powers the "
                    + "storefront's category navigation.",
            hint = "Docs: Categories — the category tree and its localized names / slugs "
                    + "(docs.commercetools.com/api/projects/categories).")
    @GetMapping("/api/categories")
    public List<CategoryView> listCategories(@RequestParam(required = false) String locale) {
        return catalogService.listCategories(locale).stream().map(CatalogViewMapper::toView).toList();
    }

    @TaskDescription(
            module = "catalog", session = "Session 2", taskNumber = 4,
            title = "Browse by category", tier = "T2", capability = "catalog.categoryProducts",
            description = "Implement the T2 logic in CatalogService.categoryProducts(key, …) in the "
                    + "catalog module's application layer, using the repository read "
                    + "ProductRepository.findByCategory(...). Map the results to product cards so GET "
                    + "/api/categories/{key}/products shows everything under a category — a parent includes "
                    + "its whole subtree, not just directly-assigned products — with the total match count "
                    + "for 'Total N products'.",
            hint = "Docs: Categories + query predicates — filter products by category id "
                    + "(docs.commercetools.com/api/predicates/query). (Product Search's categoriesSubTree "
                    + "comes in S3.)",
            decisions = {
                    "Direct vs subtree — products sit on leaf categories, so a parent must include its "
                            + "whole subtree; decide and defend it.",
                    "Identify by key, not id — stable and non-localized.",
                    "Unknown-key fallback — empty list, never a 500. (Contrast 1.3's 404: a filter "
                            + "returning nothing ≠ a lookup miss.)",
                    "One filtered query, not N+1 — collect the ids first, then filter once."
            })
    @GetMapping("/api/categories/{key}/products")
    public ProductPageView categoryProducts(@PathVariable String key,
                                            @RequestParam(required = false) String locale,
                                            @RequestParam(required = false) String priceCurrency,
                                            @RequestParam(required = false) String priceCountry,
                                            @RequestParam(required = false) String priceChannel,
                                            @RequestParam(required = false) String priceCustomerGroup) {
        return CatalogViewMapper.toView(catalogService.categoryProducts(
                key, locale, price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup)));
    }

    @TaskDescription(
            module = "catalog", session = "Session 2", taskNumber = 5,
            title = "Localization fallback + slug routing", tier = "T2", capability = "catalog.localeSlugs",
            description = "Implement the T2 logic in CatalogService.productBySlug(slug, locale, …) in the "
                    + "catalog module's application layer, using the repository read "
                    + "ProductRepository.findBySlug(...). GET /api/products/by-slug/{slug} resolves "
                    + "localized URLs — a German shopper gets the German slug, a slug missing in the "
                    + "requested locale falls back to another project locale, and a truly unknown slug "
                    + "returns 404.",
            hint = "Docs: LocalizedString + query predicates on localized fields "
                    + "(docs.commercetools.com/api/predicates/query). Note key (stable) vs slug "
                    + "(localized, SEO-facing).",
            decisions = {
                    "The locale fallback chain — e.g. de-DE → de → project default.",
                    "Per-locale slugs and the canonical-URL policy.",
                    "404 vs fallback when a slug doesn't exist in the requested locale — a wrong slug must "
                            + "404 (never silently resolve to a different product); a missing-locale slug "
                            + "falls back."
            })
    @GetMapping("/api/products/by-slug/{slug}")
    public ProductView getProductBySlug(@PathVariable String slug,
                                        @RequestParam(required = false) String locale,
                                        @RequestParam(required = false) String priceCurrency,
                                        @RequestParam(required = false) String priceCountry,
                                        @RequestParam(required = false) String priceChannel,
                                        @RequestParam(required = false) String priceCustomerGroup) {
        return CatalogViewMapper.toView(catalogService.productBySlug(
                slug, locale, price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup)));
    }

    @TaskDescription(
            module = "catalog", session = "Session 2", taskNumber = 6,
            title = "Variant selection matrix", tier = "T2", capability = "catalog.variantMatrix",
            description = "Implement the T2 logic in CatalogService.variantMatrix / "
                    + "CatalogMapper.toVariantMatrix in the catalog module's application layer, from a "
                    + "product read's variants. GET /api/products/{key}/variants gives the PDP the data to "
                    + "resolve the right SKU and block impossible combinations — the selectable axes, each "
                    + "variant's values + price + availability, and a sensible default.",
            hint = "Docs: Product Projection variants & attributes — attribute values are typed, not raw "
                    + "maps (docs.commercetools.com/api/projects/productProjections).",
            decisions = {
                    "Which attributes are selectable — an attribute is an axis only when it varies across "
                            + "variants (ask \"why is there no Finish selector?\").",
                    "How to represent combination availability — which combos actually exist (the variant "
                            + "rows can be the availability map, so impossible combos are absent by "
                            + "construction).",
                    "The default-variant policy — e.g. open on the master variant."
            })
    @GetMapping("/api/products/{key}/variants")
    public VariantMatrixView getVariantMatrix(@PathVariable String key,
                                              @RequestParam(required = false) String locale,
                                              @RequestParam(required = false) String priceCurrency,
                                              @RequestParam(required = false) String priceCountry,
                                              @RequestParam(required = false) String priceChannel,
                                              @RequestParam(required = false) String priceCustomerGroup) {
        return CatalogViewMapper.toView(catalogService.variantMatrix(
                key, locale, price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup)));
    }

    @TaskDescription(
            module = "catalog", session = "Session 2", taskNumber = 7,
            title = "Bundle / composite resolution", tier = "T2", capability = "catalog.bundles",
            description = "Implement the T2 logic in CatalogService.bundle(key, …) in the catalog "
                    + "module's application layer, using the repository reads ProductRepository.findByKey(...) "
                    + "and findByIds(...). GET /api/products/{key}/bundle expands a 'Reading Nook' bundle "
                    + "into its component products with a combined total price; a non-bundle product returns "
                    + "isBundle=false (empty components), never an error.",
            hint = "Docs: reference attributes & id-in predicates — references deserialize to typed "
                    + "Reference objects (docs.commercetools.com/api/projects/products). (Availability "
                    + "roll-up needs the Inventory API — S9.)",
            decisions = {
                    "How bundles are modeled — product references (a set of reference→product) vs a "
                            + "bundle attribute.",
                    "The price roll-up rule — the bundle has no price of its own; sum the components' "
                            + "selected prices (same currency).",
                    "The availability roll-up rule — e.g. min across components (deferred to S9; S2 rolls "
                            + "up price only)."
            })
    @GetMapping("/api/products/{key}/bundle")
    public BundleView getBundle(@PathVariable String key,
                                @RequestParam(required = false) String locale,
                                @RequestParam(required = false) String priceCurrency,
                                @RequestParam(required = false) String priceCountry,
                                @RequestParam(required = false) String priceChannel,
                                @RequestParam(required = false) String priceCustomerGroup) {
        return CatalogViewMapper.toView(catalogService.bundle(
                key, locale, price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup)));
    }
}
