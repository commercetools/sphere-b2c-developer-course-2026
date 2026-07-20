package com.lifestylehomecorp.training.domain;

/**
 * One persisted task-completion entry. Stored inside {@link ParticipantProgress#tasks()} as an
 * ARRAY element (not a map keyed by id) — commercetools Custom Object field names cannot contain
 * dots, and our task ids do (e.g. "project.Session 1.1"), so the id lives as a value here.
 */
public record TaskEntry(String id, boolean completed, String completedAt) {
}
