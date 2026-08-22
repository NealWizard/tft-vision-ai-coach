# TFT Vision AI Coach

> 更新时间：2026-08-22 15:10 +08:00  
> 当前阶段：**P0 · 工程地基（V0.1）— 已完成**  
> 代码托管：**GitHub** — https://github.com/NealWizard/tft-vision-ai-coach

## 项目定位

纯视觉/截图输入的云顶之弈辅助决策系统（副驾驶）。**不读内存、不注入、不模拟键鼠、不拦截游戏通信**。Live 动态推荐默认关闭。

| 文档 | 路径 |
|------|------|
| 产品需求 | `云顶辅助决策agent需求文档.txt` |
| 任务路线图 | `TFT_Vision_AI_Coach_userstory (1).html` |
| GitHub 协作 / CI | `docs/github/GITHUB.md` |
| 分支策略 | `docs/branching/BRANCHING.md` |

## P0 交付内容（全部 DONE）

| ID | 任务 | 落地位置 |
|----|------|----------|
| P0-001 | Mono-repo + 分支策略 | 根 `pom.xml` + `docs/branching/BRANCHING.md` |
| P0-002 | Java 21 + Spring Boot 3 | Java 21 / Boot 3.3.5 / Enforcer |
| P0-003 | CI/CD 基线 | `.github/workflows/ci.yml` + `scripts/local-ci.ps1` |
| P0-004 | 安全边界扫描 | `scripts/safety-scan.ps1` |
| P0-005 | Canonical Schema | `schemas/canonical/*` |
| P0-006 | Agent Contract | `schemas/agent/*` + 5 个样例 |
| P0-007 | Trace 可观测 | `TraceService` + `/api/v1/trace/*` |
| P0-008 | Feature Flag | `tft.flags.*`，Live 默认 false |
| P0-009 | Fixture 目录 | `fixtures/*` 五类 manifest |
| M0 | V0.1 门禁 | `mvn verify` + safety scan 通过 |

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

## 快速开始

### 1. 克隆

```powershell
git clone https://github.com/NealWizard/tft-vision-ai-coach.git
cd tft-vision-ai-coach
```

### 2. JDK 21

```powershell
# 可选：使用仓库旁路 JDK（见 .gitignore 的 .tools/jdk-21）
$env:JAVA_HOME = "C:\Users\ASUS\Desktop\TFT\.tools\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

### 3. 构建 & 测试

```powershell
mvn -B clean verify
# 或一键对齐 CI
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local-ci.ps1
```

### 4. Git 提交身份（本仓库）

```powershell
git config --local user.name "NealWizard"
git config --local user.email "your-email@users.noreply.github.com"
```

邮箱须在 GitHub 已验证，提交才会显示为你的账号。

## GitHub CI

- 配置文件：`.github/workflows/ci.yml`
- 触发：push / PR 到 `main`、`develop`、`feature/**`
- Jobs：`Safety boundary scan` · `Build & test`
- 状态：https://github.com/NealWizard/tft-vision-ai-coach/actions

单人开发期 **不启用 Branch protection**，可直接 push 到 `develop`。

## 关键接口（P0）

- `GET /api/v1/health/foundation` — 健康 + Feature Flag
- `GET /api/v1/trace/demo` — 演示 Trace（可带 `X-Correlation-Id`）
- `GET /api/v1/trace/{correlationId}` — 查询调用链

## 开发流程

1. 在 `develop` 分支开发
2. 提交前运行 `scripts/local-ci.ps1`
3. `git push origin develop`
4. 到 GitHub Actions 确认 CI 全绿
5. 新增 Schema / Agent 时先改 `schemas/`，再写代码

## 使用方法

详见 `docs/github/GITHUB.md`（认证、PR、Issue 映射、何时开 Branch protection）。
