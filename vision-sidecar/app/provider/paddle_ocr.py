"""Optional PaddleOCR provider. Missing install → unavailable."""

from __future__ import annotations

import base64
import os
import tempfile
from typing import Any

from app.numeric import canonicalize_raw, normalize


class PaddleOcrProvider:
    def __init__(self) -> None:
        self._ocr = None
        self._error: str | None = None
        try:
            from paddleocr import PaddleOCR

            self._ocr = _init_paddle(PaddleOCR)
        except Exception as exc:  # noqa: BLE001 — optional dependency
            self._error = str(exc)

    def name(self) -> str:
        return "paddle"

    def available(self) -> bool:
        return self._ocr is not None

    def capabilities(self) -> dict[str, Any]:
        return {
            "ocr": self.available(),
            "icon": False,
            "crop": False,
        }

    def analyze(self, request: dict[str, Any]) -> dict[str, Any]:
        field = request.get("field")
        if not self.available():
            return {
                "provider": self.name(),
                "field": field,
                "raw_value": None,
                "value": None,
                "confidence": 0.0,
                "error_code": "MODEL_NOT_READY",
                "message": self._error or "PaddleOCR not installed",
            }
        image_b64 = request.get("image_base64")
        if not image_b64:
            return {
                "provider": self.name(),
                "field": field,
                "raw_value": None,
                "value": None,
                "confidence": 0.0,
                "error_code": "INVALID_IMAGE",
                "message": "image_base64 required",
            }
        path = _write_temp_image(str(image_b64))
        try:
            raw, conf = _run_ocr(self._ocr, path)
        finally:
            try:
                os.remove(path)
            except OSError:
                pass
        value = normalize(field, raw)
        return {
            "provider": self.name(),
            "field": field,
            "raw_value": canonicalize_raw(raw) if raw else raw,
            "value": value,
            "confidence": conf,
            "model": "paddleocr",
            "error_code": None,
            "message": None,
        }


def _init_paddle(cls):
    for kwargs in (
        {"lang": "en", "use_angle_cls": False, "show_log": False},
        {"lang": "en", "use_textline_orientation": False},
        {"lang": "en"},
        {},
    ):
        try:
            return cls(**kwargs)
        except TypeError:
            continue
    raise TypeError("PaddleOCR constructor signature not supported")


def _write_temp_image(image_b64: str) -> str:
    payload = image_b64.strip()
    if "," in payload and payload.lower().startswith("data:"):
        payload = payload.split(",", 1)[1]
    raw = base64.b64decode(payload)
    fd, path = tempfile.mkstemp(suffix=".png")
    with os.fdopen(fd, "wb") as handle:
        handle.write(raw)
    return path


def _run_ocr(ocr, path: str) -> tuple[str | None, float]:
    if hasattr(ocr, "ocr"):
        result = ocr.ocr(path)
        return _parse_v2(result)
    if hasattr(ocr, "predict"):
        result = ocr.predict(path)
        return _parse_predict(result)
    return None, 0.0


def _parse_v2(result) -> tuple[str | None, float]:
    if not result:
        return None, 0.0
    lines = result[0] if isinstance(result, list) and result else result
    texts: list[str] = []
    confs: list[float] = []
    if not lines:
        return None, 0.0
    for item in lines:
        try:
            text, conf = item[1]
            texts.append(str(text))
            confs.append(float(conf))
        except (TypeError, ValueError, IndexError):
            continue
    if not texts:
        return None, 0.0
    return "".join(texts), (sum(confs) / len(confs) if confs else 0.0)


def _parse_predict(result) -> tuple[str | None, float]:
    if not result:
        return None, 0.0
    first = result[0] if isinstance(result, list) else result
    if isinstance(first, dict):
        rec = first.get("rec_text") or first.get("text")
        conf = first.get("rec_score") or first.get("score") or 0.0
        if isinstance(rec, list):
            rec = "".join(str(x) for x in rec)
        return (str(rec) if rec else None, float(conf) if conf is not None else 0.0)
    return _parse_v2(result)
