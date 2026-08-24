package com.tft.coach.knowledge.rag.rerank;

import com.tft.coach.knowledge.rag.search.HybridSearchService;

import java.util.Comparator;
import java.util.List;

/** Reranker provider SPI (`P1-RAG-Rerank-001`). */
public interface RerankerProvider {

    String providerId();

    List<HybridSearchService.SearchHit> rerank(String query, List<HybridSearchService.SearchHit> hits);
}
