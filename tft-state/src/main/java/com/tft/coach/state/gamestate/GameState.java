package com.tft.coach.state.gamestate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Canonical GameState (schema 1.0.0).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GameState(
        @JsonProperty("schema_version") String schemaVersion,
        @JsonProperty("match_id") String matchId,
        String patch,
        String stage,
        @JsonProperty("observed_at") Instant observedAt,
        Player player,
        List<ShopSlot> shop,
        List<BoardUnit> board,
        List<BoardUnit> bench,
        List<String> items,
        List<TraitCount> traits,
        List<String> augments,
        List<String> mechanics,
        Confidence confidence
) {
    public GameState {
        Objects.requireNonNull(schemaVersion, "schemaVersion");
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(patch, "patch");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(observedAt, "observedAt");
        Objects.requireNonNull(player, "player");
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Player(int level, Integer xp, int gold, int hp, Integer streak) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ShopSlot(int slot, @JsonProperty("champion_id") String championId, Integer cost) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BoardUnit(
            @JsonProperty("champion_id") String championId,
            Integer star,
            Integer row,
            Integer col
    ) {
    }

    public record TraitCount(String id, Integer count) {
    }

    public record Confidence(double score, String level) {
    }

    public static ObjectMapper mapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public JsonNode toJsonNode() {
        return mapper().valueToTree(this);
    }
}
