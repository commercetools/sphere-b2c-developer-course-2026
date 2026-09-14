package com.lifestylehomecorp.customer.application;

/**
 * Inputs for sign-up (5.1): the account fields, the store the customer belongs to (null = a global
 * customer), and the guest identity whose carts / lists / orders the new account adopts. SDK-free —
 * the repository turns this into a {@code CustomerDraft}.
 */
public record SignUpInput(
        String email,
        String password,
        String firstName,
        String lastName,
        String storeKey,
        String anonymousId) {
}
