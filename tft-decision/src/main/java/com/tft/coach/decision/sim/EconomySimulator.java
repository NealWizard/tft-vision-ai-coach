package com.tft.coach.decision.sim;

import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.knowledge.tools.KnowledgeTool;
import com.tft.coach.knowledge.tools.ProbabilityTool;
import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gold/interest/XP/level/shop-odds only. No combat. */
public final class EconomySimulator {

    public ProjectedState simulate(GameState state, ActionType action, KnowledgeTool rules, KnowledgeTool odds) {
        List<String> evidence = new ArrayList<>();
        boolean degraded = false;
        int gold = state.player().gold();
        Integer xp = state.player().xp();
        int level = state.player().level();
        int rollCount = 0;
        int interestCap = 5;
        int perTier = 10;
        int buyXpCost = 4;
        int buyXpAmount = 4;
        try {
            Map<String, Object> interest = rules.get(state.patch(), "economy.interest");
            evidence.add("evidence:rule:economy.interest");
            Object value = interest.get("value");
            if (value instanceof Map<?, ?> map) {
                if (map.get("cap") instanceof Number n) {
                    interestCap = n.intValue();
                }
                if (map.get("per_tier_gold") instanceof Number n) {
                    perTier = n.intValue();
                }
            }
            Map<String, Object> xpRule = rules.get(state.patch(), "xp.level");
            evidence.add("evidence:rule:xp.level");
            Object xpVal = xpRule.get("value");
            if (xpVal instanceof Map<?, ?> map) {
                if (map.get("buy_xp_cost") instanceof Number n) {
                    buyXpCost = n.intValue();
                }
                if (map.get("buy_xp_amount") instanceof Number n) {
                    buyXpAmount = n.intValue();
                }
            }
        } catch (RuntimeException ex) {
            degraded = true;
            evidence.add("evidence:rule:missing");
        }

        if (action == ActionType.ROLL && gold >= 2) {
            gold -= 2;
            rollCount = 1;
        } else if (action == ActionType.LEVEL && gold >= buyXpCost) {
            gold -= buyXpCost;
            if (xp == null) {
                degraded = true;
            } else {
                xp = xp + buyXpAmount;
            }
        } else if (action == ActionType.BUY) {
            gold = Math.max(0, gold - 3);
        } else if (action == ActionType.ALL_IN) {
            gold = 0;
        }

        int interest = Math.min(interestCap, gold / perTier);
        Map<String, Double> shopOdds = null;
        try {
            Map<String, Object> row = odds.get(state.patch(), "level." + level);
            evidence.add("evidence:tool:probability-tool");
            Object raw = row.get("odds_pct_by_cost");
            if (raw instanceof List<?> list) {
                shopOdds = new LinkedHashMap<>();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof Number n) {
                        shopOdds.put("cost_" + (i + 1), n.doubleValue());
                    }
                }
            }
        } catch (RuntimeException ex) {
            degraded = true;
        }
        return new ProjectedState(gold, interest, xp, level, rollCount, shopOdds, degraded, evidence);
    }
}
