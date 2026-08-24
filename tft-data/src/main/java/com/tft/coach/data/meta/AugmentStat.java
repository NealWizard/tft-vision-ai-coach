package com.tft.coach.data.meta;

public record AugmentStat(
        String augmentId,
        String name,
        double winRate,
        double pickRate,
        double avgPlace,
        long sampleSize
) {}
