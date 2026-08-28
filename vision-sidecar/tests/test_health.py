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
    assert body["data"]["ocr_ready"] is False
    assert body["meta"]["service_version"]


def test_ready_envelope():
    r = client.get("/ready")
    assert r.status_code == 200
    assert r.json()["data"]["ready"] is True


def test_analyze_stub():
    r = client.post("/vision/analyze", json={"field": "player.gold"})
    assert r.status_code == 200
    body = r.json()
    assert body["error_code"] == "MODEL_NOT_READY"
    assert body["data"]["provider"] == "stub"
