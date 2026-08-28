package com.tft.coach.knowledge.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/** OpenAI-compatible chat/completions LLM provider. */
public final class OpenAiCompatibleLlmProvider implements LlmProvider {

    private final String baseUrl;
    private final String apiKey;
    private final String modelId;
    private final String providerId;
    private final String modelVersion;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public OpenAiCompatibleLlmProvider(String baseUrl, String apiKey, String modelId, String providerId) {
        this(baseUrl, apiKey, modelId, providerId, "1.0.0", HttpClient.newHttpClient(), new ObjectMapper());
    }

    public OpenAiCompatibleLlmProvider(
            String baseUrl,
            String apiKey,
            String modelId,
            String providerId,
            String modelVersion,
            HttpClient httpClient,
            ObjectMapper mapper
    ) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.apiKey = apiKey;
        this.modelId = modelId;
        this.providerId = providerId;
        this.modelVersion = modelVersion;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    @Override
    public String providerId() {
        return providerId;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        long started = System.nanoTime();
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", modelId);
            body.put("max_tokens", request.maxTokens());
            ArrayNode messages = mapper.createArrayNode();
            ObjectNode message = mapper.createObjectNode();
            message.put("role", "user");
            message.put("content", buildPrompt(request.variables()));
            messages.add(message);
            body.set("messages", messages);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("LLM request failed: HTTP " + response.statusCode()
                        + " " + response.body());
            }
            JsonNode root = mapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            JsonNode usage = root.path("usage");
            int promptTokens = usage.path("prompt_tokens").asInt(0);
            int completionTokens = usage.path("completion_tokens").asInt(0);
            long latencyMs = (System.nanoTime() - started) / 1_000_000;
            return new LlmResponse(content, providerId, modelVersion, promptTokens, completionTokens, latencyMs, false);
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI-compatible LLM call failed", ex);
        }
    }

    private static String buildPrompt(Map<String, String> variables) {
        if (variables.isEmpty()) {
            return "";
        }
        if (variables.size() == 1 && variables.containsKey("question")) {
            return variables.get("question");
        }
        return variables.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
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
