package com.tft.coach.orchestrator.config;

import com.tft.coach.data.conflict.ConflictQueue;
import com.tft.coach.data.conflict.JdbcConflictQueue;
import com.tft.coach.data.datadragon.DataDragonKnowledgeIngestor;
import com.tft.coach.data.datadragon.PatchVersionHints;
import com.tft.coach.data.entity.CanonicalEntityResolver;
import com.tft.coach.data.evidence.EvidenceStore;
import com.tft.coach.data.evidence.JdbcEvidenceStore;
import com.tft.coach.data.jdbc.MysqlSchemaInitializer;
import com.tft.coach.data.normalize.JdbcCanonicalKnowledgeStore;
import com.tft.coach.data.normalize.KnowledgeNormalizer;
import com.tft.coach.data.patch.JdbcPatchManager;
import com.tft.coach.data.patch.PatchManager;
import com.tft.coach.data.patch.PatchRecord;
import com.tft.coach.data.patch.PatchStatus;
import com.tft.coach.data.quality.SourceQualityScorer;
import com.tft.coach.common.degrade.DegradeRouter;
import com.tft.coach.knowledge.agent.KnowledgeAgent;
import com.tft.coach.knowledge.agent.ResearchAgent;
import com.tft.coach.knowledge.bootstrap.OfflineEntityBootstrap;
import com.tft.coach.knowledge.catalog.KnowledgeCatalog;
import com.tft.coach.knowledge.llm.CloudLlmGateway;
import com.tft.coach.knowledge.llm.DeterministicLlmProvider;
import com.tft.coach.knowledge.llm.LlmProvider;
import com.tft.coach.knowledge.llm.LlmSafetyGuard;
import com.tft.coach.knowledge.llm.LlmUsageMeter;
import com.tft.coach.knowledge.llm.OpenAiCompatibleLlmProvider;
import com.tft.coach.knowledge.llm.PromptTemplateRegistry;
import com.tft.coach.knowledge.llm.StubCloudLlmProvider;
import com.tft.coach.knowledge.platform.KnowledgePlatform;
import com.tft.coach.knowledge.rag.RagIndexer;
import com.tft.coach.knowledge.rag.api.KnowledgeRagApi;
import com.tft.coach.knowledge.rag.elasticsearch.ElasticsearchTextIndex;
import com.tft.coach.knowledge.rag.elasticsearch.ElasticsearchVectorStore;
import com.tft.coach.knowledge.rag.embedding.EmbeddingProvider;
import com.tft.coach.knowledge.rag.embedding.HashEmbeddingProvider;
import com.tft.coach.knowledge.rag.embedding.OpenAiCompatibleEmbeddingProvider;
import com.tft.coach.knowledge.rag.eval.RagEvaluationRunner;
import com.tft.coach.knowledge.rag.ingest.DocumentIngestionPipeline;
import com.tft.coach.knowledge.rag.rerank.ScoreReranker;
import com.tft.coach.knowledge.rag.search.Bm25Index;
import com.tft.coach.knowledge.rag.search.HybridSearchService;
import com.tft.coach.knowledge.rag.search.TextSearchIndex;
import com.tft.coach.knowledge.rag.vector.InMemoryVectorStore;
import com.tft.coach.knowledge.rag.vector.VectorStore;
import com.tft.coach.knowledge.research.ConfigurableWebSearchProvider;
import com.tft.coach.knowledge.research.StubWebSearchProvider;
import com.tft.coach.knowledge.research.WebSearchProvider;
import com.tft.coach.knowledge.tools.AugmentTool;
import com.tft.coach.knowledge.tools.ChampionTool;
import com.tft.coach.knowledge.tools.GameRuleTool;
import com.tft.coach.knowledge.tools.ItemTool;
import com.tft.coach.knowledge.tools.KnowledgeTool;
import com.tft.coach.knowledge.tools.MechanicTool;
import com.tft.coach.knowledge.tools.ProbabilityTool;
import com.tft.coach.knowledge.tools.TraitTool;
import com.tft.coach.knowledge.tools.UnitPoolTool;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class KnowledgePlatformConfig {

    private static final Logger log = LoggerFactory.getLogger(KnowledgePlatformConfig.class);

    @Bean
    public KnowledgePlatform knowledgePlatform(
            @Value("${tft.platform.mode:auto}") String mode,
            @Value("${tft.platform.patch:set17-16.16}") String patch,
            @Value("${tft.platform.env-file:.env}") String envFile,
            @Value("${tft.platform.seed-datadragon:false}") boolean seedDataDragon,
            @Value("${tft.platform.snapshot-dir:data/snapshots}") String snapshotDir,
            @Value("${tft.rag.index:tft_rag_chunks}") String ragIndex,
            @Value("${tft.rag.embedding-dims:1024}") int embeddingDims,
            @Value("${tft.research.provider:tavily}") String researchProvider
    ) {
        Map<String, String> env = Map.of();
        try {
            env = EnvFileLoader.load(Path.of(envFile).toAbsolutePath().normalize());
        } catch (Exception ex) {
            log.warn("Failed to load env file {}: {}", envFile, ex.toString());
        }

        boolean offline = "offline".equalsIgnoreCase(mode)
                || ("auto".equalsIgnoreCase(mode) && !looksOnline(env));
        if (offline) {
            log.info("KnowledgePlatform mode=offline (InMemory + Stub)");
            return KnowledgePlatform.createDefault();
        }

        log.info("KnowledgePlatform mode=online (MySQL + ES + cloud providers)");
        return buildOnline(env, patch, seedDataDragon, snapshotDir, ragIndex, embeddingDims, researchProvider);
    }

    private static boolean looksOnline(Map<String, String> env) {
        String mysqlHost = EnvFileLoader.resolve(env, "MYSQL_HOST", "");
        String esHosts = EnvFileLoader.resolve(env, "ES_HOSTS", "");
        return !mysqlHost.isBlank() && !esHosts.isBlank();
    }

    private static KnowledgePlatform buildOnline(
            Map<String, String> env,
            String patch,
            boolean seedDataDragon,
            String snapshotDir,
            String ragIndex,
            int embeddingDims,
            String researchProvider
    ) {
        DataSource dataSource = mysqlDataSource(env);
        try {
            new MysqlSchemaInitializer(dataSource).initializeIfNeeded();
        } catch (Exception ex) {
            throw new IllegalStateException("MySQL schema init failed", ex);
        }

        PatchManager patchManager = new JdbcPatchManager(dataSource);
        if (patchManager.find(patch).isEmpty()) {
            patchManager.register(new PatchRecord(
                    patch,
                    PatchVersionHints.toSetId(patch),
                    Instant.parse("2026-08-01T00:00:00Z"),
                    null,
                    PatchStatus.CURRENT,
                    Duration.ofDays(14)));
        }

        var store = new JdbcCanonicalKnowledgeStore(dataSource);
        KnowledgeNormalizer normalizer = new KnowledgeNormalizer(store, new com.tft.coach.data.normalize.DataDragonNormalizer());
        EvidenceStore evidenceStore = new JdbcEvidenceStore(dataSource);
        ConflictQueue conflictQueue = new JdbcConflictQueue(dataSource);
        SourceQualityScorer qualityScorer = new SourceQualityScorer();
        KnowledgeCatalog catalog = KnowledgeCatalog.loadDefault();

        if (seedDataDragon) {
            try {
                CanonicalEntityResolver resolver = new CanonicalEntityResolver();
                DataDragonKnowledgeIngestor.createDefault(Path.of(snapshotDir), normalizer, resolver)
                        .ingest(patch, "en_US");
                log.info("Data Dragon ingest completed for patch={}", patch);
            } catch (Exception ex) {
                log.warn("Data Dragon ingest failed, falling back to offline champion fixture: {}", ex.toString());
                try {
                    OfflineEntityBootstrap.seedChampions(normalizer, patch);
                } catch (Exception nested) {
                    log.warn("Offline fixture seed also failed: {}", nested.toString());
                }
            }
        } else if (store.size() == 0) {
            try {
                OfflineEntityBootstrap.seedChampions(normalizer, patch);
            } catch (Exception ex) {
                log.warn("Offline fixture seed failed: {}", ex.toString());
            }
        }

        Map<String, KnowledgeTool> tools = buildTools(patchManager, evidenceStore, normalizer, catalog);

        EmbeddingProvider embeddingProvider = embeddingProvider(env, embeddingDims);
        String esHosts = EnvFileLoader.resolve(env, "ES_HOSTS", "127.0.0.1:9200");
        VectorStore vectorStore;
        TextSearchIndex textIndex;
        try {
            vectorStore = new ElasticsearchVectorStore(esHosts, ragIndex, embeddingDims);
            textIndex = new ElasticsearchTextIndex(esHosts, ragIndex, embeddingDims);
        } catch (Exception ex) {
            log.warn("ES unavailable, degrade to InMemory RAG: {}", ex.toString());
            vectorStore = new InMemoryVectorStore();
            textIndex = new Bm25Index();
            embeddingProvider = new HashEmbeddingProvider();
        }

        RagIndexer ragIndexer = new RagIndexer(textIndex, vectorStore, embeddingProvider);
        DocumentIngestionPipeline ingestion = new DocumentIngestionPipeline();
        for (var ruleKey : new String[] {"interest", "shop", "xp", "streak"}) {
            catalog.searchRules(ruleKey).forEach(rule -> ragIndexer.index(ingestion.ingestText(
                    "catalog",
                    patch,
                    PatchVersionHints.toSetId(patch),
                    String.valueOf(rule.get("summary")))));
        }

        HybridSearchService hybridSearch = new HybridSearchService(vectorStore, textIndex, embeddingProvider);
        KnowledgeRagApi ragApi = new KnowledgeRagApi(hybridSearch, new ScoreReranker(), textIndex);
        RagEvaluationRunner ragEvalRunner = new RagEvaluationRunner(ragApi);

        LlmProvider cloud = cloudLlm(env);
        CloudLlmGateway llmGateway = new CloudLlmGateway(
                new DegradeRouter(),
                cloud,
                new DeterministicLlmProvider(),
                new DeterministicLlmProvider(),
                new LlmUsageMeter(),
                new LlmSafetyGuard());

        PromptTemplateRegistry promptRegistry = new PromptTemplateRegistry();
        promptRegistry.register(new PromptTemplateRegistry.PromptTemplate(
                "knowledge.answer",
                "1.0.0",
                "Answer {{question}} using structured tools and citations only.",
                Map.of("question", "string"),
                "published"));

        KnowledgeAgent knowledgeAgent = new KnowledgeAgent(tools, ragApi, llmGateway, promptRegistry, evidenceStore);
        WebSearchProvider webSearch = webSearchProvider(env, researchProvider);
        ResearchAgent researchAgent = new ResearchAgent(knowledgeAgent, webSearch);

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

    private static Map<String, KnowledgeTool> buildTools(
            PatchManager patchManager,
            EvidenceStore evidenceStore,
            KnowledgeNormalizer normalizer,
            KnowledgeCatalog catalog
    ) {
        Map<String, KnowledgeTool> tools = new LinkedHashMap<>();
        GameRuleTool gameRuleTool = new GameRuleTool(patchManager, evidenceStore, catalog);
        tools.put(gameRuleTool.toolId(), gameRuleTool);
        tools.put(ChampionTool.TOOL_ID, new ChampionTool(patchManager, normalizer.store()));
        tools.put(TraitTool.TOOL_ID, new TraitTool(patchManager, normalizer.store()));
        tools.put(ItemTool.TOOL_ID, new ItemTool(patchManager, normalizer.store()));
        tools.put(AugmentTool.TOOL_ID, new AugmentTool(patchManager, normalizer.store()));
        tools.put(MechanicTool.TOOL_ID, new MechanicTool(patchManager, catalog));
        tools.put(ProbabilityTool.TOOL_ID, new ProbabilityTool(patchManager, catalog));
        tools.put(UnitPoolTool.TOOL_ID, new UnitPoolTool(patchManager, catalog));
        return tools;
    }

    private static DataSource mysqlDataSource(Map<String, String> env) {
        String host = EnvFileLoader.resolve(env, "MYSQL_HOST", "127.0.0.1");
        String port = EnvFileLoader.resolve(env, "MYSQL_PORT", "3306");
        String database = EnvFileLoader.resolve(env, "MYSQL_DATABASE", "tft");
        String username = EnvFileLoader.resolve(env, "MYSQL_USERNAME", "root");
        String password = EnvFileLoader.resolve(env, "MYSQL_PASSWORD", "root");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setPoolName("tft-knowledge");
        return new HikariDataSource(config);
    }

    private static LlmProvider cloudLlm(Map<String, String> env) {
        boolean enabled = Boolean.parseBoolean(EnvFileLoader.resolve(env, "LLM_ENABLED", "true"));
        String apiKey = EnvFileLoader.resolve(env, "LLM_API_KEY", "");
        String baseUrl = EnvFileLoader.resolve(env, "LLM_BASE_URL", "");
        String modelId = EnvFileLoader.resolve(env, "LLM_MODEL_ID", "");
        if (!enabled || apiKey.isBlank() || baseUrl.isBlank() || modelId.isBlank()) {
            log.warn("Cloud LLM disabled or incomplete config; using stub");
            return new StubCloudLlmProvider();
        }
        return new OpenAiCompatibleLlmProvider(baseUrl, apiKey, modelId, "zhipu-or-openai-compatible");
    }

    private static EmbeddingProvider embeddingProvider(Map<String, String> env, int dims) {
        boolean enabled = Boolean.parseBoolean(EnvFileLoader.resolve(env, "EMBEDDING_ENABLED", "true"));
        String apiKey = EnvFileLoader.resolve(env, "EMBEDDING_API_KEY", "");
        String baseUrl = EnvFileLoader.resolve(env, "EMBEDDING_BASE_URL", "");
        String modelId = EnvFileLoader.resolve(env, "EMBEDDING_MODEL_ID", "");
        if (!enabled || apiKey.isBlank() || baseUrl.isBlank() || modelId.isBlank()) {
            return new HashEmbeddingProvider();
        }
        return new OpenAiCompatibleEmbeddingProvider(baseUrl, apiKey, modelId, "zhipu-embedding", dims);
    }

    private static WebSearchProvider webSearchProvider(Map<String, String> env, String preferred) {
        String tavily = EnvFileLoader.resolve(env, "TAVILY_API_KEY", "");
        String serp = EnvFileLoader.resolve(env, "SERPAPI_API_KEY", "");
        if (tavily.isBlank() && serp.isBlank()) {
            return new StubWebSearchProvider();
        }
        String key = preferred == null || preferred.isBlank() ? "tavily" : preferred;
        if ("serpapi".equalsIgnoreCase(key) && serp.isBlank() && !tavily.isBlank()) {
            key = "tavily";
        }
        if ("tavily".equalsIgnoreCase(key) && tavily.isBlank() && !serp.isBlank()) {
            key = "serpapi";
        }
        try {
            return new ConfigurableWebSearchProvider(key, tavily, serp);
        } catch (RuntimeException ex) {
            log.warn("Web search provider init failed, using stub: {}", ex.toString());
            return new StubWebSearchProvider();
        }
    }
}
