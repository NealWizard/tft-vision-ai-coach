package com.tft.coach.orchestrator;

import com.tft.coach.data.patch.PatchRequiredException;
import com.tft.coach.knowledge.agent.KnowledgeAgent;
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

/**
 * Demo HTTP boundary for Knowledge Agent v1.
 * Cloud LLM is off unless the caller explicitly enables it.
 */
@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeAskController {

    static final String DEFAULT_PATCH = "set18-18.1";

    private final KnowledgePlatform platform;

    public KnowledgeAskController(KnowledgePlatform platform) {
        this.platform = platform;
    }

    @GetMapping("/ask")
    public Map<String, Object> askGet(
            @RequestParam String question,
            @RequestParam(defaultValue = DEFAULT_PATCH) String patch,
            @RequestParam(defaultValue = "false") boolean cloud,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        return ask(question, patch, correlationId, cloud);
    }

    @PostMapping("/ask")
    public Map<String, Object> askPost(
            @RequestBody AskBody body,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId
    ) {
        String question = body == null ? null : body.question();
        String patch = body == null || body.patch() == null || body.patch().isBlank()
                ? DEFAULT_PATCH
                : body.patch();
        boolean cloud = body != null && body.cloud();
        String corr = correlationId != null ? correlationId : (body == null ? null : body.correlationId());
        return ask(question, patch, corr, cloud);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(PatchRequiredException.class)
    public ResponseEntity<Map<String, String>> unknownPatch(PatchRequiredException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    private Map<String, Object> ask(String question, String patch, String correlationId, boolean cloud) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question is required");
        }
        String corr = correlationId == null || correlationId.isBlank()
                ? "corr-" + UUID.randomUUID()
                : correlationId;
        KnowledgeAgent.KnowledgeAgentResponse response = platform.knowledgeAgent().answer(
                new KnowledgeAgent.KnowledgeAgentRequest(question, patch, corr, cloud, cloud));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("question", question);
        body.put("patch", patch);
        body.put("candidates", response.candidates());
        body.put("notes", response.notes());
        body.put("trace", response.trace());
        body.put("degraded", response.degraded());
        return body;
    }

    public record AskBody(String question, String patch, String correlationId, boolean cloud) {}
}
