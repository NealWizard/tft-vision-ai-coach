# TFT Vision AI Coach

> 更新时间：2026-08-22 12:35 +08:00  
> 当前阶段：**P0 · 工程地基（V0.1）— 已完成并通过本地门禁**

## 项目定位

纯视觉/截图输入的云顶之弈辅助决策系统（副驾驶）。**不读内存、不注入、不模拟键鼠、不拦截游戏通信**。Live 动态推荐默认关闭。

规划基线见：`TFT_Vision_AI_Coach_userstory (1).html`  
产品需求见：`云顶辅助决策agent需求文档.txt`

## P0 交付内容（全部 DONE）

| ID | 任务 | 落地位置 | 自审结论 |
|----|------|----------|----------|
| P0-001 | Mono-repo + 分支策略 | 根 `pom.xml` 十一模块 + `docs/branching/BRANCHING.md` + `main`/`develop` | 九大业务域齐；另加 `tft-contracts`/`tft-common` 共享地基 |
| P0-002 | Java 21 + Spring Boot 3 基线 | Java 21 / Boot 3.3.5 / Enforcer | `mvn clean verify` 全绿；dependency-tree 已导出 |
| P0-003 | CI/CD 基线 | `.gitlab-ci.yml` + `scripts/local-ci.ps1` | 含 compile/test/enforcer/package/safety |
| P0-004 | 安全边界扫描 | `scripts/safety-scan.ps1` | 正常代码通过；违规样例在 `tools/safety-fixtures` |
| P0-005 | Canonical Schema | `schemas/canonical/*` | 10 类实体 schema + 单测 |
| P0-006 | Agent Contract | `schemas/agent/*` + 5 个可运行样例 | Knowledge/Meta/Economy/Shop/Composition |
| P0-007 | Trace 可观测 | `TraceService` + `/api/v1/trace/*` | correlation_id 可查询；日志含 latency/version/status |
| P0-008 | Feature Flag | `tft.flags.*` | Live 三项默认 false，配置可切换 |
| P0-009 | Fixture 目录 | `fixtures/*` 五类 + manifest | 空壳就绪，版本规则见 `fixtures/README.md` |
| M0 | V0.1 门禁 | 本地 `mvn verify` + safety scan | **通过** |

## 模块一览

```
tft-vision-ai-coach (parent)
├── tft-contracts      # JSON Schema + 校验
├── tft-common         # FeatureFlag / Trace
├── tft-data           # 数据源适配（P1+）
├── tft-knowledge      # 知识库（P1+）
├── tft-vision         # 视觉识别（P2+）
├── tft-state          # GameState（P2+）
├── tft-meta           # Meta（P3+）
├── tft-decision       # 决策 Agent（P3+）
├── tft-replay         # 复盘（P4+）
├── tft-learning       # 个人教练（P4+）
└── tft-orchestrator   # Spring Boot 入口
```

## 本地环境

1. JDK 21（可用仓库旁路 `.tools/jdk-21`，已 gitignore）
2. Maven 3.6.3+（推荐 3.9+）
3. 一键门禁：

```powershell
$env:JAVA_HOME = "C:\Users\ASUS\Desktop\TFT\.tools\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local-ci.ps1
```

## 关键接口（P0）

- `GET /api/v1/health/foundation` — 健康检查 + Feature Flag 默认值
- `GET /api/v1/trace/demo` — 演示写入 AgentRun（可带 `X-Correlation-Id`）
- `GET /api/v1/trace/{correlationId}` — 按 correlation_id 查询调用链

## 自主决策与搁置项

**已决策**

- Maven 多模块 + Spring Boot 3.3.5（对齐 User Story Java 基线，非桌面 UI）
- CI 以 GitLab CI 为准；本机用 Windows PowerShell 跑 safety scan
- Schema `1.0.0`；Agent 强制 `candidates[]`
- 本机无 JDK 21 时下载 Temurin 到 `.tools/jdk-21`

**搁置（请你检查时确认）**

- GitLab 远端 URL 与 Protected Branch 实配（本地已有 `main`/`develop` + 分支文档，**尚未 commit/push**）
- Schema 字段是否需与 Word 规格文档逐字段对齐
- 是否升级本机 Maven 到 3.9
- `LoadLibrary` 扫描规则是否过严（当前默认拦截）

## 使用方法

1. 打开 User Story HTML 跟踪进度  
2. 开发前阅读 `docs/branching/BRANCHING.md`  
3. 提交前运行 `scripts/local-ci.ps1`  
4. 新增实体/Agent 时先改 `schemas/`，再写代码
