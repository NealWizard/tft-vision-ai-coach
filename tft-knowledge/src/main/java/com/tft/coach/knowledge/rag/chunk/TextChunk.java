package com.tft.coach.knowledge.rag.chunk;

import java.util.Objects;

public record TextChunk(
        String chunkId,
        String documentId,
        String patch,
        String setId,
        String sourceType,
        int sectionIndex,
        String text
) {
    public TextChunk {
        Objects.requireNonNull(chunkId, "chunkId");
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(patch, "patch");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(text, "text");
    }
}
