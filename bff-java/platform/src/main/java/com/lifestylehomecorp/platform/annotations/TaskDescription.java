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

    /**
     * Optional hint shown in Commerce Canvas: a SHORT pointer to the relevant commercetools docs so a
     * participant can explore further — deliberately NOT the full SDK call (that lives, promptably, in
     * {@link #description()}).
     */
    String hint() default "";

    /**
     * For T2 tasks only: the design decisions the participant must make and defend — shown in Commerce
     * Canvas as "Decisions you own" so the human-in-the-loop understands what to decide before letting
     * AI help implement. Empty for T1 (there is one mechanical SDK call, no decision to own).
     */
    String[] decisions() default {};
}
