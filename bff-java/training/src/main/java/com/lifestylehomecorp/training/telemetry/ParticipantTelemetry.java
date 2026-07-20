package com.lifestylehomecorp.training.telemetry;

import java.util.List;

/**
 * One participant's approach signals — the persisted value of a Custom Object (container
 * "training-telemetry", key = participantId), separate from "training-progress" so it never bloats
 * progress. {@code signals} is a LIST (not a map keyed by taskId) because Custom Object field names
 * cannot contain dots — and task ids do (mirrors {@code ParticipantProgress}).
 */
public record ParticipantTelemetry(
        String participantId,
        String participantName,
        String updatedAt,
        List<TaskSignal> signals) {

    public static ParticipantTelemetry empty(String participantId, String participantName, String updatedAt) {
        return new ParticipantTelemetry(participantId, participantName, updatedAt, List.of());
    }
}
