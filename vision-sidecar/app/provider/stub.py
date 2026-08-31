"""Stub vision provider for Batch A (no real OCR)."""

from __future__ import annotations

from typing import Any


class StubVisionProvider:
    def name(self) -> str:
        return "stub"

    def capabilities(self) -> dict[str, Any]:
        return {
            "ocr": False,
            "icon": False,
            "crop": False,
        }

    def analyze(self, request: dict[str, Any]) -> dict[str, Any]:
        return {
            "provider": self.name(),
            "field": request.get("field"),
            "raw_value": None,
            "value": None,
            "confidence": 0.0,
            "message": "PaddleOCR not installed; pip install -r requirements-ocr.txt",
        }
