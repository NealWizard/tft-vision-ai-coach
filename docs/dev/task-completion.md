# 任务完成收尾

每完成一个或多个 User Story 任务（如 `P1-003`）后执行以下流程。

## 1. 更新 User Story 文档

文件：仓库根目录 `TFT_Vision_AI_Coach_userstory (1).html`

使用脚本写入源文件（**不要**只改浏览器 localStorage）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\update-us-status.ps1 `
  -TaskIds P1-001,P1-002 `
  -Status DONE -Owner NealWizard -CompletionDate 2026-08-22
```

写入字段：`status`、`owner`、`actual_completion`。

## 2. 同步 Wiki / readme

若任务改变项目能力或用法：

- 更新对应 `docs/**/*.md` 页面
- 更新根目录 `readme.md` 摘要与时间戳

## 3. 生成 commit message

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\gen-commit-msg.ps1 `
  -Tasks P1-001,P1-002 -Type docs -Scope us
```

脚本仅**输出**建议 message，不自动提交。

## 4. 手动 commit / push

```powershell
git add -A
git commit -m "<上一步输出的 subject>"
git push origin develop
```

## Commit message 格式

```
<type>(<scope>): <subject>

Tasks: P1-001, P1-002
Changed files:
- TFT_Vision_AI_Coach_userstory (1).html
- docs/...
```

`type` 参考：`feat` | `fix` | `docs` | `chore` | `ci` | `test` | `refactor`

## 规则来源

Cursor 规则：`.cursor/rules/task-completion.mdc`

## Wiki 发布

文档变更合并到 `main` 后，GitHub Actions **Docs** workflow 会自动部署 Wiki 站点。
