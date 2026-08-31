package com.tft.coach.state.timeline;

import com.tft.coach.state.diff.GameStateDiff;
import com.tft.coach.state.gamestate.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Ordered GameStates plus adjacent diffs.
 */
public final class MatchTimeline {

    private MatchTimeline() {
    }

    public record Snapshot(GameState state, List<GameStateDiff.Event> eventsFromPrevious) {
    }

    public record Timeline(String matchId, List<Snapshot> snapshots) {
    }

    public static Timeline fromStates(List<GameState> states) {
        Objects.requireNonNull(states, "states");
        if (states.isEmpty()) {
            throw new IllegalArgumentException("states must not be empty");
        }
        List<Snapshot> snapshots = new ArrayList<>();
        snapshots.add(new Snapshot(states.get(0), List.of()));
        for (int i = 1; i < states.size(); i++) {
            snapshots.add(new Snapshot(states.get(i), GameStateDiff.diff(states.get(i - 1), states.get(i))));
        }
        return new Timeline(states.get(0).matchId(), List.copyOf(snapshots));
    }
}
