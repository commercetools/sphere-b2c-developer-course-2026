package com.lifestylehomecorp.cart.api.dto;

import com.lifestylehomecorp.cart.domain.SavedList;

import java.util.List;

public record SavedListView(String id, String name, List<SavedItemView> items, int itemCount) {

    public static SavedListView from(SavedList l) {
        List<SavedItemView> items = l.items().stream().map(SavedItemView::from).toList();
        return new SavedListView(l.id(), l.name(), items, items.size());
    }
}
