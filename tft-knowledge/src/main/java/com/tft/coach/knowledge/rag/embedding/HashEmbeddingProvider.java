package com.tft.coach.knowledge.rag.embedding;

import com.tft.coach.knowledge.rag.chunk.TextChunk;

import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/** Deterministic local embedding for CI and offline mode. */
public final class HashEmbeddingProvider implements EmbeddingProvider {

    public static final int DIMENSION = 64;

    @Override
    public String providerId() {
        return "hash-local";
    }

    @Override
    public String modelVersion() {
        return "1.0.0";
    }

    @Override
    public float[] embed(TextChunk chunk) {
        float[] vector = new float[DIMENSION];
        byte[] bytes = chunk.text().getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < bytes.length; i++) {
            vector[i % DIMENSION] += bytes[i] / 255.0f;
        }
        CRC32 crc = new CRC32();
        crc.update(bytes);
        vector[0] += crc.getValue() / (float) Long.MAX_VALUE;
        return vector;
    }
}
