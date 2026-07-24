package com.lifestylehomecorp.catalog.api.dto;

import java.util.List;
import java.util.Map;

/** Wire model for the PDP variant selection matrix (Task 2.6). */
public record VariantMatrixView(String key, String defaultSku, List<AxisView> axes, List<VariantOptionView> variants) {

    public record AxisView(String name, List<String> options) {
    }

    public record VariantOptionView(String sku, Map<String, String> selections, MoneyView price,
                                    String imageUrl, Boolean available) {
    }
}
