package com.tft.coach.state.gamestate;

import com.tft.coach.state.diff.GameStateDiff;
import com.tft.coach.state.fusion.ObservationFusion;
import com.tft.coach.state.observation.Observation;
import com.tft.coach.state.observation.ObservationFactory;
import com.tft.coach.state.timeline.MatchTimeline;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStatePipelineTest {

    private final GameStateBuilder builder = new GameStateBuilder();

    @Test
    void buildsPlayerAndFiveShopSlotsFromFixtures() {
        GameState state = builder.build("match-1", "set18-18.1", shopFixtures());
        new GameStateValidator().requireValid(state);
        assertEquals("3-3", state.stage());
        assertEquals(6, state.player().level());
        assertEquals(65, state.player().gold());
        assertEquals(5, state.shop().size());
        assertEquals("malphite", state.shop().get(0).championId());
        assertEquals(1, state.shop().get(0).cost());
        assertEquals("neeko", state.shop().get(3).championId());
        assertEquals(2, state.shop().get(3).cost());
    }

    @Test
    void dropsLowConfidenceBeforeBuild() {
        Observation weakGold = new Observation(
                "1.0.0", "obs-weak", "player.gold", 999, "999",
                new Observation.Confidence(0.2, "low"), "ocr",
                null, null, null, null,
                Instant.parse("2026-08-31T00:00:00Z"),
                null, null, "f1"
        );
        List<Observation> fused = ObservationFusion.fuse(List.of(
                ObservationFactory.fromFixture("player.gold", 65),
                weakGold
        ));
        assertEquals(1, fused.size());
        assertEquals(65, fused.get(0).value());
    }

    @Test
    void missingPlayerFieldFails() {
        assertThrows(IllegalArgumentException.class, () ->
                builder.build("m", "set18-18.1", List.of(
                        ObservationFactory.fromFixture("stage", "3-3"),
                        ObservationFactory.fromFixture("player.level", 6),
                        ObservationFactory.fromFixture("player.gold", 65)
                )));
    }

    @Test
    void diffAndTimelineDetectGoldAndShopChange() {
        GameState a = builder.build("match-1", "set18-18.1", shopFixtures());
        List<Observation> next = List.of(
                ObservationFactory.fromFixture("stage", "3-3"),
                ObservationFactory.fromFixture("player.level", 6),
                ObservationFactory.fromFixture("player.gold", 64),
                ObservationFactory.fromFixture("player.hp", 80),
                ObservationFactory.fromFixture("shop.0.champion_id", "warwick"),
                ObservationFactory.fromFixture("shop.0.cost", 2)
        );
        GameState b = builder.build("match-1", "set18-18.1", next);
        List<GameStateDiff.Event> events = GameStateDiff.diff(a, b);
        assertTrue(events.stream().anyMatch(e -> "GoldChanged".equals(e.type())));
        assertTrue(events.stream().anyMatch(e -> "ShopChanged".equals(e.type()) || "Sell".equals(e.type()) || "Buy".equals(e.type())));
        MatchTimeline.Timeline timeline = MatchTimeline.fromStates(List.of(a, b));
        assertEquals(2, timeline.snapshots().size());
        assertEquals(events.size(), timeline.snapshots().get(1).eventsFromPrevious().size());
    }

    private static List<Observation> shopFixtures() {
        return List.of(
                ObservationFactory.fromFixture("stage", "3-3"),
                ObservationFactory.fromFixture("player.level", 6),
                ObservationFactory.fromFixture("player.xp", 2),
                ObservationFactory.fromFixture("player.gold", 65),
                ObservationFactory.fromFixture("player.hp", 80),
                ObservationFactory.fromFixture("player.streak", 1),
                ObservationFactory.fromFixture("shop.0.champion_id", "malphite"),
                ObservationFactory.fromFixture("shop.0.cost", 1),
                ObservationFactory.fromFixture("shop.1.champion_id", "warwick"),
                ObservationFactory.fromFixture("shop.1.cost", 2),
                ObservationFactory.fromFixture("shop.2.champion_id", "kogmaw"),
                ObservationFactory.fromFixture("shop.2.cost", 1),
                ObservationFactory.fromFixture("shop.3.champion_id", "neeko"),
                ObservationFactory.fromFixture("shop.3.cost", 2),
                ObservationFactory.fromFixture("shop.4.champion_id", "reksai"),
                ObservationFactory.fromFixture("shop.4.cost", 1),
                ObservationFactory.fromFixture("board.0.champion_id", "ornn"),
                ObservationFactory.fromFixture("board.0.star", 2),
                ObservationFactory.fromFixture("trait.0.id", "warden"),
                ObservationFactory.fromFixture("trait.0.count", 2),
                ObservationFactory.fromFixture("item.0", "sunfire_cape"),
                ObservationFactory.fromFixture("augment.0", "placeholder_augment")
        );
    }
}
