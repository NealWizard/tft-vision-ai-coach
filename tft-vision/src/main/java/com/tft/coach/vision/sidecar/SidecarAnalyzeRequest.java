package com.tft.coach.vision.sidecar;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for POST /vision/analyze.
 */
public record SidecarAnalyzeRequest(
        @JsonProperty("request_id") String requestId,
        String field,
        @JsonProperty("image_base64") String imageBase64
) {
}
