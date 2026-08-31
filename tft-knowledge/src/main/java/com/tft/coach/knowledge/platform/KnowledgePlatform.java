package com.tft.coach.knowledge.platform;

import com.tft.coach.common.degrade.DegradeRouter;
import com.tft.coach.data.conflict.ConflictQueue;
import com.tft.coach.data.conflict.InMemoryConflictQueue;
import com.tft.coach.data.evidence.EvidenceStore;
import com.tft.coach.data.evidence.InMemoryEvidenceStore;
import com.tft.coach.data.normalize.KnowledgeNormalizer;
import com.tft.coach.data.patch.InMemoryPatchManager;
import com.tft.coach.data.patch.PatchManager;
import com.tft.coach.data.patch.PatchRecord;
import com.tft.coach.data.patch.PatchStatus;
import com.tft.coach.data.quality.SourceQualityScorer;
import com.tft.coach.knowledge.agent.KnowledgeAgent;
import com.tft.coach.knowledge.agent.ResearchAgent;
import com.tft.coach.knowledge.llm.CloudLlmGateway;
import com.tft.coach.knowledge.llm.DeterministicLlmProvider;
import com.tft.coach.knowledge.llm.LlmProvider;
import com.tft.coach.knowledge.llm.StubCloudLlmProvider;
import com.tft.coach.knowledge.llm.LlmSafetyGuard;
import com.tft.coach.knowledge.llm.LlmUsageMeter;
import com.tft.coach.knowledge.llm.PromptTemplateRegistry;
import com.tft.coach.knowledge.rag.RagIndexer;
import com.tft.coach.knowledge.rag.api.KnowledgeRagApi;
import com.tft.coach.knowledge.rag.embedding.HashEmbeddingProvider;
import com.tft.coach.knowledge.rag.eval.RagEvaluationRunner;
import com.tft.coach.knowledge.rag.ingest.DocumentIngestionPipeline;
import com.tft.coach.knowledge.rag.rerank.ScoreReranker;
import com.tft.coach.knowledge.research.StubWebSearchProvider;
import com.tft.coach.knowledge.rag.search.Bm25Index;
import com.tft.coach.knowledge.rag.search.HybridSearchService;
import com.tft.coach.knowledge.rag.search.TextSearchIndex;
import com.tft.coach.knowledge.rag.vector.InMemoryVectorStore;
import com.tft.coach.knowledge.rag.vector.VectorStore;
import com.tft.coach.knowledge.tools.AugmentTool;
import com.tft.coach.knowledge.tools.ChampionTool;
import com.tft.coach.knowledge.tools.GameRuleTool;
import com.tft.coach.knowledge.tools.ItemTool;
import com.tft.coach.knowledge.tools.KnowledgeTool;
import com.tft.coach.knowledge.tools.MechanicTool;
import com.tft.coach.knowledge.tools.ProbabilityTool;
import com.tft.coach.knowledge.tools.TraitTool;
import com.tft.coach.knowledge.tools.UnitPoolTool;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Wires P1 knowledge, RAG and LLM components. */
public final class KnowledgePlatform {

    private final PatchManager patchManager;
    private final KnowledgeNormalizer normalizer;
    private final EvidenceStore evidenceStore;
    private final ConflictQueue conflictQueue;
    private final SourceQualityScorer qualityScorer;
    private final Map<String, KnowledgeTool> tools;
    private final KnowledgeRagApi ragApi;
    private final RagEvaluationRunner ragEvalRunner;
    private final CloudLlmGateway llmGateway;
    private final PromptTemplateRegistry promptRegistry;
    private final KnowledgeAgent knowledgeAgent;
    private final ResearchAgent researchAgent;

    public KnowledgePlatform(
            PatchManager patchManager,
            KnowledgeNormalizer normalizer,
            EvidenceStore evidenceStore,
            ConflictQueue conflictQueue,
            SourceQualityScorer qualityScorer,
            Map<String, KnowledgeTool> tools,
            KnowledgeRagApi ragApi,
            RagEvaluationRunner ragEvalRunner,
            CloudLlmGateway llmGateway,
            PromptTemplateRegistry promptRegistry,
            KnowledgeAgent knowledgeAgent,
            ResearchAgent researchAgent
    ) {
        this.patchManager = patchManager;
        this.normalizer = normalizer;
        this.evidenceStore = evidenceStore;
        this.conflictQueue = conflictQueue;
        this.qualityScorer = qualityScorer;
        this.tools = Map.copyOf(tools);
        this.ragApi = ragApi;
        this.ragEvalRunner = ragEvalRunner;
        this.llmGateway = llmGateway;
        this.promptRegistry = promptRegistry;
        this.knowledgeAgent = knowledgeAgent;
        this.researchAgent = researchAgent;
    }

    public PatchManager patchManager() {
        return patchManager;
    }

    public KnowledgeNormalizer normalizer() {
        return normalizer;
    }

    public EvidenceStore evidenceStore() {
        return evidenceStore;
    }

    public ConflictQueue conflictQueue() {
        return conflictQueue;
    }

    public SourceQualityScorer qualityScorer() {
        return qualityScorer;
    }

    public KnowledgeRagApi ragApi() {
        return ragApi;
    }

    public RagEvaluationRunner ragEvalRunner() {
        return ragEvalRunner;
    }

    public CloudLlmGateway llmGateway() {
        return llmGateway;
    }

    public PromptTemplateRegistry promptRegistry() {
        return promptRegistry;
    }

    public KnowledgeAgent knowledgeAgent() {
        return knowledgeAgent;
    }

    public ResearchAgent researchAgent() {
        return researchAgent;
    }

    public KnowledgeTool tool(String toolId) {
        return tools.get(toolId);
    }

    public Map<String, KnowledgeTool> tools() {
        return tools;
    }

    public static KnowledgePlatform createDefault() {
        String patch = "set18-18.1";
        String setId = "set18";
        PatchManager patchManager = new InMemoryPatchManager();
        patchManager.register(new PatchRecord(
                patch,
                setId,
                Instant.parse("2026-08-26T00:00:00Z"),
                null,
                PatchStatus.CURRENT,
                Duration.ofDays(14)));

        KnowledgeNormalizer normalizer = new KnowledgeNormalizer();
        try {
            com.tft.coach.knowledge.bootstrap.OfflineEntityBootstrap.seedChampions(normalizer, patch);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to seed offline entities", ex);
        }

        EvidenceStore evidenceStore = new InMemoryEvidenceStore();
        ConflictQueue conflictQueue = new InMemoryConflictQueue();
        SourceQualityScorer qualityScorer = new SourceQualityScorer();
        var catalog = com.tft.coach.knowledge.catalog.KnowledgeCatalog.loadDefault();

        GameRuleTool gameRuleTool = new GameRuleTool(patchManager, evidenceStore, catalog);
        ChampionTool championTool = new ChampionTool(patchManager, normalizer.store());
        TraitTool traitTool = new TraitTool(patchManager, normalizer.store());
        ItemTool itemTool = new ItemTool(patchManager, normalizer.store());
        AugmentTool augmentTool = new AugmentTool(patchManager, normalizer.store());
        MechanicTool mechanicTool = new MechanicTool(patchManager, catalog);
        ProbabilityTool probabilityTool = new ProbabilityTool(patchManager, catalog);
        UnitPoolTool unitPoolTool = new UnitPoolTool(patchManager, catalog);

        Map<String, KnowledgeTool> tools = new LinkedHashMap<>();
        tools.put(gameRuleTool.toolId(), gameRuleTool);
        tools.put(championTool.toolId(), championTool);
        tools.put(traitTool.toolId(), traitTool);
        tools.put(itemTool.toolId(), itemTool);
        tools.put(augmentTool.toolId(), augmentTool);
        tools.put(mechanicTool.toolId(), mechanicTool);
        tools.put(probabilityTool.toolId(), probabilityTool);
        tools.put(unitPoolTool.toolId(), unitPoolTool);

        HashEmbeddingProvider embeddingProvider = new HashEmbeddingProvider();
        TextSearchIndex textSearchIndex = new Bm25Index();
        VectorStore vectorStore = new InMemoryVectorStore();
        RagIndexer ragIndexer = new RagIndexer(textSearchIndex, vectorStore, embeddingProvider);
        DocumentIngestionPipeline ingestion = new DocumentIngestionPipeline();
        ragIndexer.index(ingestion.ingestText(
                "manual",
                patch,
                setId,
                "Interest gold is capped at 5 when holding 50 gold under standard economy rules."));
        ragIndexer.index(ingestion.ingestText(
                "manual",
                patch,
                setId,
                "Ahri is a 4-cost champion in Set 18 Enchanted Wilds."));
        ragIndexer.index(ingestion.ingestText(
                "stats",
                patch,
                setId,
                "Magnificent Ahri is a high top4 AP shell in the 24h diamond+ meta."));
        ragIndexer.index(ingestion.ingestText(
                "community",
                patch,
                setId,
                "Case: stage 3-2 gold 50 with champ.ahri in shop. Candidate action BUY champ.ahri. This is a similar-state case, not a numeric fact."));
        catalog.searchRules("interest").forEach(rule -> ragIndexer.index(ingestion.ingestText(
                "catalog",
                patch,
                setId,
                String.valueOf(rule.get("summary")))));
        catalog.searchRules("shop").forEach(rule -> ragIndexer.index(ingestion.ingestText(
                "catalog",
                patch,
                setId,
                String.valueOf(rule.get("summary")))));

        HybridSearchService hybridSearch = new HybridSearchService(vectorStore, textSearchIndex, embeddingProvider);
        KnowledgeRagApi ragApi = new KnowledgeRagApi(hybridSearch, new ScoreReranker(), textSearchIndex);
        RagEvaluationRunner ragEvalRunner = new RagEvaluationRunner(ragApi);

        LlmUsageMeter meter = new LlmUsageMeter();
        LlmProvider cloud = new StubCloudLlmProvider();
        CloudLlmGateway llmGateway = new CloudLlmGateway(
                new DegradeRouter(),
                cloud,
                new DeterministicLlmProvider(),
                new DeterministicLlmProvider(),
                meter,
                new LlmSafetyGuard());

        PromptTemplateRegistry promptRegistry = new PromptTemplateRegistry();
        promptRegistry.register(new PromptTemplateRegistry.PromptTemplate(
                "knowledge.answer",
                "1.0.0",
                "Answer {{question}} using tools only.",
                Map.of("question", "string"),
                "published"));

        KnowledgeAgent knowledgeAgent = new KnowledgeAgent(tools, ragApi, llmGateway, promptRegistry, evidenceStore);
        ResearchAgent researchAgent = new ResearchAgent(knowledgeAgent, new StubWebSearchProvider());

        return new KnowledgePlatform(
                patchManager,
                normalizer,
                evidenceStore,
                conflictQueue,
                qualityScorer,
                tools,
                ragApi,
                ragEvalRunner,
                llmGateway,
                promptRegistry,
                knowledgeAgent,
                researchAgent);
    }
}
