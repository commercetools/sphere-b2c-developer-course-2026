package com.lifestylehomecorp.cart.api.dto;

/**
 * Body for PUT /api/cart/line-items/{id}/channel (4.5) — set the distribution channel (drives the
 * channel price) and/or the supply channel (inventory source / pickup location), by channel key.
 */
public record SetChannelRequest(String distributionChannelKey, String supplyChannelKey) {
}
