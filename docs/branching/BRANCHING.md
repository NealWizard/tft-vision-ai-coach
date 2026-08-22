# Branching Strategy (`P0-FOUND-Repo-001`)

> 平台：**GitHub** · 仓库：https://github.com/NealWizard/tft-vision-ai-coach

## Branches

| Branch | Purpose | 当前策略（单人开发） |
|--------|---------|---------------------|
| `main` | 稳定 / 可发布版本 | 可直接 push；发版前再从 develop 合并 |
| `develop` | V0.x 日常集成 | **默认开发分支**，AI / 开发者直接 push |
| `feature/<id>-<slug>` | 单任务 / Story | 可选；合并回 develop |
| `hotfix/<slug>` | 紧急修复 | 修 main 并回灌 develop |

## Naming examples

- `feature/P0-FOUND-Contract-001-canonical-schemas`
- `feature/P1-DATA-Riot-001-datadragon-adapter`
- `hotfix/ci-snapshot-false-positive`

## 单人开发（当前）

1. 在 `develop` 上开发，`git push origin develop`
2. **暂不启用** Branch protection；这是单人开发阶段的显式例外
3. Push 后查看 [GitHub Actions](https://github.com/NealWizard/tft-vision-ai-coach/actions) 是否全绿
4. 提交前建议本地跑 `scripts/local-ci.ps1`

## 协作 / 发版期（后续）

1. 启用 Branch protection（`main` / `develop`）
2. 禁止直接 push，必须通过 **Pull Request**
3. PR 必须通过 CI：
   - `Safety boundary scan`
   - `Build & test`
4. 配置见 `docs/github/GITHUB.md`

## Merge 规则

1. Feature 分支优先 **Squash merge** 到 `develop`
2. 跨模块改动若属同一验收标准，放在同一个 PR
3. `main` 仅接收从 `develop` 的发布合并（或 hotfix）

## Mono-repo domains

`tft-data` · `tft-knowledge` · `tft-vision` · `tft-state` · `tft-meta` · `tft-decision` · `tft-replay` · `tft-learning` · `tft-orchestrator`

Shared foundations: `tft-contracts` (schemas) · `tft-common` (flags/observability)
