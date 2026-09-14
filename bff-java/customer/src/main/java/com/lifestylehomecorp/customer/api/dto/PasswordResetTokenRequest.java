package com.lifestylehomecorp.customer.api.dto;

/** Body for POST /api/customers/password/reset-token ("Forgot password?"). */
public record PasswordResetTokenRequest(String email) {
}
