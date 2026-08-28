package com.tft.coach.knowledge.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Tavily web search provider. */
public final class TavilyWebSearchProvider implements WebSearchProvider {

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public TavilyWebSearchProvider(String apiKey) {
        this(apiKey, HttpClient.newHttpClient(), new ObjectMapper());
    }

    TavilyWebSearchProvider(String apiKey, HttpClient httpClient, ObjectMapper mapper) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    @Override
    public List<WebSearchHit> search(String query, int maxResults) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("api_key", apiKey);
            body.put("query", query);
            body.put("max_results", maxResults);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.tavily.com/search"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Tavily search failed: HTTP " + response.statusCode()
                        + " " + response.body());
            }
            JsonNode root = mapper.readTree(response.body());
            JsonNode results = root.path("results");
            List<WebSearchHit> hits = new ArrayList<>();
            if (!results.isArray()) {
                return hits;
            }
            Instant capturedAt = Instant.now();
            for (JsonNode item : results) {
                hits.add(new WebSearchHit(
                        item.path("title").asText(""),
                        item.path("url").asText(""),
                        item.path("content").asText(item.path("snippet").asText("")),
                        capturedAt));
            }
            return hits;
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Tavily web search failed", ex);
        }
    }
}
