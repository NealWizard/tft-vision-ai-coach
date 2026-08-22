package com.tft.coach.common.observability;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TraceServiceTest {

    @Test
    void correlationChainQueryable() {
        TraceService service = new TraceService();
        CorrelationContext ctx = service.start("corr-fixed-001");
        AgentRun run = service.complete(
                ctx,
                "knowledge",
                "0.1.0",
                Instant.now().minusMillis(40),
                "ok",
                List.of(new ToolCallRecord("game-rule-tool", "1.0.0", "ok", 10L))
        );
        service.clearMdc();

        List<AgentRun> found = service.findByCorrelationId("corr-fixed-001");
        assertFalse(found.isEmpty());
        assertEquals("corr-fixed-001", found.getFirst().correlationId());
        assertEquals(run.agentRunId(), found.getFirst().agentRunId());
        assertEquals("ok", found.getFirst().status());
    }
}
