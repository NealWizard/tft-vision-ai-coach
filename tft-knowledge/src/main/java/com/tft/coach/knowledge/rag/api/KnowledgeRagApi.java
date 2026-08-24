package com.tft.coach.knowledge.rag.api;

import com.tft.coach.knowledge.rag.chunk.TextChunk;
import com.tft.coach.knowledge.rag.rerank.RerankerProvider;
import com.tft.coach.knowledge.rag.search.Bm25Index;
import com.tft.coach.knowledge.rag.search.HybridSearchService;
import com.tft.coach.knowledge.rag.vector.VectorFilter;

import java.util.List;
import java.util.Objects;

/** Unified retrieve/rerank/citation API (`P1-RAG-API-001`). */
public final class KnowledgeRagApi {

    private final HybridSearchService hybridSearch;
    private final RerankerProvider reranker;
    private final Bm25Index bm25Index;

    public KnowledgeRagApi(
            HybridSearchService hybridSearch,
            RerankerProvider reranker,
            Bm25Index bm25Index
    ) {
        this.hybridSearch = Objects.requireNonNull(hybridSearch, "hybridSearch");
        this.reranker = Objects.requireNonNull(reranker, "reranker");
        this.bm25Index = Objects.requireNonNull(bm25Index, "bm25Index");
    }

    public RagResponse retrieve(String query, VectorFilter filter, int topK) {
        List<HybridSearchService.SearchHit> hits = hybridSearch.search(query, filter, topK);
        List<HybridSearchService.SearchHit> reranked = reranker.rerank(query, hits);
        List<RagCitation> citations = reranked.stream()
                .map(hit -> {
                    TextChunk chunk = bm25Index.chunks().get(hit.chunkId());
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
