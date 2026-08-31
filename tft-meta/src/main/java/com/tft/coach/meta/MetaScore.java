package com.tft.coach.meta;

/**
 * Explainable snapshot score: Reliability × SampleSize × Freshness × PatchMatch.
 */
public record MetaScore(
        double total,
        double reliability,
        double sampleSize,
        double freshness,
        double patchMatch,
        String formula
) {
}
