package com.tft.coach.orchestrator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tft.coach.state.gamestate.GameState;
import com.tft.coach.state.gamestate.GameStateBuilder;
import com.tft.coach.state.observation.Observation;
import com.tft.coach.vision.provider.CloudVisionFallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds GameState from Observation fixtures / OCR outputs.
 */
@RestController
@RequestMapping("/api/v1/state")
public class GameStateController {

    private final GameStateBuilder builder = new GameStateBuilder();
    private final boolean cloudVisionEnabled;

    public GameStateController(
            @Value("${tft.vision.cloud.enabled:false}") boolean cloudVisionEnabled
    ) {
        this.cloudVisionEnabled = cloudVisionEnabled;
    }

    public record BuildRequest(
            @JsonProperty("match_id") String matchId,
            String patch,
            List<Observation> observations
    ) {
    }

    @PostMapping("/build")
    public Map<String, Object> build(@RequestBody BuildRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cloud_vision_enabled", cloudVisionEnabled);
        body.put("cloud_would_call", CloudVisionFallback.shouldCallCloud(0.99, cloudVisionEnabled));
        try {
            GameState state = builder.build(request.matchId(), request.patch(), request.observations());
            body.put("degraded", false);
            body.put("gamestate", state);
            return body;
        } catch (IllegalArgumentException e) {
            body.put("degraded", true);
            body.put("error_code", "INTERNAL_ERROR");
            body.put("detail", e.getMessage());
            return body;
        }
    }
}
