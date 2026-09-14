package com.lifestylehomecorp.cart.domain;

/** SDK-free money value — minor units + currency (commercetools money is centAmount + currencyCode). */
public record Money(long centAmount, String currencyCode, int fractionDigits) {

    public static Money zero(String currencyCode) {
        return new Money(0, currencyCode, 2);
    }
}
