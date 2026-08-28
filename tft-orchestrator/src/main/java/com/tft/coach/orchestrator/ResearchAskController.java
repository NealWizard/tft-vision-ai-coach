package com.tft.coach.orchestrator;

import com.tft.coach.knowledge.agent.ResearchAgent;
import com.tft.coach.knowledge.platform.KnowledgePlatform;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/research")
public class ResearchAskController {

    static final String DEFAULT_PATCH = "set17-16.16";

    private final KnowledgePlatform platform;

    public ResearchAskController(KnowledgePlatform platform) {
        this.platform = platform;
    }

    @GetMapping("/ask")
    public Map<String, Object> askGet(
            @RequestParam String topic,
            @RequestParam(defaultValue = DEFAULT_PATCH) String patch,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ask(topic, patch, correlationId);
    }

    @PostMapping("/ask")
    public Map<String, Object> askPost(
            @RequestBody ResearchBody body,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        String topic = body == null ? null : body.topic();
        String patch = body == null || body.patch() == null || body.patch().isBlank()
                ? DEFAULT_PATCH
                : body.patch();
        String corr = correlationId != null ? correlationId : (body == null ? null : body.correlationId());
        return ask(topic, patch, corr);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    private Map<String, Object> ask(String topic, String patch, String correlationId) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic is required");
        }
        String corr = correlationId == null || correlationId.isBlank()
                ? "corr-" + UUID.randomUUID()
                : correlationId;
        ResearchAgent.ResearchAgentResponse response = platform.researchAgent().research(
                new ResearchAgent.ResearchAgentRequest(topic, patch, corr));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("topic", topic);
        body.put("patch", patch);
        body.put("candidates", response.candidates());
        body.put("notes", response.notes());
        body.put("trace", response.trace());
        return body;
    }

    public record ResearchBody(String topic, String patch, String correlationId) {}
}
