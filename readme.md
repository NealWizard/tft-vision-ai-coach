# TFT Vision AI Coach

> 更新时间：2026-08-26 16:49 +08:00
> 当前阶段：**P1 做浅项真实化已落地（Stats-002 仍搁置）→ 准备 P2**
> 代码托管：**GitHub** — https://github.com/NealWizard/tft-vision-ai-coach
> **Wiki**：https://nealwizard.github.io/tft-vision-ai-coach/

纯视觉/截图输入的云顶之弈辅助决策系统（副驾驶）。**不读内存、不注入、不模拟键鼠**。Live 动态推荐默认关闭。

## 文档


| 类型               | 位置                                                          |
| ------------------ | ------------------------------------------------------------- |
| **Wiki（推荐）**   | https://nealwizard.github.io/tft-vision-ai-coach/             |
| Wiki 源文件        | `docs/` + `mkdocs.yml`                                        |
| 唯一需求与任务基线 | `TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html`              |
| 整合需求与开发规格 | `docs/product/TFT_Vision_AI_Coach_整合需求与开发规格_V3.1.md` |
| P1 数据层          | `docs/architecture/data-layer.md`                             |
| P1 做浅硬化设计    | `docs/superpowers/specs/2026-08-26-p1-shallow-hardening-design.md` |
| 历史需求文档       | `历史需求文档/`（仅归档，不再更新）                           |
| 代码示例           | `goodcode.md`                                                 |

## V3.1 重建进度


| ID         | 任务                                             | 状态                                      |
| ---------- | ------------------------------------------------ | ----------------------------------------- |
| P0-FOUND-* | 工程、契约、安全、可观测、降级门禁               | DONE                                      |
| P1-DATA-*  | Source～Quality 全链路                           | **DONE**（Stats-002 搁置）                |
| P1-KNOW-*  | 确定性 Knowledge Tools（Catalog JSON）           | DONE                                      |
| P1-RAG-*   | Hybrid RAG（InMemory / Elasticsearch）           | DONE                                      |
| P1-LLM-*   | Cloud LLM Gateway（OpenAI 兼容 / 智谱）          | DONE                                      |
| P1-AGENT-* | Knowledge / Research（Tavily+SerpAPI）           | DONE                                      |

## 运行模式

| 模式 | 触发 | 行为 |
|------|------|------|
| `offline` | `tft.platform.mode=offline` 或单测 | InMemory + Hash Embedding + Stub LLM/Search |
| `online` / `auto` | 根目录 `.env` 含 `MYSQL_*` + `ES_HOSTS` | MySQL + ES + 智谱 LLM/Embedding + Tavily/SerpAPI |

配置模板：`.env.example`（密钥放根目录 `.env`，已 gitignore）。

## 快速开始

```powershell
git clone https://github.com/NealWizard/tft-vision-ai-coach.git
cd tft-vision-ai-coach
mvn -B clean verify
# 或对齐 CI
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local-ci.ps1
```

JDK 21、Git 身份、接口说明、开发流程详见 [Wiki 快速开始](docs/getting-started/quickstart.md)。

本地启动（读取 `.env`）：

```powershell
$env:JAVA_HOME = "C:\Users\ASUS\Desktop\TFT\.tools\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
# 可选：application.yml 设 tft.platform.seed-datadragon: true 灌入 Data Dragon
mvn -pl tft-orchestrator -am spring-boot:run
```

接口：

- `GET /api/v1/knowledge/ask?question=...`
- `GET /api/v1/research/ask?topic=...`（外网检索候选，不可覆盖官方事实）

## Wiki 本地预览

```powershell
pip install -r requirements-docs.txt
mkdocs serve
# 或
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\docs-serve.ps1
```

## 开发分支

- 日常开发：`develop`
- Wiki 发布：`develop` push 自动部署；发版时 merge 到 `main` 同步（`.github/workflows/docs.yml`）
- CI：`.github/workflows/ci.yml` — push/PR 到 `main`、`develop`、`feature/**`

## 任务完成收尾

更新 Roadmap V3.1 → 生成 commit msg → **你手动提交**。详见 [Wiki · 任务完成收尾](docs/dev/task-completion.md)。
