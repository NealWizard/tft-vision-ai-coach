package com.tft.coach.knowledge.rag.search;

import com.tft.coach.knowledge.rag.chunk.TextChunk;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Simple BM25-like keyword index for hybrid search. */
public final class Bm25Index {

    private final Map<String, TextChunk> chunks = new HashMap<>();

    public void index(TextChunk chunk) {
        chunks.put(chunk.chunkId(), chunk);
    }

    public double score(TextChunk chunk, String query) {
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

    public Map<String, TextChunk> chunks() {
        return Map.copyOf(chunks);
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
