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
    # 1. Initiate authorization flow to receive state and session cookie
    auth_res = client.get("/api/v1/integrations/google/authorize")
    assert auth_res.status_code == 200
    state = auth_res.json()["state"]
    assert "oauth_state" in client.cookies

    # 2. Complete callback with matching state
    res = client.get(f"/api/v1/integrations/google/callback?code=mock_oauth_code&state={state}")
    assert res.status_code == 200
    assert res.json()["status"] == "success"
    client.cookies.clear()


def test_google_oauth_callback_without_cookie_rejected() -> None:
    client.cookies.clear()
    res = client.get("/api/v1/integrations/google/callback?code=mock_oauth_code&state=unauthorized_state")
    assert res.status_code == 400
    assert "potential CSRF attack" in res.json()["detail"]


def test_whatsapp_webhook_verification() -> None:
    token = settings.whatsapp_verify_token
    res = client.get(f"/api/v1/integrations/whatsapp/webhook?hub.mode=subscribe&hub.verify_token={token}&hub.challenge=test_challenge")
    assert res.status_code == 200
    assert res.text == "test_challenge"


def test_vision_pipeline_empty_buffer_handling() -> None:
    with pytest.raises(ValueError, match="Empty image byte buffer"):
        OpenCvNotePipeline.compute_metrics(b"")


def test_vision_pipeline_invalid_hex_handling() -> None:
    result = OpenCvNotePipeline.evaluate_duplicate_pair("INVALID_HEX", "NOT_A_HEX", "123", "456", 0.5)
    assert result.decision in ("UNIQUE", "POSSIBLE_DUPLICATE_KEEP_BOTH_FLAGGED", "CONFIRMED_DUPLICATE_SEND_LOWER_TO_TRASH")


def test_attendance_zero_classes_handling() -> None:
    payload = {
        "attended_classes": 0,
        "total_classes": 0,
        "target_pct": 75.0,
    }
    res = client.post("/api/v1/attendance/calculate", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["must_attend_classes"] == 0
    assert data["safe_to_skip_classes"] == 0
    assert "No classes recorded yet" in data["liquid_pill_copy"]


def test_attendance_high_target_pct() -> None:
    payload = {
        "attended_classes": 38,
        "total_classes": 40,
        "target_pct": 95.0,
    }
    res = client.post("/api/v1/attendance/calculate", json=payload)
    assert res.status_code == 200
    data = res.json()
    assert data["is_above_threshold"] is True
    assert data["percentage"] == 95.0


def test_attendance_ceil_rounding_consistency() -> None:
    # 74.95% raw, with CEIL becomes 75.0%
    payload = {
        "attended_classes": 7495,
        "total_classes": 10000,
        "target_pct": 75.0,
        "rounding_mode": "CEIL",
    }
    res = client.post("/api/v1/attendance/calculate", json=payload)
    assert res.status_code == 200
    data = res.json()
    # is_above_threshold and must_attend_classes must NEVER contradict
    assert data["is_above_threshold"] is True
    assert data["must_attend_classes"] == 0


def test_oauth_state_csrf_protection() -> None:
    # Missing state should be rejected
    res_missing = client.get("/api/v1/integrations/google/callback?code=mock_oauth_code")
    assert res_missing.status_code == 400

    # Mismatched state should be rejected
    client.cookies.set("oauth_state", "expected_state_123")
    res_mismatch = client.get("/api/v1/integrations/google/callback?code=mock_oauth_code&state=attacker_state_456")
    assert res_mismatch.status_code == 400

    # Matching state should succeed
    res_valid = client.get("/api/v1/integrations/google/callback?code=mock_oauth_code&state=expected_state_123")
    assert res_valid.status_code == 200
    client.cookies.clear()


def test_fernet_stable_key() -> None:
    f1 = settings.get_fernet()
    f2 = settings.get_fernet()
    test_msg = b"secret_oauth_token_data"
    token = f1.encrypt(test_msg)
    decrypted = f2.decrypt(token)
    assert decrypted == test_msg


def test_stitch_pdfs_empty_buffers() -> None:
    with pytest.raises(ValueError, match="No valid PDF pages found to stitch"):
        OpenCvNotePipeline.stitch_pdfs([b"", b""])


def test_stitch_pdfs_valid_pages() -> None:
    import io
    from pypdf import PdfWriter

    def make_dummy_pdf() -> bytes:
        w = PdfWriter()
        w.add_blank_page(width=100, height=100)
        out = io.BytesIO()
        w.write(out)
        return out.getvalue()

    p1 = make_dummy_pdf()
    p2 = make_dummy_pdf()
    stitched = OpenCvNotePipeline.stitch_pdfs([p1, p2])
    assert len(stitched) > 0
    from pypdf import PdfReader
    reader = PdfReader(io.BytesIO(stitched))
    assert len(reader.pages) == 2



