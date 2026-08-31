package com.tft.coach.knowledge.llm;

import java.util.Map;

public record ChatRequest(String system, String user, Map<String, String> variables) {
    public ChatRequest {
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }
}
