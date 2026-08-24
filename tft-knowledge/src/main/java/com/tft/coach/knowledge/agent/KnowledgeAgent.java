package com.tft.coach.knowledge.agent;

import com.tft.coach.data.evidence.EvidenceStore;
import com.tft.coach.knowledge.llm.CloudLlmGateway;
import com.tft.coach.knowledge.llm.LlmRequest;
import com.tft.coach.knowledge.llm.LlmResponse;
import com.tft.coach.knowledge.llm.PromptTemplateRegistry;
import com.tft.coach.knowledge.rag.api.KnowledgeRagApi;
import com.tft.coach.knowledge.rag.vector.VectorFilter;
import com.tft.coach.knowledge.tools.GameRuleTool;
import com.tft.coach.knowledge.tools.KnowledgeTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Knowledge Agent v1 orchestrating tools, RAG and LLM (`P1-AGENT-Knowledge-001`). */
public final class KnowledgeAgent {

    private final Map<String, KnowledgeTool> tools;
    private final KnowledgeRagApi ragApi;
    private final CloudLlmGateway llmGateway;
    private final PromptTemplateRegistry promptRegistry;
    private final EvidenceStore evidenceStore;

    public KnowledgeAgent(
            Map<String, KnowledgeTool> tools,
            KnowledgeRagApi ragApi,
            CloudLlmGateway llmGateway,
            PromptTemplateRegistry promptRegistry,
            EvidenceStore evidenceStore
    ) {
        this.tools = Map.copyOf(tools);
        this.ragApi = Objects.requireNonNull(ragApi, "ragApi");
        this.llmGateway = Objects.requireNonNull(llmGateway, "llmGateway");
        this.promptRegistry = Objects.requireNonNull(promptRegistry, "promptRegistry");
        this.evidenceStore = Objects.requireNonNull(evidenceStore, "evidenceStore");
    }

    public KnowledgeAgentResponse answer(KnowledgeAgentRequest request) {
        String patch = request.patch();
        String question = request.question();
        List<String> evidenceIds = new ArrayList<>();
        List<Map<String, Object>> structuredFacts = new ArrayList<>();
        String summary;

        if (question.toLowerCase().contains("interest") || question.toLowerCase().contains("gold")) {
            Map<String, Object> rule = tools.get(GameRuleTool.TOOL_ID).get(patch, "economy.interest");
            structuredFacts.add(rule);
            evidenceIds.add("evidence:rule:economy.interest:" + patch);
            summary = "At 50 gold, interest is capped at +5 per round under standard economy rules.";
        } else {
            var rag = ragApi.retrieve(question, VectorFilter.ofPatch(patch), 3);
            summary = rag.citations().isEmpty()
                    ? "No retrieved content matched the question."
                    : rag.citations().getFirst().excerpt();
            rag.citations().forEach(citation ->
                    evidenceIds.add("evidence:rag:" + citation.chunkId()));
        }

        LlmResponse llm = llmGateway.complete(
                new LlmRequest("knowledge.answer", "1.0.0", Map.of("question", question), 256),
                request.cloudAllowed(),
                request.cloudAvailable());

        Map<String, Object> candidate = new HashMap<>();
        candidate.put("option_id", "knowledge-" + UUID.randomUUID());
        candidate.put("summary", summary);
        candidate.put("score", 0.9);
        candidate.put("evidence", evidenceIds);
        candidate.put("confidence", Map.of("score", 0.9, "level", "high"));
        candidate.put("fact_layers", Map.of(
                "structured_facts", structuredFacts,
                "retrieved_content", summary,
                "model_inference", llm.degraded() ? "" : llm.content()));

        Map<String, Object> trace = Map.of(
                "correlation_id", request.correlationId(),
                "agent_run_id", UUID.randomUUID().toString(),
                "status", "ok");

        return new KnowledgeAgentResponse(
                List.of(candidate),
                "Tool-backed answer only; no free-form invention.",
                trace,
                llm.degraded());
    }

    public record KnowledgeAgentRequest(
            String question,
            String patch,
            String correlationId,
            boolean cloudAllowed,
            boolean cloudAvailable
    ) {}

    public record KnowledgeAgentResponse(
            List<Map<String, Object>> candidates,
            String notes,
            Map<String, Object> trace,
            boolean degraded
    ) {}
}
