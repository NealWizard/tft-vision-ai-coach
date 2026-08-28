package com.tft.coach.orchestrator;

import com.tft.coach.vision.sidecar.SidecarClient;
import com.tft.coach.vision.sidecar.SidecarClientConfig;
import com.tft.coach.vision.sidecar.SidecarHealthResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Debug endpoint for vision sidecar health (Batch A).
 */
@RestController
@RequestMapping("/api/v1/vision")
public class VisionHealthController {

    private final SidecarClient sidecarClient;

    public VisionHealthController(
            @Value("${tft.vision.sidecar.base-url:http://127.0.0.1:19090}") String baseUrl,
            @Value("${tft.vision.sidecar.connect-timeout-ms:500}") int connectTimeoutMs,
            @Value("${tft.vision.sidecar.read-timeout-ms:2000}") int readTimeoutMs
    ) {
        this.sidecarClient = new SidecarClient(new SidecarClientConfig(baseUrl, connectTimeoutMs, readTimeoutMs));
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        SidecarHealthResult result = sidecarClient.health();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("degraded", result.degraded());
        body.put("status", result.status());
        body.put("error_code", result.errorCode());
        body.put("service_version", result.serviceVersion());
        body.put("ocr_ready", result.ocrReady());
        body.put("ready", result.ready());
        body.put("latency_ms", result.latencyMs());
        body.put("detail", result.detail());
        return body;
    }
}
