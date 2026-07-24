package com.lifestylehomecorp.catalog.domain;

/**
 * Domain money value. commercetools money is minor units ({@code centAmount}) plus a
 * {@code currencyCode} — never a decimal. We keep that shape in the domain rather than
 * collapsing to a float.
 */
public record Money(String currencyCode, long centAmount) {
}
