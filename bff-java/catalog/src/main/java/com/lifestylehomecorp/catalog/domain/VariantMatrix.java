package com.lifestylehomecorp.catalog.domain;

import java.util.List;
import java.util.Map;

/**
 * The data a PDP needs to render a <b>variant selection matrix</b>: the selectable {@code axes}
 * (e.g. Colour, Finish, Size), every concrete {@code variants} row with its per-axis selections,
 * and the {@code defaultSku} to preselect. The storefront derives "which combinations exist" from
 * the {@code variants} list — a selection resolves to a variant only if a row carries that exact
 * combination, so impossible combinations are simply absent.
 *
 * @param key        product key
 * @param defaultSku the master variant's SKU (the initial selection)
 * @param axes       the selectable axes, in display order
 * @param variants   one row per variant with its axis selections, price and image
 */
public record VariantMatrix(String key, String defaultSku, List<Axis> axes, List<VariantOption> variants) {

    /** A selectable dimension, e.g. {@code name="Colour"} with {@code options=[Grey, Blue, Green]}. */
    public record Axis(String name, List<String> options) {
    }

    /** One variant as a point in the matrix: its per-axis {@code selections} plus price/image/stock. */
    public record VariantOption(String sku, Map<String, String> selections, Money price,
                                String imageUrl, Boolean available) {
    }
}
