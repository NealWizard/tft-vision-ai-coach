package com.tft.coach.data.meta;

/**
 * Parameters shared across stats sources for cross-validation.
 */
public record MetaSnapshotQuery(
        String patch,
        String region,
        String timeWindow
) {
    public static MetaSnapshotQuery of(String patch, String region, String timeWindow) {
        return new MetaSnapshotQuery(patch, region, timeWindow);
    }
}
