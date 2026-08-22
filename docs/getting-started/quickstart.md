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

## 5. 启动应用（P0）

```powershell
mvn -pl tft-orchestrator -am spring-boot:run
```

### 关键接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/health/foundation` | 健康检查 + Feature Flag |
| GET | `/api/v1/trace/demo` | Trace 演示（可带 `X-Correlation-Id`） |
| GET | `/api/v1/trace/{correlationId}` | 查询调用链 |

## 6. 本地预览 Wiki

```powershell
pip install -r requirements-docs.txt
mkdocs serve
```
