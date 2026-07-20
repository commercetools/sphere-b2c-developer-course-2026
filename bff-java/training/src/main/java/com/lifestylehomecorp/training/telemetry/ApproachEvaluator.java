package com.lifestylehomecorp.training.telemetry;

import com.lifestylehomecorp.platform.observability.ObservedCall;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Compares the commercetools calls a task actually made against its {@link ApproachExpectation} and
 * produces a {@link TaskSignal}. Pure logic — no I/O, no side effects. Raises a flag when an API
 * outside the allow-list was used, the call count exceeds the max (N+1 / fetch-all), a required call
 * is missing, or a required predicate substring is absent on the expected call.
 */
@Component
public class ApproachEvaluator {

    public TaskSignal evaluate(ApproachExpectation exp, List<ObservedCall> calls) {
        List<String> flags = new ArrayList<>();

        // Distinct resource heads actually called, in first-seen order.
        Set<String> heads = new LinkedHashSet<>();
        for (ObservedCall c : calls) {
            heads.add(c.head());
        }

        // 1) Unexpected API — any head outside the allow-list.
        for (String head : heads) {
            if (!exp.allowedHeads().contains(head)) {
                flags.add("unexpected-api: " + head);
            }
        }

        // 2) Missing required call.
        for (String req : exp.requiredHeads()) {
            if (!heads.contains(req)) {
                flags.add("missing-call: " + req);
            }
        }

        // 3) Too many calls — N+1 / fetch-all shape.
        if (calls.size() > exp.maxRequests()) {
            flags.add("too-many-calls: " + calls.size() + " (expected <=" + exp.maxRequests() + ")");
        }

        // 4) Missing predicate — checked only when that resource head was actually called.
        exp.requiredParams().forEach((head, needles) -> {
            if (!heads.contains(head)) {
                return; // conditional call not made (e.g. unknown key) — don't flag
            }
            for (String needle : needles) {
                boolean seen = calls.stream()
                        .filter(c -> c.head().equals(head))
                        .anyMatch(c -> decode(c.query()).contains(needle));
                if (!seen) {
                    flags.add("missing-predicate: " + needle);
                }
            }
        });

        // 5) Missing required resource marker (e.g. a by-key lookup) — catches fetch-all-then-filter
        //    for a get-by-key task, where the list call hits the same resource head as the by-key GET.
        for (String needle : exp.requiredResourceSubstrings()) {
            boolean seen = calls.stream().anyMatch(c -> c.resource().contains(needle));
            if (!seen) {
                flags.add("missing-by-key-access (no " + needle + " lookup)");
            }
        }

        List<String> observed = calls.stream()
                .map(c -> c.method() + " " + c.resource())
                .toList();
        String status = flags.isEmpty() ? TaskSignal.OK : TaskSignal.FLAGGED;
        return new TaskSignal(exp.taskId(), status, flags, calls.size(), observed,
                Instant.now().toString());
    }

    private static String decode(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        try {
            return URLDecoder.decode(query, StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return query;
        }
    }
}
