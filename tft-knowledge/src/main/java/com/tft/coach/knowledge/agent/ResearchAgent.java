package com.tft.coach.knowledge.agent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Web Research Agent v1 (`P1-AGENT-Research-001`). */
public final class ResearchAgent {

    private final KnowledgeAgent knowledgeAgent;

    public ResearchAgent(KnowledgeAgent knowledgeAgent) {
        this.knowledgeAgent = knowledgeAgent;
    }

    public ResearchAgentResponse research(ResearchAgentRequest request) {
        var knowledge = knowledgeAgent.answer(new KnowledgeAgent.KnowledgeAgentRequest(
                request.topic(),
                request.patch(),
                request.correlationId(),
                false,
                false));

        Map<String, Object> candidate = Map.of(
                "option_id", "research-" + UUID.randomUUID(),
                "summary", "Community trend candidate for: " + request.topic(),
                "score", 0.4,
                "confidence", Map.of("score", 0.4, "level", "low"),
                "sources", List.of(
                        Map.of(
                                "source_type", "community",
                                "captured_at", "2026-08-24T00:00:00Z",
                                "patch", request.patch(),
                                "credibility", 0.4)),
                "cannot_override_official_facts", true);

        return new ResearchAgentResponse(
                List.of(candidate),
                "Research output is candidate-only and cannot override official facts.",
                knowledge.trace());
    }

    public record ResearchAgentRequest(String topic, String patch, String correlationId) {}

    public record ResearchAgentResponse(
            List<Map<String, Object>> candidates,
            String notes,
            Map<String, Object> trace
    ) {}
}
