package com.tft.coach.knowledge.rag.eval;

import com.tft.coach.knowledge.rag.api.KnowledgeRagApi;
import com.tft.coach.knowledge.rag.vector.VectorFilter;

import java.util.List;
import java.util.Locale;

/** RAG benchmark runner with Recall@K and MRR (`P1-RAG-Eval-001`). */
public final class RagEvaluationRunner {

    private final KnowledgeRagApi ragApi;

    public RagEvaluationRunner(KnowledgeRagApi ragApi) {
        this.ragApi = ragApi;
    }

    public RagEvalReport evaluate(List<RagEvalCase> cases, int k) {
        double recallSum = 0;
        double mrrSum = 0;
        double citationCoverageSum = 0;
        for (RagEvalCase evalCase : cases) {
            var response = ragApi.retrieve(
                    evalCase.query(),
                    VectorFilter.ofPatch(evalCase.patch()),
                    k);
            boolean hit = response.citations().stream()
                    .anyMatch(citation -> containsAny(citation.excerpt(), evalCase.expectedTerms()));
            recallSum += hit ? 1.0 : 0.0;
            mrrSum += reciprocalRank(response, evalCase.expectedTerms());
            citationCoverageSum += response.citations().isEmpty() ? 0.0 : 1.0;
        }
        int n = Math.max(1, cases.size());
        return new RagEvalReport(
                cases.size(),
                k,
                recallSum / n,
                mrrSum / n,
                citationCoverageSum / n);
    }

    private static double reciprocalRank(KnowledgeRagApi.RagResponse response, List<String> expectedTerms) {
        for (int i = 0; i < response.citations().size(); i++) {
            if (containsAny(response.citations().get(i).excerpt(), expectedTerms)) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    private static boolean containsAny(String text, List<String> terms) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (lower.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public record RagEvalCase(String query, String patch, List<String> expectedTerms) {}

    public record RagEvalReport(int caseCount, int k, double recallAtK, double mrr, double citationCoverage) {}
}
