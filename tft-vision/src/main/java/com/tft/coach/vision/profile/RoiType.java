package com.tft.coach.vision.profile;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ROI content kind.
 */
public enum RoiType {
    @JsonProperty("TEXT")
    TEXT,
    @JsonProperty("ICON")
    ICON,
    @JsonProperty("AREA")
    AREA
}
