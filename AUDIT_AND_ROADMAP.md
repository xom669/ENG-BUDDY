# EngBuddy Project Analysis, Codebase Audit & Actionable Roadmap

**Date:** November 2026  
**Audited Repository:** `EngBuddy` (AI-Powered Engineering Student Companion)  
**Report File:** `AUDIT_AND_ROADMAP.md`  

---

## Executive Summary

**EngBuddy** is conceived as a comprehensive "super-app" for engineering students, combining:
1. **Intelligent Document & Note Vault:** Multi-page note scanning, on-device blur detection, perceptual hash deduplication, timetable-aware subject filing, and RAG exam prep.
2. **Smart Attendance & Bunk Manager:** Mathematical safe bunk/recovery calculator, institutional rule customization (lab weights, medical leave, tutorial bonus points), and zero-app-open interactive notification actions.
3. **Student Profile Pass:** Liquid-glass digital student ID pass with auto-cropped physical card flash preview.
4. **Omnichannel Ingestion:** Native OS share sheet support (WhatsApp, file managers) and cloud syncing (Google Classroom, Gmail).

### Current Codebase Reality
The codebase is currently in a **hybrid prototype stage**:
- **Android Native App (`app/`):** Contains ~80% of the planned UI logic, Room database models, pure-Kotlin computer vision math (Laplacian variance, pHash, dHash, SSIM), and notification action handlers. However, the camera scanner is simulated with Compose canvas, live Gemini API integration is bypassed by a mock placeholder, and automatic timetable background scheduling (`WorkManager` / `AlarmManager`) is missing.
- **Backend (`backend/`):** FastAPI models and Celery definitions exist, but the API only implements basic attendance calculation math. Note upload, vector chunking, pgvector search, Gemini RAG, and OAuth endpoints are completely missing. Celery tasks are non-functional stubs.
- **Flutter Stub (`flutter_app/`):** A skeleton project with one hardcoded file (`main.dart`), introducing architectural ambiguity against the native Android app.
- **Build & DevOps Infrastructure:** Missing Gradle wrapper scripts (`gradlew`, `gradlew.bat`), missing `alembic.ini`, and missing Docker/Kubernetes deployment manifests.

---

## 1. Architectural Discrepancies & Strategic Decision

### The "Dual Frontend" Dilemma (Android Native vs. Flutter)
The repository contains both:
1. A **Native Android project** in Kotlin + Jetpack Compose (`app/`): Highly detailed, complete Room SQLite schema, UI screens, and theme tokens.
2. A **Flutter skeleton** in Dart (`flutter_app/`): Only a basic shell with hardcoded attendance numbers.

> [!IMPORTANT]
> **Decision Required:** You must decide which frontend path to standardize on:
> - **Option A (Recommended for immediate progress): Native Android.** The Kotlin Jetpack Compose app already has 90% of the UI, Room database, and vision algorithms written. Stick with Android first to get an MVP working.
> - **Option B: Flutter Cross-Platform.** If iOS and Desktop (Windows/macOS) support is an immediate day-one requirement, delete `app/` and port the business logic from Kotlin to Dart using Riverpod and Drift.

---

## 2. In-Depth Component Audit

### 2.1 Android Application (`app/`)

| Component | Current State | What's Missing / Broken | Severity |
| :--- | :--- | :--- | :--- |
| **Build Configuration** | `app/build.gradle.kts` targets Android SDK 36. | `gradlew` and `gradlew.bat` are missing from root. `debugConfig` signing references missing `debug.keystore`. | [High] |
| **Camera & Scanner** | `CameraScannerScreen.kt` draws a simulated polygon on a Compose Canvas. | CameraX and Google ML Kit Document Scanner are commented out in `build.gradle.kts`. No real camera capture or hardware edge-detection. | [High] |
| **AI Integration** | `PlaceholderAiRepository` provides deterministic mock string responses. | Real Gemini API calls via `google-genai` SDK or backend proxy are not implemented. | [High] |
| **Timetable Notifications** | `AttendanceNotificationHelper` builds notifications; `AttendanceActionReceiver` updates Room. | No background scheduler (`WorkManager` or `AlarmManager`). Notifications can only be fired manually from UI. | [Medium] |
| **Local Persistence** | Comprehensive Room database with 9 entities and clean DAOs. | Migrations are not defined (`fallbackToDestructiveMigration` is used). Data can be wiped on schema change. | [Medium] |
| **On-Device Vision Pipeline** | `VisionQualityPipeline.kt` implements pure Kotlin Laplacian variance, pHash, dHash, and SSIM. | Good implementation! However, running software DCT and SSIM on large unscaled bitmaps on the UI thread will cause frame drops. Needs strict background dispatching and OpenCV NDK acceleration for large images. | [Low] |

---

### 2.2 Backend Service (`backend/`)

| Component | Current State | What's Missing / Broken | Severity |
| :--- | :--- | :--- | :--- |
| **API Endpoints** | Only `/healthz`, `/readyz`, and `/api/v1/attendance/...` exist. | No endpoints for note uploads, PDF generation, pgvector note chunking, RAG querying, or Google OAuth callbacks. | [High] |
| **Celery Worker** | `celery_app.py` has task signatures. | `process_note_batch_and_stitch_pdf` and `dispatch_timetable_attendance_prompt` return static dummy dictionaries. | [High] |
| **Database Migrations** | `0001_initial_pgvector.py` exists. | Missing `alembic.ini` and `env.py`. The migration omits the `oauth_connections` table defined in `models.py`. | [High] |
| **Token Encryption** | `settings.get_fernet()` in `config.py`. | **Security Flaw:** If `OAUTH_TOKEN_ENCRYPTION_KEY` is not set, it generates a new random key per call, making stored tokens un-decryptable across requests. | [High] |
| **Tests & Linting** | `ci.yml` expects `ruff`, `mypy`, and pytest. | Zero test files exist in `backend/tests/`. CI will pass with 0 tests. | [Medium] |

---

### 2.3 DevOps, Cloud & CI/CD (`infra/`)

| Area | Current State | Recommendation |
| :--- | :--- | :--- |
| **Dockerization** | No `Dockerfile` or `docker-compose.yml`. | Add multi-stage Dockerfiles for FastAPI and Celery workers, plus Docker Compose for local PostgreSQL (pgvector) and Redis. |
| **Kubernetes (K8s)** | The blueprint promises K8s + KEDA autoscaling, but zero manifests exist. | Create K8s manifests (`deployment.yaml`, `service.yaml`, `keda-scaledobject.yaml`) for exam season scaling. |
| **Secrets Management** | `.env.example` has backend keys, but lacks `GEMINI_API_KEY`. | Unify `.env.example` across Android and backend. |

---

## 3. Prioritized Step-by-Step Action Plan ("What You Need To Do")

### Phase 1: Fix Build Environment & Repositories (Immediate)
1. **Generate the Gradle Wrapper:**
   - Run `gradle wrapper --gradle-version 8.11.1` in the project root to generate `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`.
2. **Fix `app/build.gradle.kts` Signing Config:**
   - Remove or safeguard the line:
     ```kotlin
     signingConfig = signingConfigs.getByName("debugConfig")
     ```
     Or allow fallback to the standard Android SDK debug keystore (`~/.android/debug.keystore`).
3. **Consolidate Project Structure:**
   - Archive or remove `flutter_app/` if focusing on native Android, or clearly designate it as an experimental target to prevent developer confusion.
4. **Fix `.env.example`:**
   - Add `GEMINI_API_KEY=your_gemini_api_key_here` to `.env.example`.

---

### Phase 2: Complete the Android Native MVP
1. **Enable Real Camera & ML Kit Scanner:**
   - Uncomment camera dependencies in `app/build.gradle.kts`:
     ```kotlin
     implementation(libs.androidx.camera.camera2)
     implementation(libs.androidx.camera.lifecycle)
     implementation(libs.androidx.camera.view)
     // Add Google ML Kit Document Scanner:
     implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
     ```
   - Replace the simulated canvas viewfinder in `CameraScannerScreen.kt` with the official Google ML Kit Document Scanner intent or CameraX PreviewView.
2. **Implement Real Gemini AI Explainer:**
   - Integrate the official Google GenAI SDK (`com.google.ai.client.generativeai:generativeai`) or connect the Android app to your FastAPI backend.
   - Replace `PlaceholderAiRepository` with a real `GeminiAiRepository` that sends the note OCR text and prompt to `gemini-2.5-flash`.
3. **Add Automated Timetable Scheduling (WorkManager):**
   - Add `androidx.work:work-runtime-ktx` to dependencies.
   - Create `TimetableCheckWorker` that runs every 15–30 minutes, checks `timetableSlots` against the device clock, and dispatches `AttendanceNotificationHelper.showInteractiveAttendanceNotification()` when a class starts.

---

### Phase 3: Build Out the Backend & RAG Pipeline
1. **Fix Alembic & Database Schema:**
   - Initialize Alembic properly (`alembic init -t async alembic`).
   - Add `oauth_connections` table to `0001_initial_pgvector.py`.
2. **Fix Token Encryption in `config.py`:**
   - Enforce that `OAUTH_TOKEN_ENCRYPTION_KEY` is a persistent valid Fernet key:
     ```python
     @property
     def fernet(self) -> Fernet:
         if not self.oauth_token_encryption_key:
             raise ValueError("OAUTH_TOKEN_ENCRYPTION_KEY must be configured in .env")
         return Fernet(self.oauth_token_encryption_key.encode("utf-8"))
     ```
3. **Implement Core Backend Endpoints:**
   - `POST /api/v1/vault/upload`: Accepts multi-part note images, uploads to S3/MinIO, triggers Celery stitch task.
   - `POST /api/v1/ai/explain`: Receives student questions, performs vector similarity search in `note_chunks` using `pgvector`, and queries Gemini 2.5 Flash for grounded citations.
4. **Implement Celery Task Logic:**
   - Use `pypdf` in `process_note_batch_and_stitch_pdf` to actually concatenate images into a PDF and compute text embeddings via `google-genai` SDK.

---

### Phase 4: DevOps & Production Deployment
1. **Docker Compose Setup:**
   - Create `docker-compose.yml` with:
     - `api`: FastAPI container.
     - `worker`: Celery worker container.
     - `db`: PostgreSQL 16 with `pgvector/pgvector:pg16` image.
     - `redis`: Redis 7 container for broker and result backend.
     - `minio`: Local S3-compatible object storage.
2. **CI Pipeline Validation:**
   - Add unit tests for `backend/app/main.py` and `backend/app/services/vision_pipeline.py`.
   - Update `.github/workflows/ci.yml` so that tests actually execute and report pass/fail status.

---

## 4. Summary Checklist for Developer

- [ ] **Step 1:** Run `gradle wrapper` to generate missing `gradlew`.
- [ ] **Step 2:** Fix `signingConfig` in `app/build.gradle.kts`.
- [ ] **Step 3:** Add `GEMINI_API_KEY` to `.env` and `.env.example`.
- [ ] **Step 4:** Replace `PlaceholderAiRepository` with real Gemini API client.
- [ ] **Step 5:** Replace Canvas camera mockup with Google ML Kit Document Scanner.
- [ ] **Step 6:** Implement Android `WorkManager` for timetable background notifications.
- [ ] **Step 7:** Fix Alembic migration and `OAUTH_TOKEN_ENCRYPTION_KEY` vulnerability in backend.
- [ ] **Step 8:** Add `docker-compose.yml` to run Postgres (pgvector), Redis, FastAPI, and Celery locally.

