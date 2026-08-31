package com.tft.coach.meta;

import com.tft.coach.data.meta.MetaSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class MetaScorer {

    public static final double DEFAULT_RELIABILITY = 0.85;
    public static final double SAMPLE_SIZE_CAP = 100_000.0;

    public MetaScore score(MetaSnapshot snapshot, String queryPatch, Instant now) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(queryPatch, "queryPatch");
        Instant captured = snapshot.capturedAt() == null ? now : snapshot.capturedAt();
        double reliability = DEFAULT_RELIABILITY;
        double sample = Math.max(0.0, Math.min(1.0, snapshot.sampleSize() / SAMPLE_SIZE_CAP));
        double freshness = freshness(captured, now);
        double patchMatch = queryPatch.equals(snapshot.patch()) ? 1.0 : 0.0;
        double total = reliability * sample * freshness * patchMatch;
        return new MetaScore(
                total,
                reliability,
                sample,
                freshness,
                patchMatch,
                "reliability * sampleSize * freshness * patchMatch");
    }

    static double freshness(Instant capturedAt, Instant now) {
        long hours = Duration.between(capturedAt, now).toHours();
        if (hours <= 24) {
            return 1.0;
        }
        if (hours <= 168) {
            return 0.7;
        }
        return 0.3;
    }
}
