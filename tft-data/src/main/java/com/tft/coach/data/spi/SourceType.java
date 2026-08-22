package com.tft.coach.data.spi;

/**
 * Aligns with {@code evidence.schema.json} {@code source_type}.
 */
public enum SourceType {
    RIOT("riot"),
    STATS("stats"),
    COMMUNITY("community"),
    VISION("vision"),
    MANUAL("manual"),
    DERIVED("derived");

    private final String wireValue;

    SourceType(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static SourceType fromWire(String wireValue) {
        for (SourceType type : values()) {
            if (type.wireValue.equals(wireValue)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown source type: " + wireValue);
    }
}
