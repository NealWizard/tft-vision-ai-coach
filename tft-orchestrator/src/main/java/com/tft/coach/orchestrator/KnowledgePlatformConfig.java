package com.tft.coach.orchestrator;

import com.tft.coach.knowledge.platform.KnowledgePlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KnowledgePlatformConfig {

    @Bean
    public KnowledgePlatform knowledgePlatform() {
        return KnowledgePlatform.createDefault();
    }
}
