package com.tft.coach.decision.contest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tft.coach.decision.candidate.CandidateSet;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContestSnapshot(
        @JsonProperty("unit_id") String unitId,
        @JsonProperty("known_holders") Integer knownHolders,
        @JsonProperty("contested_level") double contestedLevel,
        String source,
        CandidateSet.Confidence confidence,
        @JsonProperty("captured_at") Instant capturedAt
) {
}
