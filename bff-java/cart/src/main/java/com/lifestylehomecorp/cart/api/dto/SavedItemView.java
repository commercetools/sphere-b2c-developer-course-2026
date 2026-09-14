package com.lifestylehomecorp.cart.api.dto;

import com.lifestylehomecorp.cart.domain.SavedItem;

public record SavedItemView(String lineItemId, String sku, String name, long quantity) {

    public static SavedItemView from(SavedItem i) {
        return new SavedItemView(i.lineItemId(), i.sku(), i.name(), i.quantity());
    }
}
