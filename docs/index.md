# TFT Vision AI Coach

> 更新时间：2026-08-22  
> 当前阶段：**P0 · 工程地基（V0.1）— 已完成**

纯视觉/截图输入的云顶之弈辅助决策系统（**副驾驶**）。

!!! warning "安全边界"
    **不读内存、不注入、不模拟键鼠、不拦截游戏通信**。Live 动态推荐默认关闭。

## 文档导航

| 章节 | 说明 |
|------|------|
| [快速开始](getting-started/quickstart.md) | 克隆、JDK 21、构建与本地 CI |
| [需求概述](product/requirements.md) | 产品定位、目标与架构摘要 |
| [任务路线图](roadmap/user-story.md) | User Story 看板（P0–P4） |
| [模块一览](architecture/modules.md) | Maven 多模块与 Phase 规划 |
| [Schema 契约](architecture/schemas.md) | Canonical / Agent JSON Schema |
| [GitHub 协作](github/GITHUB.md) | 认证、CI、PR、Issue 映射 |
| [优秀示例](goodcode/index.md) | Feature Flag、Trace、Schema 约定 |

## 仓库

- GitHub：<https://github.com/NealWizard/tft-vision-ai-coach>
- CI：<https://github.com/NealWizard/tft-vision-ai-coach/actions>

## Wiki 发布

- 站点：<https://nealwizard.github.io/tft-vision-ai-coach/>
- 源文件：仓库 `docs/` 目录 + 根目录 `mkdocs.yml`
- 触发：`main` 分支 push 后 GitHub Actions 自动构建部署

本地预览：

```powershell
pip install -r requirements-docs.txt
mkdocs serve
# 浏览器打开 http://127.0.0.1:8000
```
