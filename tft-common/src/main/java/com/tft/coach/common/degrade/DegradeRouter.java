package com.tft.coach.common.degrade;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Selects a local or deterministic fallback when cloud providers are unavailable
 * (`P0-FOUND-Degrade-001`).
 */
@Component
public final class DegradeRouter {

    private final Map<ProviderKind, ExecutionPath> fallbacks;

    public DegradeRouter() {
        EnumMap<ProviderKind, ExecutionPath> defaults = new EnumMap<>(ProviderKind.class);
        defaults.put(ProviderKind.LLM, ExecutionPath.DETERMINISTIC);
        defaults.put(ProviderKind.EMBEDDING, ExecutionPath.LOCAL);
        defaults.put(ProviderKind.RERANKER, ExecutionPath.DETERMINISTIC);
        defaults.put(ProviderKind.VISION, ExecutionPath.LOCAL);
        this.fallbacks = Map.copyOf(defaults);
    }

    public DegradeDecision route(
            ProviderKind provider,
            boolean cloudAllowed,
            boolean cloudAvailable,
            boolean localAvailable
    ) {
        Objects.requireNonNull(provider, "provider");
        if (cloudAllowed && cloudAvailable) {
            return new DegradeDecision(provider, ExecutionPath.CLOUD, false, "cloud-available");
        }

        ExecutionPath fallback = fallbacks.get(provider);
        if (fallback == ExecutionPath.LOCAL && !localAvailable) {
            fallback = ExecutionPath.DETERMINISTIC;
        }
        String reason = cloudAllowed ? "cloud-unavailable" : "cloud-disabled";
        return new DegradeDecision(provider, fallback, true, reason);
    }
}
