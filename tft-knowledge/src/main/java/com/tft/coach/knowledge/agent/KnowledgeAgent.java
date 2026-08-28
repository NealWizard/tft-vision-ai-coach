package com.tft.coach.knowledge.agent;

import com.tft.coach.data.evidence.EvidenceStore;
import com.tft.coach.knowledge.llm.CloudLlmGateway;
import com.tft.coach.knowledge.llm.LlmRequest;
import com.tft.coach.knowledge.llm.LlmResponse;
import com.tft.coach.knowledge.llm.PromptTemplateRegistry;
import com.tft.coach.knowledge.rag.api.KnowledgeRagApi;
import com.tft.coach.knowledge.rag.vector.VectorFilter;
import com.tft.coach.knowledge.tools.AugmentTool;
import com.tft.coach.knowledge.tools.ChampionTool;
import com.tft.coach.knowledge.tools.GameRuleTool;
import com.tft.coach.knowledge.tools.ItemTool;
import com.tft.coach.knowledge.tools.KnowledgeTool;
import com.tft.coach.knowledge.tools.MechanicTool;
import com.tft.coach.knowledge.tools.ProbabilityTool;
import com.tft.coach.knowledge.tools.TraitTool;
import com.tft.coach.knowledge.tools.UnitPoolTool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
        String q = question.toLowerCase(Locale.ROOT);
        List<String> evidenceIds = new ArrayList<>();
        List<Map<String, Object>> structuredFacts = new ArrayList<>();
        String summary;

        if (containsAny(q, "interest", "economy", "streak", "xp")) {
            String key = q.contains("streak") ? "economy.streak"
                    : q.contains("xp") ? "xp.level"
                    : q.contains("shop") && q.contains("pool") ? "shop.pool"
                    : "economy.interest";
            Map<String, Object> rule = tools.get(GameRuleTool.TOOL_ID).get(patch, key);
            structuredFacts.add(rule);
            evidenceIds.add("evidence:rule:" + key + ":" + patch);
            summary = String.valueOf(rule.getOrDefault("summary", rule.get("value")));
        } else if (containsAny(q, "shop odds", "roll odds", "three-star", "three star", "概率")) {
            String key = containsAny(q, "three") ? "shop.three-star" : "level.8";
            Map<String, Object> fact = tools.get(ProbabilityTool.TOOL_ID).get(patch, key);
            structuredFacts.add(fact);
            summary = "Shop probability fact: " + fact;
        } else if (containsAny(q, "unit pool", "pool copies", "牌池")) {
            Map<String, Object> fact = tools.get(UnitPoolTool.TOOL_ID).get(patch, extractCost(q));
            structuredFacts.add(fact);
            summary = "Unit pool fact: " + fact;
        } else if (containsAny(q, "carousel", "augment choice", "portal", "mechanic")) {
            List<Map<String, Object>> mechanics = tools.get(MechanicTool.TOOL_ID).search(patch, question);
            structuredFacts.addAll(mechanics);
            summary = mechanics.isEmpty() ? "No mechanic matched." : String.valueOf(mechanics.getFirst().get("description"));
        } else if (containsAny(q, "champion", "unit", "英雄") || looksLikeEntityQuery(q)) {
            List<Map<String, Object>> hits = tools.get(ChampionTool.TOOL_ID).search(patch, extractName(question));
            if (hits.isEmpty()) {
                hits = tools.get(TraitTool.TOOL_ID).search(patch, extractName(question));
            }
            if (hits.isEmpty()) {
                hits = tools.get(ItemTool.TOOL_ID).search(patch, extractName(question));
            }
            if (hits.isEmpty()) {
                hits = tools.get(AugmentTool.TOOL_ID).search(patch, extractName(question));
            }
            structuredFacts.addAll(hits);
            summary = hits.isEmpty()
                    ? "No structured entity matched; falling back to retrieval."
                    : String.valueOf(hits.getFirst().getOrDefault("name", hits.getFirst().get("id")));
            if (hits.isEmpty()) {
                var rag = ragApi.retrieve(question, VectorFilter.ofPatch(patch), 3);
                summary = rag.citations().isEmpty()
                        ? summary
                        : rag.citations().getFirst().excerpt();
                rag.citations().forEach(citation -> evidenceIds.add("evidence:rag:" + citation.chunkId()));
            }
        } else {
            var rag = ragApi.retrieve(question, VectorFilter.ofPatch(patch), 3);
            summary = rag.citations().isEmpty()
                    ? "No retrieved content matched the question."
                    : rag.citations().getFirst().excerpt();
            rag.citations().forEach(citation -> evidenceIds.add("evidence:rag:" + citation.chunkId()));
        }

        if (promptRegistry.find("knowledge.answer", "1.0.0").isEmpty()) {
            throw new IllegalStateException("Missing published prompt knowledge.answer@1.0.0");
        }
        LlmResponse llm = llmGateway.complete(
                new LlmRequest("knowledge.answer", "1.0.0", Map.of("question", question), 256),
                request.cloudAllowed(),
                request.cloudAvailable());

        Map<String, Object> candidate = new HashMap<>();
        candidate.put("option_id", "knowledge-" + UUID.randomUUID());
        candidate.put("summary", summary);
        candidate.put("score", structuredFacts.isEmpty() ? 0.7 : 0.9);
        candidate.put("evidence", evidenceIds);
        candidate.put("confidence", Map.of(
                "score", structuredFacts.isEmpty() ? 0.7 : 0.9,
                "level", structuredFacts.isEmpty() ? "medium" : "high"));
        candidate.put("fact_layers", Map.of(
                "structured_facts", structuredFacts,
                "retrieved_content", summary,
                "model_inference", llm.degraded() ? "" : llm.content()));

        Map<String, Object> trace = Map.of(
                "correlation_id", request.correlationId(),
                "agent_run_id", UUID.randomUUID().toString(),
                "status", "ok",
                "evidence_store_size", evidenceStore.size());

        return new KnowledgeAgentResponse(
                List.of(candidate),
                "Tool-backed answer only; no free-form invention.",
                trace,
                llm.degraded());
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeEntityQuery(String q) {
        return q.contains("what is") || q.contains("who is") || q.contains("trait") || q.contains("item");
    }

    private static String extractCost(String q) {
        String digits = q.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return "1";
        }
        return digits.substring(0, 1);
    }

    private static String extractName(String question) {
        String cleaned = question.replaceAll("(?i)what is|who is|champion|trait|item|augment|unit", " ");
        return cleaned.trim().isBlank() ? question : cleaned.trim();
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
