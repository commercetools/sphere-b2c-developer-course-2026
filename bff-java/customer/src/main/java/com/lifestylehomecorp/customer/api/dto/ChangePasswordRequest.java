package com.lifestylehomecorp.customer.api.dto;

/** Body for POST /api/customers/me/password. */
public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
