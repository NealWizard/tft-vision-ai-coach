package com.tft.coach.data.meta;

import com.tft.coach.data.evidence.FetchEvidence;
import com.tft.coach.data.fetch.FetchResult;

/**
 * One stats source outcome for a unified meta query.
 */
public record MetaSnapshotOutcome(
        String sourceId,
        FetchResult fetchResult,
        MetaSnapshot snapshot,
        FetchEvidence evidence
) {}
