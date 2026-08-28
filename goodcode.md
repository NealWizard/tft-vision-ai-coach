# Good Code Examples

> 更新时间：2026-08-27 15:25 +08:00

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
