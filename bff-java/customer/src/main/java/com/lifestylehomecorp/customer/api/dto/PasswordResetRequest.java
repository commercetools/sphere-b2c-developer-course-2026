package com.lifestylehomecorp.customer.api.dto;

/** Body for POST /api/customers/password/reset — the token from the (emailed) link + the new password. */
public record PasswordResetRequest(String tokenValue, String newPassword) {
}
