package com.tft.coach.state.diff;

import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compares two GameStates into discrete events.
 */
public final class GameStateDiff {

    private GameStateDiff() {
    }

    public record Event(String type, String field, Object from, Object to) {
    }

    public static List<Event> diff(GameState before, GameState after) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        List<Event> events = new ArrayList<>();
        compare("player.gold", before.player().gold(), after.player().gold(), "GoldChanged", events);
        compare("player.hp", before.player().hp(), after.player().hp(), "HpChanged", events);
        compare("player.level", before.player().level(), after.player().level(), "LevelChanged", events);
        compare("player.xp", before.player().xp(), after.player().xp(), "XpChanged", events);
        compare("player.streak", before.player().streak(), after.player().streak(), "StreakChanged", events);
        compare("stage", before.stage(), after.stage(), "StageChanged", events);

        int beforeSlots = before.shop() == null ? 0 : before.shop().size();
        int afterSlots = after.shop() == null ? 0 : after.shop().size();
        int n = Math.max(beforeSlots, afterSlots);
        for (int i = 0; i < n; i++) {
            String from = slotChampion(before, i);
            String to = slotChampion(after, i);
            if (!Objects.equals(from, to)) {
                String type = from == null ? "Buy" : to == null ? "Sell" : "ShopChanged";
                events.add(new Event(type, "shop." + i + ".champion_id", from, to));
            }
        }
        return events;
    }

    private static String slotChampion(GameState state, int slot) {
        if (state.shop() == null) {
            return null;
        }
        return state.shop().stream()
                .filter(s -> s.slot() == slot)
                .map(GameState.ShopSlot::championId)
                .findFirst()
                .orElse(null);
    }

    private static void compare(String field, Object from, Object to, String type, List<Event> events) {
        if (!Objects.equals(from, to)) {
            events.add(new Event(type, field, from, to));
        }
    }
}
