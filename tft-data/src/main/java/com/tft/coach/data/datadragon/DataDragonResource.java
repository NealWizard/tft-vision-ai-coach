package com.tft.coach.data.datadragon;

/**
 * Supported TFT static JSON resources on Data Dragon CDN.
 */
public enum DataDragonResource {
    CHAMPION("tft-champion"),
    TRAIT("tft-trait"),
    ITEM("tft-item"),
    AUGMENT("tft-augments");

    private final String resourceKey;

    DataDragonResource(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public String resourceKey() {
        return resourceKey;
    }

    public String fileName() {
        return resourceKey + ".json";
    }

    public static DataDragonResource fromResourceKey(String key) {
        for (DataDragonResource resource : values()) {
            if (resource.resourceKey.equals(key)) {
                return resource;
            }
        }
        throw new IllegalArgumentException("Unknown Data Dragon resource: " + key);
    }
}
