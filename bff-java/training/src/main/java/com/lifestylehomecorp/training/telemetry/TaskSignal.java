package com.lifestylehomecorp.training.telemetry;

import java.util.List;

/**
 * The trainer-facing verdict for one task attempt: whether the commercetools calls matched the
 * expected approach, with short human-readable flags and the observed call lines for drill-down.
 * This is a flag, not a gate — the task is already complete (2xx); this only says <em>how</em>.
 *
 * @param taskId    "{@code <module>.<session>.<taskNumber>}"
 * @param status    "OK" or "FLAGGED"
 * @param flags     short reasons when FLAGGED, e.g. "unexpected-api: products/search",
 *                  "too-many-calls: 3 (expected <=2)", "missing-predicate: categories(id in"
 * @param callCount number of commercetools calls observed for the task
 * @param observed  the observed call lines, e.g. ["GET product-projections", "GET categories"]
 * @param checkedAt ISO-8601 timestamp of this check
 */
public record TaskSignal(
        String taskId,
        String status,
        List<String> flags,
        int callCount,
        List<String> observed,
        String checkedAt) {

    public static final String OK = "OK";
    public static final String FLAGGED = "FLAGGED";
}
