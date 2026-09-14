package com.lifestylehomecorp.customer.api.dto;

/** Body for POST /api/customers/login. */
public record LoginRequest(String email, String password) {
}
