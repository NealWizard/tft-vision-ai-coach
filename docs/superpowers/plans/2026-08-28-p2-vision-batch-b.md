# P2 Vision Batch B 实现计划

规格：`docs/superpowers/specs/2026-08-28-p2-vision-batch-b-ocr-design.md`

1. Java `NumericNormalizer` + 单测  
2. `RoiCropper` + Profile 增加 xp/streak 占位 ROI  
3. Sidecar `PaddleOcrProvider` 可选加载；analyze 契约  
4. `SidecarClient.analyze` + ObservationFactory.fromOcr  
5. Orchestrator `POST /api/v1/vision/analyze`  
6. readme；OCR-001 不标 DONE  
