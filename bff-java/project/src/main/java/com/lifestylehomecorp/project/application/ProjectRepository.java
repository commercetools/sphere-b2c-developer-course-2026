package com.lifestylehomecorp.project.application;

import com.commercetools.api.models.project.Project;

/**
 * SDK gateway for the Project. Returns the raw commercetools SDK {@link Project} — the repository
 * (in {@code infrastructure}) contains only the SDK call; {@link ProjectService} maps it to the
 * domain. Lives in {@code application} because it speaks SDK types (keeping {@code domain} SDK-free).
 */
public interface ProjectRepository {

    /** The SDK call to read the current Project. */
    Project fetch();
}
