package com.tft.coach.data.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tft.coach.data.spi.AdapterFetchPayload;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * File-based append-only snapshot store under {@code {root}/{sourceType}/{sourceId}/{snapshotId}/}.
 */
public class FileSystemRawSnapshotStore implements RawSnapshotStore {

    static final String META_FILE = "meta.json";
    static final String BODY_FILE = "body.bin";

    private final Path root;
    private final ObjectMapper mapper;
    private Instant lastStoredAt = Instant.EPOCH;

    public FileSystemRawSnapshotStore(Path root) {
        this.root = root;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public synchronized RawSnapshot append(FetchRequest request, AdapterFetchPayload payload) throws IOException {
        String snapshotId = UUID.randomUUID().toString();
        Path dir = root
                .resolve(request.sourceType().wireValue())
                .resolve(sanitize(request.sourceId()))
                .resolve(sanitize(request.resourceKey()))
                .resolve(snapshotId);
        Files.createDirectories(dir);

        Path bodyPath = dir.resolve(BODY_FILE);
        Files.write(bodyPath, payload.body());

        RawSnapshot snapshot = new RawSnapshot(
                snapshotId,
                request.sourceType(),
                request.sourceId(),
                request.resourceKey(),
                request.sourceUrl(),
                payload.capturedAt(),
                payload.patch(),
                payload.contentType(),
                bodyPath.toString(),
                payload.body().length,
                nextStoredAt(),
                sha256(payload.body())
        );
        mapper.writeValue(dir.resolve(META_FILE).toFile(), snapshot);
        return snapshot;
    }

    @Override
    public Optional<RawSnapshot> findLatest(RawSnapshotQuery query) throws IOException {
        return findByQuery(query).stream()
                .max(snapshotOrder());
    }

    @Override
    public List<RawSnapshot> findByQuery(RawSnapshotQuery query) throws IOException {
        Path base = root
                .resolve(query.sourceType().wireValue())
                .resolve(sanitize(query.sourceId()))
                .resolve(sanitize(query.resourceKey()));
        if (!Files.isDirectory(base)) {
            return List.of();
        }

        List<RawSnapshot> results = new ArrayList<>();
        try (Stream<Path> snapshots = Files.list(base)) {
            for (Path snapshotDir : snapshots.filter(Files::isDirectory).toList()) {
                Path meta = snapshotDir.resolve(META_FILE);
                if (!Files.isRegularFile(meta)) {
                    continue;
                }
                RawSnapshot snapshot = mapper.readValue(meta.toFile(), RawSnapshot.class);
                if (!inWindow(snapshot.capturedAt(), query.fromInclusive(), query.toExclusive())) {
                    continue;
                }
                results.add(snapshot);
            }
        }
        results.sort(snapshotOrder());
        return List.copyOf(results);
    }

    @Override
    public byte[] readBody(RawSnapshot snapshot) throws IOException {
        return Files.readAllBytes(Path.of(snapshot.bodyPath()));
    }

    private static boolean inWindow(Instant capturedAt, Instant from, Instant to) {
        if (from != null && capturedAt.isBefore(from)) {
            return false;
        }
        if (to != null && !capturedAt.isBefore(to)) {
            return false;
        }
        return true;
    }

    private static String sanitize(String segment) {
        return segment.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Instant nextStoredAt() {
        Instant now = Instant.now();
        lastStoredAt = now.isAfter(lastStoredAt) ? now : lastStoredAt.plusNanos(1);
        return lastStoredAt;
    }

    private static Comparator<RawSnapshot> snapshotOrder() {
        return Comparator.comparing(RawSnapshot::capturedAt)
                .thenComparing(RawSnapshot::storedAt)
                .thenComparing(RawSnapshot::snapshotId);
    }

    private static String sha256(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
