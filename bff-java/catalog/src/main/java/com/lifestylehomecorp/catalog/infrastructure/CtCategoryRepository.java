package com.lifestylehomecorp.catalog.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.lifestylehomecorp.catalog.application.CategoryRepository;
import com.lifestylehomecorp.platform.errors.TaskNotImplementedException;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SDK gateway for categories — a participant TODO. Only the SDK call; mapping is in the service.
 * See {@link CtProductRepository} for the pattern.
 */
@Repository
public class CtCategoryRepository implements CategoryRepository {

    private final ProjectApiRoot apiRoot;

    public CtCategoryRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public List<Category> findAll() {
        // TODO (Task 2.3): return the project's categories as a raw SDK List<Category> for the
        // storefront's category navigation. Goal + docs in the @TaskDescription; also
        // session-tasks-detailed.md; ground it on the commercetools-knowledge MCP.
        throw new TaskNotImplementedException("2.3");
    }
}
