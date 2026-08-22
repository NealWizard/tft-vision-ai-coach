package com.tft.coach.data.snapshot;

import com.tft.coach.data.spi.SourceType;

import java.time.Instant;
import java.util.Objects;

/**
 * Metadata for an append-only raw snapshot on disk.
 */
public record RawSnapshot(
        String snapshotId,
        SourceType sourceType,
        String sourceId,
        String resourceKey,
        String sourceUrl,
        Instant capturedAt,
        String patch,
        String contentType,
        String bodyPath,
        long bodyBytes
) {
    public RawSnapshot {
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(resourceKey, "resourceKey");
        Objects.requireNonNull(capturedAt, "capturedAt");
        Objects.requireNonNull(bodyPath, "bodyPath");
    }

    /** Pointer for {@code evidence.payload_ref}. */
    public String payloadRef() {
        return snapshotId;
    }
}
