package com.tft.coach.data.quality;

import com.tft.coach.data.evidence.EvidenceRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Configurable scoring weights (`P1-DATA-Quality-001`). */
public record QualityConfig(
        double reliabilityWeight,
        double freshnessWeight,
        double sampleSizeWeight,
        long sampleSizeCap
) {
    public QualityConfig {
        if (sampleSizeCap <= 0) {
            throw new IllegalArgumentException("sampleSizeCap must be positive");
        }
    }

    public static QualityConfig defaults() {
        return new QualityConfig(0.5, 0.3, 0.2, 1_000_000L);
    }
}
