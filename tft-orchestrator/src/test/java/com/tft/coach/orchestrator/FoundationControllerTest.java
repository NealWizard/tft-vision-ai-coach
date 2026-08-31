package com.tft.coach.orchestrator;

import com.tft.coach.common.degrade.DegradeRouter;
import com.tft.coach.common.degrade.ExecutionPath;
import com.tft.coach.common.degrade.ProviderKind;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "tft.platform.mode=offline")
@AutoConfigureMockMvc
class FoundationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DegradeRouter degradeRouter;

    @Test
    void foundationHealthExposesDefaultFlags() throws Exception {
        mockMvc.perform(get("/api/v1/health/foundation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags.offline_lab", is(true)))
                .andExpect(jsonPath("$.flags.post_game", is(true)))
                .andExpect(jsonPath("$.flags.pre_game_meta", is(true)))
                .andExpect(jsonPath("$.flags.live_experiment", is(false)))
                .andExpect(jsonPath("$.flags.live_dynamic_recommendation", is(false)))
                .andExpect(jsonPath("$.flags.live_opponent_analysis", is(false)))
                .andExpect(jsonPath("$.live_any_enabled", is(false)));
    }

    @Test
    void traceDemoWritableAndQueryable() throws Exception {
        mockMvc.perform(get("/api/v1/trace/demo").header("X-Correlation-Id", "corr-test-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correlation_id", is("corr-test-42")))
                .andExpect(jsonPath("$.status", is("ok")));

        mockMvc.perform(get("/api/v1/trace/corr-test-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correlationId", is("corr-test-42")));
    }

    @Test
    void offlineStartupProvidesDeterministicCloudFallback() {
        var decision = degradeRouter.route(ProviderKind.LLM, false, false, false);

        assertEquals(ExecutionPath.DETERMINISTIC, decision.path());
    }

    @Test
    void knowledgeAskReturnsInterestRule() throws Exception {
        mockMvc.perform(get("/api/v1/knowledge/ask")
                        .param("question", "What does interest gold look like at 50 gold?")
                        .param("patch", "set17-16.16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patch", is("set17-16.16")))
                .andExpect(jsonPath("$.notes", is("Tool-backed answer only; no free-form invention.")))
                .andExpect(jsonPath("$.candidates[0].summary").exists())
                .andExpect(jsonPath("$.degraded", is(true)));
    }

    @Test
    void knowledgeAskPostRejectsBlankQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/knowledge/ask")
                        .contentType("application/json")
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dataDragonIngestEndpointIsMapped() throws Exception {
        // Offline CI may lack CDN access; endpoint must respond (200 or 502), never 404.
        int status = mockMvc.perform(post("/api/v1/data/ingest/datadragon")
                        .param("patch", "set17-16.16")
                        .param("locale", "en_US"))
                .andReturn()
                .getResponse()
                .getStatus();
        org.junit.jupiter.api.Assertions.assertTrue(status == 200 || status == 502,
                "unexpected status=" + status);
    }

    @Test
    void visionHealthDegradesWhenSidecarDown() throws Exception {
        mockMvc.perform(get("/api/v1/vision/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degraded", is(true)))
                .andExpect(jsonPath("$.status", is("DEGRADED")));
    }

    @Test
    void visionAnalyzeRejectsBlankPayload() throws Exception {
        mockMvc.perform(post("/api/v1/vision/analyze")
                        .contentType("application/json")
                        .content("{\"field\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void visionAnalyzeUnknownResolutionIsDegraded() throws Exception {
        byte[] png;
        try (var in = FoundationControllerTest.class.getResourceAsStream("/vision/fixtures/1x1.png")) {
            org.junit.jupiter.api.Assertions.assertNotNull(in);
            png = in.readAllBytes();
        }
        String json = "{\"field\":\"player.gold\",\"image_base64\":\""
                + Base64.getEncoder().encodeToString(png) + "\"}";
        mockMvc.perform(post("/api/v1/vision/analyze")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degraded", is(true)))
                .andExpect(jsonPath("$.error_code", is("UNSUPPORTED_PROFILE")));
    }

    @Test
    void stateBuildFromFixtureObservations() throws Exception {
        String json = """
                {"match_id":"match-1","patch":"set18-16.17","observations":[
                  {"schema_version":"1.0.0","field":"stage","value":"3-3","confidence":{"score":1,"level":"certain"},"source":"fixture","timestamp":"2026-08-31T00:00:00Z"},
                  {"schema_version":"1.0.0","field":"player.level","value":6,"confidence":{"score":1,"level":"certain"},"source":"fixture","timestamp":"2026-08-31T00:00:00Z"},
                  {"schema_version":"1.0.0","field":"player.gold","value":65,"confidence":{"score":1,"level":"certain"},"source":"fixture","timestamp":"2026-08-31T00:00:00Z"},
                  {"schema_version":"1.0.0","field":"player.hp","value":80,"confidence":{"score":1,"level":"certain"},"source":"fixture","timestamp":"2026-08-31T00:00:00Z"}
                ]}
                """;
        mockMvc.perform(post("/api/v1/state/build")
                        .contentType("application/json")
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.degraded", is(false)))
                .andExpect(jsonPath("$.cloud_vision_enabled", is(false)))
                .andExpect(jsonPath("$.cloud_would_call", is(false)))
                .andExpect(jsonPath("$.gamestate.stage", is("3-3")))
                .andExpect(jsonPath("$.gamestate.player.gold", is(65)));
    }
}
