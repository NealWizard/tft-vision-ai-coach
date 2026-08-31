package com.tft.coach.orchestrator.config;

import com.tft.coach.decision.DecisionPlatform;
import com.tft.coach.knowledge.platform.KnowledgePlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DecisionPlatformConfig {

    @Bean
    public DecisionPlatform decisionPlatform(KnowledgePlatform knowledgePlatform) {
        return DecisionPlatform.createDefault(knowledgePlatform);
    }
}
