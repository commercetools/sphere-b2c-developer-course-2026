package com.lifestylehomecorp.platform.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a domain controller handler as a course task. This annotation is the SINGLE SOURCE OF
 * TRUTH for the task list: scanning it at startup yields the master list, and an interceptor
 * marks a task complete when its endpoint returns 2xx.
 *
 * <p>Lives in {@code platform} so any domain module can annotate its controllers without
 * depending on {@code training}.
 *
 * <p>Deliberately slim: the endpoint path and HTTP method are NOT declared here — they are
 * derived from the Spring mapping annotation on the same method. The stable task id is
 * {@code "<module>.<session>.<taskNumber>"} (used as the progress key), so it survives edits to
 * the title or other metadata.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TaskDescription {

    /** Domain module, e.g. "catalog". */
    String module();

    /** Course session, e.g. "Session 2". */
    String session();

    /** Task number within the session, e.g. 1. */
    int taskNumber();

    /** Short title, e.g. "Get product by key". */
    String title();

    /** "T1" (AI-accelerated) or "T2" (human-in-the-loop). */
    String tier();

    /** Storefront capability this task unlocks, e.g. "catalog.pdp". */
    String capability();

    /** The task goal in plain terms — the "why", shown in Commerce Canvas. */
    String description() default "";

    /** Optional hint shown in Commerce Canvas: the SDK call for T1; doc links / high-level logic hints for T2. */
    String hint() default "";
}
