from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from app.core.config import settings
from app.main import app
from app.services.vision_pipeline import OpenCvNotePipeline

client = TestClient(app)


def test_healthz() -> None:
    res = client.get("/healthz")
    assert res.status_code == 200
    assert res.json()["status"] == "ok"


def test_readyz() -> None:
    res = client.get("/readyz")
    assert res.status_code == 200
    assert res.json()["status"] == "ready"


def test_attendance_calculate_safe_bunks() -> None:
    payload = {
        "attended_classes": 26,
        "total_classes": 32,
        "medical_excused_classes": 1,
        "target_pct": 75.0,
        "lab_weight_classes": 2,
        "medical_leave_excused": True,
        "rounding_mode": "NEAREST",
    }
    res = client.post("/api/v1/attendance/calculate", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["is_above_threshold"] is True
    assert data["zone"] == "SAFE_GREEN"
    assert data["safe_to_skip_classes"] == 4
    assert data["safe_to_skip_labs"] == 2
    assert "You can skip the next 4 classes" in data["liquid_pill_copy"]


def test_attendance_calculate_danger_recovery() -> None:
    payload = {
        "attended_classes": 18,
        "total_classes": 28,
        "medical_excused_classes": 0,
        "target_pct": 75.0,
        "lab_weight_classes": 2,
        "medical_leave_excused": False,
        "rounding_mode": "FLOOR",
    }
    res = client.post("/api/v1/attendance/calculate", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["is_above_threshold"] is False
    assert data["zone"] == "DANGER_RED"
    assert data["must_attend_classes"] == 12
    assert data["safe_to_skip_classes"] == 0
    assert "Attend the next 12 classes straight" in data["liquid_pill_copy"]


def test_attendance_notification_action() -> None:
    payload = {
        "subject_code": "ME-204",
        "action": "YES_ATTENDING",
        "attended_classes": 25,
        "total_classes": 30,
        "weight_points": 1,
        "target_pct": 75.0,
    }
    res = client.post("/api/v1/attendance/notification-action", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["effective_attended"] == 26
    assert data["effective_total"] == 31


def test_vault_upload() -> None:
    payload = {
        "user_id": "test_user_123",
        "title": "Thermodynamics Chapter 3",
        "chapter_title": "Entropy & Carnot",
        "subject_code": "ME-204",
        "page_count": 4,
        "page_ids": ["p1", "p2", "p3", "p4"],
    }
    res = client.post("/api/v1/vault/upload", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["status"] == "PROCESSING"
    assert "document_id" in data


def test_ai_explain_preview_mode() -> None:
    payload = {
        "query": "Explain Carnot efficiency in plain terms",
        "mode": "ELI5",
        "subject_code": "ME-204",
        "note_excerpts": ["Carnot engine operates between T_H and T_L with efficiency eta = 1 - T_L/T_H"],
    }
    res = client.post("/api/v1/ai/explain", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert "Carnot efficiency" in data["markdown_answer"]
    assert len(data["citations"]) > 0


def test_google_oauth_callback() -> None:
    res = client.get("/api/v1/integrations/google/callback?code=mock_oauth_code&state=xyz")
    assert res.status_code == 200
    assert res.json()["status"] == "success"


def test_whatsapp_webhook_verification() -> None:
    token = settings.whatsapp_verify_token
    res = client.get(f"/api/v1/integrations/whatsapp/webhook?hub.mode=subscribe&hub.verify_token={token}&hub.challenge=test_challenge")
    assert res.status_code == 200
    assert res.text == "test_challenge"

