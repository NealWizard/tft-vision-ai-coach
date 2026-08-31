package com.tft.coach.decision.candidate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CandidateSet(
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("candidate_set_id") String candidateSetId,
        @JsonProperty("decision_type") DecisionType decisionType,
        @JsonProperty("based_on") BasedOn basedOn,
        List<CandidateOption> candidates,
        boolean degraded,
        @JsonProperty("degraded_reasons") List<String> degradedReasons,
        TraceInfo trace
) {
    public CandidateSet {
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(candidateSetId, "candidateSetId");
        Objects.requireNonNull(decisionType, "decisionType");
        Objects.requireNonNull(basedOn, "basedOn");
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
        degradedReasons = degradedReasons == null ? List.of() : List.copyOf(degradedReasons);
        Objects.requireNonNull(trace, "trace");
    }

    public record BasedOn(
            @JsonProperty("match_id") String matchId,
            String patch,
            @JsonProperty("observed_at") Instant observedAt,
            @JsonProperty("game_state_fingerprint") String gameStateFingerprint
    ) {
    }

    public record TraceInfo(
            @JsonProperty("correlation_id") String correlationId,
            @JsonProperty("agent_run_id") String agentRunId,
            String status,
            @JsonProperty("latency_ms") Integer latencyMs
    ) {
    }

    public record Risk(String upside, String downside, String uncertainty) {
    }

    public record Confidence(double score, String level) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CandidateOption(
            @JsonProperty("candidate_id") String candidateId,
            @JsonProperty("action_type") ActionType actionType,
            double score,
            Risk risk,
            List<String> preconditions,
            List<String> evidence,
            Confidence confidence,
            String summary,
            String reasoning,
            List<String> tradeoffs,
            Map<String, Object> details
    ) {
        public CandidateOption {
            Objects.requireNonNull(candidateId, "candidateId");
            Objects.requireNonNull(actionType, "actionType");
            Objects.requireNonNull(risk, "risk");
            preconditions = preconditions == null ? List.of() : List.copyOf(preconditions);
            evidence = evidence == null ? List.of() : List.copyOf(evidence);
            Objects.requireNonNull(confidence, "confidence");
            Objects.requireNonNull(summary, "summary");
            tradeoffs = tradeoffs == null ? null : List.copyOf(tradeoffs);
            details = details == null ? null : Map.copyOf(details);
        }
    }
}
