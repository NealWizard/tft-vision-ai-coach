# Good Code Examples

> 更新时间：2026-08-31 14:50 +08:00

## 1. Feature Flag 默认关闭 Live（P0-FOUND-FeatureFlag-001）

```java
ConfigurationProperties(prefix = "tft.flags")
public class FeatureFlags {
    private boolean offlineLab = true;
    private boolean postGame = true;
    private boolean preGameMeta = true;
    private boolean liveExperiment = false;
    private boolean liveDynamicRecommendation = false;
    private boolean liveOpponentAnalysis = false;
}
```

要点：默认值写在代码里，配置只做覆盖；Live 能力必须显式打开。

## 2. Correlation 可查询调用链（P0-FOUND-Observability-001）

```java
CorrelationContext ctx = traceService.start(correlationId);
AgentRun run = traceService.complete(ctx, agentId, version, started, "ok", toolCalls);
List<AgentRun> chain = traceService.findByCorrelationId(ctx.correlationId());
```

要点：日志字段固定包含 `correlation_id / latency_ms / version / status`。

## 3. Schema 先行（P0-FOUND-Contract-001 / P0-FOUND-AgentContract-001）

Agent 样例必须通过 `agent-contract.schema.json` 校验；Domain Agent 不允许输出单一强制动作，必须是 `candidates[]`。

## 4. Vision FrameSource SPI + 侧车降级（P2-VISION-Frame-001）

```java
public interface FrameSource extends AutoCloseable {
    Optional<VisionFrame> nextFrame();
    FrameSourceMetadata metadata();
}

SidecarHealthResult result = new SidecarClient(SidecarClientConfig.defaults()).health();
// 侧车未启动：degraded=true，不拖垮 mvn test / orchestrator
```

要点：上层只依赖 `FrameSource`；未知分辨率抛 `UNSUPPORTED_PROFILE`；Observation 保持 `schema_version=1.0.0` 并可选扩展 `raw_value` 等字段。

## 5. 数值 OCR 归一化（P2-VISION-OCR-001 链路）

```java
Optional<Object> value = NumericNormalizer.normalize("player.gold", "4l");
// 41；Java 按 VisionProfile crop 后把小图交给 sidecar PaddleOCR
```

要点：CI 不装 Paddle；未装模型时 `/vision/analyze` 返回 `MODEL_NOT_READY`；受控集 ≥97% 需标注截图后再关门。

## 6. Observation → GameState（P2-STATE-Builder-001）

```java
GameState state = new GameStateBuilder().build("match-1", "set18-18.1", observations);
List<GameStateDiff.Event> events = GameStateDiff.diff(before, after);
MatchTimeline.Timeline timeline = MatchTimeline.fromStates(List.of(before, after));
```

要点：confidence 低于 0.80 的 Observation 在融合阶段丢弃；Cloud Vision 仅 `tft.vision.cloud.enabled=true` 且低置信度才可能调用。

## 7. CandidateSet + ChatModelGateway（P3 V1.1）

```java
CandidateSet set = DecisionPlatform.createDefault().pipeline()
    .analyze(gameState, DecisionPipeline.AnalyzeRequest.defaults());
// 2～3 候选；LLM 只填 reasoning，不改 score
ChatModelGateway mock = ChatModelGateway.mock();
ChatModelGateway cloud = ChatModelGateway.openAiCompatible(baseUrl, key, model, "openai");
```

要点：Domain Agent 不 import 厂商 SDK；无 GameState/Patch 拒绝决策；Simulator 只投影金币/利息/商店赔率。
