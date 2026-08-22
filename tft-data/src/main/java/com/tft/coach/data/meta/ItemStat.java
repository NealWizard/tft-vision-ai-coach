package com.tft.coach.data.meta;

public record ItemStat(
        String itemId,
        String name,
        double winRate,
        double pickRate,
        long sampleSize
) {}
