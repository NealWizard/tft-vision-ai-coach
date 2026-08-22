package com.tft.coach.data.lolchess;

/**
 * LoLChess.gg stats resources (normalized JSON, same shape as OP.GG bundle).
 */
public enum LoLChessStatsResource {
    META_BUNDLE("meta-bundle");

    private final String resourceKey;

    LoLChessStatsResource(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public String resourceKey() {
        return resourceKey;
    }

    public static LoLChessStatsResource fromResourceKey(String key) {
        for (LoLChessStatsResource resource : values()) {
            if (resource.resourceKey.equals(key)) {
                return resource;
            }
        }
        throw new IllegalArgumentException("Unknown LoLChess stats resource: " + key);
    }
}
