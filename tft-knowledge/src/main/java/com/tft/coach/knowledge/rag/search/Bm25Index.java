package com.tft.coach.knowledge.rag.search;

import com.tft.coach.knowledge.rag.chunk.TextChunk;
import com.tft.coach.knowledge.rag.vector.VectorFilter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** In-memory BM25-like keyword index for hybrid search and offline tests. */
public final class Bm25Index implements TextSearchIndex {

    private final Map<String, TextChunk> chunks = new HashMap<>();

    @Override
    public void index(TextChunk chunk) {
        chunks.put(chunk.chunkId(), chunk);
    }

    @Override
    public List<HybridSearchService.SearchHit> search(String query, VectorFilter filter, int topK) {
        return chunks.entrySet().stream()
                .filter(entry -> filter == null || matchesFilter(entry.getValue(), filter))
                .map(entry -> Map.entry(entry.getKey(), score(entry.getValue(), query)))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Comparator.comparingDouble(Map.Entry<String, Double>::getValue).reversed())
                .limit(topK)
                .map(entry -> new HybridSearchService.SearchHit(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public Optional<TextChunk> get(String chunkId) {
        return Optional.ofNullable(chunks.get(chunkId));
    }

    double score(TextChunk chunk, String query) {
        String[] terms = query.toLowerCase(Locale.ROOT).split("\\s+");
        String text = chunk.text().toLowerCase(Locale.ROOT);
        double score = 0;
        for (String term : terms) {
            if (term.isBlank()) {
                continue;
            }
            int count = countOccurrences(text, term);
            if (count > 0) {
                score += count / (1.0 + text.length() / 1000.0);
            }
        }
        return score;
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

    private static int countOccurrences(String text, String term) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(term, idx)) >= 0) {
            count++;
            idx += term.length();
        }
        return count;
    }
}
