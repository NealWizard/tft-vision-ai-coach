package com.tft.coach.knowledge.rag.ingest;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Ingested document with mandatory source metadata (`P1-RAG-Ingest-001`). */
public record IngestDocument(
        String documentId,
        String sourceType,
        String sourceUrl,
        String patch,
        String setId,
        Instant capturedAt,
        String contentType,
        String body
) {
    public IngestDocument {
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(patch, "patch");
        Objects.requireNonNull(capturedAt, "capturedAt");
        Objects.requireNonNull(body, "body");
        setId = setId == null ? "unknown" : setId;
        contentType = contentType == null ? "text/plain" : contentType;
    }

    public static IngestDocument ofText(String sourceType, String patch, String setId, String body) {
        return new IngestDocument(
                UUID.randomUUID().toString(),
                sourceType,
                "ingest://" + sourceType,
                patch,
                setId,
                Instant.now(),
                "text/plain",
                body);
    }
}
