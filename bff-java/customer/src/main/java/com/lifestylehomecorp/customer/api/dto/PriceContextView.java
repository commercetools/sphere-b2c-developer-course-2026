package com.lifestylehomecorp.customer.api.dto;

import com.lifestylehomecorp.customer.domain.PriceContext;

/** GET /api/price-context — the price-selection tuple the catalogue applies for this shopper. */
public record PriceContextView(
        String currency,
        String country,
        String channel,
        String customerGroupId,
        String customerGroupKey,
        boolean signedIn) {

    public static PriceContextView from(PriceContext p) {
        return new PriceContextView(p.currency(), p.country(), p.channel(), p.customerGroupId(),
                p.customerGroupKey(), p.signedIn());
    }
}
