package com.tft.coach.knowledge;

import com.tft.coach.data.evidence.EvidenceRecord;
import com.tft.coach.data.evidence.FetchEvidence;
import com.tft.coach.data.spi.SourceType;
import com.tft.coach.knowledge.agent.KnowledgeAgent;
import com.tft.coach.knowledge.platform.KnowledgePlatform;
import com.tft.coach.knowledge.rag.eval.RagEvaluationRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgePlatformTest {

    private KnowledgePlatform platform;

    @BeforeEach
    void setUp() {
        platform = KnowledgePlatform.createDefault();
    }

    @Test
    void knowledgeAgentAnswersInterestRuleWithEvidence() {
        var response = platform.knowledgeAgent().answer(new KnowledgeAgent.KnowledgeAgentRequest(
                "What does interest gold rule look like at 50 gold?",
                "set17-16.16",
                "corr-test-001",
                false,
                false));

        assertFalse(response.candidates().isEmpty());
        assertTrue(response.candidates().getFirst().get("summary").toString().contains("5"));
        assertFalse(((List<?>) response.candidates().getFirst().get("evidence")).isEmpty());
    }

    @Test
    void ragEvalProducesRecallMetrics() {
        List<RagEvaluationRunner.RagEvalCase> cases = List.of(
                new RagEvaluationRunner.RagEvalCase(
                        "interest gold cap",
                        "set17-16.16",
                        List.of("interest", "gold")),
                new RagEvaluationRunner.RagEvalCase(
                        "Ahri champion",
                        "set17-16.16",
                        List.of("ahri")));
        var report = platform.ragEvalRunner().evaluate(cases, 3);
        assertTrue(report.recallAtK() >= 0.5);
        assertTrue(report.citationCoverage() > 0);
    }

    @Test
    void knowledgeQaRegression100Cases() {
        List<KnowledgeAgent.KnowledgeAgentRequest> cases = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            cases.add(new KnowledgeAgent.KnowledgeAgentRequest(
                    i % 2 == 0
                            ? "What is interest gold at 50 gold?"
                            : "Explain shop pool size",
                    "set17-16.16",
                    "corr-qa-" + i,
                    false,
                    false));
        }
        int passed = 0;
        for (KnowledgeAgent.KnowledgeAgentRequest request : cases) {
            var response = platform.knowledgeAgent().answer(request);
            if (!response.candidates().isEmpty()
                    && response.candidates().getFirst().containsKey("fact_layers")) {
                passed++;
            }
        }
        assertEquals(100, passed);
    }

    @Test
    void researchAgentReturnsLowConfidenceCandidate() {
        var response = platform.researchAgent().research(
                new com.tft.coach.knowledge.agent.ResearchAgent.ResearchAgentRequest(
                        "latest patch trend",
                        "set17-16.16",
                        "corr-research-001"));
        assertFalse(response.candidates().isEmpty());
        assertTrue(response.notes().contains("cannot override"));
    }
}
