package com.tft.coach.knowledge.rag.elasticsearch;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Minimal Elasticsearch REST helper using JDK HttpClient + Jackson. */
final class ElasticsearchHttpClient {

    private final String baseUrl;
    private final String indexName;
    private final int embeddingDims;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private volatile boolean indexEnsured;

    ElasticsearchHttpClient(String hosts, String indexName, int embeddingDims) {
        this(hosts, indexName, embeddingDims, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build(), new ObjectMapper());
    }

    ElasticsearchHttpClient(
            String hosts,
            String indexName,
            int embeddingDims,
            HttpClient httpClient,
            ObjectMapper mapper
    ) {
        this.baseUrl = normalizeBaseUrl(hosts);
        this.indexName = indexName;
        this.embeddingDims = embeddingDims;
        this.httpClient = httpClient;
        this.mapper = mapper;
    }

    void ensureIndex() {
        if (indexEnsured) {
            return;
        }
        synchronized (this) {
            if (indexEnsured) {
                return;
            }
            try {
                HttpResponse<String> head = send("HEAD", "/" + indexName, null);
                if (head.statusCode() == 404) {
                    ObjectNode mappings = mapper.createObjectNode();
                    ObjectNode properties = mapper.createObjectNode();
                    properties.set("chunk_id", keywordField());
                    properties.set("document_id", keywordField());
                    properties.set("patch", keywordField());
                    properties.set("set_id", keywordField());
                    properties.set("source_type", keywordField());
                    properties.set("region", keywordField());
                    properties.set("rank", keywordField());
                    properties.set("text", mapper.createObjectNode().put("type", "text"));
                    ObjectNode embedding = mapper.createObjectNode();
                    embedding.put("type", "dense_vector");
                    embedding.put("dims", embeddingDims);
                    embedding.put("index", true);
                    embedding.put("similarity", "cosine");
                    properties.set("embedding", embedding);
                    mappings.set("properties", properties);
                    ObjectNode body = mapper.createObjectNode();
                    body.set("mappings", mappings);
                    HttpResponse<String> put = send("PUT", "/" + indexName, body.toString());
                    if (put.statusCode() >= 300) {
                        throw new IllegalStateException("Failed to create ES index: " + put.body());
                    }
                } else if (head.statusCode() >= 300) {
                    throw new IllegalStateException("Failed to check ES index: HTTP " + head.statusCode());
                }
                indexEnsured = true;
            } catch (IOException | InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Elasticsearch index setup failed", ex);
            }
        }
    }

    void upsert(String id, Map<String, Object> document) {
        ensureIndex();
        try {
            String body = mapper.writeValueAsString(document);
            HttpResponse<String> response = send("PUT", "/" + indexName + "/_doc/" + id, body);
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("ES upsert failed: " + response.body());
            }
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elasticsearch upsert failed", ex);
        }
    }

    JsonNode search(ObjectNode queryBody) {
        ensureIndex();
        try {
            HttpResponse<String> response = send("POST", "/" + indexName + "/_search", queryBody.toString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("ES search failed: " + response.body());
            }
            return mapper.readTree(response.body());
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elasticsearch search failed", ex);
        }
    }

    Optional<JsonNode> getDocument(String id) {
        ensureIndex();
        try {
            HttpResponse<String> response = send("GET", "/" + indexName + "/_doc/" + id, null);
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("ES get failed: " + response.body());
            }
            JsonNode root = mapper.readTree(response.body());
            return Optional.ofNullable(root.get("_source"));
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Elasticsearch get failed", ex);
        }
    }

    ArrayNode buildFilterClauses(String patch, String setId, String sourceType) {
        ArrayNode filters = mapper.createArrayNode();
        if (patch != null) {
            filters.add(termFilter("patch", patch));
        }
        if (setId != null) {
            filters.add(termFilter("set_id", setId));
        }
        if (sourceType != null) {
            filters.add(termFilter("source_type", sourceType));
        }
        return filters;
    }

    ObjectNode termFilter(String field, String value) {
        ObjectNode term = mapper.createObjectNode();
        term.put(field, value);
        ObjectNode wrapper = mapper.createObjectNode();
        wrapper.set("term", term);
        return wrapper;
    }

    ObjectMapper mapper() {
        return mapper;
    }

    private HttpResponse<String> send(String method, String path, String body)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json");
        if ("HEAD".equals(method)) {
            builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
        } else if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static ObjectNode keywordField() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.createObjectNode().put("type", "keyword");
    }

    private static String normalizeBaseUrl(String hosts) {
        String host = hosts.split(",")[0].trim();
        if (!host.startsWith("http://") && !host.startsWith("https://")) {
            host = "http://" + host;
        }
        return host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
    }

    static Map<String, Object> toTextDocument(
            String chunkId,
            String documentId,
            String patch,
            String setId,
            String sourceType,
            String text
    ) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("chunk_id", chunkId);
        doc.put("document_id", documentId);
        doc.put("patch", patch);
        doc.put("set_id", setId);
        doc.put("source_type", sourceType);
        doc.put("text", text);
        return doc;
    }

    static Map<String, Object> toDocument(
            String chunkId,
            String documentId,
            String patch,
            String setId,
            String sourceType,
            String region,
            String rank,
            float[] embedding,
            String text
    ) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("chunk_id", chunkId);
        doc.put("document_id", documentId);
        doc.put("patch", patch);
        doc.put("set_id", setId);
        doc.put("source_type", sourceType);
        if (region != null) {
            doc.put("region", region);
        }
        if (rank != null) {
            doc.put("rank", rank);
        }
        doc.put("text", text);
        List<Float> vector = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            vector.add(value);
        }
        doc.put("embedding", vector);
        return doc;
    }
}
