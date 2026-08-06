package com.lifestylehomecorp.platform.observability;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Trainer-only, request-scoped record of the commercetools calls a single task endpoint makes,
 * used to check <em>how</em> a participant implemented the task (not just that it works). See
 * {@code courses/b2c-dev/planning/trainer-approach-telemetry.md}.
 *
 * <p>The scope is a {@link ThreadLocal}: an MVC interceptor opens it in {@code preHandle} for a
 * {@code @TaskDescription} handler, the commercetools client middleware appends each outbound call
 * (on the blocking path the middleware runs on the same servlet thread), and the interceptor reads
 * and closes it in {@code afterCompletion}. Calls made outside a scope — progress writes, startup
 * loads, the telemetry write itself (all on other threads) — are ignored.
 *
 * <p>Entirely passive and defensive: recording never throws and never affects the real request.
 */
public final class ApproachContext {

    /** One task's in-flight observation: which task, and the calls seen so far. */
    public static final class Scope {
        private final String taskId;
        private final List<ObservedCall> calls = new ArrayList<>();

        private Scope(String taskId) {
            this.taskId = taskId;
        }

        public String taskId() {
            return taskId;
        }

        public List<ObservedCall> calls() {
            return List.copyOf(calls);
        }
    }

    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();

    private ApproachContext() {
    }

    /** Open a scope for the given task id on this thread (replaces any stale scope). */
    public static void begin(String taskId) {
        CURRENT.set(new Scope(taskId));
    }

    /** The active scope on this thread, or {@code null} when none is open. */
    public static Scope current() {
        return CURRENT.get();
    }

    /** Close and return the active scope on this thread (clears the ThreadLocal). */
    public static Scope end() {
        Scope scope = CURRENT.get();
        CURRENT.remove();
        return scope;
    }

    /** Max captured body length — GraphQL search documents are small; cap defensively regardless. */
    private static final int MAX_BODY = 4000;

    /**
     * Record one outbound commercetools call if a scope is open on this thread. The project key
     * (first path segment) is stripped so resources are stable, e.g. "product-projections". Never
     * throws — any failure is swallowed so telemetry can't affect the real call.
     */
    public static void record(String method, URI uri) {
        record(method, uri, "");
    }

    /**
     * As {@link #record(String, URI)}, but also stores the request {@code body} (for GraphQL /
     * Product Search calls, where the query shape lives in the body, not the URL). The body is
     * truncated to a safe cap. Never throws.
     */
    public static void record(String method, URI uri, String body) {
        try {
            Scope scope = CURRENT.get();
            if (scope == null || uri == null) {
                return;
            }
            scope.calls.add(new ObservedCall(
                    method == null ? "" : method,
                    resourceOf(uri),
                    uri.getRawQuery() == null ? "" : uri.getRawQuery(),
                    truncate(body)));
        } catch (RuntimeException ignored) {
            // Telemetry must never affect the request.
        }
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > MAX_BODY ? body.substring(0, MAX_BODY) : body;
    }

    /** Path with the leading "/{projectKey}" segment removed, e.g. "/proj/product-projections" -> "product-projections". */
    private static String resourceOf(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            return "";
        }
        String p = path.startsWith("/") ? path.substring(1) : path;
        int slash = p.indexOf('/');
        return slash < 0 ? "" : p.substring(slash + 1); // drop the project-key segment
    }
}
