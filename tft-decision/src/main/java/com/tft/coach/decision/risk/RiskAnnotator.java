package com.tft.coach.decision.risk;

import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.List;

/** Annotates existing candidates. Does not invent numeric facts. */
public final class RiskAnnotator {

    public CandidateSet annotate(CandidateSet set, GameState state) {
        int hp = state.player().hp();
        int gold = state.player().gold();
        List<CandidateSet.CandidateOption> next = new ArrayList<>();
        for (CandidateSet.CandidateOption option : set.candidates()) {
            String uncertainty = option.risk().uncertainty();
            if (hp <= 30 && option.actionType() == ActionType.SAVE) {
                uncertainty = "high";
            }
            if (gold < 10 && option.actionType() == ActionType.ROLL) {
                uncertainty = "high";
            }
            next.add(new CandidateSet.CandidateOption(
                    option.candidateId(),
                    option.actionType(),
                    option.score(),
                    new CandidateSet.Risk(option.risk().upside(), option.risk().downside(), uncertainty),
                    option.preconditions(),
                    option.evidence(),
                    option.confidence(),
                    option.summary(),
                    option.reasoning(),
                    option.tradeoffs(),
                    option.details()));
        }
        return new CandidateSet(
                set.schemaVersion(),
                set.candidateSetId(),
                set.decisionType(),
                set.basedOn(),
                next,
                set.degraded(),
                set.degradedReasons(),
                set.trace());
    }
}
