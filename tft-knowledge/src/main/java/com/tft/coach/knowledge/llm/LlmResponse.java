package com.tft.coach.knowledge.llm;

public record LlmResponse(
        String content,
        String providerId,
        String modelVersion,
        int promptTokens,
        int completionTokens,
        long latencyMs,
        boolean degraded
) {}
