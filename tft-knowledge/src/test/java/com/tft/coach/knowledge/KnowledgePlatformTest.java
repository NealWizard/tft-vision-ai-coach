package com.tft.coach.knowledge;

import com.tft.coach.knowledge.agent.KnowledgeAgent;
import com.tft.coach.knowledge.platform.KnowledgePlatform;
import com.tft.coach.knowledge.rag.eval.EvalDatasetLoader;
import com.tft.coach.knowledge.rag.eval.RagEvaluationRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
                "set18-18.1",
                "corr-test-001",
                false,
                false));

        assertFalse(response.candidates().isEmpty());
        assertTrue(response.candidates().getFirst().get("summary").toString().toLowerCase().contains("interest")
                || response.candidates().getFirst().get("summary").toString().contains("5"));
        assertFalse(((List<?>) response.candidates().getFirst().get("evidence")).isEmpty());
    }

    @Test
    void ragEvalProducesRecallMetricsOnDataset() {
        List<RagEvaluationRunner.RagEvalCase> cases = EvalDatasetLoader.loadQa100().stream()
                .filter(c -> c.query().toLowerCase().contains("interest") || c.query().toLowerCase().contains("ahri"))
                .limit(20)
                .toList();
        var report = platform.ragEvalRunner().evaluate(cases, 3);
        assertTrue(report.recallAtK() >= 0.4, "recall=" + report.recallAtK());
        assertTrue(report.citationCoverage() > 0);
    }

    @Test
    void knowledgeQaRegression100CasesFromDataset() {
        List<RagEvaluationRunner.RagEvalCase> dataset = EvalDatasetLoader.loadQa100();
        assertEquals(100, dataset.size());
        int passed = 0;
        for (int i = 0; i < dataset.size(); i++) {
            RagEvaluationRunner.RagEvalCase evalCase = dataset.get(i);
            var response = platform.knowledgeAgent().answer(new KnowledgeAgent.KnowledgeAgentRequest(
                    evalCase.query(),
                    evalCase.patch(),
                    "corr-qa-" + i,
                    false,
                    false));
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
                        "set18-18.1",
                        "corr-research-001"));
        assertFalse(response.candidates().isEmpty());
        assertTrue(response.notes().contains("cannot override"));
    }

    @Test
    void championToolReturnsSeededAhri() {
        var hits = platform.tool("champion-tool").search("set18-18.1", "Ahri");
        assertFalse(hits.isEmpty());
        assertTrue(hits.getFirst().get("name").toString().contains("Ahri"));
    }
}
