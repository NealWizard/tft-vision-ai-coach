package com.tft.coach.knowledge.rag.embedding;

import com.tft.coach.knowledge.rag.chunk.TextChunk;

/** Embedding provider SPI (`P1-RAG-Embedding-001`). */
public interface EmbeddingProvider {

    String providerId();

    String modelVersion();

    float[] embed(TextChunk chunk);
}
