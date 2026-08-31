package com.tft.coach.decision.agent;

import com.tft.coach.data.meta.AugmentStat;
import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.knowledge.tools.AugmentTool;
import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.List;

public final class AugmentAgent implements DomainAgent {

    @Override
    public DecisionType decisionType() {
        return DecisionType.AUGMENT;
    }

    @Override
    public CandidateSet advise(GameState state, Context context) {
        List<String> degraded = new ArrayList<>(context.meta().degradedReasons());
        List<String> evidence = new ArrayList<>();
        context.meta().snapshot().ifPresent(s -> evidence.add("evidence:meta:" + s.id()));
        if (state.augments() == null || state.augments().isEmpty()) {
            degraded.add("AUGMENT_UNOBSERVED");
        }
        try {
            context.knowledge().tool(AugmentTool.TOOL_ID);
            evidence.add("evidence:tool:augment-tool");
        } catch (RuntimeException ex) {
            degraded.add("AUGMENT_TOOL");
        }
        List<AugmentStat> augments = context.meta().snapshot()
                .map(s -> s.snapshot().augments())
                .orElse(List.of());
        List<CandidateSet.CandidateOption> options = new ArrayList<>();
        int limit = Math.min(2, augments.size());
        for (int i = 0; i < limit; i++) {
            AugmentStat augment = augments.get(i);
            options.add(CandidateSets.option(
                    "aug-" + augment.augmentId(),
                    ActionType.SELECT_AUGMENT,
                    Math.min(1.0, augment.winRate()),
                    "Meta win rate support",
                    "May mismatch current board",
                    "medium",
                    List.of("offered " + augment.augmentId()),
                    evidence,
                    "medium",
                    "Take " + augment.name() + ".",
                    null));
        }
        options.add(CandidateSets.option(
                "aug-hold",
                ActionType.HOLD,
                0.45,
                "Skip weak offers",
                "May miss spike",
                "high",
                List.of(),
                evidence,
                "low",
                "Skip if none fit the line.",
                null));
        return CandidateSets.complete(
                DecisionType.AUGMENT, state, context.fingerprint(), context.correlationId(),
                "run-augment", options, degraded);
    }
}
