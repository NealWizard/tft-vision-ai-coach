package com.tft.coach.vision.frame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Replays image files in a directory by file name order.
 */
public final class DirectoryFrameSource implements FrameSource {

    private static final List<String> IMAGE_SUFFIXES = List.of(".png", ".jpg", ".jpeg", ".bmp", ".webp");

    private final Path directory;
    private final List<Path> files;
    private final FrameSourceMetadata metadata;
    private final AtomicInteger index = new AtomicInteger(0);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public DirectoryFrameSource(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
        if (!Files.isDirectory(this.directory)) {
            throw new IllegalArgumentException("Not a directory: " + this.directory);
        }
        this.files = listImages(this.directory);
        this.metadata = new FrameSourceMetadata(
                "dir:" + this.directory.getFileName(),
                FrameSourceType.SCREENSHOT,
                "Directory replay (" + files.size() + " files): " + this.directory
        );
    }

    @Override
    public Optional<VisionFrame> nextFrame() {
        ensureOpen();
        int i = index.getAndIncrement();
        if (i >= files.size()) {
            return Optional.empty();
        }
        Path path = files.get(i);
        try {
            byte[] bytes = Files.readAllBytes(path);
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

    private static List<Path> listImages(Path directory) {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return IMAGE_SUFFIXES.stream().anyMatch(name::endsWith);
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list directory: " + directory, e);
        }
    }
}
