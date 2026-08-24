package com.tft.coach.knowledge.llm;

/** Guards all cloud LLM calls (`P1-LLM-Guard-001`). */
public final class LlmSafetyGuard {

    public static final int MAX_PROMPT_CHARS = 16_000;

    public void validate(LlmRequest request) {
        String rendered = request.variables().values().stream().reduce("", String::concat);
        if (rendered.length() > MAX_PROMPT_CHARS) {
            throw new IllegalArgumentException("Prompt exceeds max context length");
        }
        if (containsInjection(rendered)) {
            throw new IllegalArgumentException("Potential prompt injection detected");
        }
    }

    private static boolean containsInjection(String text) {
        String lower = text.toLowerCase();
        return lower.contains("ignore previous instructions") || lower.contains("system prompt override");
    }
}
