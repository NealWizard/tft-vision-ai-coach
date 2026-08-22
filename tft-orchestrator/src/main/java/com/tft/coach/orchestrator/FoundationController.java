package com.tft.coach.orchestrator;

import com.tft.coach.common.flags.FeatureFlags;
import com.tft.coach.common.observability.AgentRun;
import com.tft.coach.common.observability.CorrelationContext;
import com.tft.coach.common.observability.ToolCallRecord;
import com.tft.coach.common.observability.TraceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class FoundationController {

    private final FeatureFlags flags;
    private final TraceService traceService;

    public FoundationController(FeatureFlags flags, TraceService traceService) {
        this.flags = flags;
        this.traceService = traceService;
    }

    @GetMapping("/health/foundation")
    public Map<String, Object> foundationHealth() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("version", "0.1.0");
        body.put("live_any_enabled", flags.anyLiveEnabled());
        body.put("flags", Map.of(
                "offline_lab", flags.isOfflineLab(),
                "post_game", flags.isPostGame(),
                "pre_game_meta", flags.isPreGameMeta(),
                "live_experiment", flags.isLiveExperiment(),
                "live_dynamic_recommendation", flags.isLiveDynamicRecommendation(),
                "live_opponent_analysis", flags.isLiveOpponentAnalysis()
        ));
        return body;
    }

    @GetMapping("/trace/demo")
    public Map<String, Object> traceDemo(
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        Instant started = Instant.now();
        CorrelationContext ctx = traceService.start(correlationId);
        try {
            List<ToolCallRecord> tools = List.of(
                    new ToolCallRecord("game-rule-tool", "1.0.0", "ok", 12L)
            );
            AgentRun run = traceService.complete(ctx, "foundation-demo-agent", "0.1.0", started, "ok", tools);
            return Map.of(
                    "correlation_id", run.correlationId(),
                    "agent_run_id", run.agentRunId(),
                    "latency_ms", run.latencyMs(),
                    "status", run.status(),
                    "version", run.agentVersion()
            );
        } finally {
            traceService.clearMdc();
        }
    }

    @GetMapping("/trace/{correlationId}")
    public ResponseEntity<List<AgentRun>> byCorrelation(@PathVariable String correlationId) {
        List<AgentRun> runs = traceService.findByCorrelationId(correlationId);
        if (runs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(runs);
    }
}
