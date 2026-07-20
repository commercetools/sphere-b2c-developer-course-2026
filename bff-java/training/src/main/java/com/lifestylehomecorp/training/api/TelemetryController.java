package com.lifestylehomecorp.training.api;

import com.lifestylehomecorp.training.telemetry.ApproachTelemetryService;
import com.lifestylehomecorp.training.telemetry.ParticipantTelemetry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Trainer-only approach-telemetry endpoints — how each task was implemented, not just whether it
 * works (see {@code trainer-approach-telemetry.md}). Present only when telemetry is enabled
 * ({@code training.approach-telemetry=on}, the default). These handlers are deliberately NOT
 * {@code @TaskDescription}-annotated, so they are neither tracked nor observed.
 */
@RestController
@ConditionalOnProperty(name = "training.approach-telemetry", havingValue = "on", matchIfMissing = true)
public class TelemetryController {

    private final ApproachTelemetryService telemetry;

    public TelemetryController(ApproachTelemetryService telemetry) {
        this.telemetry = telemetry;
    }

    /** The current participant's per-task approach signals. */
    @GetMapping("/api/training/telemetry")
    public ParticipantTelemetry telemetry() {
        return telemetry.currentTelemetry();
    }

    /** Every participant's approach signals — for the trainer dashboard. */
    @GetMapping("/api/training/telemetry/all")
    public List<ParticipantTelemetry> telemetryAll() {
        return telemetry.allTelemetry();
    }
}
