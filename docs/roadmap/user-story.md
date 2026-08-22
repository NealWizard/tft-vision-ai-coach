# 任务路线图

User Story 看板维护在仓库 HTML 文件中，支持按 Phase / Domain / 状态筛选。

## 在线查看

在 GitHub 上打开源文件（推荐下载后用浏览器打开，交互完整）：

[打开 User Story HTML](https://github.com/NealWizard/tft-vision-ai-coach/blob/develop/TFT_Vision_AI_Coach_userstory%20(1).html)

本地克隆后双击：

```
TFT_Vision_AI_Coach_userstory (1).html
```

## 当前进度（P0 已完成）

| ID | 任务 | 状态 |
|----|------|------|
| P0-001 | Mono-repo + 分支策略 | DONE |
| P0-002 | Java 21 + Spring Boot 3 | DONE |
| P0-003 | CI/CD 基线 | DONE |
| P0-004 | 安全边界扫描 | DONE |
| P0-005 | Canonical Schema | DONE |
| P0-006 | Agent Contract | DONE |
| P0-007 | Trace 可观测 | DONE |
| P0-008 | Feature Flag | DONE |
| P0-009 | Fixture 目录 | DONE |
| M0 | V0.1 门禁验证 | DONE |

P1 及以后任务仍在 HTML 看板中，状态为 TODO。

## 更新任务状态

任务完成时使用脚本写入 HTML 源文件（不要只改浏览器 localStorage）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\update-us-status.ps1 `
  -TaskIds P1-001 -Status DONE -Owner NealWizard -CompletionDate 2026-08-22
```

详见 [任务完成收尾](../dev/task-completion.md)。

## 后续计划

- 中期：按 Phase 将 HTML 任务拆为 `docs/roadmap/p1.md` 等 Markdown 页
- 长期：User Story 与 GitHub Issues / Milestone 双向映射
