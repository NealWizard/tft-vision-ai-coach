package com.tft.coach.data.meta;

import java.util.List;

public record CompStat(
        String compId,
        String name,
        String tier,
        double avgPlace,
        double firstRate,
        double top4Rate,
        double pickRate,
        long sampleSize,
        List<String> coreUnits
) {
    public CompStat {
        coreUnits = coreUnits == null ? List.of() : List.copyOf(coreUnits);
    }
}
