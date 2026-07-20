package com.lifestylehomecorp.project.api.dto;

import java.util.List;

/** Response view model for the active store/region context. */
public record StoreContextView(
        String activeStoreKey,
        String activeStoreName,
        List<String> availableStoreKeys,
        List<String> languages,
        List<String> countries,
        List<String> distributionChannelKeys) {
}
