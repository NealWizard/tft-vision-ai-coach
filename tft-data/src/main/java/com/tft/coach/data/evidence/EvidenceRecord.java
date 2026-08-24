package com.tft.coach.data.evidence;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Persisted evidence metadata aligned with canonical evidence schema (`P1-DATA-Evidence-001`). */
public record EvidenceRecord(
        String schemaVersion,
        String id,
        String sourceType,
        String sourceId,
        String sourceUrl,
        Instant capturedAt,
        String patch,
        long sampleSize,
        double freshnessHours,
        double reliability,
        String payloadRef
) {
    public static final String SCHEMA_VERSION = "1.0.0";

    public EvidenceRecord {
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(capturedAt, "capturedAt");
        Objects.requireNonNull(patch, "patch");
        Objects.requireNonNull(payloadRef, "payloadRef");
    }

    public static EvidenceRecord fromFetchEvidence(FetchEvidence evidence, long sampleSize) {
        return new EvidenceRecord(
                SCHEMA_VERSION,
                evidence.id(),
                evidence.sourceType().wireValue(),
                evidence.sourceId(),
                evidence.sourceUrl(),
                evidence.capturedAt(),
                evidence.patch() == null ? "unknown" : evidence.patch(),
                sampleSize,
                0.0,
                1.0,
                evidence.payloadRef());
    }
}
