package com.tft.coach.knowledge.llm;

import java.util.Map;

/** Adapts existing LlmProvider SPI. Domain agents never import vendor SDKs. */
public final class LlmChatModelAdapter implements ChatModelProvider {

    private final LlmProvider inner;

    public LlmChatModelAdapter(LlmProvider inner) {
        this.inner = inner;
    }

    @Override
    public String providerId() {
        return inner.providerId();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        LlmRequest llmRequest = new LlmRequest(
                "decision.reason",
                "1.0.0",
                Map.of(
                        "question", request.user(),
                        "system", request.system(),
                        "facts", request.variables().getOrDefault("facts", ""),
                        "candidates", request.variables().getOrDefault("candidates", "[]")),
                256);
        LlmResponse response = inner.complete(llmRequest);
        return new ChatResponse(response.content(), response.providerId(), response.degraded());
    }
}
