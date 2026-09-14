package com.lifestylehomecorp.customer.api.dto;

/** Body for PATCH /api/customers/me — any subset; nulls are left untouched. */
public record UpdateProfileRequest(String firstName, String lastName, String email) {
}
