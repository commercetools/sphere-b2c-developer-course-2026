package com.lifestylehomecorp.catalog.application;

import com.commercetools.api.models.category.Category;
import com.commercetools.api.models.common.Image;
import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.common.Price;
import com.commercetools.api.models.common.Reference;
import com.commercetools.api.models.common.TypedMoney;
import com.commercetools.api.models.product.Attribute;
import com.commercetools.api.models.product.ProductProjection;
import com.commercetools.api.models.product.ProductVariant;
import com.lifestylehomecorp.catalog.domain.CategorySummary;
import com.lifestylehomecorp.catalog.domain.Money;
import com.lifestylehomecorp.catalog.domain.ProductSummary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps commercetools SDK catalog types to domain records. This is the boundary where SDK types
 * stop — it lives in the service layer (trainer-provided; participants don't touch it), so the T1
 * catalog tasks focus purely on the SDK call in the repository.
 *
 * <p>Localized fields (name, slug) are resolved to a requested {@code locale} with a fallback chain:
 * exact match → same language (e.g. {@code en-GB} for {@code en-US}) → first available. When no locale
 * is supplied it falls back to first-available (a preview of the Session-2 Tier-2 localization task).
 */
final class CatalogMapper {

    private CatalogMapper() {
    }

    // --- products (PLP/PDP): map the typed Product Projection to a domain card -------------------

    static ProductSummary toSummary(ProductProjection product, String locale, String currency) {
        ProductVariant mv = product.getMasterVariant();
        return new ProductSummary(
                product.getKey(),
                localized(product.getName(), locale),
                localized(product.getSlug(), locale),
                priceOf(mv, currency),                    // effective one-time (discounted if any)
                originalPriceOf(mv, currency),            // pre-discount one-time, else null
                recurringPriceOf(mv, currency),           // effective recurring (discounted if any)
                recurringOriginalPriceOf(mv, currency),   // pre-discount recurring, else null
                masterImage(product));
    }

    private static String masterImage(ProductProjection product) {
        return imageOf(product.getMasterVariant());
    }

    /**
     * The EFFECTIVE one-time price for the currency — the discounted value when a Product Discount
     * applies, else the base — so the PDP/PLP shows what the cart will charge. Prefers the SELECTED
     * price (price selection has applied precedence + base-currency fallback); falls back to an embedded
     * one-time price in the SAME {@code currency}. Null when there is no price in the requested currency,
     * so the storefront shows "—" rather than a wrong-currency price.
     */
    static Money priceOf(ProductVariant v, String currency) {
        return effective(oneTimePrice(v, currency));
    }

    /** The pre-discount (list) one-time price — only when a Product Discount applies (else null: nothing to strike through). */
    static Money originalPriceOf(ProductVariant v, String currency) {
        return listWhenDiscounted(oneTimePrice(v, currency));
    }

    /**
     * The variant's recurrence-scoped ("Subscribe &amp; Save") EFFECTIVE price in the currency, if any.
     * Price selection only picks the one-time price, so we scan the full {@code prices[]} for the one
     * carrying a {@code recurrencePolicy}. Null when the product has no recurring price.
     */
    static Money recurringPriceOf(ProductVariant v, String currency) {
        return effective(recurrencePrice(v, currency));
    }

    /** The pre-discount (list) recurring price — only when a Product Discount applies to it (else null). */
    static Money recurringOriginalPriceOf(ProductVariant v, String currency) {
        return listWhenDiscounted(recurrencePrice(v, currency));
    }

    /** The selected one-time Price, or an embedded one-time price in the same currency; never a recurrence price. */
    private static Price oneTimePrice(ProductVariant v, String currency) {
        if (v == null) {
            return null;
        }
        if (v.getPrice() != null) {
            return v.getPrice();
        }
        if (currency != null && !currency.isBlank() && v.getPrices() != null) {
            for (Price p : v.getPrices()) {
                if (p.getRecurrencePolicy() == null && p.getValue() != null
                        && currency.equals(p.getValue().getCurrencyCode())) {
                    return p;
                }
            }
        }
        return null;
    }

    /** The embedded recurrence-scoped price for the currency, or null. */
    private static Price recurrencePrice(ProductVariant v, String currency) {
        if (v == null || currency == null || currency.isBlank() || v.getPrices() == null) {
            return null;
        }
        for (Price p : v.getPrices()) {
            if (p.getRecurrencePolicy() != null && p.getValue() != null
                    && currency.equals(p.getValue().getCurrencyCode())) {
                return p;
            }
        }
        return null;
    }

    /** The price the cart will charge: the discounted value if a Product Discount applies, else the base. */
    private static Money effective(Price p) {
        if (p == null) {
            return null;
        }
        TypedMoney value = p.getDiscounted() != null && p.getDiscounted().getValue() != null
                ? p.getDiscounted().getValue() : p.getValue();
        return value == null ? null : new Money(value.getCurrencyCode(), value.getCentAmount());
    }

    /** The base (list) value, but ONLY when a discount applies — so the caller can strike it through. */
    private static Money listWhenDiscounted(Price p) {
        if (p == null || p.getDiscounted() == null || p.getValue() == null) {
            return null;
        }
        return new Money(p.getValue().getCurrencyCode(), p.getValue().getCentAmount());
    }

    static String imageOf(ProductVariant v) {
        if (v == null || v.getImages() == null || v.getImages().isEmpty()) {
            return null;
        }
        Image first = v.getImages().get(0);
        return first == null ? null : first.getUrl();
    }

    // --- bundle / composite resolution (Task 2.7) -------------------------------------------------

    /** The set-of-product-reference attribute that models a bundle's components (bedding-bundle type). */
    private static final String BUNDLE_REF_ATTR = "product-ref";

    /** The component product ids referenced by a bundle's {@code product-ref} attribute (empty if none). */
    static List<String> componentIds(ProductProjection product) {
        Object value = attributeValue(product.getMasterVariant(), BUNDLE_REF_ATTR);
        List<String> ids = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object ref : list) {
                addReferenceId(ref, ids);
            }
        } else if (value != null) {
            addReferenceId(value, ids);
        }
        return ids;
    }

    private static void addReferenceId(Object ref, List<String> ids) {
        // The SDK deserializes a product-reference attribute value to a typed Reference.
        if (ref instanceof Reference r && r.getId() != null) {
            ids.add(r.getId());
        } else if (ref instanceof Map<?, ?> m && m.get("id") != null) {
            ids.add(String.valueOf(m.get("id")));
        }
    }

    // --- attribute readers (trainer-provided plumbing; the SDK deserializes values to typed models) --

    /** Read a variant attribute and resolve it to a display label (used by the service to build axes). */
    static String variantAttributeLabel(ProductVariant v, String name, String locale) {
        return attributeLabel(attributeValue(v, name), locale);
    }

    private static Object attributeValue(ProductVariant v, String name) {
        if (v == null || v.getAttributes() == null) {
            return null;
        }
        return v.getAttributes().stream()
                .filter(a -> name.equals(a.getName()))
                .map(Attribute::getValue)
                .findFirst()
                .orElse(null);
    }

    /** Resolve an attribute value to a display label: lenum → its localized label; ltext → localized. */
    private static String attributeLabel(Object value, String locale) {
        if (value == null) {
            return null;
        }
        // The SDK deserializes attribute values to typed models: ltext → LocalizedString.
        if (value instanceof LocalizedString ls) {
            return localized(ls, locale);
        }
        if (value instanceof Map<?, ?> m) {
            if (m.containsKey("label")) {                        // localizable enum: {key, label:{...}}
                Object label = m.get("label");
                if (label instanceof Map<?, ?> lm) {
                    return localizedMap(lm, locale);
                }
                return m.get("key") != null ? String.valueOf(m.get("key")) : null;
            }
            return localizedMap(m, locale);                      // localized text: {locale: value}
        }
        return String.valueOf(value);
    }

    /** Resolve a raw locale→value map to {@code locale} with fallback (exact → language → first). */
    private static String localizedMap(Map<?, ?> values, String locale) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (locale != null && !locale.isBlank()) {
            Object exact = values.get(locale);
            if (exact != null) {
                return String.valueOf(exact);
            }
            String lang = locale.split("-")[0];
            for (Map.Entry<?, ?> e : values.entrySet()) {
                if (e.getKey() != null && String.valueOf(e.getKey()).startsWith(lang)) {
                    return String.valueOf(e.getValue());
                }
            }
        }
        return values.values().stream().findFirst().map(String::valueOf).orElse(null);
    }

    // --- categories: build a nested tree (top-level categories with their subcategories) ---------

    /**
     * Build a nested category tree (top-level categories, each with their subcategories) from the
     * flat SDK list — pure Java, no extra API call. Parent links are resolved by id within the
     * fetched set, so this works as long as the query returns the whole tree.
     */
    static List<CategorySummary> categoryTree(List<Category> categories, String locale) {
        Map<String, Node> byId = new LinkedHashMap<>();
        for (Category c : categories) {
            byId.put(c.getId(), new Node(c));
        }
        List<Node> roots = new ArrayList<>();
        for (Category c : categories) {
            Node node = byId.get(c.getId());
            String parentId = c.getParent() != null ? c.getParent().getId() : null;
            Node parent = parentId == null ? null : byId.get(parentId);
            if (parent != null) {
                parent.children.add(node);
            } else {
                roots.add(node);
            }
        }
        return roots.stream().map(n -> toSummary(n, locale)).toList();
    }

    private static CategorySummary toSummary(Node node, String locale) {
        Category c = node.category;
        List<CategorySummary> children = node.children.stream().map(ch -> toSummary(ch, locale)).toList();
        return new CategorySummary(
                c.getKey(),
                localized(c.getName(), locale),
                localized(c.getSlug(), locale),
                children);
    }

    /** Mutable tree node used only while assembling the category hierarchy. */
    private static final class Node {
        final Category category;
        final List<Node> children = new ArrayList<>();

        Node(Category category) {
            this.category = category;
        }
    }

    /** Resolve a {@link LocalizedString} to {@code locale} with fallback (exact → language → first). */
    private static String localized(LocalizedString ls, String locale) {
        if (ls == null) {
            return null;
        }
        Map<String, String> values = ls.values();
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (locale != null && !locale.isBlank()) {
            String exact = values.get(locale);
            if (exact != null) {
                return exact;
            }
            String lang = locale.split("-")[0];
            for (Map.Entry<String, String> e : values.entrySet()) {
                if (e.getKey() != null && e.getKey().startsWith(lang)) {
                    return e.getValue();
                }
            }
        }
        return values.values().stream().findFirst().orElse(null);
    }
}
