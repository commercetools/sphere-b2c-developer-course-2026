package com.lifestylehomecorp.catalog.api;

import com.lifestylehomecorp.catalog.api.dto.BundleView;
import com.lifestylehomecorp.catalog.api.dto.CategoryView;
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
                    + "List<ProductProjection> (published projections, with a sensible limit) and applying "
                    + "price selection: withPriceCurrency + withPriceCountry, plus withPriceChannel (the "
                    + "active store's distribution channel) and withPriceCustomerGroup when present. "
                    + "commercetools picks the best-matching price by precedence and falls back to the "
                    + "currency price. CatalogService maps each to a product card so GET /api/products fills "
                    + "the PLP grid and Featured rail with the price for the shopper's channel + country + "
                    + "currency.",
            hint = "Docs: Product price selection + fallback precedence "
                    + "(docs.commercetools.com/api/pricing-and-discounts-overview#price-selection).")
    @GetMapping("/api/products")
    public List<ProductView> listProducts(@RequestParam(required = false) String locale,
                                          @RequestParam(required = false) String priceCurrency,
                                          @RequestParam(required = false) String priceCountry,
                                          @RequestParam(required = false) String priceChannel,
                                          @RequestParam(required = false) String priceCustomerGroup) {
        return catalogService.listProducts(locale, price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup))
                .stream().map(CatalogViewMapper::toView).toList();
    }

    @TaskDescription(
            module = "catalog", session = "Session 2", taskNumber = 2,
            title = "Get product by key", tier = "T1", capability = "catalog.pdp",
            description = "Implement one repository method — CtProductRepository.findByKey(key, "
                    + "PriceSelection) in the catalog module's infrastructure layer — returning the raw SDK "
                    + "ProductProjection for GET /api/products/{key} so the PDP renders, applying the same "
                    + "price selection as 2.1 (currency + country + channel + customer group when present). "
                    + "A commercetools 404 surfaces as a NotFoundException → HTTP 404 via the shared error "
                    + "advice, so an unknown key returns a clean 404.",
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
            module = "catalog", session = "Session 2", taskNumber = 3,
            title = "List categories", tier = "T1", capability = "catalog.categories",
            description = "Implement one repository method — CtCategoryRepository.findAll() in the catalog "
                    + "module's infrastructure layer — returning the raw SDK List<Category>. CatalogService "
                    + "maps each to a {key, name, slug} summary (resolving the LocalizedString, key over "
                    + "id) so GET /api/categories powers the storefront's category navigation.",
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
                    + "catalog module's application layer — resolve the category by key, collect its "
                    + "subtree ids (BFS over parent links), then filter products in ONE query via the "
                    + "repository read ProductRepository.findByCategory(ids, …) (predicate categories(id "
                    + "in :ids)). Map to product cards so GET /api/categories/{key}/products shows "
                    + "everything under a category — a parent includes its whole subtree, not just "
                    + "directly-assigned products.",
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
    public List<ProductView> categoryProducts(@PathVariable String key,
                                              @RequestParam(required = false) String locale,
                                              @RequestParam(required = false) String priceCurrency,
                                              @RequestParam(required = false) String priceCountry,
                                              @RequestParam(required = false) String priceChannel,
                                              @RequestParam(required = false) String priceCustomerGroup) {
        return catalogService.categoryProducts(
                        key, locale, price(priceCurrency, priceCountry, priceChannel, priceCustomerGroup))
                .stream().map(CatalogViewMapper::toView).toList();
    }

    @TaskDescription(
            module = "catalog", session = "Session 2", taskNumber = 5,
            title = "Localization fallback + slug routing", tier = "T2", capability = "catalog.localeSlugs",
            description = "Implement the T2 logic in CatalogService.productBySlug(slug, locale, …) in the "
                    + "catalog module's application layer — build a locale fallback chain (requested "
                    + "locale, then the project's other locales) and for each try the repository read "
                    + "ProductRepository.findBySlug(locale, slug, …) (predicate slug(<locale> = :slug)); "
                    + "first non-empty match wins, none → 404. So GET /api/products/by-slug/{slug} resolves "
                    + "localized URLs — German shoppers get German slugs, a missing-locale slug falls "
                    + "back, a truly unknown slug 404s.",
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
                    + "CatalogMapper.toVariantMatrix in the catalog module's application layer — read "
                    + "variant attributes via variant.getAttributes() (values come back TYPED: "
                    + "LocalizedString, Reference — match the type, then resolve with the locale), keep an "
                    + "attribute as a selectable axis only when it VARIES across variants, and build one "
                    + "row per variant of {axis → value} + price + availability, with default = master "
                    + "variant. So GET /api/products/{key}/variants gives the PDP the data to resolve the "
                    + "right SKU and block impossible combinations.",
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
                    + "module's application layer — fetch the bundle product by key, read its product-ref "
                    + "attribute (a set of product references) for the component ids, fetch them in ONE "
                    + "query via the repository read ProductRepository.findByIds(ids, …), restore the "
                    + "declared component order, and roll up totalPrice = sum of the components' selected "
                    + "prices. So GET /api/products/{key}/bundle expands a 'Reading Nook' bundle's "
                    + "components with a combined price; a non-bundle product returns isBundle=false "
                    + "(empty components), never an error.",
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
