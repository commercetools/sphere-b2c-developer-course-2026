package com.lifestylehomecorp.training.progress;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraft;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifestylehomecorp.training.domain.ParticipantProgress;
import io.vrap.rmf.base.client.ApiHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persists progress in a commercetools Custom Object: container "training-progress",
 * key = participantId, value = {@link ParticipantProgress}. Version-aware upsert (read current
 * version, write, retry once on 409; create when 404). Default store.
 */
@Component
@ConditionalOnProperty(name = "training.progress-store", havingValue = "ct", matchIfMissing = true)
public class CtCustomObjectProgressStore implements ProgressStore {

    private static final Logger log = LoggerFactory.getLogger(CtCustomObjectProgressStore.class);
    private static final String CONTAINER = "training-progress";

    private final ProjectApiRoot apiRoot;
    private final ObjectMapper objectMapper;

    public CtCustomObjectProgressStore(ProjectApiRoot apiRoot, ObjectMapper objectMapper) {
        this.apiRoot = apiRoot;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ParticipantProgress> load(String participantId) {
        try {
            CustomObject co = apiRoot.customObjects()
                    .withContainerAndKey(CONTAINER, participantId)
                    .get()
                    .executeBlocking()
                    .getBody();
            return Optional.of(toProgress(co));
        } catch (ApiHttpException e) {
            if (e.getStatusCode() != 404) {
                log.warn("Could not load progress for {}: {}", participantId, e.getMessage());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Could not load progress for {}: {}", participantId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(ParticipantProgress progress) {
        saveWithRetry(progress, 1);
    }

    private void saveWithRetry(ParticipantProgress progress, int retriesLeft) {
        Long version = currentVersion(progress.participantId());
        Object value = objectMapper.convertValue(progress, Map.class);
        CustomObjectDraftBuilder draft = CustomObjectDraftBuilder.of()
                .container(CONTAINER)
                .key(progress.participantId())
                .value(value);
        if (version != null) {
            draft.version(version);
        }
        try {
            apiRoot.customObjects().post(draft.build()).executeBlocking();
        } catch (ApiHttpException e) {
            if (e.getStatusCode() == 409 && retriesLeft > 0) {
                saveWithRetry(progress, retriesLeft - 1);
            } else {
                throw e;
            }
        }
    }

    private Long currentVersion(String participantId) {
        try {
            return apiRoot.customObjects()
                    .withContainerAndKey(CONTAINER, participantId)
                    .get()
                    .executeBlocking()
                    .getBody()
                    .getVersion();
        } catch (Exception e) {
            return null; // not found yet -> create
        }
    }

    @Override
    public List<ParticipantProgress> loadAll() {
        try {
            List<CustomObject> results = apiRoot.customObjects()
                    .withContainer(CONTAINER)
                    .get()
                    .executeBlocking()
                    .getBody()
                    .getResults();
            return results.stream().map(this::toProgress).toList();
        } catch (Exception e) {
            log.warn("Could not load all progress: {}", e.getMessage());
            return List.of();
        }
    }

    private ParticipantProgress toProgress(CustomObject co) {
        return objectMapper.convertValue(co.getValue(), ParticipantProgress.class);
    }
}
