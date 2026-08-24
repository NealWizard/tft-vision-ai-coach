package com.tft.coach.knowledge.llm;

/** Stub cloud provider for tests and offline bootstrap. */
public final class StubCloudLlmProvider implements LlmProvider {

    @Override
    public String providerId() {
        return "cloud-stub";
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        return new LlmResponse(
                "cloud:" + request.variables().getOrDefault("question", ""),
                providerId(),
                "1.0.0",
                10,
                20,
                50,
                false);
    }
}
