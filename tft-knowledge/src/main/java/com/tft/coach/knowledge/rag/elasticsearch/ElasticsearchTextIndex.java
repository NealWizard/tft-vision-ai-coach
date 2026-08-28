package com.tft.coach.knowledge.rag.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tft.coach.knowledge.rag.chunk.TextChunk;
import com.tft.coach.knowledge.rag.search.HybridSearchService;
import com.tft.coach.knowledge.rag.search.TextSearchIndex;
import com.tft.coach.knowledge.rag.vector.VectorFilter;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Elasticsearch BM25 text index for RAG chunks. */
public final class ElasticsearchTextIndex implements TextSearchIndex {

    private final ElasticsearchHttpClient client;

    public ElasticsearchTextIndex(String hosts, String indexName, int embeddingDims) {
        this(new ElasticsearchHttpClient(hosts, indexName, embeddingDims));
    }

    ElasticsearchTextIndex(ElasticsearchHttpClient client) {
        this.client = client;
    }

    public ElasticsearchTextIndex(
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
    public void index(TextChunk chunk) {
        client.upsert(chunk.chunkId(), ElasticsearchHttpClient.toTextDocument(
                chunk.chunkId(),
                chunk.documentId(),
                chunk.patch(),
                chunk.setId(),
                chunk.sourceType(),
                chunk.text()));
    }

    @Override
    public List<HybridSearchService.SearchHit> search(String query, VectorFilter filter, int topK) {
        ObjectNode body = client.mapper().createObjectNode();
        ObjectNode bool = client.mapper().createObjectNode();
        ArrayNode must = client.mapper().createArrayNode();
        ObjectNode match = client.mapper().createObjectNode();
        match.set("text", client.mapper().createObjectNode().put("query", query));
        ObjectNode matchWrapper = client.mapper().createObjectNode();
        matchWrapper.set("match", match);
        must.add(matchWrapper);
        bool.set("must", must);
        if (filter != null) {
            ArrayNode filters = client.buildFilterClauses(filter.patch(), filter.setId(), filter.sourceType());
            if (!filters.isEmpty()) {
                bool.set("filter", filters);
            }
        }
        ObjectNode queryNode = client.mapper().createObjectNode();
        queryNode.set("bool", bool);
        body.set("query", queryNode);
        body.put("size", topK);

        JsonNode root = client.search(body);
        JsonNode hits = root.path("hits").path("hits");
        List<HybridSearchService.SearchHit> results = new ArrayList<>();
        if (!hits.isArray()) {
            return results;
        }
        for (JsonNode hit : hits) {
            String chunkId = hit.path("_source").path("chunk_id").asText(hit.path("_id").asText());
            double score = hit.path("_score").asDouble(0.0);
            results.add(new HybridSearchService.SearchHit(chunkId, score));
        }
        return results;
    }

    @Override
    public Optional<TextChunk> get(String chunkId) {
        return client.getDocument(chunkId).map(this::toChunk);
    }

    private TextChunk toChunk(JsonNode source) {
        return new TextChunk(
                source.path("chunk_id").asText(),
                source.path("document_id").asText(""),
                source.path("patch").asText("unknown"),
                source.path("set_id").asText(null),
                source.path("source_type").asText("unknown"),
                0,
                source.path("text").asText(""));
    }
}
