package com.tft.coach.data.snapshot;

import com.tft.coach.data.spi.AdapterFetchPayload;
import com.tft.coach.data.spi.FetchRequest;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Append-only raw response storage. Repeated fetches create new records; history is never overwritten.
 */
public interface RawSnapshotStore {

    RawSnapshot append(FetchRequest request, AdapterFetchPayload payload) throws IOException;

    Optional<RawSnapshot> findLatest(RawSnapshotQuery query) throws IOException;

    List<RawSnapshot> findByQuery(RawSnapshotQuery query) throws IOException;

    byte[] readBody(RawSnapshot snapshot) throws IOException;
}
