package com.tft.coach.common.observability;

import java.util.Objects;
import java.util.UUID;

/**
 * End-to-end correlation context shared across Vision / Knowledge / Agent / DB hops.
 */
public final class CorrelationContext {

    public static final String MDC_CORRELATION_ID = "correlation_id";
    public static final String MDC_AGENT_RUN_ID = "agent_run_id";

    private final String correlationId;
    private final String agentRunId;
    private final String parentRunId;

    private CorrelationContext(String correlationId, String agentRunId, String parentRunId) {
        this.correlationId = Objects.requireNonNull(correlationId);
        this.agentRunId = Objects.requireNonNull(agentRunId);
        this.parentRunId = parentRunId;
    }

    public static CorrelationContext newRoot() {
        return new CorrelationContext(UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
    }

    public static CorrelationContext continueWith(String correlationId) {
        String cid = correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
        return new CorrelationContext(cid, UUID.randomUUID().toString(), null);
    }

    public CorrelationContext childRun() {
        return new CorrelationContext(correlationId, UUID.randomUUID().toString(), agentRunId);
    }

    public String correlationId() {
        return correlationId;
    }

    public String agentRunId() {
        return agentRunId;
    }

    public String parentRunId() {
        return parentRunId;
    }
}
