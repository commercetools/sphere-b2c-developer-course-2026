package com.lifestylehomecorp.training.telemetry;

import com.lifestylehomecorp.training.domain.TaskDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * The expected-approach table for the tasks that have one (Sessions 1 & 2 for now). Keyed by task id.
 * A task with no rule here simply produces no signal — telemetry is opt-in per task. Keep the rules
 * coarse on purpose (see {@code trainer-approach-telemetry.md}); widen an allow-list or raise a max
 * rather than adding precision if a rule produces false alarms.
 */
@Component
public class ApproachRules {

    private final Map<String, ApproachExpectation> byTaskId;

    public ApproachRules() {
        this.byTaskId = Map.ofEntries(
                // ---- Session 1 · project / stores ----
                rule("project", "Session 1", 2, // List stores
                        List.of("stores"), List.of("stores"), 1, Map.of()),
                rule("project", "Session 1", 3, // Get store by key — must be a by-key lookup, not fetch-all+filter
                        List.of("stores"), List.of("stores"), 1, Map.of(), List.of("key=")),
                rule("project", "Session 1", 4, // Active store / region (T2) — composes findAll (+ optional active lookup)
                        List.of("stores"), List.of("stores"), 2, Map.of()),

                // ---- Session 2 · catalog ----
                rule("catalog", "Session 2", 1, // List products (PLP) — must select the shopper's CONTEXTUAL price
                        List.of("product-projections"), List.of("product-projections"), 1,
                        Map.of("product-projections",
                                List.of("priceCurrency", "priceCountry", "priceChannel"))),
                rule("catalog", "Session 2", 2, // Get product by key — by-key lookup + contextual price
                        List.of("product-projections"), List.of("product-projections"), 1,
                        Map.of("product-projections",
                                List.of("priceCurrency", "priceCountry", "priceChannel")),
                        List.of("key=")),
                rule("catalog", "Session 2", 3, // List categories
                        List.of("categories"), List.of("categories"), 1, Map.of()),
                rule("catalog", "Session 2", 4, // Browse by category (T2): resolve category + filtered query
                        List.of("categories", "product-projections"), List.of("categories"), 2,
                        Map.of("product-projections", List.of("categories(id in"))),
                rule("catalog", "Session 2", 5, // Slug routing (T2): locale fallback chain (requested + 3 project locales)
                        List.of("product-projections"), List.of("product-projections"), 4,
                        Map.of("product-projections", List.of("slug("))),
                rule("catalog", "Session 2", 6, // Variant matrix (T2): one read by key
                        List.of("product-projections"), List.of("product-projections"), 1, Map.of()),
                rule("catalog", "Session 2", 7, // Bundle (T2): findByKey + findByIds
                        List.of("product-projections"), List.of("product-projections"), 2, Map.of()));
    }

    /** The expectation for a task, or {@code null} if the task has no rule. */
    public ApproachExpectation forTask(String taskId) {
        return byTaskId.get(taskId);
    }

    private static Map.Entry<String, ApproachExpectation> rule(
            String module, String session, int taskNumber,
            List<String> allowed, List<String> required, int maxRequests,
            Map<String, List<String>> requiredParams) {
        return rule(module, session, taskNumber, allowed, required, maxRequests, requiredParams, List.of());
    }

    private static Map.Entry<String, ApproachExpectation> rule(
            String module, String session, int taskNumber,
            List<String> allowed, List<String> required, int maxRequests,
            Map<String, List<String>> requiredParams, List<String> requiredResourceSubstrings) {
        String id = TaskDefinition.idOf(module, session, taskNumber);
        return Map.entry(id, new ApproachExpectation(
                id, allowed, required, maxRequests, requiredParams, requiredResourceSubstrings));
    }
}
