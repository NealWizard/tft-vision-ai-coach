"""FastAPI entry for TFT vision sidecar."""

from __future__ import annotations

import time
import uuid
from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel

from app.provider.paddle_ocr import PaddleOcrProvider
from app.provider.stub import StubVisionProvider

SERVICE_VERSION = "0.1.0"
app = FastAPI(title="tft-vision-sidecar", version=SERVICE_VERSION)


def _load_provider():
    paddle = PaddleOcrProvider()
    if paddle.available():
        return paddle
    return StubVisionProvider()


provider = _load_provider()


def envelope(
    *,
    status: str = "OK",
    error_code: str | None = None,
    data: dict[str, Any] | None = None,
    request_id: str | None = None,
    started: float | None = None,
) -> dict[str, Any]:
    latency_ms = None if started is None else int((time.perf_counter() - started) * 1000)
    return {
        "request_id": request_id or f"req-{uuid.uuid4()}",
        "status": status,
        "error_code": error_code,
        "data": data or {},
        "meta": {
            "service_version": SERVICE_VERSION,
            "latency_ms": latency_ms if latency_ms is not None else 0,
        },
    }


def _ocr_ready() -> bool:
    return bool(provider.capabilities().get("ocr"))


@app.get("/health")
def health() -> dict[str, Any]:
    started = time.perf_counter()
    return envelope(
        data={
            "ocr_ready": _ocr_ready(),
            "ready": True,
            "provider": provider.name(),
        },
        started=started,
    )


@app.get("/ready")
def ready() -> dict[str, Any]:
    started = time.perf_counter()
    caps = provider.capabilities()
    return envelope(
        data={
            "ready": True,
            "ocr_ready": caps.get("ocr", False),
            "capabilities": caps,
        },
        started=started,
    )


class AnalyzeRequest(BaseModel):
    request_id: str | None = None
    field: str | None = None
    image_base64: str | None = None


@app.post("/vision/analyze")
def analyze(body: AnalyzeRequest) -> dict[str, Any]:
    started = time.perf_counter()
    result = provider.analyze(body.model_dump())
    error_code = result.get("error_code")
    ocr_ok = provider.name() != "stub" and not error_code
    status = "OK" if ocr_ok else "DEGRADED"
    if status == "DEGRADED" and not error_code:
        error_code = "MODEL_NOT_READY"
    return envelope(
        request_id=body.request_id,
        status=status,
        error_code=error_code,
        data=result,
        started=started,
    )
