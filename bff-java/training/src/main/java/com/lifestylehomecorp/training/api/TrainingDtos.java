package com.lifestylehomecorp.training.api;

import java.util.List;
import java.util.Map;

/** Response view models for the training endpoints (records, not Map<String,Object>). */
public final class TrainingDtos {

    private TrainingDtos() {
    }

    /** One task in the master list, with the current participant's completion flag. */
    public record TaskItem(
            String id, String module, String session, int taskNumber, String title,
            String tier, String capability, String endpoint, String httpMethod,
            String description, String hint, boolean completed) {
    }

    public record SessionGroup(String session, List<TaskItem> tasks) {
    }

    /** Master list grouped module -> session. */
    public record ModuleGroup(String module, List<SessionGroup> sessions) {
    }

    public record ProgressResponse(
            String participantId, int completed, int total, Map<String, Boolean> perTask) {
    }

    public record Counts(int completed, int total) {
    }

    /** One participant row for the trainer dashboard. */
    public record ParticipantSummary(
            String participantId, String participantName, int completed, int total,
            String updatedAt, Map<String, Counts> perSession, Map<String, Counts> perTier,
            Map<String, Boolean> perTask) {
    }

    /** What the Storefront FeatureGate consumes. */
    public record CapabilitiesResponse(List<String> unlocked) {
    }
}
