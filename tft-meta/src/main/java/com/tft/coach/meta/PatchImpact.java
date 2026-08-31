package com.tft.coach.meta;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PatchImpact(
        @JsonProperty("from_patch") String fromPatch,
        @JsonProperty("to_patch") String toPatch,
        @JsonProperty("score_delta") double scoreDelta,
        boolean degraded,
        List<String> evidence,
        String reason
) {
    public PatchImpact {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
