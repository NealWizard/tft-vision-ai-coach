# GitHub 协作与 CI 指南

> 更新时间：2026-08-22 15:10 +08:00  
> 仓库：https://github.com/NealWizard/tft-vision-ai-coach

## 仓库信息

| 项 | 值 |
|----|-----|
| 远端（HTTPS） | `https://github.com/NealWizard/tft-vision-ai-coach.git` |
| 默认开发分支 | `develop` |
| 稳定分支 | `main` |
| CI 配置 | `.github/workflows/ci.yml` |
| 本地 CI 对齐 | `scripts/local-ci.ps1` |

## 克隆与远端

```powershell
git clone https://github.com/NealWizard/tft-vision-ai-coach.git
cd tft-vision-ai-coach
git remote -v
```

已存在本地目录时绑定远端：

```powershell
git remote set-url origin https://github.com/NealWizard/tft-vision-ai-coach.git
```

## Git 提交身份

GitHub 按 **commit 邮箱** 关联账号。请在**本机**自行配置，**勿将私人邮箱写入仓库文档**：

```powershell
git config --local user.name "<你的 GitHub 用户名>"
git config --local user.email "<你的 GitHub 已验证邮箱或 noreply 地址>"
```

在 GitHub **Settings → Emails** 可查看 `...@users.noreply.github.com`；勾选 **Keep my email addresses private** 时推荐使用 noreply 地址。

## 认证方式

### HTTPS + Personal Access Token（推荐）

1. GitHub → **Settings → Developer settings → Personal access tokens**
2. 生成 token，勾选 `repo`
3. `git push` 时 Password 填 token

### SSH

1. 生成密钥并添加到 GitHub → **SSH and GPG keys**
2. 远端改为：`git@github.com:NealWizard/tft-vision-ai-coach.git`

## 分支策略（单人开发期）

当前为 **单人 + AI 自动提交** 模式：

- 日常开发：直接 push 到 `develop`
- **暂不启用** Branch protection（避免阻塞 AI / 直接推送）
- 大改动可选：`feature/P1-xxx-描述` → 合并回 `develop`

发版或多人协作后再对 `main` / `develop` 启用 Branch protection。

详见：[分支策略](../branching/BRANCHING.md)

## GitHub Actions CI

Push / Pull Request 到 `main`、`develop`、`feature/**` 时触发 workflow **CI**。

| Job 名称 | 说明 |
|----------|------|
| `Safety boundary scan` | 运行 `scripts/safety-scan.ps1`，拦截违规 API |
| `Build & test` | JDK 21 + `mvn clean verify` + SNAPSHOT 检查 + 打包 |

查看运行结果：**Actions** 标签页  
https://github.com/NealWizard/tft-vision-ai-coach/actions

### 本地对齐 CI

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local-ci.ps1
```

## Pull Request 流程（协作期）

1. `git checkout -b feature/P1-003-datadragon-adapter`
2. 开发并 push 分支
3. GitHub 创建 **Pull Request** → `develop`
4. 等待 CI 两个 job 全绿后合并（建议 Squash merge）

## Branch protection（协作 / 发版时再开）

路径：**Settings → Branches → Add rule**

前提：Actions 至少成功跑过一次，status check 列表才会有：

- `Build & test`
- `Safety boundary scan`

（也可能显示为 `CI / Build & test`）

## Issue 与任务追踪

Roadmap V3.1 任务 ID（如 `P1-DATA-Riot-001`）可映射为 **GitHub Issues**：

- **Label**：Epic / Capability（如 `DATA`、`Riot`）
- **Milestone**：Phase（如 `P1`）
- **Title**：`[P1-DATA-Riot-001] 实现 Riot/Data Dragon Adapter`

### 任务完成收尾

1. 更新 `TFT_Vision_AI_Coach_Complete_Roadmap_V3_1.html` 中对应任务为 `DONE`（用 `scripts/update-us-status.ps1`）
2. 运行 `scripts/gen-commit-msg.ps1` 生成 commit message
3. **开发者手动** `git commit` / `git push`（AI 不自动提交）

规则：[任务完成收尾](../dev/task-completion.md)

## Wiki 文档站

| 项 | 值 |
|----|-----|
| 线上地址 | https://nealwizard.github.io/tft-vision-ai-coach/ |
| 源文件 | `docs/` + 根目录 `mkdocs.yml` |
| 构建工具 | MkDocs + Material |
| 部署 Workflow | `.github/workflows/docs.yml` |
| 触发分支 | `main`、`develop`（`docs/**` 或 `mkdocs.yml` 变更） |

首次启用 GitHub Pages：

1. 仓库 **Settings → Pages**
2. **Build and deployment → Source** 选 **GitHub Actions**
3. push 到 `develop`（或 merge 到 `main`）并 push，等待 **Docs** workflow 跑绿

本地预览：

```powershell
pip install -r requirements-docs.txt
mkdocs serve
```

### Wiki 部署失败排查

**报错**：`Branch "main" is not allowed to deploy to github-pages due to environment protection rules`

原因：`github-pages` **Environment** 限制了可部署分支，默认可能未包含 `main`。

**修复步骤**（仓库管理员）：

1. **Settings → Environments → github-pages**
2. **Deployment branches and tags** → 选 **All branches**（或 **Selected branches** 并添加 `main`）
3. 若启用了 **Required reviewers** / **Wait timer**，单人开发期请**关闭**（否则会一直 pending / rejected）
4. 保存后，到 **Actions → Docs → Re-run all jobs**

**仍失败时检查**：

| 检查项 | 路径 |
|--------|------|
| Pages 来源 | Settings → Pages → Source = **GitHub Actions** |
| 仓库可见性 | 免费 GitHub Pages 需 **Public** 仓库 |
| Workflow 权限 | Settings → Actions → General → Workflow permissions 含 **Read and write**（或至少允许 `pages: write`） |

## 与 GitLab 文档的差异

本项目已从 GitLab 迁移至 GitHub：

- ~~`.gitlab-ci.yml`~~ → `.github/workflows/ci.yml`
- ~~Merge Request (MR)~~ → Pull Request (PR)
- ~~GitLab Protected Branches~~ → GitHub Branch protection rules
