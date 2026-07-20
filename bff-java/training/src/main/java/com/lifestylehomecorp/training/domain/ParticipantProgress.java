package com.lifestylehomecorp.training.domain;

import java.util.List;

/**
 * The ONLY persisted state: one participant's progress, stored as the {@code value} of a
 * commercetools Custom Object (container "training-progress", key = participantId).
 *
 * <p>{@code tasks} is a LIST of {@link TaskEntry} (not a map keyed by taskId) because commercetools
 * Custom Object field names cannot contain dots — and task ids do (e.g. "project.Session 1.1").
 */
public record ParticipantProgress(
        String participantId,
        String participantName,
        String updatedAt,
        List<TaskEntry> tasks) {

    public static ParticipantProgress empty(String participantId, String participantName, String updatedAt) {
        return new ParticipantProgress(participantId, participantName, updatedAt, List.of());
    }
}
