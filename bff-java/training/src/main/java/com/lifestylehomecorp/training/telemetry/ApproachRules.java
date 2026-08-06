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
                        List.of("product-projections"), List.of("product-projections"), 2, Map.of()),

                // ---- Session 3 · catalog (in-store PDP) ----
                rule("catalog", "Session 3", 2, // In-store PDP — in-store by-key lookup + contextual price
                        List.of("in-store", "channels"), List.of("in-store"), 2,
                        Map.of("in-store", List.of("priceCurrency", "priceCountry", "priceChannel")),
                        List.of("key=")),

                // ---- Session 3 · product-discovery (Product Search, one hydrated GraphQL call) ----
                // Needles are matched inside the GraphQL request BODY (the search shape lives there, not
                // in the URL). stores+channels are allowed as they resolve the key→id lookups (cached, so a
                // warm search is 1 graphql call); a REST Product Search + product-projections hydration
                // would show `products`/`product-projections` heads and trip unexpected-api + too-many-calls.
                rule("product-discovery", "Session 3", 1, // Store-scoped search — store scope + price context
                        List.of("graphql", "stores", "channels"), List.of("graphql"), 4,
                        Map.of("graphql", List.of("stores", "channelId"))),
                rule("product-discovery", "Session 3", 3, // Full-text — fullText clause inside the store scope
                        List.of("graphql", "stores", "channels"), List.of("graphql"), 4,
                        Map.of("graphql", List.of("fullText", "stores"))),
                rule("product-discovery", "Session 3", 4, // Browse by category — categoriesSubTree (2.4 graduates)
                        // `categories` allowed: the service resolves the category key→id (cached) before the search.
                        List.of("graphql", "stores", "channels", "categories"), List.of("graphql"), 4,
                        Map.of("graphql", List.of("categoriesSubTree", "stores"))),
                rule("product-discovery", "Session 3", 5, // Facets — facets computed with the store-scoped results
                        List.of("graphql", "stores", "channels"), List.of("graphql"), 4,
                        Map.of("graphql", List.of("facets", "stores"))),
                rule("product-discovery", "Session 3", 6, // PLP (T2) — ONE composed search (catch N+1)
                        // `categories` allowed: the composed PLP resolves the category key→id when one is set.
                        List.of("graphql", "stores", "channels", "categories"), List.of("graphql"), 4,
                        Map.of("graphql", List.of("stores"))),
                rule("product-discovery", "Session 3", 7, // Post-filter (T2) — selection must go to postFilter
                        List.of("graphql", "stores", "channels"), List.of("graphql"), 4,
                        Map.of("graphql", List.of("postFilter", "stores"))),
                rule("product-discovery", "Session 3", 8, // Configurable facets (T2) — read product-types, then faceted search
                        List.of("graphql", "stores", "channels", "product-types"),
                        List.of("graphql", "product-types"), 4,
                        Map.of("graphql", List.of("facets", "stores"))));
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
