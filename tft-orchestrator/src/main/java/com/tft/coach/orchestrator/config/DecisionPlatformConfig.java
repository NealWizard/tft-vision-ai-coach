package com.tft.coach.orchestrator.config;

import com.tft.coach.decision.DecisionPlatform;
import com.tft.coach.knowledge.llm.ChatModelGateway;
import com.tft.coach.knowledge.platform.KnowledgePlatform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.Map;

@Configuration
public class DecisionPlatformConfig {

    private static final Logger log = LoggerFactory.getLogger(DecisionPlatformConfig.class);

    @Bean
    public DecisionPlatform decisionPlatform(
            KnowledgePlatform knowledgePlatform,
            @Value("${tft.platform.mode:auto}") String mode,
            @Value("${tft.platform.env-file:.env}") String envFile
    ) {
        ChatModelGateway gateway = ChatModelGateway.mock();
        if (!"offline".equalsIgnoreCase(mode)) {
            gateway = fromEnv(envFile);
        }
        return DecisionPlatform.createDefault(knowledgePlatform, gateway);
    }

    private static ChatModelGateway fromEnv(String envFile) {
        Map<String, String> env = Map.of();
        try {
            env = EnvFileLoader.load(Path.of(envFile).toAbsolutePath().normalize());
        } catch (Exception ex) {
            log.warn("Failed to load env file {}: {}", envFile, ex.toString());
        }
        boolean enabled = Boolean.parseBoolean(EnvFileLoader.resolve(env, "LLM_ENABLED", "true"));
        String apiKey = EnvFileLoader.resolve(env, "LLM_API_KEY", "");
        String baseUrl = EnvFileLoader.resolve(env, "LLM_BASE_URL", "");
        String modelId = EnvFileLoader.resolve(env, "LLM_MODEL_ID", "");
        if (!enabled || apiKey.isBlank() || baseUrl.isBlank() || modelId.isBlank()) {
            log.info("Decision ChatModelGateway=mock (LLM incomplete or disabled)");
            return ChatModelGateway.mock();
        }
        log.info("Decision ChatModelGateway=openai-compatible model={}", modelId);
        return ChatModelGateway.openAiCompatible(baseUrl, apiKey, modelId, "zhipu-or-openai-compatible");
    }
}
