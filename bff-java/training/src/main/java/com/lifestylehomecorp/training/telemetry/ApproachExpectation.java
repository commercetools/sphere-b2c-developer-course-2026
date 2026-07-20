package com.lifestylehomecorp.training.telemetry;

import java.util.List;
import java.util.Map;

/**
 * One task's expected commercetools approach — trainer-owned, deliberately coarse (an allow-list, a
 * max call count, and optionally one required predicate substring). Lives in the training module, not
 * in the participant-facing controllers, so the "answer key" isn't in their working files.
 *
 * @param taskId          "{@code <module>.<session>.<taskNumber>}"
 * @param allowedHeads    resource heads the task may call (e.g. "product-projections", "categories");
 *                        any call outside this set is flagged as an unexpected API
 * @param requiredHeads   resource heads that MUST appear (a subset of allowed); missing = flag
 * @param maxRequests     max commercetools calls the task's shape needs; more = N+1 / fetch-all flag
 * @param requiredParams  head -> query substrings that must appear on at least one matching call
 *                        (checked only when that head was called); a missing one flags e.g.
 *                        fetch-all-then-filter. Substrings are matched against the URL-decoded query.
 * @param requiredResourceSubstrings substrings that must appear in at least one observed call's
 *                        RESOURCE path (not query) — e.g. "key=" to require a by-key lookup. Catches
 *                        a fetch-all-then-filter for a get-by-key task, where the list call hits the
 *                        same resource head as the by-key GET so the head/count checks can't tell them
 *                        apart. Empty for most tasks.
 */
public record ApproachExpectation(
        String taskId,
        List<String> allowedHeads,
        List<String> requiredHeads,
        int maxRequests,
        Map<String, List<String>> requiredParams,
        List<String> requiredResourceSubstrings) {
}
