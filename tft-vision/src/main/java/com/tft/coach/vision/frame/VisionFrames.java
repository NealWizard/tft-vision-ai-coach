package com.tft.coach.vision.frame;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds a VisionFrame from PNG/JPEG bytes.
 */
public final class VisionFrames {

    private VisionFrames() {
    }

    public static VisionFrame fromImageBytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        try {
            FileFrameSource.Dimensions dim = FileFrameSource.readDimensions(bytes);
            Instant now = Instant.now();
            return new VisionFrame(
                    UUID.randomUUID().toString(),
                    now,
                    null,
                    now,
                    dim.width(),
                    dim.height(),
                    FrameSourceType.SCREENSHOT,
                    null,
                    new FramePayload.InlineBytes(bytes)
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("INVALID_IMAGE", e);
        }
    }
}
