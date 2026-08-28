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
}
