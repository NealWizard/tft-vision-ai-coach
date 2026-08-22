package com.tft.coach.data.spi;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable request passed to a {@link SourceAdapter}.
 */
public record FetchRequest(
        SourceType sourceType,
        String sourceId,
        String resourceKey,
        String sourceUrl,
        String patch,
        Map<String, String> params
) {
    public FetchRequest {
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(resourceKey, "resourceKey");
        params = params == null ? Map.of() : Map.copyOf(params);
    }
}
