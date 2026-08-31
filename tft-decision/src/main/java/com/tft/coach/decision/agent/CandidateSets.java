package com.tft.coach.decision.agent;

import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.state.gamestate.GameState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CandidateSets {

    private CandidateSets() {}

    static CandidateSet complete(
            DecisionType type,
            GameState state,
            String fingerprint,
            String correlationId,
            String agentRunId,
            List<CandidateSet.CandidateOption> options,
            List<String> degraded
    ) {
        List<CandidateSet.CandidateOption> clipped = new ArrayList<>(options);
        if (clipped.size() > 3) {
            clipped = new ArrayList<>(clipped.subList(0, 3));
        }
        boolean flag = degraded != null && !degraded.isEmpty();
        Instant observed = state.observedAt();
        return new CandidateSet(
                "1.0.0",
                "cs-" + UUID.randomUUID(),
                type,
                new CandidateSet.BasedOn(state.matchId(), state.patch(), observed, fingerprint),
                List.copyOf(clipped),
                flag,
                degraded == null ? List.of() : List.copyOf(degraded),
                new CandidateSet.TraceInfo(correlationId, agentRunId, flag ? "degraded" : "ok", null));
    }

    static CandidateSet.CandidateOption option(
            String id,
            ActionType action,
            double score,
            String upside,
            String downside,
            String uncertainty,
            List<String> preconditions,
            List<String> evidence,
            String level,
            String summary,
            Map<String, Object> details
    ) {
        List<String> ev = (evidence == null || evidence.isEmpty()) ? List.of("evidence:none") : List.copyOf(evidence);
        String confLevel = ev.contains("evidence:none") ? "low" : level;
        return new CandidateSet.CandidateOption(
                id,
                action,
                score,
                new CandidateSet.Risk(upside, downside, uncertainty),
                preconditions == null ? List.of() : List.copyOf(preconditions),
                ev,
                new CandidateSet.Confidence(Math.max(0.0, Math.min(1.0, score)), confLevel),
                summary,
                null,
                null,
                details);
    }
}
