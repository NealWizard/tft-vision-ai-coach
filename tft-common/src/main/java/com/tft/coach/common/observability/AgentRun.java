package com.tft.coach.common.observability;

import java.time.Instant;
import java.util.List;

/**
 * Structured agent execution record for audit and latency analysis.
 */
public record AgentRun(
        String correlationId,
        String agentRunId,
        String agentId,
        String agentVersion,
        Instant startedAt,
        Instant endedAt,
        String status,
        long latencyMs,
        List<ToolCallRecord> toolCalls
) {
    public AgentRun {
        toolCalls = List.copyOf(toolCalls == null ? List.of() : toolCalls);
    }
}
