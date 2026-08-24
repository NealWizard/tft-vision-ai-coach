package com.tft.coach.data.conflict;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Cross-source conflict entry (`P1-DATA-Conflict-001`). */
public record ConflictRecord(
        String canonicalId,
        String patch,
        String leftSourceType,
        String leftSourceId,
        String rightSourceType,
        String rightSourceId,
        Instant detectedAt,
        String summary
) {
    public ConflictRecord {
        Objects.requireNonNull(canonicalId, "canonicalId");
        Objects.requireNonNull(patch, "patch");
        detectedAt = detectedAt == null ? Instant.now() : detectedAt;
    }
}
