package com.tft.coach.meta;

import java.util.List;

public record MetaTrend(
        String patch,
        String fromWindow,
        String toWindow,
        List<CompTrend> comps,
        List<String> evidence
) {
    public MetaTrend {
        comps = comps == null ? List.of() : List.copyOf(comps);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public record CompTrend(String compId, double pickRateDelta, double top4RateDelta) {
    }
}
