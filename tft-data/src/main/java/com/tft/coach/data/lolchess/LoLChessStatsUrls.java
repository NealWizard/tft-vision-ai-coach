package com.tft.coach.data.lolchess;

public final class LoLChessStatsUrls {

    public static final String DEFAULT_BASE = "https://lolchess.gg";

    private LoLChessStatsUrls() {}

    public static String metaBundle(String base, String region, String patch, String timeWindow) {
        return base + "/api/meta/bundle?region=" + region + "&patch=" + patch + "&window=" + timeWindow;
    }
}
