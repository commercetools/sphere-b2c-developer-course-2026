package com.lifestylehomecorp.project.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.project.Project;
import com.lifestylehomecorp.project.application.ProjectRepository;
import org.springframework.stereotype.Repository;

/**
 * SDK gateway for the Project — the worked REFERENCE. It is *only* the SDK fluent call, returning
 * the raw SDK {@link Project}; the mapping to the domain happens in
 * {@link com.lifestylehomecorp.project.application.ProjectService}. This is the exact shape every
 * task's repository follows: repository = SDK call, service = mapping/logic.
 */
@Repository
public class CtProjectRepository implements ProjectRepository {

    private final ProjectApiRoot apiRoot;

    public CtProjectRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public Project fetch() {
        // GET /{projectKey} — the project's own settings. A single-resource GET: getBody() IS the Project.
        return apiRoot.get().executeBlocking().getBody();
    }
}
