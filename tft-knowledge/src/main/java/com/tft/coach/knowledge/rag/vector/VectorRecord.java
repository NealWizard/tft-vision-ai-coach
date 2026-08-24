package com.tft.coach.knowledge.rag.vector;

import java.util.Objects;

public record VectorRecord(
        String chunkId,
        String documentId,
        String patch,
        String setId,
        String sourceType,
        String region,
        String rank,
        float[] embedding,
        String text
) {
    public VectorRecord {
        Objects.requireNonNull(chunkId, "chunkId");
        Objects.requireNonNull(patch, "patch");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(embedding, "embedding");
        Objects.requireNonNull(text, "text");
    }
}
