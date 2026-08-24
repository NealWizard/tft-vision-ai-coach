package com.tft.coach.knowledge.rag.vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** In-memory vector index for tests and offline mode. */
public final class InMemoryVectorStore implements VectorStore {

    private final List<VectorRecord> records = new ArrayList<>();

    @Override
    public void upsert(VectorRecord record) {
        records.removeIf(existing -> existing.chunkId().equals(record.chunkId()));
        records.add(record);
    }

    @Override
    public List<VectorRecord> search(float[] queryVector, VectorFilter filter, int topK) {
        return records.stream()
                .filter(record -> filter == null || filter.matches(record))
                .map(record -> Map.entry(record, cosine(queryVector, record.embedding())))
                .sorted((left, right) -> Double.compare(right.getValue(), left.getValue()))
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static double cosine(float[] left, float[] right) {
        int len = Math.min(left.length, right.length);
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < len; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
