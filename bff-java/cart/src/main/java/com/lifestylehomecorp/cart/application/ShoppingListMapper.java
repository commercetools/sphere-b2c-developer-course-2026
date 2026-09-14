package com.lifestylehomecorp.cart.application;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListLineItem;
import com.lifestylehomecorp.cart.domain.SavedItem;
import com.lifestylehomecorp.cart.domain.SavedList;

import java.util.ArrayList;
import java.util.List;

/** Maps the raw SDK {@link ShoppingList} into the SDK-free {@link SavedList} the storefront renders. */
public final class ShoppingListMapper {

    private ShoppingListMapper() {
    }

    public static SavedList toDomain(ShoppingList list) {
        List<SavedItem> items = new ArrayList<>();
        if (list.getLineItems() != null) {
            for (ShoppingListLineItem li : list.getLineItems()) {
                String sku = li.getVariant() != null ? li.getVariant().getSku() : null;
                long qty = li.getQuantity() != null ? li.getQuantity() : 1L;
                items.add(new SavedItem(li.getId(), sku, localized(li.getName(), sku), qty));
            }
        }
        return new SavedList(list.getId(), list.getVersion(), localized(list.getName(), "Saved items"), items);
    }

    private static String localized(LocalizedString name, String fallback) {
        if (name == null) {
            return fallback;
        }
        for (String locale : List.of("en-GB", "en-US", "en", "de-DE")) {
            String v = name.get(locale);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return name.values().values().stream().findFirst().orElse(fallback);
    }
}
