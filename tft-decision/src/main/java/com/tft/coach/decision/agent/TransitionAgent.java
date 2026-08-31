package com.tft.coach.decision.agent;

import com.tft.coach.data.meta.CompStat;
import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.decision.contest.ContestBook;
import com.tft.coach.decision.contest.ContestSnapshot;
import com.tft.coach.meta.MetaService;
import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TransitionAgent implements DomainAgent {

    @Override
    public DecisionType decisionType() {
        return DecisionType.TRANSITION;
    }

    @Override
    public CandidateSet advise(GameState state, Context context) {
        List<String> degraded = new ArrayList<>(context.meta().degradedReasons());
        List<String> evidence = new ArrayList<>();
        context.meta().snapshot().ifPresent(s -> evidence.add("evidence:meta:" + s.id()));
        List<ContestSnapshot> contests = context.meta().snapshot()
                .map(ContestBook::fromMeta)
                .orElse(List.of());
        if (contests.isEmpty()) {
            degraded.add("CONTEST_UNKNOWN");
        } else {
            evidence.add("evidence:contest:meta");
        }
        Set<String> owned = new HashSet<>();
        if (state.board() != null) {
            state.board().forEach(u -> owned.add(u.championId()));
        }
        List<CandidateSet.CandidateOption> options = new ArrayList<>();
        List<MetaService.ScoredComp> comps = context.meta().comps();
        if (!comps.isEmpty()) {
            CompStat top = comps.getFirst().comp();
            int have = 0;
            for (String core : top.coreUnits()) {
                if (owned.contains(core)) {
                    have++;
                }
            }
            boolean commit = have >= 2;
            options.add(CandidateSets.option(
                    "trans-commit",
                    ActionType.HOLD,
                    commit ? 0.76 : 0.5,
                    "Core already started",
                    "Contest from Meta pick rate",
                    contests.isEmpty() ? "high" : "medium",
                    List.of("stage=" + state.stage()),
                    evidence,
                    "medium",
                    commit ? "Commit to " + top.name() + "." : "Too few cores to commit.",
                    null));
            if (comps.size() > 1) {
                CompStat alt = comps.get(1).comp();
                options.add(CandidateSets.option(
                        "trans-pivot",
                        ActionType.PIVOT,
                        commit ? 0.48 : 0.7,
                        "Lower contest alternative",
                        "Transition cost",
                        "medium",
                        List.of("stage=" + state.stage()),
                        evidence,
                        "medium",
                        "Pivot toward " + alt.name() + ".",
                        null));
            }
        }
        if (options.size() < 2) {
            options.add(CandidateSets.option(
                    "trans-hold",
                    ActionType.HOLD,
                    0.4,
                    "Wait",
                    "No path",
                    "high",
                    List.of(),
                    evidence,
                    "low",
                    "Hold; no transition path.",
                    null));
            options.add(CandidateSets.option(
                    "trans-pivot-unknown",
                    ActionType.PIVOT,
                    0.35,
                    "Explore",
                    "Unknown contest",
                    "high",
                    List.of(),
                    evidence,
                    "low",
                    "Flexible pivot if cores appear.",
                    null));
        }
        return CandidateSets.complete(
                DecisionType.TRANSITION, state, context.fingerprint(), context.correlationId(),
                "run-transition", options, degraded);
    }
}
