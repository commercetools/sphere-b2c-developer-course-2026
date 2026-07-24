package com.lifestylehomecorp.training.domain;

/**
 * A course task, derived at startup from a {@code @TaskDescription} on a controller handler plus
 * that handler's request mapping. The master list is DERIVED, never persisted.
 *
 * @param id          stable id "{@code <module>.<session>.<taskNumber>}" — the progress key
 * @param endpoint    derived from the mapping annotation (e.g. "/api/products/{key}")
 * @param httpMethod  derived from the mapping annotation (e.g. "GET")
 */
public record TaskDefinition(
        String id,
        String module,
        String session,
        int taskNumber,
        String title,
        String tier,
        String capability,
        String endpoint,
        String httpMethod,
        String description,
        String hint,
        java.util.List<String> decisions) {

    /** The stable task id — derived only from fields that don't change when metadata is edited. */
    public static String idOf(String module, String session, int taskNumber) {
        return module + "." + session + "." + taskNumber;
    }
}
