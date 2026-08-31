package com.tft.coach.knowledge.llm;

public record ChatResponse(String content, String providerId, boolean degraded) {
}
