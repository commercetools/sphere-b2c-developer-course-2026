package com.lifestylehomecorp.discovery.application;

import com.commercetools.api.models.product_type.AttributeDefinition;
import com.commercetools.api.models.product_type.ProductType;
import com.lifestylehomecorp.discovery.domain.PlpCard;
import com.lifestylehomecorp.discovery.domain.PlpResponse;
import com.lifestylehomecorp.platform.errors.TaskNotImplementedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Discovery use-cases. The repository returns the raw GraphQL response; this service resolves the
 * price context (channel key→id) and the store key→id scope, assembles a {@link SearchRequest}, then
 * maps the response to the domain via {@link SearchMapper} (trainer-provided). One Product Search
 * call backs every endpoint — cards and facets come back together, hits are hydrated in-query.
 */
@Service
public class SearchService {

    /** Product Search sort/facet field for the embedded price (minor units). */
    private static final String PRICE_FIELD = "variants.prices.centAmount";
    /** Default page size for the storefront PLP. */
    private static final int DEFAULT_PAGE_SIZE = 24;

    // --- 3.8 curation policy (the human-in-the-loop decision AI can't own) ------------------------
    // An AI would happily facet EVERY searchable attribute; that clutters the rail and costs compute.
    // The policy below is the judgement call: which searchable attributes become facets, how each
    // type maps to a facet shape, and a cap. Change these three knobs to re-curate the rail.

    /** Attribute names to keep OFF the rail even when searchable (noisy / not useful as filters). */
    private static final Set<String> FACET_DENY_LIST = Set.of("product-ref");
    /** Cap on DERIVED facets (price is always added on top) — a filter rail longer than this is clutter. */
    private static final int MAX_DERIVED_FACETS = 8;

    private final SearchRepository searchRepository;
    private final StoreRepository storeRepository;
    private final ChannelRepository channelRepository;
    private final ProductTypeRepository productTypeRepository;
    private final CategoryRepository categoryRepository;

    public SearchService(SearchRepository searchRepository, StoreRepository storeRepository,
                         ChannelRepository channelRepository, ProductTypeRepository productTypeRepository,
                         CategoryRepository categoryRepository) {
        this.searchRepository = searchRepository;
        this.storeRepository = storeRepository;
        this.channelRepository = channelRepository;
        this.productTypeRepository = productTypeRepository;
        this.categoryRepository = categoryRepository;
    }

    /** Task 3.1 (T1) — store-scoped search returning hydrated cards. */
    public List<PlpCard> search(String store, String locale, PriceSelection price) {
        SearchRequest request = base(store, locale, price).build();
        return SearchMapper.toCards(searchRepository.search(request));
    }

    /** Task 3.3 (T1) — add a full-text (+fuzzy) clause to the store-scoped search. */
    public List<PlpCard> fullText(String store, String q, String locale, PriceSelection price) {
        // TODO (Task 3.3): add a full-text (with typo tolerance) clause to the store-scoped search so a
        // keyword query returns relevance-ranked cards. Assemble via base(store, locale, price), then
        // searchRepository.search(...) + SearchMapper.toCards(...). See the worked search() above +
        // session-tasks-detailed.md; ground the Product Search full-text options on the commercetools-knowledge MCP.
        throw new TaskNotImplementedException("3.3");
    }

    /** Task 3.4 (T1) — filter the store-scoped search to a category subtree (by category key). */
    public List<PlpCard> byCategory(String store, String category, String locale, PriceSelection price) {
        // TODO (Task 3.4): filter the store-scoped search to a category and its whole subtree, so a parent
        // category includes its descendants. The nav sends a category KEY — resolve it to an id with the
        // provided resolveCategory(category) helper, then base(...).categoryId(id) + searchRepository
        // .search(...) + SearchMapper.toCards(...). See the worked search() above + session-tasks-detailed.md;
        // ground the Product Search categoriesSubTree filter on the commercetools-knowledge MCP.
        throw new TaskNotImplementedException("3.4");
    }

    /** Task 3.5 (T1) — store-scoped search with colour/price facets computed alongside the cards. */
    public PlpResponse facets(String store, String locale, PriceSelection price) {
        // TODO (Task 3.5): add colour + price facets to the store-scoped search and return the cards AND
        // the facet buckets together. Assemble via base(...), then searchRepository.search(...) +
        // SearchMapper.toResponse(...). See the worked search() above + session-tasks-detailed.md; ground
        // the Product Search facets on the commercetools-knowledge MCP.
        throw new TaskNotImplementedException("3.5");
    }

    /**
     * Task 3.8 (T2) — configurable facets. Instead of 3.5's hardcoded three, DERIVE the facet set from
     * the product types' <b>searchable</b> attributes, under a curation policy, so adding a searchable
     * attribute in the Merchant Center surfaces a filter with no code change. This is the T2 method the
     * participant implements; the ProductType read ({@link ProductTypeRepository}), the {@link FacetSpec}
     * shape and the builder's facet emission are trainer-provided plumbing.
     *
     * <p><b>Derivation.</b> Collect every {@code isSearchable} attribute definition across all product
     * types (deduped by name — an attribute shared by several types is one facet), then map each by its
     * attribute type: {@code enum}/{@code lenum} → a {@code distinct} facet on
     * {@code variants.attributes.<name>.key}; {@code number}/{@code money} → a {@code stats} facet on
     * the attribute; and <b>skip</b> {@code text}/{@code ltext}/{@code boolean}/{@code reference}/
     * {@code set}/date-like types (they make poor facets — free text, unbounded, or opaque ids).
     *
     * <p><b>Curation policy (the decision AI can't own).</b> (1) start from searchable only; (2) drop
     * anything on {@link #FACET_DENY_LIST}; (3) apply the per-type mapping above (which is itself a
     * curation choice — e.g. numeric attributes get {@code stats} for a slider, not arbitrary
     * {@code ranges}, because sensible range boundaries are domain-specific); (4) cap at
     * {@link #MAX_DERIVED_FACETS} to keep the rail short; (5) ALWAYS append the curated <b>price</b>
     * facets ({@code ranges} + {@code stats}) on top. Note colour is no longer special-cased — it flows
     * in automatically because {@code search-color} is a searchable localized-enum.
     */
    public PlpResponse facetConfig(String store, String locale, PriceSelection price) {
        // TODO (Task 3.8): stop hardcoding the facet set — DERIVE it from the catalogue. Read the product
        // types via productTypeRepository.findAll(), collect the attribute definitions flagged isSearchable,
        // and map each to a FacetSpec by attribute type (enum/lenum → distinct on
        // variants.attributes.<name>.key; number/money → stats; skip text/ltext/boolean/reference/set).
        // Apply YOUR curation policy — which attributes actually make good facets (not all do), a cap, and
        // always include price — then base(store, locale, price).withFacets(true).facetSpecs(specs).build()
        // → searchRepository.search(...) → SearchMapper.toResponse(...). The ProductType read, the FacetSpec
        // shape, and the builder's facet emission are trainer-provided; the derivation + curation are yours.
        // See session-tasks-detailed.md (Task 3.8); ground AttributeDefinition/ProductType on the MCP.
        throw new TaskNotImplementedException("3.8");
    }

    /** Turn the product types' searchable attributes into a curated facet set (see {@link #facetConfig}). */
    private List<FacetSpec> deriveFacets(List<ProductType> productTypes, String locale) {
        // Dedupe by attribute name, first definition wins (an attribute shared across types is one facet).
        Map<String, FacetSpec> derived = new LinkedHashMap<>();
        for (ProductType type : productTypes) {
            if (type == null || type.getAttributes() == null) {
                continue;
            }
            for (AttributeDefinition attr : type.getAttributes()) {
                if (derived.size() >= MAX_DERIVED_FACETS) {
                    break;
                }
                if (!isFacetable(attr) || derived.containsKey(attr.getName())) {
                    continue;
                }
                FacetSpec spec = toSpec(attr, locale);
                if (spec != null) {
                    derived.put(attr.getName(), spec);
                }
            }
        }
        List<FacetSpec> specs = new ArrayList<>(derived.values());
        // Always include the curated price facets on top (ranges need human-chosen boundaries).
        specs.add(FacetSpec.ranges("price", PRICE_FIELD));
        specs.add(FacetSpec.stats("priceStats", PRICE_FIELD));
        return specs;
    }

    /** Searchable, named, and not denied — the gate before the per-type mapping. */
    private static boolean isFacetable(AttributeDefinition attr) {
        return attr != null
                && Boolean.TRUE.equals(attr.getIsSearchable())
                && attr.getName() != null && !attr.getName().isBlank()
                && !FACET_DENY_LIST.contains(attr.getName())
                && attr.getType() != null;
    }

    /**
     * Map ONE searchable attribute to a facet spec by its type discriminator ({@code AttributeType.name}
     * — {@code enum}/{@code lenum}/{@code number}/{@code money}/…), or {@code null} to skip a type that
     * doesn't facet well.
     */
    private static FacetSpec toSpec(AttributeDefinition attr, String locale) {
        String name = attr.getName();
        String type = attr.getType().getName();
        String base = "variants.attributes." + name;
        return switch (type) {
            case "enum" -> FacetSpec.distinct(name, base + ".key", "enum", null);
            case "lenum" -> FacetSpec.distinct(name, base + ".key", "lenum",
                    locale == null || locale.isBlank() ? null : locale);
            case "number" -> FacetSpec.stats(name, base);
            case "money" -> FacetSpec.stats(name, base + ".centAmount");
            // Skipped: text, ltext, boolean, reference, set, date/time/datetime, nested — poor facets.
            default -> null;
        };
    }

    /**
     * Tasks 3.6 / 3.7 (T2) — the one storefront PLP search. Composes store scope + full-text +
     * category + facets + sort + pagination into a SINGLE Product Search, and (3.7) routes the
     * selected facet filters to {@code postFilter} so they narrow the results WITHOUT changing the
     * other facets' counts (stable multi-facet navigation).
     */
    public PlpResponse plp(String store, String q, String category, String sort, int page, int size,
                           List<String> filters, String locale, PriceSelection price) {
        // TODO (Task 3.6 + 3.7): compose ONE search for the whole PLP — store scope + optional full-text +
        // optional category subtree (resolve the category KEY→id via resolveCategory(category)) + facets +
        // sort + pagination — and (3.7) route the shopper's selected `filters` so they narrow the hits
        // WITHOUT changing the other facets' counts (stable multi-facet nav). The base(...), resolveCategory,
        // applySort, applyPostFilters and applyPriceRange helpers are trainer-provided — call them. Goal +
        // decisions in the @TaskDescription; see session-tasks-detailed.md; ground on the commercetools-knowledge MCP.
        throw new TaskNotImplementedException("3.6");
    }

    // --- assembly helpers -------------------------------------------------------------------------

    /** Store scope + resolved price context, shared by every endpoint. */
    private SearchRequest.Builder base(String store, String locale, PriceSelection price) {
        return SearchRequest.builder()
                .storeIds(storeIds(store))
                .locale(locale)
                .price(resolve(price));
    }

    /** Resolve the store key to its id for the {@code stores} scope (empty when unknown/blank). */
    private List<String> storeIds(String storeKey) {
        String id = storeRepository.idByKey(storeKey);
        return id == null ? List.of() : List.of(id);
    }

    /** Resolve the channel <em>key</em> the storefront sends to the channel <em>id</em> price selection needs. */
    private PriceSelection resolve(PriceSelection price) {
        if (price == null) {
            return PriceSelection.none();
        }
        return price.withChannel(channelRepository.idByKey(price.channel()));
    }

    /**
     * Resolve the category <em>key</em> the storefront nav sends to the <em>id</em> the {@code
     * categoriesSubTree} filter matches on (cached). An absent/unknown key yields {@code null}, so the
     * builder simply omits the category clause (the store's whole assortment) rather than failing.
     */
    private String resolveCategory(String categoryKey) {
        return categoryRepository.idByKey(categoryKey);
    }

    /** Map the storefront's sort token to a Product Search sort field (only price is exposed for now). */
    private void applySort(SearchRequest.Builder builder, String sort) {
        if (sort == null || sort.isBlank()) {
            return;
        }
        String token = sort.toLowerCase();
        if (token.startsWith("price")) {
            builder.sort(PRICE_FIELD, !token.endsWith("desc"));
        }
    }

    /** Parse the {@code filter} params ("colour:&lt;key&gt;", "price:&lt;from&gt;-&lt;to&gt;") into post-filters. */
    private void applyPostFilters(SearchRequest.Builder builder, List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return;
        }
        List<String> colours = new ArrayList<>();
        for (String filter : filters) {
            if (filter == null) {
                continue;
            }
            int colon = filter.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String field = filter.substring(0, colon).trim().toLowerCase();
            String value = filter.substring(colon + 1).trim();
            if ("colour".equals(field) || "color".equals(field)) {
                if (!value.isBlank()) {
                    colours.add(value);
                }
            } else if ("price".equals(field)) {
                applyPriceRange(builder, value);
            }
        }
        builder.colourFilters(colours);
    }

    private void applyPriceRange(SearchRequest.Builder builder, String range) {
        int dash = range.indexOf('-');
        if (dash < 0) {
            return;
        }
        Long from = parseLong(range.substring(0, dash));
        Long to = parseLong(range.substring(dash + 1));
        builder.priceRange(from, to);
    }

    private static Long parseLong(String s) {
        try {
            String t = s.trim();
            return t.isEmpty() ? null : Long.parseLong(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
