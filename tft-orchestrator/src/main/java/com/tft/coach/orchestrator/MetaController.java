package com.tft.coach.orchestrator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tft.coach.decision.DecisionPlatform;
import com.tft.coach.meta.MetaQuery;
import com.tft.coach.meta.MetaService;
import com.tft.coach.meta.PatchImpact;
import com.tft.coach.meta.StoredMetaSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

    private final DecisionPlatform platform;

    public MetaController(DecisionPlatform platform) {
        this.platform = platform;
    }

    public record SearchRequest(
            String patch,
            String region,
            @JsonProperty("time_window") String timeWindow,
            String rank,
            String queue
    ) {
    }

    @GetMapping("/snapshot/{id}")
    public Map<String, Object> snapshot(@PathVariable String id) {
        StoredMetaSnapshot stored = platform.metaService().snapshot(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "snapshot not found"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", stored.id());
        body.put("snapshot", stored.snapshot());
        return body;
    }

    @PostMapping("/search")
    public Map<String, Object> search(@RequestBody SearchRequest request) {
        if (request == null || request.patch() == null || request.patch().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patch is required");
        }
        MetaQuery query = new MetaQuery(
                request.patch(),
                request.region(),
                request.timeWindow(),
                request.rank(),
                request.queue());
        MetaService.SearchResult result = platform.metaService().search(query, Instant.now());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("degraded", result.degraded());
        body.put("degraded_reasons", result.degradedReasons());
        body.put("snapshot_id", result.snapshot().map(StoredMetaSnapshot::id).orElse(null));
        body.put("comps", result.comps());
        body.put("trend", result.trend().orElse(null));
        return body;
    }

    @PostMapping("/patch-impact")
    public PatchImpact patchImpact(@RequestBody SearchRequest request) {
        if (request == null || request.patch() == null || request.patch().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patch is required");
        }
        return platform.metaService().patchImpact(request.patch(), request.patch(), Instant.now());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public Map<String, Object> badRequest(IllegalArgumentException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("degraded", true);
        body.put("error", "BAD_REQUEST");
        body.put("message", ex.getMessage());
        return body;
    }
}
