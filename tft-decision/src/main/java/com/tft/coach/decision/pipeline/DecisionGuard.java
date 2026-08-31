package com.tft.coach.decision.pipeline;

import com.tft.coach.state.gamestate.GameState;

/** INV-004 / INV-005. */
public final class DecisionGuard {

    private DecisionGuard() {}

    public static void requireGameState(GameState state) {
        if (state == null) {
            throw new MissingGameStateException("No GameState; refusing board decision");
        }
        if (state.patch() == null || state.patch().isBlank()) {
            throw new MissingPatchException("No patch; refusing current Meta");
        }
    }

    public static class MissingGameStateException extends IllegalArgumentException {
        public MissingGameStateException(String message) {
            super(message);
        }
    }

    public static class MissingPatchException extends IllegalArgumentException {
        public MissingPatchException(String message) {
            super(message);
        }
    }
}
