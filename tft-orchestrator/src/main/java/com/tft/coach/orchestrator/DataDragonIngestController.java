package com.tft.coach.orchestrator;

import com.tft.coach.data.datadragon.DataDragonKnowledgeIngestor;
import com.tft.coach.data.datadragon.PatchVersionHints;
import com.tft.coach.data.entity.CanonicalEntityResolver;
import com.tft.coach.data.patch.PatchRecord;
import com.tft.coach.data.patch.PatchStatus;
import com.tft.coach.data.spi.AdapterFetchException;
import com.tft.coach.knowledge.platform.KnowledgePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One-shot Data Dragon full ingest into the active CanonicalKnowledgeStore
 * (MySQL when online, in-memory when offline).
 */
@RestController
@RequestMapping("/api/v1/data/ingest")
public class DataDragonIngestController {

    private static final Logger log = LoggerFactory.getLogger(DataDragonIngestController.class);

    private final KnowledgePlatform platform;
    private final String defaultPatch;
    private final String snapshotDir;

    public DataDragonIngestController(
            KnowledgePlatform platform,
            @Value("${tft.platform.patch:set18-18.1}") String defaultPatch,
            @Value("${tft.platform.snapshot-dir:data/snapshots}") String snapshotDir
    ) {
        this.platform = platform;
        this.defaultPatch = defaultPatch;
        this.snapshotDir = snapshotDir;
    }

    @PostMapping("/datadragon")
    public Map<String, Object> ingest(
            @RequestParam(required = false) String patch,
            @RequestParam(required = false) String locale,
            @RequestBody(required = false) IngestBody body
    ) throws AdapterFetchException, IOException {
        String resolvedPatch = firstNonBlank(patch, body == null ? null : body.patch(), defaultPatch);
        String resolvedLocale = firstNonBlank(locale, body == null ? null : body.locale(), "en_US");
        ensurePatchRegistered(resolvedPatch);

        long started = System.currentTimeMillis();
        DataDragonKnowledgeIngestor.IngestReport report = DataDragonKnowledgeIngestor
                .createDefault(Path.of(snapshotDir), platform.normalizer(), new CanonicalEntityResolver())
                .ingest(resolvedPatch, resolvedLocale);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("patch", report.patchId());
        response.put("dragon_version", report.dragonVersion());
        response.put("entity_count", report.entityCount());
        response.put("locale", resolvedLocale);
        response.put("snapshot_dir", snapshotDir);
        response.put("store_size", platform.normalizer().store().size());
        response.put("latency_ms", System.currentTimeMillis() - started);
        log.info("Data Dragon ingest OK patch={} version={} entities={}",
                report.patchId(), report.dragonVersion(), report.entityCount());
        return response;
    }

    @ExceptionHandler({AdapterFetchException.class, IOException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> ingestFailed(Exception ex) {
        log.warn("Data Dragon ingest failed: {}", ex.toString());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", ex.getMessage() == null ? ex.toString() : ex.getMessage()));
    }

    private void ensurePatchRegistered(String patchId) {
        if (platform.patchManager().find(patchId).isPresent()) {
            return;
        }
        platform.patchManager().register(new PatchRecord(
                patchId,
                PatchVersionHints.toSetId(patchId),
                Instant.now(),
                null,
                PatchStatus.CURRENT,
                Duration.ofDays(14)));
    }

    private static String firstNonBlank(String a, String b, String fallback) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return fallback;
    }

    public record IngestBody(String patch, String locale) {}
}
