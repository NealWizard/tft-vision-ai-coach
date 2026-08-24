package com.tft.coach.knowledge.rag.vector;

import com.tft.coach.knowledge.rag.chunk.TextChunk;

import java.util.List;
import java.util.Map;

/** Vector store SPI; production uses pgvector (`P1-RAG-Vector-001`). */
public interface VectorStore {

    void upsert(VectorRecord record);

    List<VectorRecord> search(float[] queryVector, VectorFilter filter, int topK);
}
