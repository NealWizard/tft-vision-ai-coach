package com.tft.coach.data.evidence;

import com.tft.coach.data.fetch.FetchResult;
import com.tft.coach.data.spi.SourceType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Canonical evidence metadata aligned with {@code evidence.schema.json}.
 */
public record FetchEvidence(
        String schemaVersion,
        String id,
        SourceType sourceType,
        String sourceId,
        String sourceUrl,
        Instant capturedAt,
        String patch,
        String payloadRef
) {
    public static final String SCHEMA_VERSION = "1.0.0";

    public FetchEvidence {
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(capturedAt, "capturedAt");
        Objects.requireNonNull(payloadRef, "payloadRef");
    }

    public static FetchEvidence fromFetchResult(FetchResult result) {
        return new FetchEvidence(
                SCHEMA_VERSION,
                UUID.randomUUID().toString(),
                result.sourceType(),
                result.sourceId(),
                result.request().sourceUrl(),
                result.capturedAt(),
                result.patch(),
                result.payloadRef()
        );
    }
}
