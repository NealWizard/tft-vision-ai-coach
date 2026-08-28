package com.tft.coach.vision.frame;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One captured or loaded visual frame.
 */
public record VisionFrame(
        String frameId,
        Instant capturedAt,
        Long sourceTimestampMs,
        Instant ingestedAt,
        int width,
        int height,
        FrameSourceType sourceType,
        String profileHint,
        FramePayload payload
) {
    public VisionFrame {
        Objects.requireNonNull(frameId, "frameId");
        Objects.requireNonNull(capturedAt, "capturedAt");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(payload, "payload");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width/height must be positive");
        }
    }

    public Optional<Long> sourceTimestampMsOptional() {
        return Optional.ofNullable(sourceTimestampMs);
    }

    public Optional<Instant> ingestedAtOptional() {
        return Optional.ofNullable(ingestedAt);
    }

    public Optional<String> profileHintOptional() {
        return Optional.ofNullable(profileHint);
    }
}
