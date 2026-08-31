# 环境搭建

## 1. 克隆仓库

```powershell
git clone https://github.com/NealWizard/tft-vision-ai-coach.git
cd tft-vision-ai-coach
```

## 2. JDK 21

项目基线为 **Java 21** + **Spring Boot 3.3.5**。

```powershell
# 可选：使用仓库旁路 JDK（见 .gitignore 的 .tools/jdk-21）
$env:JAVA_HOME = "C:\path\to\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

## 3. 构建与测试

```powershell
mvn -B clean verify
```

一键对齐 CI：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\local-ci.ps1
```

## 4. Git 提交身份

在**本机**配置（勿将私人邮箱提交到仓库文档）：

```powershell
git config --local user.name "<你的 GitHub 用户名>"
git config --local user.email "<你的 GitHub 已验证邮箱或 noreply 地址>"
```

邮箱须在 GitHub **Settings → Emails** 中验证，或使用 `...@users.noreply.github.com`。

## 5. 启动应用

```powershell
$env:JAVA_HOME = "C:\Users\ASUS\Desktop\TFT\.tools\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -pl tft-orchestrator -am spring-boot:run
```

### 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/health/foundation` | 健康检查 + Feature Flag |
| GET | `/api/v1/trace/demo` | Trace 演示（可带 `X-Correlation-Id`） |
| GET | `/api/v1/trace/{correlationId}` | 查询调用链 |
| GET | `/api/v1/knowledge/ask?question=...` | Knowledge Agent（默认 patch=`set18-18.1`，云 LLM 关闭） |
| POST | `/api/v1/knowledge/ask` | JSON：`question` / `patch` / `cloud` |
| GET | `/api/v1/research/ask?topic=...` | Research Agent（不可覆盖官方事实） |
| POST | `/api/v1/data/ingest/datadragon` | Data Dragon 灌库（CDN 不通会 502） |
| GET | `/api/v1/vision/health` | 视觉侧车探活（未启动则 `degraded=true`） |
| POST | `/api/v1/vision/analyze` | 数值 OCR（需 1920×1080；无 Paddle 则 degraded） |
| POST | `/api/v1/state/build` | Observation[] → GameState |
| POST | `/api/v1/recommendations/analyze` | GameState → CandidateSet（`decision_type` 可选；offline 用 mock 解释，非 offline 且 `.env` LLM 齐全则走同一套 OpenAI 兼容接口） |
| POST | `/api/v1/meta/search` | patch 必填；无 MCP 时 fixture + degraded |
| POST | `/api/v1/meta/patch-impact` | JSON：`from_patch` + `to_patch`（缺一则 400） |
| GET | `/api/v1/meta/snapshot/{id}` | 按 id 取 Meta 快照 |

浏览器直接打开 GET 即可看 JSON。含 `gold`/`interest` 的问题走规则工具；其它问题走本地 RAG。

## 6. 本地预览 Wiki

```powershell
pip install -r requirements-docs.txt
mkdocs serve
```
