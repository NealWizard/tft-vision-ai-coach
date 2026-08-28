package com.tft.coach.knowledge.rag.search;

import com.tft.coach.knowledge.rag.chunk.TextChunk;
import com.tft.coach.knowledge.rag.embedding.EmbeddingProvider;
import com.tft.coach.knowledge.rag.vector.VectorFilter;
import com.tft.coach.knowledge.rag.vector.VectorRecord;
import com.tft.coach.knowledge.rag.vector.VectorStore;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** BM25 + vector hybrid recall (`P1-RAG-Hybrid-001`). */
public final class HybridSearchService {

    private final VectorStore vectorStore;
    private final TextSearchIndex textSearchIndex;
    private final EmbeddingProvider embeddingProvider;

    public HybridSearchService(
            VectorStore vectorStore,
            TextSearchIndex textSearchIndex,
            EmbeddingProvider embeddingProvider
    ) {
        this.vectorStore = vectorStore;
        this.textSearchIndex = textSearchIndex;
        this.embeddingProvider = embeddingProvider;
    }

    public List<SearchHit> search(String query, VectorFilter filter, int topK) {
        TextChunk queryChunk = new TextChunk(
                "query",
                "query-doc",
                filter.patch() == null ? "unknown" : filter.patch(),
                filter.setId() == null ? "unknown" : filter.setId(),
                filter.sourceType() == null ? "query" : filter.sourceType(),
                0,
                query);
        float[] queryVector = embeddingProvider.embed(queryChunk);
        List<VectorRecord> vectorHits = vectorStore.search(queryVector, filter, topK);

        Map<String, Double> fused = new HashMap<>();
        for (int i = 0; i < vectorHits.size(); i++) {
            VectorRecord record = vectorHits.get(i);
            fused.merge(record.chunkId(), 1.0 / (i + 1), Double::sum);
        }
        for (HybridSearchService.SearchHit hit : textSearchIndex.search(query, filter, topK)) {
            if (hit.score() > 0) {
                fused.merge(hit.chunkId(), hit.score(), Double::sum);
            }
        }
        return fused.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry<String, Double>::getValue).reversed())
                .limit(topK)
                .map(entry -> new SearchHit(entry.getKey(), entry.getValue()))
                .toList();
    }

    public record SearchHit(String chunkId, double score) {}
}
