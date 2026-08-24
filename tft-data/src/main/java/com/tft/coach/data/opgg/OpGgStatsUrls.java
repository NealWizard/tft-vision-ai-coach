package com.tft.coach.data.opgg;

/**
 * URL templates for OP.GG TFT stats (configurable base for regional endpoints).
 */
public final class OpGgStatsUrls {

    public static final String DEFAULT_BASE = "https://tft.op.gg";

    private OpGgStatsUrls() {}

    /**
     * Normalized meta bundle endpoint used by the adapter.
     * Live wiring may evolve; raw responses are always stored in snapshot history.
     */
    public static String metaBundle(String base, String region, String patch, String timeWindow) {
        return base + "/api/meta/bundle?region=" + region + "&patch=" + patch + "&window=" + timeWindow;
    }
}
