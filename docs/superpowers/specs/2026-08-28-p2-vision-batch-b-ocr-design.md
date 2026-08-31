# P2 Vision Batch B 设计（数值 OCR）

> 日期：2026-08-28  
> 状态：已确认（方案 A）  
> Roadmap：`P2-VISION-OCR-001`（本批打通链路；**不**把受控集 ≥97% 作为 Done）  
> 明确延后：Shop/Board/Entity、GameState、Cloud Vision、标定 UI、黄金截图集

## 0. 已确认决策

| 项 | 选择 |
|----|------|
| OCR 引擎 | PaddleOCR，装在 `vision-sidecar`；未安装则 Stub + `MODEL_NOT_READY` |
| CI | **不**装 Paddle / Python OCR；`mvn test` 只测 Java 归一化、crop、client 降级 |
| ROI | 仍用 1080p **占位**坐标；Java 按 Profile crop 后再送给侧车 |
| 字段 | `player.gold` / `player.level` / `player.hp` / `player.xp` / `player.streak` / `stage` |
| 97% 门禁 | 有标注截图后再补；本批 OCR-001 **保持 TODO** |
| 合成图 | 仅用于 `NumericNormalizer` 单测（如 `4l`→41），不是假 OCR 引擎 |

## 1. 链路

```
FrameSource → VisionProfile ROI → Java crop（ImageIO）
    → POST /vision/analyze { field, image_base64 }
    → PaddleOCR | Stub
    → raw_value + confidence
    → NumericNormalizer → Observation（source=ocr）
```

侧车只认「已裁切小图」；Profile / ROI 留在 Java。

## 2. Sidecar

- `GET /health` `/ready`：`ocr_ready=true` 仅当 Paddle 实际可初始化  
- `POST /vision/analyze`：Paddle 成功 → `status=OK`；否则 `DEGRADED` + `MODEL_NOT_READY`  
- `requirements.txt` 仍无 Paddle；`requirements-ocr.txt` 为可选  
- 归一化：Python 与 Java 规则对齐（`l/I→1`，`O/o→0`）；`stage` 保留 `N-M`

## 3. Java

- `NumericNormalizer`  
- `RoiCropper`：越界 → `INVALID_ROI`  
- `SidecarClient.analyze`：超时独立配置（默认 15s，因模型冷启动）  
- `ObservationFactory.fromOcr(...)`  
- `POST /api/v1/vision/analyze`：侧车挂则 200 + `degraded=true`

## 4. 非目标

Shop 五卡、Board、Builder、Fusion、Benchmark 黄金集、auto-spawn、Cloud Vision。
