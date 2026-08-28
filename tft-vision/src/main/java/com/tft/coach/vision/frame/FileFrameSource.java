package com.tft.coach.vision.frame;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads a single image file once.
 */
public final class FileFrameSource implements FrameSource {

    private final Path path;
    private final FrameSourceMetadata metadata;
    private final AtomicBoolean consumed = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public FileFrameSource(Path path) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.metadata = new FrameSourceMetadata(
                "file:" + this.path.getFileName(),
                FrameSourceType.SCREENSHOT,
                "Single image file: " + this.path
        );
    }

    @Override
    public Optional<VisionFrame> nextFrame() {
        ensureOpen();
        if (!consumed.compareAndSet(false, true)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            Dimensions dim = readDimensions(bytes);
            Instant now = Instant.now();
            return Optional.of(new VisionFrame(
                    UUID.randomUUID().toString(),
                    now,
                    null,
                    now,
                    dim.width(),
                    dim.height(),
                    FrameSourceType.SCREENSHOT,
                    null,
                    new FramePayload.LocalFile(path)
            ));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read frame file: " + path, e);
        }
    }

    @Override
    public FrameSourceMetadata metadata() {
        return metadata;
    }

    @Override
    public void close() {
        closed.set(true);
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("FrameSource already closed");
        }
    }

    static Dimensions readDimensions(byte[] bytes) throws IOException {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("Unsupported or corrupt image");
            }
            return new Dimensions(image.getWidth(), image.getHeight());
        }
    }

    record Dimensions(int width, int height) {
    }
}
