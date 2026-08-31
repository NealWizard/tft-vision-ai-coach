package com.tft.coach.decision.agent;

import com.tft.coach.data.meta.CompStat;
import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.meta.MetaService;
import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Provides Meta composition context; does not decide shop actions. */
public final class MetaContextProvider implements DomainAgent {

    @Override
    public DecisionType decisionType() {
        return DecisionType.COMPOSITION;
    }

    @Override
    public CandidateSet advise(GameState state, Context context) {
        List<CandidateSet.CandidateOption> options = new ArrayList<>();
        List<MetaService.ScoredComp> comps = context.meta().comps();
        int limit = Math.min(3, comps.size());
        for (int i = 0; i < limit; i++) {
            MetaService.ScoredComp scored = comps.get(i);
            CompStat comp = scored.comp();
            ActionType action = i == 0 ? ActionType.PIVOT : ActionType.HOLD;
            List<String> evidence = new ArrayList<>();
            context.meta().snapshot().ifPresent(s -> evidence.add("evidence:meta:" + s.id()));
            evidence.addAll(context.ragEvidence());
            if (evidence.isEmpty()) {
                evidence.add("evidence:meta:none");
            }
            String level = evidence.stream().anyMatch(e -> e.startsWith("evidence:meta:")) ? "high" : "low";
            options.add(new CandidateSet.CandidateOption(
                    "meta-" + comp.compId(),
                    action,
                    scored.score(),
                    new CandidateSet.Risk(
                            "Aligned with current Meta top4",
                            "Contest and patch drift",
                            "medium"),
                    List.of("patch=" + state.patch()),
                    evidence,
                    new CandidateSet.Confidence(Math.min(1.0, scored.score()), level),
                    action == ActionType.PIVOT
                            ? "Consider " + comp.name() + " as current Meta shell."
                            : "Alternative shell: " + comp.name() + ".",
                    null,
                    null,
                    Map.of(
                            "current_comp", comp.compId(),
                            "target_comp", comp.compId(),
                            "missing_core_units", comp.coreUnits(),
                            "compatibility", scored.score(),
                            "transition_cost_estimate", 0)
            ));
        }
        while (options.size() < 2) {
            options.add(holdFallback(state, context));
        }
        List<String> degraded = new ArrayList<>(context.meta().degradedReasons());
        if (context.ragEvidence().isEmpty()) {
            degraded.add("RAG_EMPTY");
        }
        boolean isDegraded = !degraded.isEmpty();
        return new CandidateSet(
                "1.0.0",
                "cs-" + UUID.randomUUID(),
                DecisionType.COMPOSITION,
                new CandidateSet.BasedOn(
                        state.matchId(),
                        state.patch(),
                        state.observedAt(),
                        context.fingerprint()),
                options.subList(0, Math.min(3, options.size())),
                isDegraded,
                degraded,
                new CandidateSet.TraceInfo(
                        context.correlationId(),
                        "run-meta-context",
                        isDegraded ? "degraded" : "ok",
                        null)
        );
    }

    private static CandidateSet.CandidateOption holdFallback(GameState state, Context context) {
        List<String> evidence = new ArrayList<>(context.ragEvidence());
        if (evidence.isEmpty()) {
            evidence.add("evidence:meta:none");
        }
        return new CandidateSet.CandidateOption(
                "meta-hold",
                ActionType.HOLD,
                0.4,
                new CandidateSet.Risk("Preserve board", "May miss Meta spike", "high"),
                List.of("patch=" + state.patch()),
                evidence,
                new CandidateSet.Confidence(0.4, evidence.getFirst().equals("evidence:meta:none") ? "low" : "medium"),
                "Hold current board; Meta comps unavailable.",
                null,
                null,
                null);
    }
}
