package com.lifestylehomecorp.training.telemetry;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.custom_object.CustomObject;
import com.commercetools.api.models.custom_object.CustomObjectDraftBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vrap.rmf.base.client.ApiHttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persists approach telemetry in a commercetools Custom Object: container "training-telemetry",
 * key = participantId, value = {@link ParticipantTelemetry}. Version-aware upsert (read version,
 * write, retry once on 409; create on 404) — mirrors {@code CtCustomObjectProgressStore}. Default
 * store; swap to in-memory with {@code training.progress-store=memory}.
 */
@Component
@ConditionalOnProperty(name = "training.progress-store", havingValue = "ct", matchIfMissing = true)
public class CtApproachTelemetryStore implements ApproachTelemetryStore {

    private static final Logger log = LoggerFactory.getLogger(CtApproachTelemetryStore.class);
    private static final String CONTAINER = "training-telemetry";

    private final ProjectApiRoot apiRoot;
    private final ObjectMapper objectMapper;

    public CtApproachTelemetryStore(ProjectApiRoot apiRoot, ObjectMapper objectMapper) {
        this.apiRoot = apiRoot;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ParticipantTelemetry> load(String participantId) {
        try {
            CustomObject co = apiRoot.customObjects()
                    .withContainerAndKey(CONTAINER, participantId)
                    .get()
                    .executeBlocking()
                    .getBody();
            return Optional.of(toTelemetry(co));
        } catch (ApiHttpException e) {
            if (e.getStatusCode() != 404) {
                log.warn("Could not load telemetry for {}: {}", participantId, e.getMessage());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Could not load telemetry for {}: {}", participantId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(ParticipantTelemetry telemetry) {
        saveWithRetry(telemetry, 1);
    }

    private void saveWithRetry(ParticipantTelemetry telemetry, int retriesLeft) {
        Long version = currentVersion(telemetry.participantId());
        Object value = objectMapper.convertValue(telemetry, Map.class);
        CustomObjectDraftBuilder draft = CustomObjectDraftBuilder.of()
                .container(CONTAINER)
                .key(telemetry.participantId())
                .value(value);
        if (version != null) {
            draft.version(version);
        }
        try {
            apiRoot.customObjects().post(draft.build()).executeBlocking();
        } catch (ApiHttpException e) {
            if (e.getStatusCode() == 409 && retriesLeft > 0) {
                saveWithRetry(telemetry, retriesLeft - 1);
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
    public List<ParticipantTelemetry> loadAll() {
        try {
            List<CustomObject> results = apiRoot.customObjects()
                    .withContainer(CONTAINER)
                    .get()
                    .executeBlocking()
                    .getBody()
                    .getResults();
            return results.stream().map(this::toTelemetry).toList();
        } catch (Exception e) {
            log.warn("Could not load all telemetry: {}", e.getMessage());
            return List.of();
        }
    }

    private ParticipantTelemetry toTelemetry(CustomObject co) {
        return objectMapper.convertValue(co.getValue(), ParticipantTelemetry.class);
    }
}
