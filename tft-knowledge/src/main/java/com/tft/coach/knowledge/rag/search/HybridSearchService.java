package com.tft.coach.knowledge.rag.search;

import com.tft.coach.knowledge.rag.chunk.TextChunk;
import com.tft.coach.knowledge.rag.embedding.EmbeddingProvider;
import com.tft.coach.knowledge.rag.vector.VectorFilter;
import com.tft.coach.knowledge.rag.vector.VectorRecord;
import com.tft.coach.knowledge.rag.vector.VectorStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** BM25 + vector hybrid recall (`P1-RAG-Hybrid-001`). */
public final class HybridSearchService {

    private final VectorStore vectorStore;
    private final Bm25Index bm25Index;
    private final EmbeddingProvider embeddingProvider;

    public HybridSearchService(VectorStore vectorStore, Bm25Index bm25Index, EmbeddingProvider embeddingProvider) {
        this.vectorStore = vectorStore;
        this.bm25Index = bm25Index;
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
        for (var entry : bm25Index.chunks().entrySet()) {
            TextChunk chunk = entry.getValue();
            if (filter != null && !matchesFilter(chunk, filter)) {
                continue;
            }
            double score = bm25Index.score(chunk, query);
            if (score > 0) {
                fused.merge(entry.getKey(), score, Double::sum);
            }
        }
        return fused.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry<String, Double>::getValue).reversed())
                .limit(topK)
                .map(entry -> new SearchHit(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static boolean matchesFilter(TextChunk chunk, VectorFilter filter) {
        if (filter.patch() != null && !filter.patch().equals(chunk.patch())) {
            return false;
        }
        if (filter.setId() != null && !filter.setId().equals(chunk.setId())) {
            return false;
        }
        return filter.sourceType() == null || filter.sourceType().equals(chunk.sourceType());
    }

    public record SearchHit(String chunkId, double score) {}
}
