package com.lifestylehomecorp.platform.observability;

/**
 * One observed outbound commercetools API call — the request LINE, and (for GraphQL / Product Search
 * calls only) the request BODY, never credentials. Recorded by the client middleware while a task
 * scope is active (see {@link ApproachContext}) and later compared against the task's expected
 * approach.
 *
 * @param method   HTTP method, e.g. "GET"
 * @param resource API resource path with the project key stripped, e.g. "product-projections" or
 *                 "product-projections/key=chair" or "graphql"
 * @param query    the raw (URL-encoded) query string, or "" — used only for coarse predicate checks
 * @param body     the request body for GraphQL / Product Search calls (the search shape lives here,
 *                 not in the query string), else "". Captured only for those endpoints, truncated to
 *                 a safe cap, and never contains Authorization headers or credentials.
 */
public record ObservedCall(String method, String resource, String query, String body) {

    /** Backwards-compatible constructor for callers/tests that don't capture a body (body = ""). */
    public ObservedCall(String method, String resource, String query) {
        this(method, resource, query, "");
    }

    /** First path segment of the resource ("product-projections/key=x" -> "product-projections"). */
    public String head() {
        int slash = resource.indexOf('/');
        return slash < 0 ? resource : resource.substring(0, slash);
    }
}
