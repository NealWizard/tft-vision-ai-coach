package com.tft.coach.data.opgg;

/**
 * OP.GG stats resources captured as raw JSON snapshots.
 */
public enum OpGgStatsResource {
    /** Full MetaSnapshot bundle: comps + units + items + augments */
    META_BUNDLE("meta-bundle"),
    META_COMPS("meta-comps"),
    META_UNITS("meta-units"),
    META_ITEMS("meta-items"),
    META_AUGMENTS("meta-augments");

    private final String resourceKey;

    OpGgStatsResource(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public String resourceKey() {
        return resourceKey;
    }

    public static OpGgStatsResource fromResourceKey(String key) {
        for (OpGgStatsResource resource : values()) {
            if (resource.resourceKey.equals(key)) {
                return resource;
            }
        }
        throw new IllegalArgumentException("Unknown OP.GG stats resource: " + key);
    }
}
