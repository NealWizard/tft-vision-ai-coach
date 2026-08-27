package com.tft.coach.knowledge.rag.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tft.coach.knowledge.rag.vector.VectorFilter;
import com.tft.coach.knowledge.rag.vector.VectorRecord;
import com.tft.coach.knowledge.rag.vector.VectorStore;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;

/** Elasticsearch dense_vector store for RAG chunks. */
public final class ElasticsearchVectorStore implements VectorStore {

    private final ElasticsearchHttpClient client;

    public ElasticsearchVectorStore(String hosts, String indexName, int embeddingDims) {
        this(new ElasticsearchHttpClient(hosts, indexName, embeddingDims));
    }

    ElasticsearchVectorStore(ElasticsearchHttpClient client) {
        this.client = client;
    }

    public ElasticsearchVectorStore(
            String hosts,
            String indexName,
            int embeddingDims,
            HttpClient httpClient
    ) {
        this(new ElasticsearchHttpClient(
                hosts,
                indexName,
                embeddingDims,
                httpClient,
                new com.fasterxml.jackson.databind.ObjectMapper()));
    }

    @Override
    public void upsert(VectorRecord record) {
        client.upsert(
                record.chunkId(),
                ElasticsearchHttpClient.toDocument(
                        record.chunkId(),
                        record.documentId(),
                        record.patch(),
                        record.setId(),
                        record.sourceType(),
                        record.region(),
                        record.rank(),
                        record.embedding(),
                        record.text()));
    }

    @Override
    public List<VectorRecord> search(float[] queryVector, VectorFilter filter, int topK) {
        ObjectNode body = client.mapper().createObjectNode();
        ObjectNode knn = client.mapper().createObjectNode();
        knn.put("field", "embedding");
        ArrayNode vector = client.mapper().createArrayNode();
        for (float value : queryVector) {
            vector.add(value);
        }
        knn.set("query_vector", vector);
        knn.put("k", topK);
        knn.put("num_candidates", Math.max(topK * 10, 100));
        if (filter != null) {
            ArrayNode filters = client.buildFilterClauses(filter.patch(), filter.setId(), filter.sourceType());
            if (!filters.isEmpty()) {
                ObjectNode bool = client.mapper().createObjectNode();
                bool.set("filter", filters);
                ObjectNode filterWrapper = client.mapper().createObjectNode();
                filterWrapper.set("bool", bool);
                knn.set("filter", filterWrapper);
            }
        }
        body.set("knn", knn);
        body.put("size", topK);

        JsonNode root = client.search(body);
        JsonNode hits = root.path("hits").path("hits");
        List<VectorRecord> results = new ArrayList<>();
        if (!hits.isArray()) {
            return results;
        }
        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            results.add(new VectorRecord(
                    source.path("chunk_id").asText(hit.path("_id").asText()),
                    source.path("document_id").asText(""),
                    source.path("patch").asText("unknown"),
                    source.path("set_id").asText(null),
                    source.path("source_type").asText("unknown"),
                    textOrNull(source, "region"),
                    textOrNull(source, "rank"),
                    readEmbedding(source.path("embedding")),
                    source.path("text").asText("")));
        }
        return results;
    }

    private static String textOrNull(JsonNode source, String field) {
        JsonNode node = source.get(field);
        return node == null || node.isNull() ? null : node.asText();
    }

    private static float[] readEmbedding(JsonNode node) {
        if (!node.isArray()) {
            return new float[0];
        }
        float[] embedding = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            embedding[i] = (float) node.get(i).asDouble();
        }
        return embedding;
    }
}
