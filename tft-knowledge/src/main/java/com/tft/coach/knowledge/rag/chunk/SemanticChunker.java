package com.tft.coach.knowledge.rag.chunk;

import com.tft.coach.knowledge.rag.ingest.IngestDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Semantic chunking with document/patch/source metadata (`P1-RAG-Chunk-001`). */
public final class SemanticChunker {

    public List<TextChunk> chunk(IngestDocument document, int maxChars) {
        List<TextChunk> chunks = new ArrayList<>();
        String body = document.body();
        int index = 0;
        for (int start = 0; start < body.length(); start += maxChars) {
            int end = Math.min(body.length(), start + maxChars);
            chunks.add(new TextChunk(
                    UUID.randomUUID().toString(),
                    document.documentId(),
                    document.patch(),
                    document.setId(),
                    document.sourceType(),
                    index++,
                    body.substring(start, end)));
        }
        if (chunks.isEmpty()) {
            chunks.add(new TextChunk(
                    UUID.randomUUID().toString(),
                    document.documentId(),
                    document.patch(),
                    document.setId(),
                    document.sourceType(),
                    0,
                    ""));
        }
        return List.copyOf(chunks);
    }
}
