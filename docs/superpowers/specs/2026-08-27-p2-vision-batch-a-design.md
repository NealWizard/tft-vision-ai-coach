# P2 Vision Batch A 设计（V1.1 推荐收敛版）

> 日期：2026-08-27  
> 状态：已确认（按审计推荐收敛）  
> 基线：用户 V1.1 实现规格 + 审计 α/Calibration 决策  
> Roadmap：`P2-VISION-Frame-001`、`P2-VISION-ROI-001`、`P2-STATE-Observation-001` + Sidecar Contract 地基  
> 明确延后：Batch B = 真实数值 OCR；标定拖拽 UI；大规模 Golden 集

## 0. 已确认决策（相对 V1.0 / 用户 V1.1）

| 项 | 决策 |
|----|------|
| Observation | **方案 α**：保持 `schema_version=1.0.0`；`value` = 归一化结果；可选扩展 `raw_value` / `detector` / `model` / `model_version` / `bbox` / `preprocess_version`（contracts 发 **1.0.x 兼容扩展**，本批不 bump 1.1.0） |
| Calibration | **A 只做数据 + 校验 + Profile JSON 写出**；拖拽 UI → A+ / 独立任务 |
| JSON 命名 | 对外 **snake_case**；Java DTO camelCase + `@JsonProperty` |
| Camera / Desktop | 仅冻 SPI，Batch A **不实现** |
| Confidence 分层 | 设计备注；A **只记录 confidence，不做业务门禁** |
| Golden Fixture | A 最小集（1080p×1 套 ROI 占位 + 各 3～5 负例 + Envelope 样例）；大规模 backlog |
| 工程路径 | 仓库根下 `tft-vision/`、`tft-state/`、`vision-sidecar/`（非 `tft-agent/`） |

## 1. 目标与非目标

**目标（Batch A）**

- 统一 File / Directory / Fixture 的 `FrameSource`（SPI 预留 Camera/Desktop）
- 可版本化 `VisionProfile`：`layout_version`、分辨率、可选 `ui_scale` / `language` / `client_version` / `patch_hint` + ROI
- Sidecar Local HTTP Contract：`request_id` / `status` / `error_code` / Envelope + `/health` `/ready` + Stub Provider
- Observation 1.0.x 扩展 + `ObservationValidator`
- Calibration **数据结构**：Profile 校验、未知分辨率失败、可写出 JSON（无 UI）

**非目标**

- 真实 OCR / Shop / Board / GameState / Diff / Timeline / Cloud Vision 默认路径
- Live 采集、读游戏进程/内存、自动键鼠
- 标定拖拽 UI、PyInstaller 真打包、大规模 Golden

## 2. 架构

```
FrameSource → VisionFrame + FramePayload
       → VisionProfile Resolver / RoiRegion 校验
       → vision-sidecar（FastAPI：health/ready/roi crop 可选/Stub Provider）
       → Observation（1.0.x）→ ObservationValidator → tft-state（仅 Observation）
```

职责：

| 组件 | 负责 | 不负责 |
|------|------|--------|
| `tft-vision` | FrameSource、VisionFrame、FramePayload、VisionProfile、Roi 校验、SidecarClient、VisionProvider **Java 侧接口占位** | 不解 TFT 业务规则 |
| `vision-sidecar` | FastAPI、health/ready、可选 crop、Stub Provider | 不构造 GameState |
| `tft-state` | Observation DTO、Validator、fixture 工厂 | Builder/Fusion/Diff/Timeline |
| `tft-orchestrator` | `GET /api/v1/vision/health`、装配 | 不默认 Live / auto-spawn |

## 3. FrameSource / VisionFrame

```text
FrameSource extends AutoCloseable {
  Optional<VisionFrame> nextFrame()
  FrameSourceMetadata metadata()
  void close()
}

VisionFrame {
  frame_id, captured_at, source_timestamp_ms?, ingested_at?,
  width, height, source_type (SCREENSHOT|VIDEO|CAMERA|DESKTOP),
  profile_hint?, payload
}

FramePayload = InlineBytes | LocalFile | SharedFile
```

Batch A 实现：`FileFrameSource`、`DirectoryFrameSource`、`FixtureFrameSource`。

## 4. VisionProfile / ROI / Calibration（无 UI）

```text
VisionProfile {
  profile_id, layout_version,
  resolution: {width, height},
  ui_scale?, language?, client_version?, patch_hint?,
  regions: Map<field, RoiRegion>
}

RoiRegion {
  id, type (TEXT|ICON|AREA),
  coordinate_system (SCREEN|NORMALIZED),
  x, y, width, height,
  preprocess?, expected_field?
}
```

- 基线：`classpath:vision/profiles/1920x1080-default.json`；ROI：`player.gold` / `player.level` / `player.hp` / `stage`（占位）
- 未知分辨率 → `UNSUPPORTED_PROFILE`，**不静默拉伸**
- Calibration A：`RoiRegion` 边界校验 + Profile 序列化写出；**无拖拽 UI**

## 5. Sidecar Contract

```yaml
tft.vision.sidecar:
  base-url: http://127.0.0.1:19090
  auto-start: false
  connect-timeout-ms: 500
  read-timeout-ms: 2000
```

Envelope（snake_case）：

```json
{
  "request_id": "...",
  "status": "OK",
  "error_code": null,
  "data": {},
  "meta": { "service_version": "0.1.0", "latency_ms": 12 }
}
```

| HTTP | 路径 | Batch A |
|------|------|---------|
| GET | `/health` | 必做 |
| GET | `/ready` | 必做（轻量：stub 即 ready） |
| POST | `/roi/crop` | 可选（无 OpenCV 可跳过） |
| POST | `/vision/analyze` | Stub Provider |

错误码：`INVALID_IMAGE` / `INVALID_ROI` / `UNSUPPORTED_PROFILE` / `MODEL_NOT_READY` / `TIMEOUT` / `INTERNAL_ERROR`。

Python：`VisionProvider` SPI；A = `StubVisionProvider`；B = Paddle。

## 6. Observation（1.0.x 兼容扩展）

保留必填：`schema_version=1.0.0`、`field`、`value`、`confidence`、`source`、`timestamp`。

新增可选（snake_case）：`raw_value`、`detector`、`model`、`model_version`、`bbox`、`preprocess_version`、`observation_id`（已有 `roi` / `frame_id`）。

- `value` = 归一化结果；`raw_value` = OCR/检测原始串  
- `source` ∈ `ocr|icon|manual|derived|fixture`  
- Confidence 分层仅文档备注，A 不实现门禁  
- 同步更新：`tft-contracts` + 根目录 `schemas/canonical/observation.schema.json`（若双份）

## 7. 工程结构

```
tft-vision/src/main/java/.../vision/{frame,profile,sidecar}/
tft-state/src/main/java/.../state/observation/
vision-sidecar/{app,profiles,tests,scripts}/
tft-orchestrator/.../VisionHealthController.java
```

## 8. 测试（A 最小集）

| 层级 | 内容 |
|------|------|
| Unit | File/Directory/Fixture；Profile 1080p；ROI 越界拒绝；Observation 合法/非法 |
| Sidecar | TestClient `/health` `/ready`（无 Paddle） |
| Contract | Envelope 字段一致（样例 JSON） |
| Integration | 侧车 down → SidecarClient degraded，不拖垮 `mvn test` |
| CI | **不**装 Python/Paddle |

## 9. 实施顺序

1. A1 FrameSource / VisionFrame / FramePayload  
2. A2 VisionProfile / RoiRegion / Resolver + 占位 JSON  
3. A3 Calibration 数据校验 + Profile 写出（无 UI）  
4. A4 vision-sidecar FastAPI + health/ready + Stub  
5. A5 Java SidecarClient + Envelope + degraded  
6. A6 Observation 1.0.x 扩展 + Validator + fixture 工厂  
7. A7 最小单测 / 负例 / Contract 样例  
8. A8 Orchestrator health API + readme + Roadmap 三任务 DONE  

## 10. Done Definition

| Task | Done |
|------|------|
| Frame-001 | File/Directory/Fixture 单测通过 |
| ROI-001 | 1080p baseline 可加载；未知分辨率失败；ROI 校验；**无 UI** |
| Observation-001 | 1.0.x schema 扩展 + Validator + fixture 工厂 |
| Foundation（内部） | Sidecar health/ready + Stub + Java client degraded |

## 11. 非目标复查

不灌 Data Dragon；不改 P1 在线栈默认行为；不做 Live；不做 GameState。
