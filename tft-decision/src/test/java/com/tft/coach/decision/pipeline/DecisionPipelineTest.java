package com.tft.coach.decision.pipeline;

import com.tft.coach.decision.DecisionPlatform;
import com.tft.coach.decision.candidate.ActionType;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.state.gamestate.GameState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecisionPipelineTest {

    @Test
    void analyzeReturnsTwoToThreeCandidatesWithoutLlm() {
        DecisionPlatform platform = DecisionPlatform.createDefault();
        CandidateSet set = platform.pipeline().analyze(sampleState(), DecisionPipeline.AnalyzeRequest.defaults());
        assertEquals(DecisionType.COMPOSITION, set.decisionType());
        assertTrue(set.candidates().size() >= 2 && set.candidates().size() <= 3);
        assertTrue(set.candidates().stream().anyMatch(c -> c.actionType() == ActionType.PIVOT));
        assertTrue(set.candidates().stream().allMatch(c -> !c.evidence().isEmpty()));
        assertTrue(set.degradedReasons().contains("SNAPSHOT_FIXTURE"));
        assertEquals("set18-18.1", set.basedOn().patch());
    }

    @Test
    void refusesMissingGameState() {
        DecisionPlatform platform = DecisionPlatform.createDefault();
        assertThrows(DecisionGuard.MissingGameStateException.class,
                () -> platform.pipeline().analyze(null, DecisionPipeline.AnalyzeRequest.defaults()));
    }

    @Test
    void shopAndEconomyAgentsRunWithoutLlm() {
        DecisionPlatform platform = DecisionPlatform.createDefault();
        GameState shopState = new GameState(
                "1.0.0",
                "match-1",
                "set18-18.1",
                "3-2",
                Instant.parse("2026-08-31T00:00:00Z"),
                new GameState.Player(6, 0, 50, 80, 0),
                List.of(
                        new GameState.ShopSlot(0, "champ.xayah", 4),
                        new GameState.ShopSlot(1, "champ.ahri", 4)),
                List.of(new GameState.BoardUnit("champ.jinx", 1, 0, 0)),
                null,
                null,
                null,
                null,
                null,
                new GameState.Confidence(0.9, "high"));
        CandidateSet shop = platform.pipeline().analyze(
                shopState,
                new DecisionPipeline.AnalyzeRequest("global", "24h", null, null, null, DecisionType.SHOP));
        assertEquals(DecisionType.SHOP, shop.decisionType());
        assertTrue(shop.candidates().stream().anyMatch(c -> c.actionType() == ActionType.BUY));
        assertTrue(shop.candidates().size() >= 2 && shop.candidates().size() <= 3);

        CandidateSet econ = platform.pipeline().analyze(
                shopState,
                new DecisionPipeline.AnalyzeRequest("global", "24h", null, null, null, DecisionType.ECONOMY));
        assertEquals(DecisionType.ECONOMY, econ.decisionType());
        assertTrue(econ.candidates().stream().anyMatch(c -> c.actionType() == ActionType.SAVE));

        CandidateSet comp = platform.pipeline().analyze(shopState, DecisionPipeline.AnalyzeRequest.defaults());
        assertTrue(comp.candidates().getFirst().details().containsKey("target_comp"));
        assertTrue(comp.candidates().getFirst().details().containsKey("missing_core_units"));
    }

    @Test
    void fingerprintIsStable() {
        GameState state = sampleState();
        assertEquals(GameStateFingerprint.sha256(state), GameStateFingerprint.sha256(state));
    }

    static GameState sampleState() {
        return new GameState(
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
    }
}
