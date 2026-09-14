package com.lifestylehomecorp.customer.api.dto;

import com.lifestylehomecorp.customer.domain.IssuedToken;

/** A reset / verification token — returned ONLY because the course has no email service (see {@code note}). */
public record TokenView(String tokenValue, String expiresAt, String note) {

    public static TokenView from(IssuedToken t) {
        return new TokenView(t.tokenValue(), t.expiresAt(), t.note());
    }
}
