package com.tft.coach.common.observability;

/**
 * Single tool invocation inside an {@link AgentRun}.
 */
public record ToolCallRecord(
        String toolId,
        String toolVersion,
        String status,
        long latencyMs
) {
}
