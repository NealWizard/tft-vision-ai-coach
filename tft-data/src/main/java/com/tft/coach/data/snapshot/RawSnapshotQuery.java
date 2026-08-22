package com.tft.coach.data.snapshot;

import com.tft.coach.data.spi.SourceType;

import java.time.Instant;
import java.util.Optional;

/**
 * Query raw snapshots by source and optional time window.
 */
public record RawSnapshotQuery(
        SourceType sourceType,
        String sourceId,
        String resourceKey,
        Instant fromInclusive,
        Instant toExclusive
) {
    public RawSnapshotQuery(SourceType sourceType, String sourceId, String resourceKey) {
        this(sourceType, sourceId, resourceKey, null, null);
    }
}
