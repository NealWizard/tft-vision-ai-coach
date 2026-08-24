package com.tft.coach.knowledge.platform;

import com.tft.coach.common.degrade.DegradeRouter;
import com.tft.coach.data.conflict.ConflictQueue;
import com.tft.coach.data.evidence.EvidenceStore;
import com.tft.coach.data.normalize.KnowledgeNormalizer;
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
import com.tft.coach.knowledge.rag.search.Bm25Index;
import com.tft.coach.knowledge.rag.search.HybridSearchService;
import com.tft.coach.knowledge.rag.vector.InMemoryVectorStore;
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

    private KnowledgePlatform(
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

    public static KnowledgePlatform createDefault() {
        PatchManager patchManager = new PatchManager();
        patchManager.register(new PatchRecord(
                "set17-16.16",
                "set17",
                Instant.parse("2026-08-01T00:00:00Z"),
                null,
                PatchStatus.CURRENT,
                Duration.ofDays(14)));

        KnowledgeNormalizer normalizer = new KnowledgeNormalizer();
        EvidenceStore evidenceStore = new EvidenceStore();
        ConflictQueue conflictQueue = new ConflictQueue();
        SourceQualityScorer qualityScorer = new SourceQualityScorer();

        GameRuleTool gameRuleTool = new GameRuleTool(patchManager, evidenceStore);
        ChampionTool championTool = new ChampionTool(patchManager, normalizer.store());
        TraitTool traitTool = new TraitTool(patchManager, normalizer.store());
        ItemTool itemTool = new ItemTool(patchManager, normalizer.store());
        AugmentTool augmentTool = new AugmentTool(patchManager, normalizer.store());
        MechanicTool mechanicTool = new MechanicTool(patchManager);
        ProbabilityTool probabilityTool = new ProbabilityTool(patchManager);
        UnitPoolTool unitPoolTool = new UnitPoolTool(patchManager);

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
        Bm25Index bm25Index = new Bm25Index();
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();
        RagIndexer ragIndexer = new RagIndexer(bm25Index, vectorStore, embeddingProvider);
        DocumentIngestionPipeline ingestion = new DocumentIngestionPipeline();
        ragIndexer.index(ingestion.ingestText(
                "manual",
                "set17-16.16",
                "set17",
                "Interest gold is capped at 5 when holding 50 gold under standard economy rules."));
        ragIndexer.index(ingestion.ingestText(
                "manual",
                "set17-16.16",
                "set17",
                "Ahri is a 2-cost champion in Set 17."));

        HybridSearchService hybridSearch = new HybridSearchService(vectorStore, bm25Index, embeddingProvider);
        KnowledgeRagApi ragApi = new KnowledgeRagApi(hybridSearch, new ScoreReranker(), bm25Index);
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
        ResearchAgent researchAgent = new ResearchAgent(knowledgeAgent);

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
