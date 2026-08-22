# TFT Vision AI Coach

> 更新时间：2026-08-22 15:40 +08:00  
> 当前阶段：**P0 · 工程地基（V0.1）— 已完成**  
> 代码托管：**GitHub** — https://github.com/NealWizard/tft-vision-ai-coach  
> **Wiki**：https://nealwizard.github.io/tft-vision-ai-coach/

纯视觉/截图输入的云顶之弈辅助决策系统（副驾驶）。**不读内存、不注入、不模拟键鼠**。Live 动态推荐默认关闭。

## 文档

| 类型 | 位置 |
|------|------|
| **Wiki（推荐）** | https://nealwizard.github.io/tft-vision-ai-coach/ |
| Wiki 源文件 | `docs/` + `mkdocs.yml` |
| 产品 PRD | `云顶辅助决策agent需求文档.txt` |
| 任务看板 | `TFT_Vision_AI_Coach_userstory (1).html` |
| 代码示例 | `goodcode.md` |

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

更新 US → 生成 commit msg → **你手动提交**。详见 [Wiki · 任务完成收尾](docs/dev/task-completion.md)。
