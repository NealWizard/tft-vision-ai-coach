package com.tft.coach.data.fetch;

import com.tft.coach.data.registry.SourceAdapterRegistry;
import com.tft.coach.data.snapshot.RawSnapshot;
import com.tft.coach.data.snapshot.RawSnapshotQuery;
import com.tft.coach.data.snapshot.RawSnapshotStore;
import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.AdapterFetchPayload;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceAdapter;
import com.tft.coach.data.spi.SourceType;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Coordinates live adapter fetch with append-only snapshot persistence and cache fallback.
 */
public class SourceFetchService {

    private final SourceAdapterRegistry registry;
    private final RawSnapshotStore snapshotStore;
    private final ConcurrentMap<String, SourceHealth> healthBySource = new ConcurrentHashMap<>();

    public SourceFetchService(SourceAdapterRegistry registry, RawSnapshotStore snapshotStore) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.snapshotStore = Objects.requireNonNull(snapshotStore, "snapshotStore");
    }

    public FetchResult fetch(FetchRequest request) throws IOException, AdapterFetchException {
        Optional<SourceAdapter> adapter = registry.resolve(request);
        if (adapter.isEmpty()) {
            updateHealth(request, SourceHealthStatus.UNAVAILABLE, "adapter-not-found");
            throw new AdapterFetchException("No adapter for " + request.sourceType() + "/" + request.sourceId());
        }

        try {
            AdapterFetchPayload payload = adapter.get().fetch(request);
            RawSnapshot saved = snapshotStore.append(request, payload);
            byte[] body = snapshotStore.readBody(saved);
            updateHealth(request, SourceHealthStatus.HEALTHY, "live");
            return FetchResult.fromLive(request, saved, body);
        } catch (AdapterFetchException | IOException ex) {
            RawSnapshotQuery query = new RawSnapshotQuery(
                    request.sourceType(), request.sourceId(), request.resourceKey());
            Optional<RawSnapshot> cached = snapshotStore.findLatest(query);
            if (cached.isPresent()) {
                byte[] body = snapshotStore.readBody(cached.get());
                String message = ex instanceof AdapterFetchException
                        ? ex.getMessage()
                        : "io-error: " + ex.getMessage();
                updateHealth(request, SourceHealthStatus.DEGRADED, message);
                return FetchResult.fromCache(request, cached.get(), body, message);
            }
            updateHealth(request, SourceHealthStatus.UNAVAILABLE, ex.getMessage());
            if (ex instanceof AdapterFetchException fetchEx) {
                throw fetchEx;
            }
            throw new AdapterFetchException("Fetch and cache fallback failed", ex);
        }
    }

    public SourceHealth health(SourceType sourceType, String sourceId) {
        return healthBySource.getOrDefault(
                healthKey(sourceType, sourceId),
                new SourceHealth(sourceType, sourceId, SourceHealthStatus.UNKNOWN, Instant.now(), "not-checked"));
    }

    private void updateHealth(FetchRequest request, SourceHealthStatus status, String message) {
        SourceHealth health = new SourceHealth(
                request.sourceType(),
                request.sourceId(),
                status,
                Instant.now(),
                message == null || message.isBlank() ? status.name().toLowerCase() : message);
        healthBySource.put(healthKey(request.sourceType(), request.sourceId()), health);
    }

    private static String healthKey(SourceType sourceType, String sourceId) {
        return sourceType.wireValue() + ":" + sourceId;
    }
}
