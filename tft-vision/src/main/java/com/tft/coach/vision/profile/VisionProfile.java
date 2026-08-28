package com.tft.coach.vision.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Objects;

/**
 * Versioned vision layout profile keyed primarily by layout_version + resolution.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VisionProfile(
        @JsonProperty("profile_id") String profileId,
        @JsonProperty("layout_version") String layoutVersion,
        Resolution resolution,
        @JsonProperty("ui_scale") String uiScale,
        String language,
        @JsonProperty("client_version") String clientVersion,
        @JsonProperty("patch_hint") String patchHint,
        Map<String, RoiRegion> regions
) {
    public VisionProfile {
        Objects.requireNonNull(profileId, "profileId");
        Objects.requireNonNull(layoutVersion, "layoutVersion");
        Objects.requireNonNull(resolution, "resolution");
        Objects.requireNonNull(regions, "regions");
        regions = Map.copyOf(regions);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resolution(int width, int height) {
        public Resolution {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("resolution must be positive");
            }
        }
    }
}
