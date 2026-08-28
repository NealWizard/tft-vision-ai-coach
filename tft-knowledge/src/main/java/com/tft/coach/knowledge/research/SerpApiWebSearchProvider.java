package com.tft.coach.knowledge.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** SerpAPI web search provider. */
public final class SerpApiWebSearchProvider implements WebSearchProvider {

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public SerpApiWebSearchProvider(String apiKey) {
        this(apiKey, HttpClient.newHttpClient(), new ObjectMapper());
    }

    SerpApiWebSearchProvider(String apiKey, HttpClient httpClient, ObjectMapper mapper) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    @Override
    public List<WebSearchHit> search(String query, int maxResults) {
        try {
            String url = "https://serpapi.com/search.json?engine=google&q="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("SerpAPI search failed: HTTP " + response.statusCode()
                        + " " + response.body());
            }
            JsonNode root = mapper.readTree(response.body());
            JsonNode results = root.path("organic_results");
            List<WebSearchHit> hits = new ArrayList<>();
            if (!results.isArray()) {
                return hits;
            }
            Instant capturedAt = Instant.now();
            int count = 0;
            for (JsonNode item : results) {
                hits.add(new WebSearchHit(
                        item.path("title").asText(""),
                        item.path("link").asText(""),
                        item.path("snippet").asText(""),
                        capturedAt));
                count++;
                if (count >= maxResults) {
                    break;
                }
            }
            return hits;
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SerpAPI web search failed", ex);
        }
    }
}
