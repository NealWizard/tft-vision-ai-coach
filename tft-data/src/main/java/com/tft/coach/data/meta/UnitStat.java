package com.tft.coach.data.meta;

import java.util.List;

public record UnitStat(
        String unitId,
        String name,
        double winRate,
        double pickRate,
        double avgPlace,
        long sampleSize,
        List<String> topItems
) {
    public UnitStat {
        topItems = topItems == null ? List.of() : List.copyOf(topItems);
    }
}
