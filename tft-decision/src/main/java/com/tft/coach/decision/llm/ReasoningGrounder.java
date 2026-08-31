package com.tft.coach.decision.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.knowledge.llm.ChatModelGateway;
import com.tft.coach.knowledge.llm.ChatRequest;
import com.tft.coach.knowledge.llm.ChatResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fills reasoning only. Never mutates score/risk/preconditions/action_type/candidate count.
 */
public final class ReasoningGrounder {

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");

    private final ChatModelGateway gateway;
    private final ObjectMapper mapper;

    public ReasoningGrounder(ChatModelGateway gateway) {
        this.gateway = gateway;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public CandidateSet apply(CandidateSet set) {
        try {
            List<Map<String, Object>> slim = new ArrayList<>();
            StringBuilder allowedNumbers = new StringBuilder();
            for (CandidateSet.CandidateOption option : set.candidates()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("candidate_id", option.candidateId());
                row.put("summary", option.summary());
                row.put("score", option.score());
                slim.add(row);
                allowedNumbers.append(option.score()).append(' ');
                option.evidence().forEach(id -> allowedNumbers.append(id).append(' '));
            }
            Map<String, String> variables = Map.of(
                    "facts", allowedNumbers.toString(),
                    "candidates", mapper.writeValueAsString(slim));
            ChatResponse response = gateway.chat(new ChatRequest(
                    "Explain candidates. Do not change scores or invent TFT numbers.",
                    "Write reasoning for each candidate_id.",
                    variables));
            JsonNode parsed = mapper.readTree(response.content());
            Map<String, String> byId = new LinkedHashMap<>();
            if (parsed.isArray()) {
                for (JsonNode node : parsed) {
                    byId.put(node.path("candidate_id").asText(), node.path("reasoning").asText("uncertain"));
                }
            }
            List<CandidateSet.CandidateOption> next = new ArrayList<>();
            for (CandidateSet.CandidateOption option : set.candidates()) {
                String reasoning = byId.getOrDefault(option.candidateId(), "uncertain");
                if (containsUnsupportedNumber(reasoning, allowedNumbers.toString())) {
                    reasoning = "uncertain";
                }
                next.add(new CandidateSet.CandidateOption(
                        option.candidateId(),
                        option.actionType(),
                        option.score(),
                        option.risk(),
                        option.preconditions(),
                        option.evidence(),
                        option.confidence(),
                        option.summary(),
                        reasoning,
                        option.tradeoffs(),
                        option.details()));
            }
            List<String> degraded = new ArrayList<>(set.degradedReasons());
            if (response.degraded()) {
                degraded.add("LLM");
            }
            return new CandidateSet(
                    set.schemaVersion(),
                    set.candidateSetId(),
                    set.decisionType(),
                    set.basedOn(),
                    next,
                    !degraded.isEmpty(),
                    List.copyOf(degraded.stream().distinct().toList()),
                    set.trace());
        } catch (RuntimeException | java.io.IOException ex) {
            return uncertain(set, "LLM_FAILED");
        }
    }

    static boolean containsUnsupportedNumber(String reasoning, String facts) {
        Matcher matcher = NUMBER.matcher(reasoning);
        while (matcher.find()) {
            if (!facts.contains(matcher.group())) {
                return true;
            }
        }
        return false;
    }

    static CandidateSet uncertain(CandidateSet set, String reason) {
        List<CandidateSet.CandidateOption> next = new ArrayList<>();
        for (CandidateSet.CandidateOption option : set.candidates()) {
            next.add(new CandidateSet.CandidateOption(
                    option.candidateId(),
                    option.actionType(),
                    option.score(),
                    option.risk(),
                    option.preconditions(),
                    option.evidence(),
                    option.confidence(),
                    option.summary(),
                    "uncertain",
                    option.tradeoffs(),
                    option.details()));
        }
        List<String> degraded = new ArrayList<>(set.degradedReasons());
        degraded.add(reason);
        return new CandidateSet(
                set.schemaVersion(),
                set.candidateSetId(),
                set.decisionType(),
                set.basedOn(),
                next,
                true,
                List.copyOf(degraded),
                set.trace());
    }
}
