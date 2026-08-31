package com.tft.coach.decision.sim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.knowledge.tools.GameRuleTool;
import com.tft.coach.knowledge.tools.KnowledgeTool;
import com.tft.coach.knowledge.tools.ProbabilityTool;
import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SimulatorAnnotator {

    private final EconomySimulator simulator = new EconomySimulator();
    private final ObjectMapper mapper = new ObjectMapper();

    public CandidateSet attach(CandidateSet set, GameState state, KnowledgeTool rules, KnowledgeTool odds) {
        if (set.decisionType() != DecisionType.SHOP && set.decisionType() != DecisionType.ECONOMY) {
            return set;
        }
        List<CandidateSet.CandidateOption> next = new ArrayList<>();
        List<String> degraded = new ArrayList<>(set.degradedReasons());
        for (CandidateSet.CandidateOption option : set.candidates()) {
            ProjectedState projected = simulator.simulate(
                    state, option.actionType(), rules, odds, buyCost(option));
            if (projected.degraded()) {
                degraded.add("SIMULATOR");
            }
            Map<String, Object> details = new LinkedHashMap<>();
            if (option.details() != null) {
                details.putAll(option.details());
            }
            details.put("projected", mapper.convertValue(projected, Map.class));
            next.add(new CandidateSet.CandidateOption(
                    option.candidateId(),
                    option.actionType(),
                    option.score(),
                    option.risk(),
                    option.preconditions(),
                    option.evidence(),
                    option.confidence(),
                    option.summary(),
                    option.reasoning(),
                    option.tradeoffs(),
                    details));
        }
        return new CandidateSet(
                set.schemaVersion(),
                set.candidateSetId(),
                set.decisionType(),
                set.basedOn(),
                next,
                !degraded.isEmpty() || set.degraded(),
                List.copyOf(degraded.stream().distinct().toList()),
                set.trace());
    }

    public EconomySimulator simulator() {
        return simulator;
    }

    static Integer buyCost(CandidateSet.CandidateOption option) {
        if (option.details() == null) {
            return null;
        }
        Object raw = option.details().get("buy_cost");
        if (raw instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    public static KnowledgeTool rules(com.tft.coach.knowledge.platform.KnowledgePlatform knowledge) {
        return knowledge.tool(GameRuleTool.TOOL_ID);
    }

    public static KnowledgeTool odds(com.tft.coach.knowledge.platform.KnowledgePlatform knowledge) {
        return knowledge.tool(ProbabilityTool.TOOL_ID);
    }
}
