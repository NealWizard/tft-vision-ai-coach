# TFT Vision AI Coach 整合需求与开发规格 V3.1

> 文档版本：V3.1  
> 整合日期：2026-08-22  
> 状态：研发基线  
> Canonical Roadmap：`TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html`

## 1. 文档定位

### 1.1 目的

本文将产品需求、工程架构、数据契约、接口边界、技术栈、分期范围和验收门禁整合为一份可独立阅读的开发规格。它回答四个问题：

1. 产品为什么做、服务什么场景、哪些边界永远不能突破；
2. P0～P7 每阶段交付哪些 Canonical Capability；
3. 数据、RAG、模型、Agent、视觉和基础设施如何协作及降级；
4. 如何以任务 ID、证据链和可自动验证的门禁判断完成。

本文不是新增 Roadmap，不扩充 V3.1 的 116 个任务，也不替代任务状态维护。

### 1.2 权威顺序

发生冲突时按以下顺序裁决：

1. **V3.1 Roadmap**：Task ID、Phase、Capability、依赖、优先级和任务验收的唯一最高基线；
2. 《整合版需求与总体开发蓝图 v2.0》：产品总纲、九大工程域、平台路线；
3. 《完整需求文档 PRD v2.0》：用户场景、运行模式、产品指标、风险与合规；
4. 《V2.x 分期开发实施与可开发规格 v2.0》：开发顺序、阶段交付和门禁；
5. 《V1.1 可开发规格总纲与细则》：接口、数据模型、技术栈和测试细节。

低优先级来源只补全 V3.1 未定义的细节，不得改写 V3.1 的任务范围。

### 1.3 适用范围

适用于产品、后端、数据、AI、视觉、客户端、测试和运维团队，覆盖 OFFLINE LAB、POST-GAME、PRE-GAME 以及被隔离的 LIVE-EXPERIMENT。V1.0 的可发布主线是前三种模式；Live 不是主线承诺。

### 1.4 术语

- **Canonical Task**：V3.1 中具有唯一 ID、阶段、依赖和验收的任务。
- **Canonical Entity**：跨来源统一后的 TFT 实体，如 Champion、Trait、Item、Augment、Mechanic、Rule、Patch。
- **Patch**：数据生效版本及其有效期、当前状态和隔离边界。
- **Raw Snapshot**：来源响应的不可变原始文件及其元数据，不被后续抓取覆盖。
- **Evidence**：事实或结论的来源引用、抓取时间、Patch、样本量和来源类型。
- **Confidence**：感知、数据或推断结果的置信度及其计算依据。
- **Tool**：确定性、结构化、可独立测试的能力；规则和概率优先由 Tool 提供。
- **Agent**：围绕一个领域目标编排 Tool、RAG 和模型的受约束执行单元。
- **RAG**：带元数据过滤、召回、重排和引用的检索增强生成链路。
- **0-LLM**：无需调用大模型即可由结构化数据或确定性 Tool 完成的路径。
- **Provider SPI**：将 Chat、Vision、Embedding、Reranker 与具体厂商隔离的统一接口。

## 2. 产品定义与安全边界

### 2.1 产品目标

TFT Vision AI Coach 是只读的 Personal TFT AI Coach，而非阵容查询器或自动操作工具。目标闭环为：

`可见画面/公开数据 → Observation → Canonical GameState → Knowledge/Meta → 多方案建议 → 玩家反馈 → Outcome → Replay → Player Profile`

成功不以“模型会说话”为标准，而以知识可追溯、状态可验证、建议可解释、反馈可回放和个人画像可复现为标准。

### 2.2 用户场景

- 赛前查询当前 Patch 的规则、热门阵容、装备、强化与趋势；
- 导入截图或录像，在离线环境验证 OCR、状态重建和模型版本；
- 赛后逐回合重建 Timeline，定位关键决策点并生成复盘；
- 对建议选择采用、否定、部分采用、忽略或纠错，记录实际行动；
- 多局后比较“通用统计最优”和“个人历史最优”，识别重复错误；
- 研发人员使用固定 Ground Truth 比较数据、RAG、视觉和 Agent 版本。

### 2.3 运行模式

| 模式 | 输入 | 核心能力 | 产品定位 |
|---|---|---|---|
| OFFLINE LAB | 截图、视频文件、固定数据集 | OCR、GameState、Agent、可回放评测 | 首选研发环境 |
| POST-GAME | 录像、截图、人工确认 | Timeline、决策评估、报告、个人学习 | V1.0 核心 |
| PRE-GAME | 官方及合规公开数据 | Patch、规则、Meta、知识问答 | V1.0 核心 |
| LIVE-EXPERIMENT | Camera/Desktop Capture | 实时视觉与实验性建议 | P6 独立、默认关闭、须政策门禁 |

### 2.4 硬安全边界

任何阶段均禁止：

- 读取游戏进程内存、进程附加、DLL 注入、API Hook；
- 拦截、解密或修改游戏网络通信；
- 修改 Riot/TFT 客户端文件或安装目录；
- 模拟键鼠、自动点击、自动购买、刷新、升级、装备、站位或选择强化；
- 以绕过 Vanguard、反作弊或平台政策为目标；
- 将“纯视觉只读”等同于已获得 Riot 许可或不会受处罚。

工程依赖扫描、静态扫描、行为测试和人工审查必须共同阻断违规路径。Live Dynamic Recommendation 与 Live Opponent Analysis 必须各自独立 Feature Flag，默认 `false`。

## 3. 总体架构

### 3.1 分层数据流

```text
Game Client（黑盒）
  → FrameSource（Screenshot / Video / Camera / Desktop）
  → OpenCV / ROI / PaddleOCR / Icon Model
  → Observation → GameState Builder → Validation / Diff / Timeline
  → Structured Tools + Meta + RAG
  → Domain Agents → Candidate Set
  → Model Router（0-LLM / Local / Cloud）
  → Response / Evidence / Confidence
  → Feedback / Outcome → Replay / Personal RAG
```

Vision 只负责感知，不能直接做决策；LLM 只负责受约束的综合与解释，不能作为事实数据库或独立计算 TFT 精确数值。

### 3.2 九大工程域

| 工程域 | 职责 |
|---|---|
| `tft-data` | Source Adapter、Snapshot、标准化、Patch、冲突、Evidence |
| `tft-knowledge` | Rule/Entity/Probability Tool、Knowledge RAG、LLM Gateway、Knowledge/Research Agent |
| `tft-vision` | Frame、ROI、OCR、图标/实体识别、Vision Benchmark |
| `tft-state` | Observation、GameState、融合、Diff、Timeline |
| `tft-meta` | Meta Snapshot、评分、趋势、Patch Impact |
| `tft-decision` | Shop、Economy、Composition、Item、Augment、Transition、Risk、Simulator |
| `tft-replay` | Match 生命周期、关键节点、评估、报告 |
| `tft-learning` | Feedback、Player Profile、Mistake Pattern、Personal Coach |
| `tft-orchestrator` | Registry、路由、Planner、上下文、冲突、响应、Trace、评测 |

逻辑能力可以细分，但物理工程域保持九个，不按每个 Tool/Agent 拆微服务。

### 3.3 Provider SPI 与模型路线

业务代码不得直接依赖 OpenAI、DeepSeek 或其他厂商 SDK：

- `ChatModelGateway`：OpenAI-compatible/HTTP；统一超时、重试、Fallback；
- `VisionProvider`：Image/Frame → 结构化 JSON；本地 OCR/CV 为默认；
- `EmbeddingProvider`：Text → Vector；记录 provider/model/version；
- `RerankerProvider`：Query + Documents → Score；失败回退原始排序。

演进路线：

1. **0-LLM**：规则、概率、结构化查询、Diff 必须优先走确定性 Tool；
2. **Local**：本地 Embedding、Reranker 或小模型用于隐私、成本和离线降级；
3. **Cloud**：P1 首次用于 Knowledge Agent/Research/机制解释；P3 才进入决策综合；
4. **Router**：P5 才按复杂度、延迟、隐私和成本自动选择 0-LLM/Local/Cloud。

## 4. P0：工程地基与安全边界

**阶段目标**：形成可构建、可测试、可追踪、无云也能启动且安全边界可由 CI 阻断的工程基线。

**Canonical 范围（10 项）**：

- `P0-FOUND-Repo-001` 九大工程域 mono-repo；
- `P0-FOUND-Build-001` Java 21 / Spring Boot 3 / Maven；
- `P0-FOUND-CI-001` compile、unit test、static check、package；
- `P0-FOUND-Contract-001` Entity/Patch/Evidence/Confidence/Observation/GameState Schema；
- `P0-FOUND-AgentContract-001` Input/Output/Tool/Timeout/Fallback/Confidence/Trace；
- `P0-FOUND-FeatureFlag-001` OFFLINE/POST-GAME/PRE-GAME/LIVE Flag；
- `P0-FOUND-Observability-001` TraceId/AgentRun/ToolCall；
- `P0-FOUND-Safety-001` 安全边界扫描；
- `P0-FOUND-TestData-001` 六类测试数据 Manifest；
- `P0-FOUND-Degrade-001` 本地优先与云端降级矩阵。

**技术与接口**：公共 JSON Schema 版本化；`correlation_id` 串联 AgentRun 与 ToolCall；配置可切换模式但 Live 默认关闭。Agent 输出必须包含候选集、Evidence、Confidence 和降级信息。

**存储部署**：本阶段只要求契约和最小可运行骨架，不把数据库、RAG 或 Cloud LLM 作为启动前提。

**依赖**：以 Repo→Build→Contract/CI→AgentContract/FeatureFlag→Observability/Safety/Degrade 的 V3.1 依赖链为准。

**验收门禁**：干净环境一键构建；Schema 发布；任一调用可完整追踪；无云模式启动；违规依赖样例可被 CI 阻断；六类数据集有 Manifest。

**明确不做**：业务知识、视觉识别、复杂 Agent、Live 能力。

## 5. P1：Knowledge & Data Platform

### 5.1 阶段目标与边界

交付第一个可独立使用的平台能力：任意后续 Agent 能可靠回答“实体是什么、规则是什么、版本改了什么、证据在哪里”。P1 共 **35 项**，仅保留一个 Knowledge Agent：`P1-AGENT-Knowledge-001`。Knowledge Agent 是 Structured Tools + Knowledge RAG + Cloud LLM 的组合，不再复制第二个同名 Agent。

P1 不参与局内复杂决策，不训练万能模型，不让 Agent 直接抓网页，不把社区内容或 LLM 推断覆盖为官方事实。

### 5.2 DATA（12 项）

| Canonical Task | 能力、关键依赖与验收 |
|---|---|
| `P1-DATA-SourceAdapter-001` | 统一抓取、解析、健康检查和缓存降级 SPI；依赖 P0 Contract；所有来源必须经 SPI。 |
| `P1-DATA-Snapshot-001` | 保存原始响应、URL、checksum、captured_at、source、patch 推断；依赖 SourceAdapter；历史可回放且不覆盖。 |
| `P1-DATA-Riot-001` | Riot/Data Dragon 官方静态数据；依赖 Adapter+Snapshot；指定 Patch 可重复获取并带 Evidence。 |
| `P1-DATA-Stats-001` | 第一统计源的 Comp/Unit/Item/Augment/Trend；依赖 Adapter+Snapshot；Meta DTO、样本量和时间完整。 |
| `P1-DATA-Stats-002` | 第二统计源用于交叉验证；依赖 Stats-001；同一查询保留多来源。 |
| `P1-DATA-EntityResolve-001` | Source ID/别名映射到 Canonical ID；依赖 Riot+Stats-001；未知实体入待处理队列。 |
| `P1-DATA-Normalize-001` | 多源数据归一为 Canonical DTO 并保留 raw payload；依赖 EntityResolve；原始字段不丢失。 |
| `P1-DATA-Patch-001` | 管理 Set/Patch、生效时间、当前版本、TTL；依赖 Riot；所有查询显式 Patch。 |
| `P1-DATA-Patch-002` | from/to Patch 实体新增、删除、数值变化及影响；依赖 Normalize+Patch。 |
| `P1-DATA-Conflict-001` | 冲突不覆盖，进入 Conflict Queue；依赖 Normalize+Patch；可查看双方来源、Patch 和时间。 |
| `P1-DATA-Evidence-001` | 保存 source_url、captured_at、patch、sample_size、source_type、reference_id；依赖 Snapshot+Normalize；事实可反查。 |
| `P1-DATA-Quality-001` | Source Reliability/Freshness/SampleSize 评分；依赖 Conflict+Evidence；公式配置化且可解释。 |

**Raw Snapshot 文件存储**：

- P1 采用不可变文件存储作为原始事实底稿，不将大段 raw payload 塞入关系表；
- 建议逻辑键为 `source/set/patch/captured_at/checksum`，正文文件与 Manifest 分离；
- Manifest 在 MySQL 8 保存 snapshot_id、URI、content_type、checksum、captured_at、source、推断 Patch、解析状态；
- 写入采用“新文件+新版本”，禁止覆盖；checksum 用于去重与完整性校验；
- Adapter 故障时可使用最近一次合格 Snapshot 降级，并明确 freshness/stale 状态；
- P1 可部署为受控本地/共享文件卷；对象存储迁移不得改变 Snapshot URI 契约。

统计站来源只定义 Adapter 能力，不宣称 OP.GG、LoLChess、Tactics.tools、MetaTFT 存在可直接使用的公开 REST URL。**真实接口与合规需在 Adapter 验收时验证**，验收同时核对授权、抓取条款和频率限制。

### 5.3 KNOW（8 项）

| Canonical Task | 能力、关键依赖与验收 |
|---|---|
| `P1-KNOW-Rules-001` | 等级、金币、利息、经验、商店、回合、战斗及特殊规则；依赖 Patch+Evidence；规则值按 Patch 查询。 |
| `P1-KNOW-Champion-001` | 属性、费用、星级成长、技能、机制、历史版本；依赖 Normalize+Evidence；支持 get/search 并返回 Evidence。 |
| `P1-KNOW-Trait-001` | 单位集合、阈值、效果、历史版本；依赖 Normalize；阈值结构化。 |
| `P1-KNOW-Item-001` | 组件、成装、属性、被动、合成、交互；依赖 Normalize+Evidence；支持 get/search。 |
| `P1-KNOW-Augment-001` | 类型、条件、效果、限制、历史版本；依赖 Normalize+Evidence；静态定义与动态统计分离。 |
| `P1-KNOW-Mechanic-001` | 奇遇、Portal、特殊选择、特殊羁绊、临时规则；依赖 Normalize+Patch；与 Augment 解耦。 |
| `P1-KNOW-Probability-001` | 商店、滚牌、三星等概率；依赖 Rules；公式可验证且边界单测通过。 |
| `P1-KNOW-UnitPool-001` | 牌池基础定义和统计配置；依赖 Rules；按 Set/Patch 查询且不依赖 LLM。 |

Tool 接口概览：`get/searchChampion`、`getTrait`、`getItem`、`getAugment`、`getMechanic`、`getRule`、`getPatchDiff`、`getEvidence`。所有结构化查询必须接受显式 Patch，禁止默认混用旧版本。

### 5.4 RAG（9 项）

| Canonical Task | 能力、关键依赖与验收 |
|---|---|
| `P1-RAG-Ingest-001` | URL/HTML/Text 文档流，保留 source/patch/captured_at；依赖 Adapter+Snapshot。 |
| `P1-RAG-Chunk-001` | 按章节、实体、机制、表格语义切分；依赖 Ingest；含 document_id/section/patch/source。 |
| `P1-RAG-Embedding-001` | 本地/云 Embedding Provider SPI；依赖 Chunk+P0 Degrade；记录版本和 Provider。 |
| `P1-RAG-Vector-001` | PostgreSQL+pgvector；依赖 Embedding；按 patch/set/source_type/region/rank 过滤。 |
| `P1-RAG-Hybrid-001` | OpenSearch BM25 + pgvector 组合召回；依赖 Vector；100 条基准输出 Recall@K。 |
| `P1-RAG-Rerank-001` | TopK 二次排序 Provider SPI；依赖 Hybrid；有 baseline，失败回退原排序。 |
| `P1-RAG-Metadata-001` | 强制 patch/set/source_type/captured_at 等；依赖 Chunk+Patch；缺元数据不得进生产索引。 |
| `P1-RAG-API-001` | retrieve/rerank/citation/metadata filter；依赖 Rerank+Metadata；Agent 不直连向量库。 |
| `P1-RAG-Eval-001` | 规则、机制、Patch、装备、趋势 100+ 条评测；依赖 Hybrid；输出 Recall@K/MRR/Citation Coverage。 |

每个 Chunk 至少包含 `document_id、section、set、patch、source_type、source_url、captured_at、sample_size、status、embedding_provider、embedding_model_version`。Patch/Set 过滤在 Retrieval 层强制执行，不能依赖 Prompt 自觉过滤。

### 5.5 LLM（4 项）

| Canonical Task | 能力、关键依赖与验收 |
|---|---|
| `P1-LLM-Gateway-001` | Cloud LLM Gateway、Provider 切换、超时、重试、Fallback；依赖 P0 Degrade；上层零厂商 SDK。 |
| `P1-LLM-Prompt-001` | Prompt 模板、变量 Schema、版本、发布状态；依赖 AgentContract；AgentRun 可反查版本。 |
| `P1-LLM-Meter-001` | tokens、latency、cost_estimate；依赖 Gateway+Observability；每次云调用可追踪聚合。 |
| `P1-LLM-Guard-001` | Prompt Injection、超长上下文、无关/敏感数据上传防护；依赖 Gateway+Safety；所有云调用必经 Guard。 |

Cloud LLM Gateway 的降级顺序：结构化 Tool/缓存答案 → Hybrid RAG 原始引用 → 本地模型（若已配置）→ 明确标记无法生成自然语言综合。Embedding 故障可读取已有向量并退到 BM25；Reranker 故障使用融合召回排序；OpenSearch 故障可退到 pgvector；向量库故障可退到 BM25；两者均不可用时只使用结构化 Tool。

### 5.6 AGENT（2 项）

| Canonical Task | 能力、关键依赖与验收 |
|---|---|
| `P1-AGENT-Knowledge-001` | 唯一 Knowledge Agent v1；编排 Rules/Champion/Trait/Item/Augment/Mechanic、RAG、Gateway、Guard、Evidence；100 条问答回归，事实/RAG/推断分层，关键事实均带 Evidence。 |
| `P1-AGENT-Research-001` | 最新 Patch、社区趋势、争议主题的搜索与交叉验证；依赖 Knowledge Agent+RAG API；输出来源、时间、Patch 判断、可信度，不能覆盖官方事实。 |

### 5.7 P1 存储、部署与门禁

- **MySQL 8**：Canonical Entity、Source 映射、Patch、Snapshot Manifest、Evidence、Conflict Queue、质量评分和结构化知识；
- **PostgreSQL + pgvector**：Chunk 向量、Embedding 版本与 metadata filter；
- **OpenSearch**：文档正文、BM25、字段过滤和检索诊断；
- **文件存储**：Raw Snapshot 原文和 Manifest 引用；本地/共享卷先行；
- **Cloud LLM Gateway**：独立边界服务或模块，通过 HTTP/SPI 接入；业务层不持有厂商客户端。

阶段门禁：35 个任务按依赖完成；同实体跨源可验证映射；旧 Patch 不静默混入；100+ RAG 评测输出指标；100 条 Knowledge QA 回归通过；任何事实可追溯；云端全部关闭时结构化知识仍可查询。

## 6. P2：Vision & GameState Platform

**阶段目标**：先“看懂画面”，再谈建议；交付 Screenshot/Video 的可审计状态重建，并仅对低置信度结果提供 Cloud Vision Fallback。

**Canonical 范围（13 项）**：

- VISION：`P2-VISION-Frame-001`、`P2-VISION-ROI-001`、`P2-VISION-OCR-001`、`P2-VISION-OCR-002`、`P2-VISION-Board-001`、`P2-VISION-Entity-001`、`P2-VISION-Fallback-001`、`P2-VISION-Benchmark-001`；
- STATE：`P2-STATE-Observation-001`、`P2-STATE-Builder-001`、`P2-STATE-Fusion-001`、`P2-STATE-Diff-001`、`P2-STATE-Timeline-001`。

**技术路线**：统一 `VisionFrame/FrameSource`；OpenCV ROI；PaddleOCR；ONNX/Icon Model；Observation 携带 field/value/confidence/source/timestamp/ROI；多帧融合与去抖后构建 GameState，再生成 Diff 和 Timeline。Cloud Vision 只处理低置信度二次确认，关闭后主链仍可运行。

**关键接口/数据**：`VisionFrameSource.nextFrame()`、`GameStateBuilder.build/validate()`；GameState 覆盖 Player、Shop、Board、Bench、Items、Traits、Augments、Mechanics。低置信度不得静默污染状态。

**存储部署**：MySQL 保存结构化 Observation/状态索引；大体积截图/视频默认本地短期保存；测试集与模型版本通过 Manifest 管理。

**依赖**：P0 Contract；P1 Champion/Item/Mechanic；Fallback 依赖 P1 LLM Guard。

**验收门禁**：OCR 关键字段受控集 `>=97%`；五卡顺序/名称/费用正确；Board P0 字段达到基线；Observation 可审计；视频可重建连续 Timeline；Benchmark 输出 Accuracy/Precision/Recall/Low-confidence。

**明确不做**：局内决策、自动操作、默认 Cloud Vision、Live 生产输入。

## 7. P3：Meta & Decision Platform

**阶段目标**：用 Patch 隔离的 Meta、确定性 Tool、Decision RAG 和受 Grounding 约束的 Cloud LLM 生成 2～3 个候选方案。

**Canonical 范围（17 项）**：

- META：`P3-META-Snapshot-001`、`P3-META-Score-001`、`P3-META-Trend-001`、`P3-META-PatchImpact-001`；
- RAG：`P3-RAG-DecisionContext-001`、`P3-RAG-Case-001`；
- DECISION：`P3-DECISION-Shop-001`、`P3-DECISION-Economy-001`、`P3-DECISION-Composition-001`、`P3-DECISION-Item-001`、`P3-DECISION-Augment-001`、`P3-DECISION-Transition-001`、`P3-DECISION-Risk-001`、`P3-DECISION-Simulator-001`、`P3-DECISION-Candidate-001`；
- LLM：`P3-LLM-Reasoning-001`、`P3-LLM-Grounding-001`。

**技术路线**：Meta Snapshot 绑定 Patch/Region/Rank/Queue；评分由 Reliability、SampleSize、Freshness、Patch、Rank 可解释组合；Domain Agent 只消费 GameState、Tool、Meta、RAG；LLM 不自行计算规则和概率。

**接口/数据**：统一 Candidate Set，候选含 `score、confidence、reasoning、risk、evidence、expected_tradeoff`；Decision Context 带 patch filter/source/relevance/evidence，检索不直接做决策。

**存储部署**：MySQL 保存 Meta Snapshot 与 Candidate；OpenSearch/pgvector 复用 P1；高量时序统计可在评测证明必要后引入 ClickHouse，不作为 P3 Canonical 前置。

**验收门禁**：所有 Agent 符合 Candidate Schema；每个 Agent 满足自身 V3.1 验收；无 Evidence 时标记不确定；Grounding 防未来信息倒灌和 Prompt Injection。

**明确不做**：单一“必须动作”、在线自动执行、让 LLM 直接生成 TFT 数值事实。

## 8. P4：Replay & Personal Coach

**阶段目标**：建立对局—回合—建议—反馈—结果的完整生命周期，基于“当时可知信息”复盘，并形成可版本化个人画像与 Personal RAG。

**Canonical 范围（12 项）**：

- REPLAY：`P4-REPLAY-Model-001`、`P4-REPLAY-Feedback-001`、`P4-REPLAY-DecisionPoint-001`、`P4-REPLAY-Evaluation-001`、`P4-REPLAY-Report-001`；
- LEARN：`P4-LEARN-Profile-001`、`P4-LEARN-Mistake-001`、`P4-LEARN-FeedbackClassify-001`、`P4-LEARN-PersonalCoach-001`；
- RAG：`P4-RAG-PersonalCase-001`、`P4-RAG-PersonalSearch-001`；
- EVAL：`P4-EVAL-PersonalBenchmark-001`。

**技术路线**：Feedback 分为采用、否定、忽略、部分采用、纠错；Outcome 区分即时、中期、最终；错误分为 Perception/State/Knowledge/Decision；Personal Case 保存 patch/state/decision/outcome/player_choice。

**存储部署**：MySQL 8 保存 Match、Round、Recommendation、Feedback、Outcome、Profile Version；pgvector 保存 Personal Case；原始画面默认不长期保留。

**依赖**：P2 Timeline、P3 Candidate、P1 Embedding；Personal Coach 依赖 Profile、PersonalSearch、Mistake 和 P3 Reasoning。

**验收门禁**：完整导入一局并生成报告；20+ 局形成基础画像；错误模式满足统计阈值，禁止单局定性；比较无历史、Profile、Personal RAG 三种策略。

**明确不做**：玩家历史覆盖通用事实、在线训练、未来信息倒灌、实时对手 Scout。

## 9. P5：Orchestrator & AI Routing Platform

**阶段目标**：此时才将成熟 Domain Capability 统一编排，建立 Registry、Planner、Model Router、RAG Router、冲突处理、Trace 和评测。

**Canonical 范围（13 项）**：

- ORCH：`P5-ORCH-ToolRegistry-001`、`P5-ORCH-AgentRegistry-001`、`P5-ORCH-RAGRegistry-001`、`P5-ORCH-ModelRegistry-001`、`P5-ORCH-IntentRouter-001`、`P5-ORCH-ContextBuilder-001`、`P5-ORCH-Planner-001`、`P5-ORCH-Conflict-001`、`P5-ORCH-Response-001`；
- ROUTER：`P5-ROUTER-Model-001`、`P5-ROUTER-RAG-001`；
- OBS/EVAL：`P5-OBS-Trace-001`、`P5-EVAL-Harness-001`。

**技术路线**：Registry 管理 Schema、权限、版本、健康度、成本和上下文限制；Context Builder 只装配最小必要信息；Planner 控制调用顺序、并发、超时和预算；Model Router 在 0-LLM/Local/Cloud 中选择；RAG Router 强制 Patch/Set/Region 过滤。

**接口/数据**：无 Schema 的 Tool/Agent 不可运行；所有 RAG 调用经 Registry；Response 输出 2～3 候选、风险、Evidence 和解释；Trace 展示 Orchestrator→Agent→Tool→RAG→LLM。

**存储部署**：Registry、Trace、Eval Result 进入关系存储；模型和索引只登记引用与版本，不在业务表保存密钥。

**验收门禁**：知识查询和复杂决策两条链路端到端运行；简单事实不调用高级云模型；云故障可降级；固定数据集比较 Recall/MRR/Citation/Latency/Cost。

**明确不做**：把所有能力微服务化、绕过 metadata filter、无预算的无限 Agent 循环。

## 10. P6：Live Experimental Track

**阶段目标**：在政策明确允许、产品注册和具体用例核验通过后，隔离验证实时输入、状态延迟和建议质量；否则停止，P0～P5 产品仍完整可用。

**Canonical 范围（6 项）**：`P6-LIVE-Camera-001`、`P6-LIVE-Desktop-001`、`P6-LIVE-Latency-001`、`P6-LIVE-Benchmark-001`、`P6-LIVE-Flag-001`、`P6-LIVE-PolicyGate-001`。

**技术路线**：Camera/Desktop 复用 FrameSource；Desktop 不访问游戏进程 API；独立 Flag；Benchmark 记录 FPS、P50、P95、丢帧率。

**依赖**：P2 Frame/Fusion、P5 Planner、P0 Safety/FeatureFlag。

**验收门禁**：普通启动不启用 Live；静态扫描通过；受控环境结果可观测；`P6-LIVE-PolicyGate-001` 未明确 Go 时不得继续产品化 Live。

**明确不做**：默认开启、实时 Opponent Analysis、规避检测、将实验延迟目标写成上线承诺。

## 11. P7：Productization

**阶段目标**：将 Offline/Post-Game/Pre-Game 形成可安装、可监控、可回滚、可删除数据并受成本预算约束的 V1.0。

**Canonical 范围（10 项）**：

- RELEASE：`P7-RELEASE-Policy-001`、`P7-RELEASE-Secrets-001`、`P7-RELEASE-Privacy-001`、`P7-RELEASE-Client-001`、`P7-RELEASE-Regression-001`、`P7-RELEASE-Rollback-001`、`P7-RELEASE-V1-001`；
- COST/QUALITY/OBS：`P7-COST-Budget-001`、`P7-QUALITY-Dashboard-001`、`P7-OBS-Ops-001`。

**技术路线**：Tauri 客户端安装/更新/恢复；Secrets 环境注入；按用户/Agent/Provider 预算；质量 Dashboard 聚合幻觉、Citation Coverage、Retrieval Hit、Latency、Cost；应用、Schema、Model、Prompt、RAG Index 可回滚。

**存储部署**：生产数据库备份与迁移；截图/视频本地优先、可删除、默认不长期保存；Dashboard 与告警覆盖 Vision/State/Agent/RAG/LLM/Data。

**验收门禁**：安全材料与政策状态齐全；端到端回归覆盖三种主线模式；完成回滚演练；安装包、部署文档、回归报告、已知问题齐全；Live 仍默认关闭。

**明确不做**：未获政策许可的商业化或 Live 承诺、密钥入库或入仓、不可验证的数据删除。

## 12. 数据契约与来源治理

### 12.1 Canonical ID

实体 ID 固定为 `{type}.{slug}`，例如 `champ.ahri`、`item.guardbreaker`。约束：

- `type` 使用稳定小写枚举；`slug` 使用稳定、可读、与来源无关的标识；
- Source ID、名称、地区语言和别名通过映射表关联，不能直接充当 Canonical ID；
- 未解析实体进入待处理队列，不临时生成会漂移的业务 ID；
- Canonical ID 与 Roadmap Task ID 是两套标识，禁止混用。

### 12.2 Patch、Evidence、Confidence

- **Patch**：至少含 set、patch、effective_from/to、status、TTL；所有知识与统计查询必须显式绑定；
- **Evidence**：至少含 reference_id、source_type、source_url/URI、captured_at、patch、sample_size、checksum；
- **Confidence**：至少含 score、method、threshold/version、reason；低于关键字段阈值时进入确认或降级；
- **事实分层**：Structured Fact、Retrieved Content、Model Inference 必须在输出中清晰区分；
- **冲突**：不做最后写入者覆盖；保留各方证据并进入 Conflict Queue。

### 12.3 数据源优先级

1. Riot 官方 API/Data Dragon/Patch Notes：规则、补丁和静态事实；
2. 验收通过的统计来源：Meta 趋势，必须带 Patch、样本量和时间；
3. 社区/论坛/内容：观点和候选趋势，不能直接成为官方事实；
4. Web Research/LLM Inference：发现、解释和假设，权重最低。

官方数据不一定覆盖统计结论；统计源只能交叉验证，不可改写官方规则。任何来源在 Adapter 验收前均视为“候选来源”。

## 13. RAG 与 LLM 规则

- 精确规则、概率、经济计算只来自结构化 Tool；
- Agent 不得直连 OpenSearch、pgvector 或厂商模型；
- Retrieval 强制 Patch/Set 过滤，Region/Rank/Queue 按任务强制；
- 引用必须能回到原始 Snapshot/文档与抓取时间；
- Prompt 将检索内容视为不可信数据，隔离系统指令，禁止执行文档内指令；
- Guard 做输入长度、敏感信息、无关上下文、Prompt Injection 和输出 Schema 校验；
- Provider 可替换，业务层不依赖厂商 SDK；
- 云调用记录 `provider/model/prompt_version/tokens/latency/cost_estimate/correlation_id/status`；
- 缺 Evidence 时输出“不确定”，不得补写看似合理的事实；
- 缓存键包含 Patch、Provider、Model、Prompt Version 和检索版本，避免跨版本污染。

## 14. API 与 Tool 概览

建议 HTTP 能力边界：

- `POST /api/v1/vision/frames`：提交离线帧；
- `POST /api/v1/state/build`：Observation → GameState；
- `GET /api/v1/matches/{id}`、`/timeline`：对局与时间线；
- `POST /api/v1/recommendations/analyze`：生成 Candidate Set；
- `POST /api/v1/recommendations/{id}/feedback`：提交反馈；
- `GET /api/v1/profile`、`GET /api/v1/replay/{id}`；
- `POST /api/v1/meta/search`、`GET /api/v1/meta/snapshot/{id}`；
- Knowledge Tool：Champion/Trait/Item/Augment/Mechanic/Rule/Probability/UnitPool/PatchDiff/Evidence；
- RAG Tool：`retrieve`、`rerank`、`citation`、`metadataFilter`；
- Provider SPI：Chat、Vision、Embedding、Reranker。

接口样例是边界说明，不代表全部端点已进入 V3.1 Canonical Task；具体实现必须回链到对应 Task。

## 15. 数据库与基础设施分阶段矩阵

| 组件 | P0 | P1 | P2～P4 | P5～P7 |
|---|---|---|---|---|
| MySQL 8 | Schema/连接骨架可选 | Canonical、Patch、Evidence、Manifest、知识 | GameState 索引、Meta、Match、Feedback、Profile | Registry、Trace、预算、运维 |
| 文件存储 | 测试 Manifest | Raw Snapshot 不可变文件 | 截图/视频短期存储 | 保留期、删除、备份；可迁对象存储 |
| PostgreSQL+pgvector | 不需要 | Knowledge Vector | Decision/Personal Case | RAG Registry、索引版本与回滚 |
| OpenSearch | 不需要 | BM25/文档检索 | Decision 文档与诊断 | 质量治理、索引回滚 |
| Redis | 非必需 | 可用于短缓存 | Session/Meta 热缓存 | 限流、预算与运行缓存 |
| Kafka/消息 | 本地队列优先 | 非前置 | 经吞吐验证后按需引入 | 生产异步链路按指标决策 |
| ClickHouse | 不需要 | 不作为 P1 前置 | 大规模 Meta/事件统计按需 | 达到量级门槛后启用 |
| Cloud LLM | 不依赖 | Gateway 首次接入 | P2 Fallback、P3 决策、P4 Coach | Router、预算、治理 |
| Desktop UI | 不需要 | 不需要 | 内部工具可选 | P7 Tauri 正式产品化 |

## 16. 非功能需求、测试与评测

### 16.1 非功能要求

- 可构建：Java 21、Spring Boot 3、Maven 在干净环境通过；
- 可追踪：请求、Agent、Tool、RAG、模型均有 correlation_id；
- 可回放：同输入、知识快照、规则/模型/Prompt 版本可复现或解释差异；
- 可降级：Cloud LLM、Embedding、Reranker、Cloud Vision 任一不可用不阻断核心确定性路径；
- 可维护：Tool 优先、九大工程域收敛、Schema 先行；
- 性能：离线单帧 P95 目标 `<1.5s`；Live `<500ms` 仅为实验目标；
- 隐私：默认日志不存完整截图，调试采集需用户主动开启。

### 16.2 测试与评测

- 数据：Adapter 合同测试、Snapshot checksum、Patch 隔离、Conflict Queue；
- Knowledge：100 条问答回归，核对 Patch、事实层级和 Evidence；
- RAG：100+ 条规则/机制/Patch/装备/趋势集，输出 Recall@K、MRR、Citation Coverage；
- LLM：Prompt Injection、无证据问题、超长上下文、Provider 故障；
- Vision：gold/level/round、shop、board、augment、negative、patch regression 数据集；
- State：多帧融合、状态倒退、Diff、Timeline 连续性；
- Decision：Candidate Schema、多方案、规则计算、Evidence Coverage；
- Replay：禁止未来信息倒灌，验证即时/中期/最终 Outcome；
- Personal：无历史/Profile/Personal RAG 对照实验；
- 安全：内存、注入、Hook、输入模拟、网络拦截的依赖与代码路径扫描；
- E2E：OFFLINE、POST-GAME、PRE-GAME 全链路与回滚演练。

## 17. 隐私与合规

- 本地优先，云端只上传完成任务所需的最小上下文；
- 原始截图/视频默认不长期保存，保留期可配置且用户可验证删除；
- Secrets 不进入代码仓库、日志、Prompt 或普通业务表；
- 外部内容遵守授权、版权、品牌展示、抓取频率和数据使用条款；
- Riot 政策在开发前、测试前、发布前重新核验并记录日期；
- 产品注册、API 使用、商业化和 Live 用例必须分别确认；
- 纯视觉只读是技术边界，不是政策豁免、零处罚或可上线保证。

## 18. 主要风险

- **政策变化**：Live 独立 Flag 和 Go/No-Go，赛后主线不依赖 Live；
- **来源改版/不可用**：Adapter 隔离、健康检查、Snapshot、缓存与多来源；
- **旧 Patch 污染**：Patch Isolation、TTL、Diff、生产索引门禁；
- **多源冲突**：Conflict Queue、Evidence、可解释质量评分；
- **LLM 幻觉/注入**：Tool First、Evidence First、Guard、Grounding；
- **OCR 错误**：Confidence、人工确认、多帧融合、负样本；
- **反馈偏差**：区分纠错、偏好、策略性反对，禁止单局定性；
- **成本与网络波动**：0-LLM、缓存、本地 Provider、预算与自动降级；
- **隐私泄露**：最小上传、短保留、可删除、默认不记录完整画面；
- **过度工程化**：九大工程域、按 Canonical Task 交付、基础设施按指标启用。

## 19. Definition of Done

单个任务完成必须满足其 V3.1 acceptance，并同时满足：

1. 输入、输出和依赖符合已发布 Schema；
2. 单元测试齐全，核心链路有集成测试；
3. 成功、失败、超时和降级可由 Trace/Metric/Log 观察；
4. 事实、Patch、Evidence、Confidence 可回溯；
5. 不含硬安全边界禁止的依赖或代码路径；
6. 相关固定数据集回归通过；
7. Provider、Model、Prompt、Rule、Index 等版本可记录；
8. 文档中的“完成”只以 V3.1 任务状态和验收证据为准。

阶段完成还必须通过本阶段列出的门禁；P7 V1.0 需安装包、部署文档、回归报告、已知问题和回滚方案齐全。

## 20. 冲突裁决

1. **任务总数**：四份 DOCX 的历史结论称 119 项；V3.1 为 116 项，本文采用 116 项。
2. **仓库平台**：旧规格出现 GitLab；V3.1 与当前基线为 GitHub mono-repo，采用 GitHub。
3. **阶段映射**：旧文档曾将 Vision 标作较早优先级、Live 标作 P2；V3.1 明确 Vision/State=P2、Live=P6，采用 V3.1。
4. **Knowledge Agent 重复**：历史描述多处重复；Canonical 只保留 `P1-AGENT-Knowledge-001`。
5. **RAG 向量存储**：旧文档允许 pgvector/Milvus；V3.1 明确 pgvector 优先，本文采用 PostgreSQL+pgvector，Milvus 不进入当前范围。
6. **文档搜索**：旧文档允许 OpenSearch/Elasticsearch；本文按用户指定和 V3.1 Hybrid 路线采用 OpenSearch。
7. **OCR 指标**：PRD 有稳定目标 `>=99%`，V3.1 P2 任务验收为受控集关键字段 `>=97%`；`97%` 是 P2 门禁，`99%` 仅保留为后续质量提升目标。
8. **基础设施**：旧规格列出 Kafka、Redis、ClickHouse、MinIO；V3.1 未将其作为 P1 必选任务，本文将其设为按负载和阶段引入，不得阻塞 P1。
9. **统计站接口**：历史文档列举 OP.GG、LoLChess 等来源，但没有证明公开 REST URL 可用；本文只保留 Adapter 候选，真实接口与合规在验收时验证。
10. **知识图谱**：PRD 描述知识图谱，但 V3.1 无独立 Canonical Task；可作为现有数据关系的实现方式，不得扩成新增阶段交付。
11. **实时能力**：任何“技术可行”描述均不能覆盖 V3.1 `P6-LIVE-PolicyGate-001`；未获明确允许即 No-Go。

## 21. 源文档追溯

- `TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html`：116 个 Canonical Task、P0～P7、依赖、优先级、验收、Provider 可替换架构；
- `TFT_Vision_AI_Coach_整合版需求与总体开发蓝图_v2.0.docx`：产品愿景、九大工程域、Tool/Agent 分层、数据治理和总体技术路线；
- `TFT_Vision_AI_Coach_完整需求文档_PRD_v2.0.docx`：用户场景、运行模式、安全边界、非功能需求、风险、隐私与产品指标；
- `TFT_Vision_AI_Coach_V2.x_分期开发实施与可开发规格_v2.0.docx`：先确定性后概率、分期“不做”、API/Tool 示例、Sprint 和阶段门禁；
- `TFT_Vision_AI_Coach_V1.1_可开发规格_总纲与细则.docx`：Java/Spring 技术栈、接口样例、数据库表、事件流、VisionProfile、测试集和 DoD。

## 22. 仍需产品决策

以下事项没有被 V3.1 唯一确定，必须在对应任务开始或验收前决策：

1. 第一、第二统计源及社区来源的授权方式、接口/抓取方式、频率、地区覆盖和合规结论；
2. Raw Snapshot 的生产根目录/对象存储、配额、保留期、加密、备份和删除策略；
3. Cloud LLM、Embedding、Reranker 的首选 Provider、数据处理地域、预算上限和密钥托管方式；
4. 关键 Observation 的人工确认阈值是否统一采用 `0.90`，或按字段/模型版本分别配置；
5. P1 OpenSearch、PostgreSQL、MySQL 的单机/容器/托管部署形态及可用性目标；
6. 社区内容的版权展示、引用长度、下架和用户投诉处理流程；
7. P6 前重新核验的 Riot 政策版本、产品注册状态和允许的具体 Live 用例；
8. 用户截图、视频、对局与 Personal Case 的默认保留期限及“一键删除”范围。
