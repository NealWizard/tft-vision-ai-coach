package com.tft.coach.decision.agent;

import com.tft.coach.data.meta.ItemStat;
import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.knowledge.tools.ItemTool;
import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.List;

public final class ItemAgent implements DomainAgent {

    @Override
    public DecisionType decisionType() {
        return DecisionType.ITEM;
    }

    @Override
    public CandidateSet advise(GameState state, Context context) {
        List<String> degraded = new ArrayList<>(context.meta().degradedReasons());
        List<String> evidence = new ArrayList<>();
        context.meta().snapshot().ifPresent(s -> evidence.add("evidence:meta:" + s.id()));
        boolean identityMissing = state.items() == null || state.items().isEmpty();
        if (identityMissing) {
            degraded.add("ITEM_IDENTITY_MISSING");
        }
        try {
            context.knowledge().tool(ItemTool.TOOL_ID);
            evidence.add("evidence:tool:item-tool");
        } catch (RuntimeException ex) {
            degraded.add("ITEM_TOOL");
        }
        List<ItemStat> items = context.meta().snapshot()
                .map(s -> s.snapshot().items())
                .orElse(List.of());
        List<CandidateSet.CandidateOption> options = new ArrayList<>();
        int limit = Math.min(2, items.size());
        for (int i = 0; i < limit; i++) {
            ItemStat item = items.get(i);
            options.add(CandidateSets.option(
                    "item-craft-" + item.itemId(),
                    ActionType.CRAFT,
                    identityMissing ? 0.4 : Math.min(1.0, item.winRate()),
                    "Meta completed item",
                    "Wrong carry identity",
                    identityMissing ? "high" : "medium",
                    List.of("need components for " + item.itemId()),
                    evidence,
                    identityMissing ? "low" : "medium",
                    "Craft " + item.name() + " if components match.",
                    null));
        }
        options.add(CandidateSets.option(
                "item-hold",
                ActionType.HOLD,
                0.5,
                "Wait for slam timing",
                "Raw components are weak",
                "medium",
                List.of(),
                evidence,
                identityMissing ? "low" : "medium",
                "Hold components.",
                null));
        return CandidateSets.complete(
                DecisionType.ITEM, state, context.fingerprint(), context.correlationId(),
                "run-item", options, degraded);
    }
}
