package com.lifestylehomecorp.training.domain;

/** Per-task completion state persisted for a participant. */
public record TaskProgress(boolean completed, String completedAt) {
}
