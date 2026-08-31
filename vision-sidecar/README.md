# TFT Vision Sidecar

本地 HTTP 侧车：`127.0.0.1:19090`。

- 默认：`/health` `/ready` + Stub `POST /vision/analyze`（`MODEL_NOT_READY`）
- 可选：`pip install -r requirements-ocr.txt` 后自动切 PaddleOCR；`ocr_ready=true`

```powershell
cd vision-sidecar
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
# 可选真实 OCR：
# pip install -r requirements-ocr.txt
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\run-dev.ps1
```

Java CI **不**安装 Paddle。
