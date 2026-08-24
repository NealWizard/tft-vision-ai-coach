package com.tft.coach.data.quality;

import com.tft.coach.data.evidence.EvidenceRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Computes explainable source quality scores (`P1-DATA-Quality-001`). */
public final class SourceQualityScorer {

    private final QualityConfig config;

    public SourceQualityScorer() {
        this(QualityConfig.defaults());
    }

    public SourceQualityScorer(QualityConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public QualityScore score(EvidenceRecord evidence, Instant now) {
        double reliability = clamp(evidence.reliability());
        double freshness = freshnessScore(evidence.capturedAt(), now);
        double sample = sampleScore(evidence.sampleSize());
        double total = reliability * config.reliabilityWeight()
                + freshness * config.freshnessWeight()
                + sample * config.sampleSizeWeight();
        return new QualityScore(
                clamp(total),
                "reliability*"
                        + config.reliabilityWeight()
                        + " + freshness*"
                        + config.freshnessWeight()
                        + " + sample*"
                        + config.sampleSizeWeight(),
                reliability,
                freshness,
                sample);
    }

    private double freshnessScore(Instant capturedAt, Instant now) {
        long hours = Duration.between(capturedAt, now).toHours();
        if (hours <= 24) {
            return 1.0;
        }
        if (hours <= 168) {
            return 0.7;
        }
        return 0.3;
    }

    private double sampleScore(long sampleSize) {
        return clamp((double) sampleSize / config.sampleSizeCap());
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public record QualityScore(
            double total,
            String formula,
            double reliabilityComponent,
            double freshnessComponent,
            double sampleSizeComponent
    ) {}
}
