# TFT Vision AI Coach

> 更新时间：2026-08-24 10:05 +08:00
> 当前阶段：**P1 完成（除 Stats-002 BLOCKED）→ 准备 P2**
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
| 历史需求文档       | `历史需求文档/`（仅归档，不再更新）                           |
| 代码示例           | `goodcode.md`                                                 |

## V3.1 重建进度


| ID         | 任务                                             | 状态                                      |
| ---------- | ------------------------------------------------ | ----------------------------------------- |
| P0-FOUND-* | 工程、契约、安全、可观测、降级门禁               | DONE                                      |
| P1-DATA-*  | Source～Quality 全链路                           | **DONE**（Stats-002 BLOCKED）             |
| P1-KNOW-*  | 确定性 Knowledge Tools                           | DONE                                      |
| P1-RAG-*   | Hybrid RAG Platform                              | DONE                                      |
| P1-LLM-*   | Cloud LLM Gateway / Guard / Meter                | DONE                                      |
| P1-AGENT-* | Knowledge / Research Agent                       | DONE                                      |

## 快速开始

```powershell
git clone https://github.com/NealWizard/tft-vision-ai-coach.git
cd tft-vision-ai-coach
mvn -B clean verify
# 或对齐 CI
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local-ci.ps1
```

JDK 21、Git 身份、接口说明、开发流程详见 [Wiki 快速开始](docs/getting-started/quickstart.md)。

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
