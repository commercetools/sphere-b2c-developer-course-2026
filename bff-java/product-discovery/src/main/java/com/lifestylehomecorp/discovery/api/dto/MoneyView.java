package com.lifestylehomecorp.discovery.api.dto;

/** Wire model for money: minor units plus currency code. */
public record MoneyView(String currencyCode, long centAmount) {
}
