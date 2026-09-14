package com.lifestylehomecorp.cart.api.dto;

import com.lifestylehomecorp.cart.domain.ShippingOption;

public record ShippingOptionView(String id, String key, String name, boolean isDefault) {

    public static ShippingOptionView from(ShippingOption o) {
        return new ShippingOptionView(o.id(), o.key(), o.name(), o.isDefault());
    }
}
