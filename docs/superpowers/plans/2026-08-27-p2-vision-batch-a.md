# P2 Vision Batch A 实现计划

> 日期：2026-08-27  
> 规格：`docs/superpowers/specs/2026-08-27-p2-vision-batch-a-design.md`（V1.1 推荐收敛版）  
> 目标：完成 Frame / Profile / Sidecar Contract / Observation 1.0.x 扩展；Roadmap 三任务 DONE

## 约束

- 最小可行：不实现 Camera/Desktop、标定 UI、真实 OCR、crop 依赖可缺省
- JSON snake_case；CI 不依赖 Python/Paddle
- Observation 保持 `1.0.0`，只加 optional 字段
- 不自动 commit

## 任务拆分

### Task 1 — Observation schema 1.0.x 扩展

- 更新 `tft-contracts/.../observation.schema.json` 与根 `schemas/canonical/observation.schema.json`
- 可选字段：`raw_value`、`detector`、`model`、`model_version`、`bbox`、`preprocess_version`、`observation_id`
- 更新 `SchemaContractTest` 样例（若有）
- 验证：`mvn -pl tft-contracts test`

### Task 2 — tft-vision：Frame

- 包：`com.tft.coach.vision.frame`
- `FrameSource`、`VisionFrame`、`FramePayload`（sealed/records）、`FrameSourceMetadata`、`SourceType`
- `FileFrameSource`、`DirectoryFrameSource`、`FixtureFrameSource`
- 单测 + 最小 fixture 图（可用 1×1 PNG bytes 或 classpath 小图）
- pom：如需 Jackson / 读图尺寸，仅加最小依赖（可用 `ImageIO` 读宽高）

### Task 3 — tft-vision：Profile + ROI 校验

- 包：`com.tft.coach.vision.profile`
- `VisionProfile`、`RoiRegion`、`VisionProfileLoader`、`RoiValidator`
- `classpath:vision/profiles/1920x1080-default.json`（4 个占位 ROI）
- 未知分辨率 → 明确异常/`UNSUPPORTED_PROFILE`
- Calibration 无 UI：`VisionProfileWriter` 写出 JSON（可选）

### Task 4 — vision-sidecar

- `vision-sidecar/requirements.txt`：fastapi、uvicorn（opencv 可选注释）
- `app/main.py`：`/health`、`/ready`、`POST /vision/analyze` stub
- Envelope snake_case
- `scripts/run-dev.ps1`
- `README.md` 简短说明
- pytest 或说明用 TestClient 的最小测试（可选；Java CI 不跑）

### Task 5 — tft-vision：SidecarClient

- 包：`com.tft.coach.vision.sidecar`
- 配置 properties；JDK `HttpClient` 或 Spring RestClient（vision 模块尽量不强制 Spring Web——用 JDK HttpClient）
- health → degraded on timeout/connect fail
- 单测用 MockWebServer 或 stub（若无依赖则单元测解析 Envelope）

### Task 6 — tft-state：Observation

- 依赖 `tft-contracts`
- `Observation` record/DTO、`ObservationValidator`、`ObservationFactory.fromFixture`
- 单测合法/非法

### Task 7 — orchestrator 调试 API

- `GET /api/v1/vision/health` → 200 + `degraded` 字段
- `application.yml` 增加 `tft.vision.sidecar.*`
- 扩展现有 `FoundationControllerTest` 或新测：侧车未启仍 200 degraded

### Task 8 — 收尾

- 更新 `readme.md`
- Roadmap 三任务 DONE（`scripts/update-us-status.ps1`）
- `scripts/gen-commit-msg.ps1` 输出给用户（不 commit）

## 验证清单

- [x] `mvn -pl tft-contracts,tft-vision,tft-state,tft-orchestrator -am test` 通过
- [ ] 侧车手动：`uvicorn` 后 `/health` OK（可选本地）
- [x] Roadmap / readme / commit message 就绪
