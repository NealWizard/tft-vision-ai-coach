package com.tft.coach.knowledge.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Token/cost/latency meter (`P1-LLM-Meter-001`). */
public final class LlmUsageMeter {

    private final List<LlmResponse> records = new ArrayList<>();

    public synchronized void record(LlmResponse response) {
        records.add(response);
    }

    public synchronized LlmUsageSummary summarize() {
        long totalTokens = records.stream()
                .mapToLong(response -> response.promptTokens() + response.completionTokens())
                .sum();
        long totalLatency = records.stream().mapToLong(LlmResponse::latencyMs).sum();
        return new LlmUsageSummary(records.size(), totalTokens, totalLatency);
    }

    public synchronized List<LlmResponse> snapshot() {
        return Collections.unmodifiableList(List.copyOf(records));
    }

    public record LlmUsageSummary(int callCount, long totalTokens, long totalLatencyMs) {}
}
