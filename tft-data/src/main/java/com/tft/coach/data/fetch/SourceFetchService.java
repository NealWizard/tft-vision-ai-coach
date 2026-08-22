package com.tft.coach.data.fetch;

import com.tft.coach.data.registry.SourceAdapterRegistry;
import com.tft.coach.data.snapshot.RawSnapshot;
import com.tft.coach.data.snapshot.RawSnapshotQuery;
import com.tft.coach.data.snapshot.RawSnapshotStore;
import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.data.spi.AdapterFetchPayload;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceAdapter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates live adapter fetch with append-only snapshot persistence and cache fallback.
 */
public class SourceFetchService {

    private final SourceAdapterRegistry registry;
    private final RawSnapshotStore snapshotStore;

    public SourceFetchService(SourceAdapterRegistry registry, RawSnapshotStore snapshotStore) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.snapshotStore = Objects.requireNonNull(snapshotStore, "snapshotStore");
    }

    public FetchResult fetch(FetchRequest request) throws IOException, AdapterFetchException {
        Optional<SourceAdapter> adapter = registry.resolve(request);
        if (adapter.isEmpty()) {
            throw new AdapterFetchException("No adapter for " + request.sourceType() + "/" + request.sourceId());
        }

        try {
            AdapterFetchPayload payload = adapter.get().fetch(request);
            RawSnapshot saved = snapshotStore.append(request, payload);
            byte[] body = snapshotStore.readBody(saved);
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
                return FetchResult.fromCache(request, cached.get(), body, message);
            }
            if (ex instanceof AdapterFetchException fetchEx) {
                throw fetchEx;
            }
            throw new AdapterFetchException("Fetch and cache fallback failed", ex);
        }
    }
}
