package com.tft.coach.common.degrade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DegradeRouterTest {

    private final DegradeRouter router = new DegradeRouter();

    @Test
    void usesCloudWhenAllowedAndAvailable() {
        DegradeDecision decision = router.route(ProviderKind.LLM, true, true, false);

        assertEquals(ExecutionPath.CLOUD, decision.path());
        assertFalse(decision.degraded());
    }

    @Test
    void cloudFailureUsesProviderFallbackMatrix() {
        assertEquals(ExecutionPath.DETERMINISTIC,
                router.route(ProviderKind.LLM, true, false, false).path());
        assertEquals(ExecutionPath.LOCAL,
                router.route(ProviderKind.EMBEDDING, true, false, true).path());
        assertEquals(ExecutionPath.DETERMINISTIC,
                router.route(ProviderKind.RERANKER, true, false, false).path());
        assertEquals(ExecutionPath.LOCAL,
                router.route(ProviderKind.VISION, true, false, true).path());
    }

    @Test
    void missingLocalProviderFallsBackToDeterministicPath() {
        DegradeDecision decision = router.route(ProviderKind.VISION, false, false, false);

        assertEquals(ExecutionPath.DETERMINISTIC, decision.path());
        assertTrue(decision.degraded());
        assertEquals("cloud-disabled", decision.reason());
    }
}
