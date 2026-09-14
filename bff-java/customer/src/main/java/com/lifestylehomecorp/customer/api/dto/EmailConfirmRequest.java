package com.lifestylehomecorp.customer.api.dto;

/** Body for POST /api/customers/email/confirm — the token from the (emailed) verification link. */
public record EmailConfirmRequest(String tokenValue) {
}
