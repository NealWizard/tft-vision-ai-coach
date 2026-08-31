package com.tft.coach.state.gamestate;

import com.tft.coach.state.fusion.ObservationFusion;
import com.tft.coach.state.observation.Observation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds GameState from fused Observations.
 */
public final class GameStateBuilder {

    private final GameStateValidator validator;

    public GameStateBuilder() {
        this(new GameStateValidator());
    }

    public GameStateBuilder(GameStateValidator validator) {
        this.validator = Objects.requireNonNull(validator);
    }

    public GameState build(String matchId, String patch, List<Observation> observations) {
        List<Observation> fused = ObservationFusion.fuse(observations);
        Map<String, Observation> byField = fused.stream()
                .collect(Collectors.toMap(Observation::field, o -> o, (a, b) -> a));

        String stage = requireString(byField, "stage");
        int level = requireInt(byField, "player.level");
        int gold = requireInt(byField, "player.gold");
        int hp = requireInt(byField, "player.hp");
        Integer xp = optionalInt(byField, "player.xp");
        Integer streak = optionalInt(byField, "player.streak");
        Instant observedAt = fused.stream()
                .map(Observation::timestamp)
                .max(Instant::compareTo)
                .orElse(Instant.parse("2026-08-31T00:00:00Z"));

        List<GameState.ShopSlot> shop = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Observation champ = byField.get("shop." + i + ".champion_id");
            if (champ == null) {
                continue;
            }
            Integer cost = optionalInt(byField, "shop." + i + ".cost");
            shop.add(new GameState.ShopSlot(i, String.valueOf(champ.value()), cost));
        }

        List<GameState.BoardUnit> board = units("board", byField);
        List<GameState.BoardUnit> bench = units("bench", byField);
        List<String> items = indexedIds("item", byField);
        List<GameState.TraitCount> traits = traits(byField);
        List<String> augments = indexedIds("augment", byField);
        List<String> mechanics = indexedIds("mechanic", byField);

        double minScore = fused.stream().mapToDouble(o -> o.confidence().score()).min().orElse(1.0);
        String levelLabel = minScore >= 0.95 ? "certain" : minScore >= 0.80 ? "high" : "medium";

        GameState state = new GameState(
                "1.0.0",
                matchId,
                patch,
                stage,
                observedAt,
                new GameState.Player(level, xp, gold, hp, streak),
                shop.isEmpty() ? null : shop,
                board.isEmpty() ? null : board,
                bench.isEmpty() ? null : bench,
                items.isEmpty() ? null : items,
                traits.isEmpty() ? null : traits,
                augments.isEmpty() ? null : augments,
                mechanics.isEmpty() ? null : mechanics,
                new GameState.Confidence(minScore, levelLabel)
        );
        validator.requireValid(state);
        return state;
    }

    private static List<GameState.BoardUnit> units(String prefix, Map<String, Observation> byField) {
        List<GameState.BoardUnit> units = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Observation champ = byField.get(prefix + "." + i + ".champion_id");
            if (champ == null) {
                continue;
            }
            units.add(new GameState.BoardUnit(
                    String.valueOf(champ.value()),
                    optionalInt(byField, prefix + "." + i + ".star"),
                    optionalInt(byField, prefix + "." + i + ".row"),
                    optionalInt(byField, prefix + "." + i + ".col")
            ));
        }
        return units;
    }

    private static List<String> indexedIds(String prefix, Map<String, Observation> byField) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Observation obs = byField.get(prefix + "." + i);
            if (obs != null) {
                ids.add(String.valueOf(obs.value()));
            }
        }
        return ids;
    }

    private static List<GameState.TraitCount> traits(Map<String, Observation> byField) {
        List<GameState.TraitCount> traits = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Observation id = byField.get("trait." + i + ".id");
            if (id == null) {
                continue;
            }
            traits.add(new GameState.TraitCount(
                    String.valueOf(id.value()),
                    optionalInt(byField, "trait." + i + ".count")
            ));
        }
        return traits;
    }

    private static String requireString(Map<String, Observation> byField, String field) {
        Observation obs = byField.get(field);
        if (obs == null) {
            throw new IllegalArgumentException("missing fused observation: " + field);
        }
        return String.valueOf(obs.value());
    }

    private static int requireInt(Map<String, Observation> byField, String field) {
        Integer v = optionalInt(byField, field);
        if (v == null) {
            throw new IllegalArgumentException("missing fused observation: " + field);
        }
        return v;
    }

    private static Integer optionalInt(Map<String, Observation> byField, String field) {
        Observation obs = byField.get(field);
        if (obs == null) {
            return null;
        }
        Object value = obs.value();
        if (value instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
