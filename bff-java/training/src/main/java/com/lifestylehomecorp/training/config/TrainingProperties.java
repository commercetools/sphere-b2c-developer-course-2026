package com.lifestylehomecorp.training.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Participant identity (from .env / config). All participants in a shared training project write
 * to the same "training-progress" container under their own {@code participantId} key, so the
 * trainer dashboard can read the whole container.
 */
@ConfigurationProperties(prefix = "training")
public class TrainingProperties {

    /** Unique participant id — the progress key. */
    private String participantId = "local";

    /** Display name for the dashboard. */
    private String participantName = "Local Participant";

    /** Which ProgressStore to use: "ct" (Custom Objects) or "memory" (local). */
    private String progressStore = "ct";

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getProgressStore() {
        return progressStore;
    }

    public void setProgressStore(String progressStore) {
        this.progressStore = progressStore;
    }
}
