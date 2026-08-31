package com.tft.coach.decision.sim;

import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.knowledge.platform.KnowledgePlatform;
import com.tft.coach.knowledge.tools.GameRuleTool;
import com.tft.coach.knowledge.tools.ProbabilityTool;
import com.tft.coach.state.gamestate.GameState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EconomySimulatorTest {

    @Test
    void sameInputSameOutput() {
        KnowledgePlatform knowledge = KnowledgePlatform.createDefault();
        EconomySimulator simulator = new EconomySimulator();
        GameState state = new GameState(
                "1.0.0",
                "match-1",
                "set18-18.1",
                "3-2",
                Instant.parse("2026-08-31T00:00:00Z"),
                new GameState.Player(6, 0, 50, 80, 0),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new GameState.Confidence(0.9, "high"));
        ProjectedState a = simulator.simulate(
                state,
                ActionType.ROLL,
                knowledge.tool(GameRuleTool.TOOL_ID),
                knowledge.tool(ProbabilityTool.TOOL_ID));
        ProjectedState b = simulator.simulate(
                state,
                ActionType.ROLL,
                knowledge.tool(GameRuleTool.TOOL_ID),
                knowledge.tool(ProbabilityTool.TOOL_ID));
        assertEquals(a, b);
        assertEquals(48, a.gold());
        assertEquals(4, a.interest());
    }

    @Test
    void buySubtractsShopSlotCost() {
        KnowledgePlatform knowledge = KnowledgePlatform.createDefault();
        EconomySimulator simulator = new EconomySimulator();
        GameState state = new GameState(
                "1.0.0",
                "match-1",
                "set18-18.1",
                "3-2",
                Instant.parse("2026-08-31T00:00:00Z"),
                new GameState.Player(6, 0, 50, 80, 0),
                List.of(new GameState.ShopSlot(0, "champ.ahri", 4)),
                null,
                null,
                null,
                null,
                null,
                null,
                new GameState.Confidence(0.9, "high"));
        ProjectedState projected = simulator.simulate(
                state,
                ActionType.BUY,
                knowledge.tool(GameRuleTool.TOOL_ID),
                knowledge.tool(ProbabilityTool.TOOL_ID),
                4);
        assertEquals(46, projected.gold());
        assertEquals(4, projected.interest());
    }
}
