package com.lifestylehomecorp.customer.domain;

/**
 * A password-reset / email-verification token (5.7 / 5.8). In production the value goes into an
 * EMAIL and never into an HTTP response to the browser — the course has no email service, so the
 * BFF returns it with an explicit demo note so nobody mistakes this for the real pattern.
 */
public record IssuedToken(String tokenValue, String expiresAt, String note) {

    public static final String DEMO_NOTE =
            "DEMO ONLY — in production this token is emailed to the customer, never returned to the browser.";
}
