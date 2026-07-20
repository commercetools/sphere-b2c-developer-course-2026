package com.lifestylehomecorp.project.api.dto;

import java.util.List;

/**
 * Response view model for {@code GET /api/project}. What the HTTP client actually receives.
 * Kept separate from the domain {@link com.lifestylehomecorp.project.domain.ProjectSummary}
 * so the wire contract can evolve independently of the domain.
 */
public record ProjectView(
        String key,
        String name,
        List<String> currencies,
        List<String> languages,
        List<String> countries) {
}
