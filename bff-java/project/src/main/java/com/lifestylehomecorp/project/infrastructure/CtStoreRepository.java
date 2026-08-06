package com.lifestylehomecorp.project.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.store.Store;
import com.lifestylehomecorp.platform.errors.TaskNotImplementedException;
import com.lifestylehomecorp.project.application.StoreRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SDK gateway for Stores — the ONE place participants write commercetools SDK code for the Session 1
 * store tasks. Each method is *only* the SDK fluent call, returning raw SDK types; the mapping to
 * domain models happens in {@link com.lifestylehomecorp.project.application.StoreService}.
 *
 * <p>Compare against {@link CtProjectRepository} for the pattern. Ground every call on the
 * {@code commercetools-knowledge} MCP.
 */
@Repository
public class CtStoreRepository implements StoreRepository {

    private final ProjectApiRoot apiRoot;

    public CtStoreRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public List<Store> findAll() {
        // TODO (Task 1.2): return the project's Stores as a raw SDK List<Store>, with enough on each
        // store for the mapper to surface its distribution-channel keys. Goal + docs in the
        // @TaskDescription; also session-tasks-detailed.md; ground it on the commercetools-knowledge MCP.
        throw new TaskNotImplementedException("1.2");
    }

    @Override
    public Store findByKey(String key) {
        // TODO (Task 1.3): fetch one Store BY KEY (not id), returning the raw SDK Store. A commercetools
        // 404 becomes a clean HTTP 404 via the platform error advice. See the @TaskDescription hint +
        // session-tasks-detailed.md; ground it on the commercetools-knowledge MCP.
        throw new TaskNotImplementedException("1.3");
    }
}
