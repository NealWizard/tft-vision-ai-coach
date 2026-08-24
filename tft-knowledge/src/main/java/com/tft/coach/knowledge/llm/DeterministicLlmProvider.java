package com.tft.coach.knowledge.llm;

import java.util.Map;

/** Deterministic local fallback when cloud LLM is unavailable. */
public final class DeterministicLlmProvider implements LlmProvider {

    @Override
    public String providerId() {
        return "deterministic-local";
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        long started = System.nanoTime();
        String question = request.variables().getOrDefault("question", "");
        String content = "Tool-backed answer only; deterministic fallback for: " + question;
        long latencyMs = (System.nanoTime() - started) / 1_000_000;
        return new LlmResponse(content, providerId(), "1.0.0", 0, content.length(), latencyMs, true);
    }
}
