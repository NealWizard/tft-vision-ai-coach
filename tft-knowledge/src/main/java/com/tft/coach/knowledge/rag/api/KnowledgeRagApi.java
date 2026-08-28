package com.tft.coach.knowledge.rag.api;

import com.tft.coach.knowledge.rag.chunk.TextChunk;
import com.tft.coach.knowledge.rag.rerank.RerankerProvider;
import com.tft.coach.knowledge.rag.search.HybridSearchService;
import com.tft.coach.knowledge.rag.search.TextSearchIndex;
import com.tft.coach.knowledge.rag.vector.VectorFilter;

import java.util.List;
import java.util.Objects;

/** Unified retrieve/rerank/citation API (`P1-RAG-API-001`). */
public final class KnowledgeRagApi {

    private final HybridSearchService hybridSearch;
    private final RerankerProvider reranker;
    private final TextSearchIndex textSearchIndex;

    public KnowledgeRagApi(
            HybridSearchService hybridSearch,
            RerankerProvider reranker,
            TextSearchIndex textSearchIndex
    ) {
        this.hybridSearch = Objects.requireNonNull(hybridSearch, "hybridSearch");
        this.reranker = Objects.requireNonNull(reranker, "reranker");
        this.textSearchIndex = Objects.requireNonNull(textSearchIndex, "textSearchIndex");
    }

    public RagResponse retrieve(String query, VectorFilter filter, int topK) {
        List<HybridSearchService.SearchHit> hits = hybridSearch.search(query, filter, topK);
        List<HybridSearchService.SearchHit> reranked = reranker.rerank(query, hits);
        List<RagCitation> citations = reranked.stream()
                .map(hit -> {
                    TextChunk chunk = textSearchIndex.get(hit.chunkId()).orElse(null);
                    return new RagCitation(
                            hit.chunkId(),
                            hit.score(),
                            chunk == null ? "" : chunk.text(),
                            chunk == null ? filter.patch() : chunk.patch(),
                            chunk == null ? filter.sourceType() : chunk.sourceType());
                })
                .toList();
        return new RagResponse(query, citations);
    }

    public record RagCitation(
            String chunkId,
            double score,
            String excerpt,
            String patch,
            String sourceType
    ) {}

    public record RagResponse(String query, List<RagCitation> citations) {}
}
