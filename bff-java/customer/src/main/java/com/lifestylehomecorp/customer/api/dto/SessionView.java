package com.lifestylehomecorp.customer.api.dto;

import com.lifestylehomecorp.customer.domain.ShopperContext;

/** GET /api/session — what the browser may know: signed in, a first name, a pricing tier. No ids. */
public record SessionView(boolean signedIn, String firstName, String customerGroupKey) {

    public static SessionView from(ShopperContext c) {
        return new SessionView(c.signedIn(), c.firstName(), c.customerGroupKey());
    }
}
