package com.lifestylehomecorp.catalog.domain;

import java.util.List;

/**
 * A resolved bundle/composite product: its referenced {@code components} expanded to domain
 * summaries, plus the rolled-up {@code totalPrice} (the sum of the components' selected prices).
 * A non-bundle product (no component references) resolves to {@code isBundle=false} with an empty
 * component list — never an error.
 *
 * @param key        the bundle product key
 * @param name       resolved bundle name
 * @param isBundle   true when the product carries component references
 * @param totalPrice sum of the components' selected prices (same currency), or null if none priced
 * @param components the expanded component products
 */
public record Bundle(String key, String name, boolean isBundle, Money totalPrice, List<ProductSummary> components) {
}
