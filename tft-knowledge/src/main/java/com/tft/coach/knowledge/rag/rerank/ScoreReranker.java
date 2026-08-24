package com.tft.coach.knowledge.rag.rerank;

import com.tft.coach.knowledge.rag.search.HybridSearchService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Baseline score-based reranker with deterministic fallback. */
public final class ScoreReranker implements RerankerProvider {

    @Override
    public String providerId() {
        return "score-baseline";
    }

    @Override
    public List<HybridSearchService.SearchHit> rerank(
            String query,
            List<HybridSearchService.SearchHit> hits
    ) {
        List<HybridSearchService.SearchHit> copy = new ArrayList<>(hits);
        copy.sort(Comparator.comparingDouble(HybridSearchService.SearchHit::score).reversed());
        return List.copyOf(copy);
    }
}
