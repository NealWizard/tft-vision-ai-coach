package com.tft.coach.knowledge.llm;

import com.tft.coach.common.degrade.DegradeRouter;
import com.tft.coach.common.degrade.ExecutionPath;
import com.tft.coach.common.degrade.ProviderKind;

import java.util.Objects;

/** Routes cloud/local/deterministic LLM execution (`P1-LLM-Gateway-001`). */
public final class CloudLlmGateway {

    private final DegradeRouter degradeRouter;
    private final LlmProvider cloudProvider;
    private final LlmProvider localProvider;
    private final LlmProvider deterministicProvider;
    private final LlmUsageMeter meter;
    private final LlmSafetyGuard guard;

    public CloudLlmGateway(
            DegradeRouter degradeRouter,
            LlmProvider cloudProvider,
            LlmProvider localProvider,
            LlmProvider deterministicProvider,
            LlmUsageMeter meter,
            LlmSafetyGuard guard
    ) {
        this.degradeRouter = Objects.requireNonNull(degradeRouter, "degradeRouter");
        this.cloudProvider = Objects.requireNonNull(cloudProvider, "cloudProvider");
        this.localProvider = Objects.requireNonNull(localProvider, "localProvider");
        this.deterministicProvider = Objects.requireNonNull(deterministicProvider, "deterministicProvider");
        this.meter = Objects.requireNonNull(meter, "meter");
        this.guard = Objects.requireNonNull(guard, "guard");
    }

    public LlmResponse complete(LlmRequest request, boolean cloudAllowed, boolean cloudAvailable) {
        guard.validate(request);
        var decision = degradeRouter.route(ProviderKind.LLM, cloudAllowed, cloudAvailable, false);
        LlmProvider provider = switch (decision.path()) {
            case CLOUD -> cloudProvider;
            case LOCAL -> localProvider;
            case DETERMINISTIC -> deterministicProvider;
        };
        LlmResponse response = provider.complete(request);
        meter.record(response);
        if (decision.degraded()) {
            return new LlmResponse(
                    response.content(),
                    response.providerId(),
                    response.modelVersion(),
                    response.promptTokens(),
                    response.completionTokens(),
                    response.latencyMs(),
                    true);
        }
        return response;
    }
}
