package com.tft.coach.knowledge.llm;

import java.util.Map;

public record LlmRequest(
        String promptTemplateId,
        String promptVersion,
        Map<String, String> variables,
        int maxTokens
) {
    public LlmRequest {
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }
}
