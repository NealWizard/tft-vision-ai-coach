package com.tft.coach.decision.rag;

import com.tft.coach.knowledge.rag.api.KnowledgeRagApi;
import com.tft.coach.knowledge.rag.vector.VectorFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Retrieval only; never writes Candidate score. */
public final class DecisionRagContext {

    private final KnowledgeRagApi ragApi;

    public DecisionRagContext(KnowledgeRagApi ragApi) {
        this.ragApi = Objects.requireNonNull(ragApi, "ragApi");
    }

    public List<String> retrieveEvidence(String patch, String query) {
        VectorFilter filter = VectorFilter.ofPatch(patch);
        KnowledgeRagApi.RagResponse response = ragApi.retrieve(query, filter, 3);
        List<String> ids = new ArrayList<>();
        for (KnowledgeRagApi.RagCitation citation : response.citations()) {
            ids.add("evidence:rag:" + citation.chunkId());
        }
        return List.copyOf(ids);
    }
}
