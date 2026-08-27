package com.tft.coach.vision.frame;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loads a classpath fixture image once (zero external filesystem dependency in tests).
 */
public final class FixtureFrameSource implements FrameSource {

    private final String classpathResource;
    private final FrameSourceMetadata metadata;
    private final AtomicBoolean consumed = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public FixtureFrameSource(String classpathResource) {
        this.classpathResource = Objects.requireNonNull(classpathResource, "classpathResource");
        this.metadata = new FrameSourceMetadata(
                "fixture:" + classpathResource,
                FrameSourceType.SCREENSHOT,
                "Classpath fixture: " + classpathResource
        );
    }

    public static FixtureFrameSource defaultFixture() {
        return new FixtureFrameSource("/vision/fixtures/1x1.png");
    }

    @Override
    public Optional<VisionFrame> nextFrame() {
        ensureOpen();
        if (!consumed.compareAndSet(false, true)) {
            return Optional.empty();
        }
        try (InputStream in = FixtureFrameSource.class.getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException("Fixture not found: " + classpathResource);
            }
            byte[] bytes = in.readAllBytes();
            FileFrameSource.Dimensions dim = FileFrameSource.readDimensions(bytes);
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
                    new FramePayload.InlineBytes(bytes)
            ));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read fixture: " + classpathResource, e);
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
}
