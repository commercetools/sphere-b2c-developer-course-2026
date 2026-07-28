package com.lifestylehomecorp.catalog.application;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductVariant;
import com.lifestylehomecorp.catalog.domain.Bundle;
import com.lifestylehomecorp.catalog.domain.CategorySummary;
import com.lifestylehomecorp.catalog.domain.Money;
import com.lifestylehomecorp.catalog.domain.ProductSummary;
import com.lifestylehomecorp.catalog.domain.VariantMatrix;
import com.lifestylehomecorp.platform.errors.TaskNotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Catalog use-cases. The repositories return raw SDK types; this service maps them to the domain
 * via {@link CatalogMapper} (trainer-provided, not edited by participants). While a repository's
 * SDK call is still stubbed it throws and the endpoint surfaces 501; once implemented, the same
 * call returns real data and the mapping here carries it up — no change to this service.
 */
@Service
public class CatalogService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ChannelRepository channelRepository;

    public CatalogService(ProductRepository productRepository, CategoryRepository categoryRepository,
                          ChannelRepository channelRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.channelRepository = channelRepository;
    }

    /**
     * Prepare a client-supplied {@link PriceSelection} for a read: resolve the channel <em>key</em>
     * (what the storefront sends) to the channel <em>id</em> price selection needs. Currency, country
     * and customer-group pass through unchanged.
     */
    private PriceSelection resolve(PriceSelection price) {
        if (price == null) {
            return PriceSelection.none();
        }
        return price.withChannel(channelRepository.idByKey(price.channel()));
    }

    /** The requested currency (unaffected by channel resolution) — the mapper shows a price only in it. */
    private static String currencyOf(PriceSelection price) {
        return price == null ? null : price.currency();
    }

    public List<ProductSummary> listProducts(String locale, PriceSelection price) {
        // PLP: Product Projections list with price selection; the mapper resolves localized fields to
        // `locale` and reads the selected `price` (in the requested currency, else "—"). A bundle has
        // no own price, so its card price is the rolled-up component total (see summaryWithBundleRollup).
        PriceSelection resolved = resolve(price);
        return productRepository.findAll(resolved).stream()
                .map(pp -> summaryWithBundleRollup(pp, locale, price))
                .toList();
    }

    public ProductSummary getProduct(String key, String locale, PriceSelection price) {
        return summaryWithBundleRollup(productRepository.findByKey(key, resolve(price)), locale, price);
    }

    public List<CategorySummary> listCategories(String locale) {
        // Build the category tree (top-level categories with their subcategories) from the flat SDK list.
        return CatalogMapper.categoryTree(categoryRepository.findAll(), locale);
    }

    /**
     * Task 2.4 (T2) — the products of a category, INCLUDING its subcategories. Resolves the category
     * by key, walks the tree to collect the subtree ids, then filters products by that set. An unknown
     * key returns an empty list — never throws, so a bad link can't crash the storefront.
     */
    public List<ProductSummary> categoryProducts(String key, String locale, PriceSelection price) {
        List<Category> all = categoryRepository.findAll();
        Category start = all.stream().filter(c -> key.equals(c.getKey())).findFirst().orElse(null);
        if (start == null) {
            return List.of(); // unknown category → empty, deterministic fallback
        }
        List<String> subtreeIds = subtreeIds(start.getId(), all);
        return productRepository.findByCategory(subtreeIds, resolve(price)).stream()
                .map(pp -> summaryWithBundleRollup(pp, locale, price))
                .toList();
    }

    /**
     * Task 2.5 (T2) — resolve a product from a localized slug, with a locale fallback chain. Try the
     * requested locale first, then the project's other locales; the first slug match wins. When no
     * locale carries the slug, return 404 (the deliberate "missing slug" case) rather than a fallback
     * product — a wrong slug must not silently resolve to some other product.
     */
    public ProductSummary productBySlug(String slug, String locale, PriceSelection price) {
        // TODO (Task 2.5): resolve a product from its LOCALIZED slug. Build a locale fallback chain
        // (requested locale, then the project's other locales) and try the trainer-provided read
        // productRepository.findBySlug(locale, slug, price) for each — first match wins. Decide the
        // 404-vs-fallback policy: a missing-locale slug falls back, a truly-unknown slug 404s
        // (ResponseStatusException NOT_FOUND). See session-tasks-detailed.md.
        throw new TaskNotImplementedException("2.5");
    }

    /**
     * Task 2.6 (T2) — build the PDP variant selection matrix. The SDK read ({@code findByKey}) and the
     * attribute-reading helpers ({@link CatalogMapper}) are trainer-provided; the participant writes
     * THIS logic: which attributes are selectable axes (only those that vary), one row per variant
     * with its per-axis selections, and the default (master) variant. "Which combinations exist" is
     * implicit in the rows, so an impossible combination is simply absent.
     */
    public VariantMatrix variantMatrix(String key, String locale, PriceSelection price) {
        // TODO (Task 2.6): build the PDP variant selection matrix from productRepository.findByKey(...).
        // Decide which attributes are selectable AXES (only those that VARY across variants), build one
        // row per variant of {axis → value} + price/availability, default = master variant. Read variant
        // attributes via CatalogMapper.variantAttributeLabel(...) (values are typed — resolve with the
        // locale). "Which combinations exist" is implicit in the rows. See session-tasks-detailed.md.
        throw new TaskNotImplementedException("2.6");
    }

    /**
     * Task 2.7 (T2) — resolve a bundle. The SDK reads ({@code findByKey}/{@code findByIds}) and
     * ref-extraction ({@link CatalogMapper#componentIds}) are trainer-provided; the participant writes
     * THIS logic: fetch the components, restore the bundle's declared order, and roll up the total
     * price (sum of the components' selected prices). A non-bundle product resolves to isBundle=false.
     */
    public Bundle bundle(String key, String locale, PriceSelection price) {
        // TODO (Task 2.7): resolve a bundle. Read the component ids via CatalogMapper.componentIds(...),
        // fetch them in ONE query (productRepository.findByIds(ids, price)), restore the bundle's declared
        // component order, and roll up the total price (sum of the components' selected prices). A
        // non-bundle product returns isBundle=false with no components. See session-tasks-detailed.md.
        throw new TaskNotImplementedException("2.7");
    }

    /**
     * Summary price for the PLP card and PDP headline. Precedence:
     * <ol>
     *   <li><b>Own price</b> — if the product's master variant has a selected price, use it. This
     *       covers ordinary products AND a bundle that has been given a real price (e.g. later
     *       materialized by a Connect connector) — that price is authoritative and discountable.</li>
     *   <li><b>Rolled-up total</b> — a BUNDLE with <i>no</i> own price has its summary price computed
     *       on read as the roll-up of its components, reusing {@link #bundle} (task 2.7). Implement 2.7
     *       and the PDP bundle section, PDP headline, and PLP card light up together; until then this
     *       degrades to "—".</li>
     *   <li><b>None</b> — otherwise null, and the storefront shows "—".</li>
     * </ol>
     *
     * <p>Roadmap: a Connect connector can MATERIALIZE the roll-up as the bundle's own Standalone price
     * (on component {@code PriceChanged} / bundle {@code ProductPublished}) — after which branch&nbsp;1
     * serves it, the per-read roll-up stops, and Product Discounts (e.g. 10% off bundles) apply.
     */
    private ProductSummary summaryWithBundleRollup(ProductProjection product, String locale, PriceSelection price) {
        ProductSummary summary = CatalogMapper.toSummary(product, locale, currencyOf(price));
        if (summary.price() != null) {
            return summary; // own price wins — ordinary product, or a bundle with a materialized price
        }
        if (CatalogMapper.componentIds(product).isEmpty()) {
            return summary; // not a bundle → nothing to roll up ("—")
        }
        try {
            Money total = bundle(product.getKey(), locale, price).totalPrice(); // reuse 2.7's roll-up
            return new ProductSummary(summary.key(), summary.name(), summary.slug(), total, summary.imageUrl());
        } catch (RuntimeException e) {
            return summary; // bundle resolution unavailable yet (e.g. before task 2.7) → "—"
        }
    }

    /** The category id plus all of its descendant ids (breadth-first over parent links). */
    private static List<String> subtreeIds(String rootId, List<Category> all) {
        Map<String, List<String>> childrenByParent = new HashMap<>();
        for (Category c : all) {
            String parentId = c.getParent() != null ? c.getParent().getId() : null;
            if (parentId != null) {
                childrenByParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(c.getId());
            }
        }
        List<String> ids = new ArrayList<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            String id = queue.poll();
            ids.add(id);
            queue.addAll(childrenByParent.getOrDefault(id, List.of()));
        }
        return ids;
    }
}
