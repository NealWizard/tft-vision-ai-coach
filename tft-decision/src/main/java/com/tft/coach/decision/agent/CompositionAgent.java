package com.tft.coach.decision.agent;

import com.tft.coach.data.meta.CompStat;
import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.meta.MetaService;
import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Current vs target shell; persist or pivot. */
public final class CompositionAgent implements DomainAgent {

    @Override
    public DecisionType decisionType() {
        return DecisionType.COMPOSITION;
    }

    @Override
    public CandidateSet advise(GameState state, Context context) {
        List<String> degraded = new ArrayList<>(context.meta().degradedReasons());
        List<String> evidence = new ArrayList<>();
        context.meta().snapshot().ifPresent(s -> evidence.add("evidence:meta:" + s.id()));
        evidence.addAll(context.ragEvidence());

        Set<String> owned = ownedUnits(state);
        List<MetaService.ScoredComp> comps = context.meta().comps();
        List<CandidateSet.CandidateOption> options = new ArrayList<>();
        ScoredOverlap current = bestOverlap(comps, owned);
        for (int i = 0; i < Math.min(3, comps.size()); i++) {
            MetaService.ScoredComp scored = comps.get(i);
            CompStat comp = scored.comp();
            List<String> missing = missing(comp.coreUnits(), owned);
            int overlap = comp.coreUnits().size() - missing.size();
            boolean persist = current != null && current.compId().equals(comp.compId()) && overlap >= 2;
            ActionType action = persist ? ActionType.HOLD : ActionType.PIVOT;
            double compatibility = comp.coreUnits().isEmpty()
                    ? 0.0
                    : (double) overlap / comp.coreUnits().size();
            options.add(CandidateSets.option(
                    "comp-" + comp.compId(),
                    action,
                    scored.score(),
                    persist ? "Already overlapping core" : "Stronger Meta shell",
                    persist ? "Contest" : "Transition cost",
                    "medium",
                    List.of("patch=" + state.patch()),
                    evidence,
                    "high",
                    persist
                            ? "Stay on " + comp.name() + "."
                            : "Pivot toward " + comp.name() + ".",
                    Map.of(
                            "current_comp", current == null ? "unknown" : current.compId(),
                            "target_comp", comp.compId(),
                            "missing_core_units", missing,
                            "compatibility", compatibility,
                            "transition_cost_estimate", missing.size())));
        }
        if (options.size() < 2) {
            options.add(CandidateSets.option(
                    "comp-hold",
                    ActionType.HOLD,
                    0.4,
                    "Wait",
                    "No Meta shell",
                    "high",
                    List.of(),
                    evidence,
                    "low",
                    "Hold board; Meta comps unavailable.",
                    Map.of(
                            "current_comp", "unknown",
                            "target_comp", "unknown",
                            "missing_core_units", List.of(),
                            "compatibility", 0,
                            "transition_cost_estimate", 0)));
        }
        return CandidateSets.complete(
                DecisionType.COMPOSITION, state, context.fingerprint(), context.correlationId(),
                "run-composition", options, degraded);
    }

    private static Set<String> ownedUnits(GameState state) {
        Set<String> ids = new HashSet<>();
        if (state.board() != null) {
            state.board().forEach(u -> {
                if (u.championId() != null) {
                    ids.add(u.championId());
                }
            });
        }
        if (state.bench() != null) {
            state.bench().forEach(u -> {
                if (u.championId() != null) {
                    ids.add(u.championId());
                }
            });
        }
        return ids;
    }

    private static List<String> missing(List<String> cores, Set<String> owned) {
        List<String> missing = new ArrayList<>();
        for (String core : cores) {
            if (!owned.contains(core)) {
                missing.add(core);
            }
        }
        return missing;
    }

    private static ScoredOverlap bestOverlap(List<MetaService.ScoredComp> comps, Set<String> owned) {
        ScoredOverlap best = null;
        for (MetaService.ScoredComp scored : comps) {
            int overlap = 0;
            for (String core : scored.comp().coreUnits()) {
                if (owned.contains(core)) {
                    overlap++;
                }
            }
            if (best == null || overlap > best.overlap()) {
                best = new ScoredOverlap(scored.comp().compId(), overlap);
            }
        }
        return best;
    }

    private record ScoredOverlap(String compId, int overlap) {
    }
}
