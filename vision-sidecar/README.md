# TFT Vision Sidecar (Batch A)

本地 HTTP 侧车：`127.0.0.1:19090`。Batch A 仅提供 `/health`、`/ready`、`POST /vision/analyze` Stub（无真实 OCR）。

## 开发启动

```powershell
cd vision-sidecar
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\run-dev.ps1
```

探测：

```text
GET  http://127.0.0.1:19090/health
GET  http://127.0.0.1:19090/ready
POST http://127.0.0.1:19090/vision/analyze
```

Java CI **不**依赖本目录或 Paddle。商用 spawn / PyInstaller 后续任务。
