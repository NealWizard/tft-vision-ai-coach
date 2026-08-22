package com.tft.coach.data.meta;

import java.util.List;

/**
 * Result of querying multiple stats sources with the same parameters.
 */
public record MultiSourceMetaResult(
        MetaSnapshotQuery query,
        List<MetaSnapshotOutcome> outcomes
) {
    public MultiSourceMetaResult {
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
    }
}
