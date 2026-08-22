# 任务路线图

唯一任务基线为仓库根目录
`TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html`，支持按
Phase / Epic / Capability / 状态筛选。`历史需求文档/` 仅归档，不再同步状态。

## 查看

[打开 Roadmap V3.1 HTML](https://github.com/NealWizard/tft-vision-ai-coach/blob/develop/TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html)

本地克隆后可直接用浏览器打开：

```
TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html
```

## 当前阶段

P0 按 V3.1 Canonical Task ID 重新验收，随后进入 P1 Knowledge & Data Platform。
任务只有通过代码、测试和文档验收后才标记为 DONE。

## 更新任务状态

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\update-us-status.ps1 `
  -TaskIds P0-FOUND-Contract-001 `
  -Status DONE -Owner NealWizard -CompletionDate 2026-08-22
```

详见 [任务完成收尾](../dev/task-completion.md)。
