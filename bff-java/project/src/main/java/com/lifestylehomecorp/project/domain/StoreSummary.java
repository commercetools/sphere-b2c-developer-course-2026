package com.lifestylehomecorp.project.domain;

import java.util.List;

/**
 * Frontend-facing view of a commercetools Store. Introduced in Session 1 at a high level to
 * teach list reads and the platform anatomy (Project → Stores → Channels); the distribution
 * module goes deeper later. No SDK types cross into the domain.
 *
 * @param channelKeys distribution-channel keys referenced by the store (channels, high level)
 */
public record StoreSummary(
        String key,
        String name,
        List<String> languages,
        List<String> countries,
        List<String> channelKeys) {
}
