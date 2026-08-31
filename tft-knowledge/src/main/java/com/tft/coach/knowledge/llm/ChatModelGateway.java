package com.tft.coach.knowledge.llm;

import java.util.Objects;

/**
 * P3-D gateway. Business agents depend on this type only.
 */
public final class ChatModelGateway {

    private final ChatModelProvider provider;
    private final LlmSafetyGuard guard;

    public ChatModelGateway(ChatModelProvider provider, LlmSafetyGuard guard) {
        this.provider = Objects.requireNonNull(provider);
        this.guard = Objects.requireNonNull(guard);
    }

    public ChatResponse chat(ChatRequest request) {
        LlmRequest probe = new LlmRequest(
                "decision.reason",
                "1.0.0",
                request.variables(),
                256);
        guard.validate(probe);
        return provider.chat(request);
    }

    public String providerId() {
        return provider.providerId();
    }

    public static ChatModelGateway mock() {
        return new ChatModelGateway(new MockChatModelProvider(), new LlmSafetyGuard());
    }

    public static ChatModelGateway openAiCompatible(String baseUrl, String apiKey, String modelId, String providerId) {
        return new ChatModelGateway(
                new LlmChatModelAdapter(new OpenAiCompatibleLlmProvider(baseUrl, apiKey, modelId, providerId)),
                new LlmSafetyGuard());
    }

    public static ChatModelGateway deepSeek(String baseUrl, String apiKey, String modelId) {
        return openAiCompatible(baseUrl, apiKey, modelId, "deepseek");
    }

    public static ChatModelGateway local(String baseUrl, String apiKey, String modelId) {
        return openAiCompatible(baseUrl, apiKey, modelId, "local");
    }
}
