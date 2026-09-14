package com.lifestylehomecorp.customer.api.dto;

/** Body for PUT /api/customers/me/group — the customer group KEY (e.g. "vip"). */
public record SetGroupRequest(String customerGroupKey) {
}
