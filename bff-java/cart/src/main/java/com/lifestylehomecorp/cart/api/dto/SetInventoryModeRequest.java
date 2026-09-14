package com.lifestylehomecorp.cart.api.dto;

/** Body for PUT /api/cart/line-items/{id}/inventory-mode (4.6): None / ReserveOnCart / ReserveOnOrder / TrackOnly. */
public record SetInventoryModeRequest(String inventoryMode) {
}
