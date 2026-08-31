package com.tft.coach.decision.agent;

import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.knowledge.platform.KnowledgePlatform;
import com.tft.coach.meta.MetaService;
import com.tft.coach.state.gamestate.GameState;

import java.util.List;

public interface DomainAgent {

    DecisionType decisionType();

    CandidateSet advise(GameState state, Context context);

    record Context(
            String correlationId,
            MetaService.SearchResult meta,
            List<String> ragEvidence,
            String fingerprint,
            KnowledgePlatform knowledge
    ) {
        public Context {
            ragEvidence = ragEvidence == null ? List.of() : List.copyOf(ragEvidence);
        }
    }
}
