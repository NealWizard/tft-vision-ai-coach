package com.tft.coach.orchestrator;

import com.tft.coach.state.observation.Observation;
import com.tft.coach.state.observation.ObservationFactory;
import com.tft.coach.vision.frame.VisionFrame;
import com.tft.coach.vision.frame.VisionFrames;
import com.tft.coach.vision.ocr.NumericNormalizer;
import com.tft.coach.vision.ocr.RoiCropper;
import com.tft.coach.vision.profile.RoiRegion;
import com.tft.coach.vision.profile.UnsupportedProfileException;
import com.tft.coach.vision.profile.VisionProfile;
import com.tft.coach.vision.profile.VisionProfileLoader;
import com.tft.coach.vision.sidecar.SidecarAnalyzeRequest;
import com.tft.coach.vision.sidecar.SidecarAnalyzeResult;
import com.tft.coach.vision.sidecar.SidecarClient;
import com.tft.coach.vision.sidecar.SidecarClientConfig;
import com.tft.coach.vision.sidecar.SidecarHealthResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Debug endpoints for vision sidecar health and numeric OCR.
 */
@RestController
@RequestMapping("/api/v1/vision")
public class VisionHealthController {

    private final SidecarClient sidecarClient;
    private final VisionProfileLoader profileLoader = new VisionProfileLoader();

    public VisionHealthController(
            @Value("${tft.vision.sidecar.base-url:http://127.0.0.1:19090}") String baseUrl,
            @Value("${tft.vision.sidecar.connect-timeout-ms:500}") int connectTimeoutMs,
            @Value("${tft.vision.sidecar.read-timeout-ms:2000}") int readTimeoutMs,
            @Value("${tft.vision.sidecar.analyze-timeout-ms:15000}") int analyzeTimeoutMs
    ) {
        this.sidecarClient = new SidecarClient(
                new SidecarClientConfig(baseUrl, connectTimeoutMs, readTimeoutMs, analyzeTimeoutMs)
        );
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

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyze(@RequestBody Map<String, Object> body) {
        Object fieldObj = body.get("field");
        Object imageObj = body.get("image_base64");
        if (fieldObj == null || String.valueOf(fieldObj).isBlank()
                || imageObj == null || String.valueOf(imageObj).isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "degraded", true,
                    "error_code", "INVALID_IMAGE",
                    "detail", "field and image_base64 are required"
            ));
        }
        String field = String.valueOf(fieldObj);
        try {
            byte[] png = Base64.getDecoder().decode(String.valueOf(imageObj).replaceAll("\\s", ""));
            VisionFrame frame = VisionFrames.fromImageBytes(png);
            VisionProfile profile = profileLoader.resolveForResolution(frame.width(), frame.height());
            RoiRegion roi = profile.regions().get(field);
            if (roi == null) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("degraded", true);
                err.put("error_code", "INVALID_ROI");
                err.put("detail", "unknown field " + field);
                return ResponseEntity.ok(err);
            }
            String cropB64 = RoiCropper.cropPngBase64(frame, roi);
            SidecarAnalyzeResult result = sidecarClient.analyze(
                    new SidecarAnalyzeRequest("req-" + UUID.randomUUID(), field, cropB64)
            );
            if (result.degraded()) {
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("degraded", true);
                out.put("status", result.status());
                out.put("error_code", result.errorCode() == null ? "MODEL_NOT_READY" : result.errorCode());
                out.put("raw_value", result.rawValue());
                out.put("detail", result.detail());
                return ResponseEntity.ok(out);
            }
            Optional<Object> normalized = NumericNormalizer.normalize(field, result.rawValue());
            if (normalized.isEmpty()) {
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("degraded", true);
                out.put("error_code", "INVALID_IMAGE");
                out.put("raw_value", result.rawValue());
                out.put("detail", "numeric normalize failed");
                return ResponseEntity.ok(out);
            }
            double score = result.confidence() == null ? 0.0 : result.confidence();
            Observation obs = ObservationFactory.fromOcr(
                    field,
                    normalized.get(),
                    result.rawValue(),
                    score,
                    result.provider(),
                    result.model(),
                    frame.frameId(),
                    new Observation.Rect(roi.x(), roi.y(), roi.width(), roi.height())
            );
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("degraded", false);
            out.put("status", "OK");
            out.put("observation", obs);
            return ResponseEntity.ok(out);
        } catch (UnsupportedProfileException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("degraded", true);
            err.put("error_code", e.errorCode());
            err.put("detail", e.getMessage());
            return ResponseEntity.ok(err);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("degraded", true);
            err.put("error_code", "INVALID_IMAGE");
            err.put("detail", e.getMessage());
            return ResponseEntity.ok(err);
        }
    }
}
