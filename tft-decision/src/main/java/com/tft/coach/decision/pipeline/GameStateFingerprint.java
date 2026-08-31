package com.tft.coach.decision.pipeline;

import com.tft.coach.state.gamestate.GameState;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class GameStateFingerprint {

    private GameStateFingerprint() {}

    public static String sha256(GameState state) {
        try {
            byte[] json = GameState.mapper().writeValueAsBytes(state);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to fingerprint GameState", ex);
        }
    }
}
