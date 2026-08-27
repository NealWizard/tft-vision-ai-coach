package com.tft.coach.knowledge.rag.search;

import com.tft.coach.knowledge.rag.chunk.TextChunk;
import com.tft.coach.knowledge.rag.vector.VectorFilter;

import java.util.List;
import java.util.Optional;

/** BM25 / full-text search SPI for hybrid RAG recall. */
public interface TextSearchIndex {

    void index(TextChunk chunk);

    List<HybridSearchService.SearchHit> search(String query, VectorFilter filter, int topK);

    Optional<TextChunk> get(String chunkId);
}
