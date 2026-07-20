package com.lifestylehomecorp.project.domain;

import java.util.List;

/**
 * Frontend-facing view of a commercetools Project. A plain domain record — it deliberately
 * carries NO commercetools SDK types, so the rest of the app never depends on the SDK.
 */
public record ProjectSummary(
        String key,
        String name,
        List<String> currencies,
        List<String> languages,
        List<String> countries) {
}
