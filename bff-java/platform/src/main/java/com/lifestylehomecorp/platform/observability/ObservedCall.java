package com.lifestylehomecorp.platform.observability;

/**
 * One observed outbound commercetools API call — the request LINE only, never bodies or credentials.
 * Recorded by the client middleware while a task scope is active (see {@link ApproachContext}) and
 * later compared against the task's expected approach.
 *
 * @param method   HTTP method, e.g. "GET"
 * @param resource API resource path with the project key stripped, e.g. "product-projections" or
 *                 "product-projections/key=chair" or "products/search"
 * @param query    the raw (URL-encoded) query string, or "" — used only for coarse predicate checks
 */
public record ObservedCall(String method, String resource, String query) {

    /** First path segment of the resource ("product-projections/key=x" -> "product-projections"). */
    public String head() {
        int slash = resource.indexOf('/');
        return slash < 0 ? resource : resource.substring(0, slash);
    }
}
