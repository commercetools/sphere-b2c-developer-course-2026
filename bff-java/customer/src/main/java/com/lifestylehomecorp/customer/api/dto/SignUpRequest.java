package com.lifestylehomecorp.customer.api.dto;

/** Body for POST /api/customers/signup. {@code store} is optional — null makes a global customer. */
public record SignUpRequest(String email, String password, String firstName, String lastName, String store) {
}
