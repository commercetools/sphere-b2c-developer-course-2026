package com.lifestylehomecorp.cart.domain;

import java.util.List;

/** A guest's saved-items list (wishlist / save-for-later), SDK-free. */
public record SavedList(String id, Long version, String name, List<SavedItem> items) {

    public static SavedList empty() {
        return new SavedList(null, null, "Saved items", List.of());
    }
}
