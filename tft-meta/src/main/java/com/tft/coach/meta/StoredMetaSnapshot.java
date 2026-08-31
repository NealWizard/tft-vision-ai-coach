package com.tft.coach.meta;

import com.tft.coach.data.meta.MetaSnapshot;

/**
 * Immutable stored snapshot. Historical rows are never overwritten.
 */
public record StoredMetaSnapshot(String id, MetaSnapshot snapshot) {
}
