package com.tft.coach.state.observation;

import java.time.Instant;
import java.util.UUID;

/**
 * Test/fixture helpers for Observation.
 */
public final class ObservationFactory {

    private ObservationFactory() {
    }

    public static Observation fromFixture(String field, Object value) {
        return new Observation(
                "1.0.0",
                "obs-" + UUID.randomUUID(),
                field,
                value,
                String.valueOf(value),
                new Observation.Confidence(1.0, "certain"),
                "fixture",
                "fixture",
                "fixture",
                "0",
                null,
                Instant.parse("2026-08-27T00:00:00Z"),
                null,
                null,
                "frame-fixture"
        );
    }

    public static Observation fromOcr(
            String field,
            Object value,
            String rawValue,
            double score,
            String detector,
            String model,
            String frameId,
            Observation.Rect roi
    ) {
        String level = score >= 0.95 ? "certain" : score >= 0.80 ? "high" : score >= 0.50 ? "medium" : "low";
        return new Observation(
                "1.0.0",
                "obs-" + UUID.randomUUID(),
                field,
                value,
                rawValue,
                new Observation.Confidence(score, level),
                "ocr",
                detector,
                model,
                null,
                "numeric-v1",
                Instant.now(),
                roi,
                null,
                frameId
        );
    }
}
