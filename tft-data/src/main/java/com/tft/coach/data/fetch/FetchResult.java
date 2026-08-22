package com.tft.coach.data.fetch;

import com.tft.coach.data.snapshot.RawSnapshot;
import com.tft.coach.data.spi.FetchRequest;
import com.tft.coach.data.spi.SourceType;

import java.time.Instant;
import java.util.Objects;

/**
 * Unified fetch outcome with optional snapshot fallback metadata.
 */
public record FetchResult(
        FetchRequest request,
        SourceType sourceType,
        String sourceId,
        String resourceKey,
        String patch,
        Instant capturedAt,
        byte[] body,
        String contentType,
        String payloadRef,
        boolean live,
        boolean degraded,
        String message
) {
    public FetchResult {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(capturedAt, "capturedAt");
    }

    public static FetchResult fromLive(FetchRequest request, RawSnapshot snapshot, byte[] body) {
        return new FetchResult(
                request,
                snapshot.sourceType(),
                snapshot.sourceId(),
                snapshot.resourceKey(),
                snapshot.patch(),
                snapshot.capturedAt(),
                body,
                snapshot.contentType(),
                snapshot.payloadRef(),
                true,
                false,
                "live"
        );
    }

    public static FetchResult fromCache(FetchRequest request, RawSnapshot snapshot, byte[] body, String message) {
        return new FetchResult(
                request,
                snapshot.sourceType(),
                snapshot.sourceId(),
                snapshot.resourceKey(),
                snapshot.patch(),
                snapshot.capturedAt(),
                body,
                snapshot.contentType(),
                snapshot.payloadRef(),
                false,
                true,
                message
        );
    }
}
