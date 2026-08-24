package com.tft.coach.common.degrade;

import java.util.Objects;

/** Auditable provider routing decision. */
public record DegradeDecision(
        ProviderKind provider,
        ExecutionPath path,
        boolean degraded,
        String reason
) {
    public DegradeDecision {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(reason, "reason");
    }
}
