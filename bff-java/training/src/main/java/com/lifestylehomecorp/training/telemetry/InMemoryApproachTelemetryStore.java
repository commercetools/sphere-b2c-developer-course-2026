package com.lifestylehomecorp.training.telemetry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local, non-persistent telemetry store — the pluggable alternative to the commercetools store
 * (dev/tests). Selected with {@code training.progress-store=memory}, alongside the in-memory
 * progress store.
 */
@Component
@ConditionalOnProperty(name = "training.progress-store", havingValue = "memory")
public class InMemoryApproachTelemetryStore implements ApproachTelemetryStore {

    private final ConcurrentHashMap<String, ParticipantTelemetry> store = new ConcurrentHashMap<>();

    @Override
    public Optional<ParticipantTelemetry> load(String participantId) {
        return Optional.ofNullable(store.get(participantId));
    }

    @Override
    public void save(ParticipantTelemetry telemetry) {
        store.put(telemetry.participantId(), telemetry);
    }

    @Override
    public List<ParticipantTelemetry> loadAll() {
        return List.copyOf(store.values());
    }
}
