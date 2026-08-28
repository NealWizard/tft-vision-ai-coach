package com.tft.coach.vision.frame;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Image payload for a frame. Prefer LocalFile/SharedFile for large assets.
 */
public sealed interface FramePayload permits FramePayload.InlineBytes, FramePayload.LocalFile, FramePayload.SharedFile {

    record InlineBytes(byte[] bytes) implements FramePayload {
        public InlineBytes {
            Objects.requireNonNull(bytes, "bytes");
        }
    }

    record LocalFile(Path path) implements FramePayload {
        public LocalFile {
            Objects.requireNonNull(path, "path");
        }
    }

    record SharedFile(Path path) implements FramePayload {
        public SharedFile {
            Objects.requireNonNull(path, "path");
        }
    }
}
