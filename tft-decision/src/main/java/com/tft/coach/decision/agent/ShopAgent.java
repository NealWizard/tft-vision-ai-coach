package com.tft.coach.decision.agent;

import com.tft.coach.data.meta.CompStat;
import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.knowledge.tools.ProbabilityTool;
import com.tft.coach.meta.MetaService;
import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** BUY/HOLD/ROLL/LOCK on the current shop only. */
public final class ShopAgent implements DomainAgent {

    @Override
    public DecisionType decisionType() {
        return DecisionType.SHOP;
    }

    @Override
    public CandidateSet advise(GameState state, Context context) {
        List<String> degraded = new ArrayList<>(context.meta().degradedReasons());
        List<String> evidence = baseEvidence(context);
        try {
            context.knowledge().tool(ProbabilityTool.TOOL_ID).get(state.patch(), "level." + state.player().level());
            evidence.add("evidence:tool:probability-tool");
        } catch (RuntimeException ex) {
            degraded.add("PROBABILITY_TOOL");
        }
        evidence.addAll(context.ragEvidence());

        Set<String> cores = metaCores(context);
        List<GameState.ShopSlot> shop = state.shop() == null ? List.of() : state.shop();
        int gold = state.player().gold();
        List<CandidateSet.CandidateOption> options = new ArrayList<>();

        GameState.ShopSlot buy = null;
        for (GameState.ShopSlot slot : shop) {
            if (slot.championId() != null && cores.contains(slot.championId()) && slot.cost() != null) {
                if (gold >= slot.cost()) {
                    buy = slot;
                    break;
                }
            }
        }
        if (buy != null) {
            int cost = buy.cost();
            options.add(CandidateSets.option(
                    "shop-buy-" + buy.championId(),
                    ActionType.BUY,
                    0.82,
                    "Progress core unit",
                    "Spends gold",
                    "medium",
                    List.of("gold>=" + cost, "shop contains " + buy.championId()),
                    evidence,
                    "high",
                    "Buy " + buy.championId() + " from shop.",
                    Map.of("champion_id", buy.championId(), "buy_cost", cost)));
        }
        options.add(CandidateSets.option(
                "shop-hold",
                ActionType.HOLD,
                0.6,
                "Keep options",
                "May miss spike",
                "medium",
                List.of("shop visible"),
                evidence,
                "medium",
                "Hold gold and current shop.",
                null));
        if (gold >= 50 && buy != null) {
            options.add(CandidateSets.option(
                    "shop-lock",
                    ActionType.LOCK,
                    0.55,
                    "Protect key shop",
                    "Cannot roll",
                    "medium",
                    List.of("gold>=50"),
                    evidence,
                    "medium",
                    "Lock shop to keep " + buy.championId() + ".",
                    null));
        } else if (gold >= 2) {
            options.add(CandidateSets.option(
                    "shop-roll",
                    ActionType.ROLL,
                    0.5,
                    "Look for core",
                    "Gold variance",
                    "high",
                    List.of("gold>=2"),
                    evidence,
                    "medium",
                    "Roll shop once.",
                    null));
        }
        if (options.size() < 2) {
            options.add(CandidateSets.option(
                    "shop-hold-2",
                    ActionType.HOLD,
                    0.45,
                    "Wait",
                    "Tempo loss",
                    "high",
                    List.of(),
                    evidence,
                    "low",
                    "No shop action is clearly better.",
                    null));
        }
        return CandidateSets.complete(
                DecisionType.SHOP, state, context.fingerprint(), context.correlationId(),
                "run-shop", options, degraded);
    }

    private static List<String> baseEvidence(Context context) {
        List<String> evidence = new ArrayList<>();
        context.meta().snapshot().ifPresent(s -> evidence.add("evidence:meta:" + s.id()));
        return evidence;
    }

    private static Set<String> metaCores(Context context) {
        Set<String> cores = new HashSet<>();
        for (MetaService.ScoredComp scored : context.meta().comps()) {
            CompStat comp = scored.comp();
            cores.addAll(comp.coreUnits());
        }
        return cores;
    }
}
