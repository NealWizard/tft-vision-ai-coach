package com.tft.coach.decision;

import com.tft.coach.decision.pipeline.DecisionPipeline;
import com.tft.coach.knowledge.platform.KnowledgePlatform;
import com.tft.coach.meta.MetaService;

import java.util.Objects;

public final class DecisionPlatform {

    private final KnowledgePlatform knowledge;
    private final DecisionPipeline pipeline;

    public DecisionPlatform(KnowledgePlatform knowledge, DecisionPipeline pipeline) {
        this.knowledge = Objects.requireNonNull(knowledge);
        this.pipeline = Objects.requireNonNull(pipeline);
    }

    public static DecisionPlatform createDefault() {
        KnowledgePlatform knowledge = KnowledgePlatform.createDefault();
        return new DecisionPlatform(knowledge, DecisionPipeline.createDefault(knowledge));
    }

    public static DecisionPlatform createDefault(KnowledgePlatform knowledge) {
        return new DecisionPlatform(knowledge, DecisionPipeline.createDefault(knowledge));
    }

    public KnowledgePlatform knowledge() {
        return knowledge;
    }

    public DecisionPipeline pipeline() {
        return pipeline;
    }

    public MetaService metaService() {
        return pipeline.metaService();
    }
}
