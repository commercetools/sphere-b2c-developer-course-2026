package com.lifestylehomecorp.training.telemetry;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for per-participant approach telemetry. Pluggable like {@code ProgressStore}: a
 * commercetools Custom Object store (default) or a local in-memory store for dev/tests.
 */
public interface ApproachTelemetryStore {

    /** Load one participant's telemetry, or empty if none stored yet. */
    Optional<ParticipantTelemetry> load(String participantId);

    /** Version-aware upsert of one participant's telemetry. */
    void save(ParticipantTelemetry telemetry);

    /** Load every participant's telemetry (trainer dashboard). */
    List<ParticipantTelemetry> loadAll();
}
