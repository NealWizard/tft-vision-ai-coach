package com.tft.coach.vision.frame;

import java.util.Optional;

/**
 * SPI for reading vision frames. Callers must not care about capture backend.
 */
public interface FrameSource extends AutoCloseable {

    Optional<VisionFrame> nextFrame();

    FrameSourceMetadata metadata();

    @Override
    void close();
}
