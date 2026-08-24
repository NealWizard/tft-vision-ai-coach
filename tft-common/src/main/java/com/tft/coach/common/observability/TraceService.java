package com.tft.coach.common.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * P0 observability baseline (`P0-FOUND-Observability-001`): bind MDC,
 * record AgentRun/ToolCall, and query by correlation_id.
 */
@Component
public class TraceService {

    private static final Logger log = LoggerFactory.getLogger(TraceService.class);

    private final ConcurrentMap<String, List<AgentRun>> byCorrelation = new ConcurrentHashMap<>();

    public CorrelationContext start(String maybeCorrelationId) {
        CorrelationContext ctx = CorrelationContext.continueWith(maybeCorrelationId);
        MDC.put(CorrelationContext.MDC_CORRELATION_ID, ctx.correlationId());
        MDC.put(CorrelationContext.MDC_AGENT_RUN_ID, ctx.agentRunId());
        return ctx;
    }

    public void clearMdc() {
        MDC.remove(CorrelationContext.MDC_CORRELATION_ID);
        MDC.remove(CorrelationContext.MDC_AGENT_RUN_ID);
    }

    public AgentRun complete(
            CorrelationContext ctx,
            String agentId,
            String agentVersion,
            Instant startedAt,
            String status,
            List<ToolCallRecord> toolCalls
    ) {
        Objects.requireNonNull(ctx, "ctx");
        Instant endedAt = Instant.now();
        long latency = Math.max(0, endedAt.toEpochMilli() - startedAt.toEpochMilli());
        AgentRun run = new AgentRun(
                ctx.correlationId(),
                ctx.agentRunId(),
                agentId,
                agentVersion,
                startedAt,
                endedAt,
                status,
                latency,
                toolCalls
        );
        byCorrelation.computeIfAbsent(ctx.correlationId(), k -> new ArrayList<>()).add(run);
        log.info(
                "agent_run correlation_id={} agent_run_id={} agent_id={} version={} status={} latency_ms={}",
                run.correlationId(),
                run.agentRunId(),
                run.agentId(),
                run.agentVersion(),
                run.status(),
                run.latencyMs()
        );
        return run;
    }

    public List<AgentRun> findByCorrelationId(String correlationId) {
        List<AgentRun> runs = byCorrelation.getOrDefault(correlationId, List.of());
        return List.copyOf(runs);
    }
}
