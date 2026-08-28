package com.tft.coach.vision.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * One ROI region inside a VisionProfile.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RoiRegion(
        String id,
        RoiType type,
        @JsonProperty("coordinate_system") CoordinateSystem coordinateSystem,
        double x,
        double y,
        double width,
        double height,
        @JsonProperty("expected_field") String expectedField,
        Map<String, Object> preprocess
) {
    public RoiRegion {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(coordinateSystem, "coordinateSystem");
    }
}
