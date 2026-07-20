package com.lifestylehomecorp.project.api.dto;

import java.util.List;

/** Response view model for store endpoints. */
public record StoreView(
        String key,
        String name,
        List<String> languages,
        List<String> countries,
        List<String> channelKeys) {
}
