package com.lifestylehomecorp.project.domain;

import java.util.List;

/**
 * The resolved "active" store/region for the storefront — the result of the Session 1 Tier-2
 * task. How the active store is chosen (and the fallback when it is missing/misconfigured) is
 * the participant's design decision.
 */
public record StoreContext(
        String activeStoreKey,
        String activeStoreName,
        List<String> availableStoreKeys,
        List<String> languages,
        List<String> countries,
        List<String> distributionChannelKeys) {
}
