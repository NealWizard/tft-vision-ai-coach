package com.tft.coach.vision.profile;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ROI coordinate space.
 */
public enum CoordinateSystem {
    @JsonProperty("SCREEN")
    SCREEN,
    @JsonProperty("NORMALIZED")
    NORMALIZED
}
