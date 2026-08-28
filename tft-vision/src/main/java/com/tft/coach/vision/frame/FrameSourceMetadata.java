package com.tft.coach.vision.frame;

import java.util.Objects;

/**
 * Static metadata about a FrameSource implementation.
 */
public record FrameSourceMetadata(
        String sourceId,
        FrameSourceType sourceType,
        String description
) {
    public FrameSourceMetadata {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(description, "description");
    }
}
