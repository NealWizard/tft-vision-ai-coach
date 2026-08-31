package com.tft.coach.orchestrator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tft.coach.decision.DecisionPlatform;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.decision.pipeline.DecisionGuard;
import com.tft.coach.decision.pipeline.DecisionPipeline;
import com.tft.coach.state.gamestate.GameState;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final DecisionPlatform platform;

    public RecommendationController(DecisionPlatform platform) {
        this.platform = platform;
    }

    public record AnalyzeRequest(
            GameState gamestate,
            String region,
            @JsonProperty("time_window") String timeWindow,
            String rank,
            String queue,
            @JsonProperty("correlation_id") String correlationId,
            @JsonProperty("decision_type") DecisionType decisionType
    ) {
    }

    @PostMapping("/analyze")
    public CandidateSet analyze(@RequestBody AnalyzeRequest request) {
        if (request == null || request.gamestate() == null) {
            throw new DecisionGuard.MissingGameStateException("gamestate is required");
        }
        return platform.pipeline().analyze(
                request.gamestate(),
                new DecisionPipeline.AnalyzeRequest(
                        request.region(),
                        request.timeWindow(),
                        request.rank(),
                        request.queue(),
                        request.correlationId(),
                        request.decisionType() == null ? DecisionType.COMPOSITION : request.decisionType()));
    }

    @ExceptionHandler(DecisionGuard.MissingGameStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> missingState(DecisionGuard.MissingGameStateException ex) {
        return error("NO_GAMESTATE", ex.getMessage());
    }

    @ExceptionHandler(DecisionGuard.MissingPatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> missingPatch(DecisionGuard.MissingPatchException ex) {
        return error("NO_PATCH", ex.getMessage());
    }

    private static Map<String, Object> error(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("degraded", true);
        body.put("error", code);
        body.put("message", message);
        return body;
    }
}
