package com.tft.coach.data.meta;

import java.time.Instant;
import java.util.List;

/**
 * Unified meta statistics snapshot from an external stats source (e.g. OP.GG).
 */
public record MetaSnapshot(
        String sourceId,
        String patch,
        String region,
        String timeWindow,
        Instant capturedAt,
        long sampleSize,
        List<CompStat> comps,
        List<UnitStat> units,
        List<ItemStat> items,
        List<AugmentStat> augments
) {
    public MetaSnapshot {
        comps = comps == null ? List.of() : List.copyOf(comps);
        units = units == null ? List.of() : List.copyOf(units);
        items = items == null ? List.of() : List.copyOf(items);
        augments = augments == null ? List.of() : List.copyOf(augments);
    }
}
