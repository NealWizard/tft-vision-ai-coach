package com.tft.coach.knowledge.agent;

import com.tft.coach.knowledge.research.WebSearchHit;
import com.tft.coach.knowledge.research.WebSearchProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Web Research Agent v1 (`P1-AGENT-Research-001`). */
public final class ResearchAgent {

    private static final Pattern PATCH_PATTERN = Pattern.compile(
            "(set\\s*\\d+|\\d+\\.\\d+|patch\\s*\\d+\\.\\d+)",
            Pattern.CASE_INSENSITIVE);

    private final KnowledgeAgent knowledgeAgent;
    private final WebSearchProvider webSearch;

    public ResearchAgent(KnowledgeAgent knowledgeAgent, WebSearchProvider webSearch) {
        this.knowledgeAgent = knowledgeAgent;
        this.webSearch = webSearch;
    }

    public ResearchAgentResponse research(ResearchAgentRequest request) {
        List<WebSearchHit> webHits = webSearch.search(request.topic(), 5);

        var knowledge = knowledgeAgent.answer(new KnowledgeAgent.KnowledgeAgentRequest(
                request.topic(),
                request.patch(),
                request.correlationId(),
                false,
                false));

        List<Map<String, Object>> candidates = new ArrayList<>();
        for (WebSearchHit hit : webHits) {
            String credibility = credibilityLevel(hit.url());
            double score = "medium".equals(credibility) ? 0.55 : 0.4;
            Map<String, Object> source = new HashMap<>();
            source.put("title", hit.title());
            source.put("url", hit.url());
            source.put("snippet", hit.snippet());
            source.put("captured_at", hit.capturedAt().toString());
            source.put("source_type", "community");
            source.put("patch", guessPatch(hit, request.patch()));
            source.put("credibility", score);
            source.put("credibility_level", credibility);

            Map<String, Object> candidate = new HashMap<>();
            candidate.put("option_id", "research-" + UUID.randomUUID());
            candidate.put("summary", summarizeHit(hit));
            candidate.put("score", score);
            candidate.put("confidence", Map.of("score", score, "level", credibility));
            candidate.put("sources", List.of(source));
            candidate.put("cannot_override_official_facts", true);
            candidate.put("official_cross_check", knowledge.candidates().isEmpty()
                    ? "no official match"
                    : knowledge.candidates().getFirst().get("summary"));
            candidates.add(candidate);
        }

        Map<String, Object> trace = new HashMap<>(knowledge.trace());
        trace.put("web_hits", webHits.size());

        return new ResearchAgentResponse(
                candidates,
                "Research output is candidate-only and cannot override official facts.",
                trace);
    }

    private static String summarizeHit(WebSearchHit hit) {
        if (!hit.snippet().isBlank()) {
            return hit.snippet();
        }
        return hit.title();
    }

    private static String guessPatch(WebSearchHit hit, String requestPatch) {
        Matcher matcher = PATCH_PATTERN.matcher(hit.title() + " " + hit.snippet());
        if (matcher.find()) {
            return matcher.group(1).toLowerCase(Locale.ROOT).replace(" ", "");
        }
        return requestPatch;
    }

    private static String credibilityLevel(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("riotgames.com") || lower.contains("leagueoflegends.com")) {
            return "medium";
        }
        return "low";
    }

    public record ResearchAgentRequest(String topic, String patch, String correlationId) {}

    public record ResearchAgentResponse(
            List<Map<String, Object>> candidates,
            String notes,
            Map<String, Object> trace
    ) {}
}
