package com.tft.coach.meta;

/**
 * Query dimensions for Meta Snapshot (`P3-META-Snapshot-001`).
 * rank/queue are optional; missing data is degraded, not invented.
 */
public record MetaQuery(String patch, String region, String timeWindow, String rank, String queue) {

    public static MetaQuery of(String patch, String region, String timeWindow) {
        return new MetaQuery(patch, region, timeWindow, null, null);
    }

    public MetaQuery {
        if (patch == null || patch.isBlank()) {
            throw new IllegalArgumentException("patch is required");
        }
        region = region == null || region.isBlank() ? "global" : region;
        timeWindow = timeWindow == null || timeWindow.isBlank() ? "24h" : timeWindow;
    }
}
