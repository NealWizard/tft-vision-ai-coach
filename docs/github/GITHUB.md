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

## Git 提交身份（必配）

GitHub 按 **commit 邮箱** 关联账号，与登录名无关。本仓库已配置：

```powershell
git config --local user.name "NealWizard"
git config --local user.email "your-email@users.noreply.github.com"
```

请确保 `your-email@users.noreply.github.com` 已在 GitHub **Settings → Emails** 中验证，否则提交显示为未关联用户。

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

详见：`docs/branching/BRANCHING.md`

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
$env:JAVA_HOME = "C:\Users\ASUS\Desktop\TFT\.tools\jdk-21"
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

User Story 任务 ID（如 `P1-003`）可映射为 **GitHub Issues**：

- **Label**：Domain（如 `DATA`、`VISION`）
- **Milestone**：Phase（如 `P1`）
- **Title**：`[P1-003] 实现 Riot/Data Dragon Adapter`

## 与 GitLab 文档的差异

本项目已从 GitLab 迁移至 GitHub：

- ~~`.gitlab-ci.yml`~~ → `.github/workflows/ci.yml`
- ~~Merge Request (MR)~~ → Pull Request (PR)
- ~~GitLab Protected Branches~~ → GitHub Branch protection rules
