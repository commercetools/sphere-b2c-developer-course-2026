package com.lifestylehomecorp.project.api;

import com.lifestylehomecorp.project.api.dto.ProjectView;
import com.lifestylehomecorp.project.domain.ProjectSummary;

/**
 * Maps the domain {@link ProjectSummary} to the {@link ProjectView} wire model.
 * Trivial here, but it keeps the api layer as the single translation point.
 */
public final class ProjectViewMapper {

    private ProjectViewMapper() {
    }

    public static ProjectView toView(ProjectSummary summary) {
        return new ProjectView(
                summary.key(),
                summary.name(),
                summary.currencies(),
                summary.languages(),
                summary.countries());
    }
}
