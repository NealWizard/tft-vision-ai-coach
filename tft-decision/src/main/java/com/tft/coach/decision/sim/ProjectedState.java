package com.tft.coach.decision.sim;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectedState(
        Integer gold,
        Integer interest,
        Integer xp,
        Integer level,
        @JsonProperty("roll_count") Integer rollCount,
        @JsonProperty("estimated_shop_odds") Map<String, Double> estimatedShopOdds,
        boolean degraded,
        List<String> evidence
) {
    public ProjectedState {
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        estimatedShopOdds = estimatedShopOdds == null ? null : Map.copyOf(estimatedShopOdds);
    }
}
