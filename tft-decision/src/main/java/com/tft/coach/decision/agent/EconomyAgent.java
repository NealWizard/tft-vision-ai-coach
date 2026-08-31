package com.tft.coach.decision.agent;

import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.knowledge.tools.GameRuleTool;
import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** SAVE/LEVEL/ROLL/ALL_IN resource tempo. */
public final class EconomyAgent implements DomainAgent {

    @Override
    public DecisionType decisionType() {
        return DecisionType.ECONOMY;
    }

    @Override
    public CandidateSet advise(GameState state, Context context) {
        List<String> degraded = new ArrayList<>(context.meta().degradedReasons());
        List<String> evidence = new ArrayList<>();
        context.meta().snapshot().ifPresent(s -> evidence.add("evidence:meta:" + s.id()));
        int interestCap = 5;
        int levelCost = 4;
        try {
            Map<String, Object> interest = context.knowledge().tool(GameRuleTool.TOOL_ID)
                    .get(state.patch(), "economy.interest");
            evidence.add("evidence:rule:economy.interest");
            Object value = interest.get("value");
            if (value instanceof Map<?, ?> map && map.get("cap") instanceof Number n) {
                interestCap = n.intValue();
            }
            Map<String, Object> xp = context.knowledge().tool(GameRuleTool.TOOL_ID)
                    .get(state.patch(), "xp.level");
            evidence.add("evidence:rule:xp.level");
            Object xpVal = xp.get("value");
            if (xpVal instanceof Map<?, ?> map && map.get("buy_xp_cost") instanceof Number n) {
                levelCost = n.intValue();
            }
        } catch (RuntimeException ex) {
            degraded.add("RULE_TOOL");
        }
        evidence.addAll(context.ragEvidence());

        int gold = state.player().gold();
        int hp = state.player().hp();
        int level = state.player().level();
        List<CandidateSet.CandidateOption> options = new ArrayList<>();
        if (gold >= 50) {
            options.add(CandidateSets.option(
                    "econ-save",
                    ActionType.SAVE,
                    0.78,
                    "Keep +" + interestCap + " interest",
                    "Weaker board this round",
                    "medium",
                    List.of("gold>=50"),
                    evidence,
                    "high",
                    "Hold interest breakpoint.",
                    null));
        }
        if (gold >= levelCost && level < 9) {
            options.add(CandidateSets.option(
                    "econ-level",
                    ActionType.LEVEL,
                    hp < 40 ? 0.74 : 0.62,
                    "Higher shop odds",
                    "Breaks interest temporarily",
                    "medium",
                    List.of("gold>=" + levelCost),
                    evidence,
                    "medium",
                    "Buy XP to level.",
                    null));
        }
        if (hp <= 30) {
            options.add(CandidateSets.option(
                    "econ-allin",
                    ActionType.ALL_IN,
                    0.7,
                    "Stabilize HP",
                    "Economy collapse",
                    "high",
                    List.of("hp<=30"),
                    evidence,
                    "medium",
                    "Spend to survive.",
                    null));
        } else if (gold >= 2 && options.size() < 3) {
            options.add(CandidateSets.option(
                    "econ-roll",
                    ActionType.ROLL,
                    0.48,
                    "Find copies",
                    "Gold variance",
                    "high",
                    List.of("gold>=2"),
                    evidence,
                    "medium",
                    "Roll if upgrades are on curve.",
                    null));
        }
        if (options.size() < 2) {
            options.add(CandidateSets.option(
                    "econ-hold",
                    ActionType.HOLD,
                    0.5,
                    "Wait",
                    "Tempo",
                    "medium",
                    List.of(),
                    evidence,
                    "medium",
                    "Hold current economy plan.",
                    null));
        }
        while (options.size() < 2) {
            options.add(CandidateSets.option(
                    "econ-hold-" + options.size(),
                    ActionType.HOLD,
                    0.4,
                    "Wait",
                    "Tempo",
                    "high",
                    List.of(),
                    evidence,
                    "low",
                    "Insufficient economy signal.",
                    null));
        }
        return CandidateSets.complete(
                DecisionType.ECONOMY, state, context.fingerprint(), context.correlationId(),
                "run-economy", options, degraded);
    }
}
