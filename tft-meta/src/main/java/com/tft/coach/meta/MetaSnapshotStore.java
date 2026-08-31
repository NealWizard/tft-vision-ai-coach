package com.tft.coach.meta;

import java.util.List;
import java.util.Optional;

public interface MetaSnapshotStore {

    StoredMetaSnapshot save(StoredMetaSnapshot snapshot);

    Optional<StoredMetaSnapshot> findById(String id);

    Optional<StoredMetaSnapshot> findLatest(MetaQuery query);

    List<StoredMetaSnapshot> findAll(String patch, String region);
}
