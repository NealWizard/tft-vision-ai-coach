package com.tft.coach.decision.pipeline;

import com.tft.coach.decision.agent.AugmentAgent;
import com.tft.coach.decision.agent.CompositionAgent;
import com.tft.coach.decision.agent.DomainAgent;
import com.tft.coach.decision.agent.EconomyAgent;
import com.tft.coach.decision.agent.ItemAgent;
import com.tft.coach.decision.agent.ShopAgent;
import com.tft.coach.decision.agent.TransitionAgent;
import com.tft.coach.decision.candidate.CandidateSet;
import com.tft.coach.decision.candidate.CandidateSetValidator;
import com.tft.coach.decision.candidate.DecisionType;
import com.tft.coach.decision.llm.ReasoningGrounder;
import com.tft.coach.decision.rag.DecisionRagContext;
import com.tft.coach.decision.risk.RiskAnnotator;
import com.tft.coach.decision.sim.SimulatorAnnotator;
import com.tft.coach.knowledge.llm.ChatModelGateway;
import com.tft.coach.knowledge.platform.KnowledgePlatform;
import com.tft.coach.meta.MetaQuery;
import com.tft.coach.meta.MetaService;
import com.tft.coach.state.gamestate.GameState;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Fixed Batch A pipeline. Not a P5 Planner.
 */
public final class DecisionPipeline {

    private final MetaService metaService;
    private final DecisionRagContext ragContext;
    private final Map<DecisionType, DomainAgent> agents;
    private final CandidateSetValidator validator;
    private final KnowledgePlatform knowledge;
    private final RiskAnnotator riskAnnotator = new RiskAnnotator();
    private final SimulatorAnnotator simulatorAnnotator = new SimulatorAnnotator();
    private final ReasoningGrounder reasoningGrounder;

    public DecisionPipeline(
            MetaService metaService,
            DecisionRagContext ragContext,
            Map<DecisionType, DomainAgent> agents,
            KnowledgePlatform knowledge,
            ChatModelGateway chatGateway
    ) {
        this.metaService = Objects.requireNonNull(metaService, "metaService");
        this.ragContext = Objects.requireNonNull(ragContext, "ragContext");
        this.agents = Map.copyOf(agents);
        this.validator = new CandidateSetValidator();
        this.knowledge = Objects.requireNonNull(knowledge, "knowledge");
        this.reasoningGrounder = new ReasoningGrounder(chatGateway);
    }

    public static DecisionPipeline createDefault(KnowledgePlatform knowledge) {
        return createDefault(knowledge, ChatModelGateway.mock());
    }

    public static DecisionPipeline createDefault(KnowledgePlatform knowledge, ChatModelGateway chatGateway) {
        MetaService meta = MetaService.createDefault();
        DecisionRagContext rag = new DecisionRagContext(knowledge.ragApi());
        Map<DecisionType, DomainAgent> agents = new LinkedHashMap<>();
        agents.put(DecisionType.SHOP, new ShopAgent());
        agents.put(DecisionType.ECONOMY, new EconomyAgent());
        agents.put(DecisionType.COMPOSITION, new CompositionAgent());
        agents.put(DecisionType.ITEM, new ItemAgent());
        agents.put(DecisionType.AUGMENT, new AugmentAgent());
        agents.put(DecisionType.TRANSITION, new TransitionAgent());
        return new DecisionPipeline(meta, rag, agents, knowledge, chatGateway);
    }

    public MetaService metaService() {
        return metaService;
    }

    public CandidateSet analyze(GameState state, AnalyzeRequest request) {
        Instant started = Instant.now();
        DecisionGuard.requireGameState(state);
        String correlationId = request.correlationId() == null || request.correlationId().isBlank()
                ? "corr-" + UUID.randomUUID()
                : request.correlationId();
        MetaQuery query = new MetaQuery(
                state.patch(),
                request.region(),
                request.timeWindow(),
                request.rank(),
                request.queue());
        MetaService.SearchResult meta = metaService.search(query, Instant.now());
        String fingerprint = GameStateFingerprint.sha256(state);
        DecisionType type = request.decisionType() == null ? DecisionType.COMPOSITION : request.decisionType();
        List<String> ragEvidence = ragContext.retrieveEvidence(
                state.patch(),
                "case " + type.name() + " " + state.patch());
        DomainAgent agent = agents.getOrDefault(type, agents.get(DecisionType.COMPOSITION));
        CandidateSet set = agent.advise(
                state,
                new DomainAgent.Context(correlationId, meta, ragEvidence, fingerprint, knowledge));
        set = riskAnnotator.annotate(set, state);
        set = simulatorAnnotator.attach(
                set,
                state,
                SimulatorAnnotator.rules(knowledge),
                SimulatorAnnotator.odds(knowledge));
        set = reasoningGrounder.apply(set);
        long latency = Duration.between(started, Instant.now()).toMillis();
        CandidateSet timed = new CandidateSet(
                set.schemaVersion(),
                set.candidateSetId(),
                set.decisionType(),
                set.basedOn(),
                set.candidates(),
                set.degraded(),
                set.degradedReasons(),
                new CandidateSet.TraceInfo(
                        set.trace().correlationId(),
                        set.trace().agentRunId(),
                        set.trace().status(),
                        (int) Math.min(Integer.MAX_VALUE, latency)));
        validator.requireValid(timed);
        return timed;
    }

    public record AnalyzeRequest(
            String region,
            String timeWindow,
            String rank,
            String queue,
            String correlationId,
            DecisionType decisionType
    ) {
        public static AnalyzeRequest defaults() {
            return new AnalyzeRequest("global", "24h", null, null, null, DecisionType.COMPOSITION);
        }
    }
}
