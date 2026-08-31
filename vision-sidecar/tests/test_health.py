"""Minimal health tests (no Paddle). Run: pytest from vision-sidecar with PYTHONPATH=. """

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_envelope():
    r = client.get("/health")
    assert r.status_code == 200
    body = r.json()
    assert body["status"] == "OK"
    assert "request_id" in body
    assert body["data"]["ocr_ready"] in (True, False)
    assert body["meta"]["service_version"]


def test_ready_envelope():
    r = client.get("/ready")
    assert r.status_code == 200
    assert r.json()["data"]["ready"] is True


def test_analyze_without_model_or_image():
    r = client.post("/vision/analyze", json={"field": "player.gold"})
    assert r.status_code == 200
    body = r.json()
    if body["data"].get("provider") == "stub":
        assert body["status"] == "DEGRADED"
        assert body["error_code"] == "MODEL_NOT_READY"
    else:
        assert body["error_code"] in ("INVALID_IMAGE", "MODEL_NOT_READY", None)
