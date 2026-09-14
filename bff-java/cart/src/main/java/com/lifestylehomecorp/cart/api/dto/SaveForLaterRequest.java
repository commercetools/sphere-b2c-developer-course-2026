package com.lifestylehomecorp.cart.api.dto;

/** Request body for POST /api/shopping-list/save-for-later — the cart line to move to the saved list. */
public record SaveForLaterRequest(String lineItemId) {
}
