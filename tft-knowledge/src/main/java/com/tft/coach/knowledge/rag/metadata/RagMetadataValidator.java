package com.tft.coach.knowledge.rag.metadata;

import com.tft.coach.knowledge.rag.chunk.TextChunk;

/** Enforces required RAG metadata before indexing (`P1-RAG-Metadata-001`). */
public final class RagMetadataValidator {

    public void validate(TextChunk chunk) {
        require(chunk.patch(), "patch");
        require(chunk.sourceType(), "source_type");
        require(chunk.documentId(), "document_id");
        require(chunk.text(), "text");
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required RAG metadata: " + field);
        }
    }
}
