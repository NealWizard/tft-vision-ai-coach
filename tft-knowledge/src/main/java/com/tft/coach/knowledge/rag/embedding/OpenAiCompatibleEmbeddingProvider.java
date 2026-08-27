package com.tft.coach.knowledge.rag.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tft.coach.knowledge.rag.chunk.TextChunk;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** OpenAI-compatible /embeddings provider. */
public final class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {

    private final String baseUrl;
    private final String apiKey;
    private final String modelId;
    private final String providerId;
    private final String modelVersion;
    private final int configuredDimension;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public OpenAiCompatibleEmbeddingProvider(
            String baseUrl,
            String apiKey,
            String modelId,
            String providerId,
            int configuredDimension
    ) {
        this(baseUrl, apiKey, modelId, providerId, "1.0.0", configuredDimension,
                HttpClient.newHttpClient(), new ObjectMapper());
    }

    OpenAiCompatibleEmbeddingProvider(
            String baseUrl,
            String apiKey,
            String modelId,
            String providerId,
            String modelVersion,
            int configuredDimension,
            HttpClient httpClient,
            ObjectMapper mapper
    ) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey;
        this.modelId = modelId;
        this.providerId = providerId;
        this.modelVersion = modelVersion;
        this.configuredDimension = configuredDimension;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    @Override
    public String providerId() {
        return providerId;
    }

    @Override
    public String modelVersion() {
        return modelVersion;
    }

    @Override
    public float[] embed(TextChunk chunk) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", modelId);
            body.put("input", chunk.text());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/embeddings"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Embedding request failed: HTTP " + response.statusCode()
                        + " " + response.body());
            }
            JsonNode root = mapper.readTree(response.body());
            JsonNode embeddingNode = root.path("data").path(0).path("embedding");
            if (!embeddingNode.isArray()) {
                throw new IllegalStateException("Embedding response missing vector");
            }
            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }
            if (configuredDimension > 0 && vector.length != configuredDimension) {
                throw new IllegalStateException("Embedding dimension mismatch: expected "
                        + configuredDimension + ", got " + vector.length);
            }
            return vector;
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI-compatible embedding call failed", ex);
        }
    }

    public int dimension() {
        return configuredDimension;
    }

    /** Use base URL as configured (e.g. Zhipu {@code .../api/paas/v4}); do not force {@code /v1}. */
    private static String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
