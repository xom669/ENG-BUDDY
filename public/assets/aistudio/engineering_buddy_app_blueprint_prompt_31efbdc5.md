# EngBuddy: The Ultimate Engineering Student Companion
**Architecture, Master AI Prompt, & Risk Analysis Blueprint**

---

## 1. Modern Tech Stack (Cross-Platform + Cloud-Native)

To achieve seamless cross-platform compatibility (Mobile + PC), deep OS-level sharing (WhatsApp), background processing (image deduplication & OCR), and Kubernetes scalability, here is the recommended modern stack:

### **Frontend (Mobile & PC)**
*   **Framework:** **Flutter** (Single codebase compiling natively to iOS, Android, Web, Windows, and macOS) *OR* **React Native (Expo) + Next.js ( via Solito/Tamagui)**. *Recommendation: Flutter is superior here for consistent camera/scanner APIs and replicating the fluid, gradient-heavy Gemini UI across both mobile and desktop.*
*   **State Management:** Riverpod (Flutter) or Zustand + TanStack Query (React).
*   **OS Integration:** `receive_sharing_intent` (allows the app to appear in the Android/iOS native "Share Sheet" so users can share PDFs/images directly from WhatsApp or Email into your app).

### **Backend & API Layer**
*   **Core API:** **FastAPI (Python)** or **Go (Fiber)**. *Recommendation: FastAPI is ideal because your backend is heavily reliant on AI, Computer Vision, and OCR pipelines.*
*   **Async Task Queue:** **Celery + Redis** or **Temporal.io** (Crucial for handling heavy background tasks like stitching PDFs, running OCR, and calling LLMs without freezing the user's phone screen).

### **AI, Vision & Smart Processing**
*   **Multimodal AI & Reasoning:** **Gemini 2.5 Flash / Pro API** (Used for explaining complex engineering notes, extracting syllabus schedules from academic calendar PDFs, and classifying which subject/chapter a photographed note belongs to).
*   **Computer Vision (On-Device + Cloud):** **OpenCV + Google ML Kit Document Scanner**.
    *   *Blur Detection:* Variance of the Laplacian method (OpenCV) to score image sharpness.
    *   *Deduplication:* Perceptual Hashing (`pHash` / `dHash`) or SSIM (Structural Similarity Index) to detect if two photos are of the same page, automatically deleting the one with the lower sharpness score.

### **Third-Party Integrations**
*   **Google Classroom & Gmail:** Google Workspace REST APIs via OAuth 2.0 (with Google Cloud Pub/Sub webhooks for real-time push notifications when a professor uploads an assignment).
*   **WhatsApp:** Native OS Share Target (Zero cost, high privacy) + Optional WhatsApp Cloud API forwarder bot.

### **Database & Storage**
*   **Primary Database:** **PostgreSQL** (hosted on Supabase or Neon) for relational data (users, attendance logs, subjects, timetable).
*   **Vector Database:** **`pgvector`** (PostgreSQL extension) to store embeddings of all student notes so the AI "Buddy" can answer questions based on the student's actual handwritten notes (RAG).
*   **Object Storage:** **Cloudflare R2** or **AWS S3** (for storing raw note images and compiled PDFs).

### **DevOps & Infrastructure**
*   **Containerization:** **Docker** (Multi-stage builds for lightweight Python/FastAPI and worker containers).
*   **Orchestration:** **Kubernetes (GKE / AWS EKS / DigitalOcean Kubernetes)** with **KEDA** (Kubernetes Event-driven Autoscaling) to automatically spin up more vision-processing pods during exam season when thousands of students scan notes simultaneously.
*   **Ingress & Traffic:** NGINX Ingress Controller or Traefik with Cert-Manager for SSL.

---

## 2. The Master Prompt (Copy & Paste for AI Builders/Developers)

Use the prompt below when instructing an AI coding assistant (like Cursor, Windsurf, or Gemini) or a development team to build the application:

> **System Prompt / Project Specification:**
> 
> Act as a Principal Full-Stack Architect and UI/UX Designer. We are building **"EngBuddy"**, a cross-platform (Mobile iOS/Android & Desktop Web) super-app designed specifically as an all-in-one survival and organization tool for engineering students.
> 
> ### **1. UI/UX Design Language (Gemini Aesthetic)**
> *   **Style:** Moderate complexity, ultra-clean, modern conversational + dashboard hybrid modeled after the **Google Gemini app UI**.
> *   **Theme:** Deep dark mode (`#131314` background, `#1E1F20` surface cards) and clean light mode, featuring subtle Aurora/Gemini-style blue-to-purple-to-peach gradients (`#4b90ff` to `#ff5546`) for AI actions, active states, and floating action buttons.
> *   **Navigation:** Bottom navigation bar on mobile (Home/Feed, Smart Vault, Camera Scanner, Attendance & Hacks, AI Buddy Chat) that transforms into a collapsible left sidebar on Desktop/PC.
> *   **Typography:** Google Sans / Inter for crisp readability, with JetBrains Mono for code/formula blocks.
> 
> ### **2. Core Functional Modules**
> *   **Module A: Omnichannel Ingestion Engine (Connectivity)**
>     *   Implement OS Share Target intents so users can share files/images directly from **WhatsApp** or native file managers into EngBuddy.
>     *   Integrate OAuth2 for **Google Classroom** (auto-sync coursework, announcements, and drive attachments) and **Gmail** (auto-import emails matching academic filters/keywords).
> *   **Module B: Intelligent Camera & Note Segregation Pipeline**
>     *   Build an in-app camera scanner with auto-edge detection.
>     *   **Deduplication & Quality Filter:** When scanning a batch of notes, calculate Perceptual Hash (`pHash`) to find duplicate pages clicked twice. Calculate Laplacian Variance to measure blur/sharpness, automatically discarding the lower-quality duplicate.
>     *   **Auto-Ordering & Classification:** Use Multimodal LLM (Gemini API) to read page numbers, headings, and topics. Automatically sort pages into chronological/logical order, stitch them into a clean PDF, identify the engineering subject and chapter based on the user's active syllabus/timetable, and file it into the correct folder.
> *   **Module C: "Engineering Cheats" & Survival Toolkit**
>     *   **Smart Attendance Calculator ("Bunk Manager"):** Track subject-wise attendance percentage. Calculate exact numbers of classes a student *must attend* to hit 75% (or custom threshold), or how many classes they can *safely skip* without dropping below the threshold.
>     *   **AI Notes Explainer & Exam Crammer:** Allow students to highlight any scanned note, circuit diagram, or derivation and ask the AI to "Explain like I'm 5", "Generate 5 viva questions", or "Summarize into a 1-page cheat sheet".
> *   **Module D: Academic Calendar & Contextual Reminders**
>     *   Allow users to upload a photo/PDF of their semester academic calendar and weekly timetable. Parse dates automatically into an interactive calendar with smart push notifications for exams, lab submissions, and holidays.
> 
> ### **3. Technical & Infrastructure Deliverables**
> *   Write modular, production-ready code using **[Flutter / React Native + Next.js]** for the frontend and **Python FastAPI** for the backend.
> *   Provide `Dockerfile` definitions for the API server and Celery background workers.
> *   Provide **Kubernetes (`k8s`) manifests** (`deployment.yaml`, `service.yaml`, `ingress.yaml`, `hpa.yaml`) configured for scalable microservice deployment.

---

## 3. Architectural Suggestions & Pro-Tips

1.  **Do Blur/Duplicate Detection On-Device First:** Don't upload 20 high-res photos to your Kubernetes server just to find out 5 of them are blurry duplicates. Run a lightweight OpenCV/TFLite script right on the phone the second the photo is taken. It saves bandwidth, server costs, and gives instant feedback to the student.
2.  **The "WhatsApp Bot" Alternative:** Since WhatsApp doesn't have an API that lets you read a user's personal chat history (for privacy reasons), rely primarily on the **"Share to EngBuddy"** button inside WhatsApp. As a bonus feature, you can set up a dedicated WhatsApp Business number where students can simply *forward* messages/PDFs, and your webhook automatically puts them into their app account!
3.  **Timetable-Aware Filing:** Have the camera check the phone's clock first. If it's Tuesday at 10:15 AM, and the student's timetable says they are in *Thermodynamics*, the AI should default to filing the scanned notes under *Thermodynamics* unless the text on the page strongly suggests otherwise.
4.  **K3s for Cost-Effective Kubernetes:** Full managed Kubernetes (like AWS EKS) can get expensive for a student startup/project before you have thousands of users. Start with a lightweight Kubernetes distribution like **K3s** on Hetzner or DigitalOcean, which gives you full Kubernetes power at a fraction of the cost.

---

## 4. 10 Critical Questions: "What Can Go Wrong?"

Before we write the actual code or lock in the database schema, I need your thoughts on these **10 real-world edge cases** where things could break:

1.  **The "Messy Handwriting & No Page Numbers" Problem:** Engineering students rarely write neat page numbers on every page, and handwriting can be chaotic. If a student photographs 10 pages out of order with no page numbers, how should the app behave if the AI isn't 100% sure which page comes first? Should it show a quick "drag-and-drop preview" before saving, or just guess and save?
2.  **The Accidental Delete Risk:** When you photograph a page twice, what if the second photo isn't actually a duplicate, but just a very similar-looking page (e.g., two pages full of huge integration matrices)? Should the "worst quality" duplicate go into a **30-day Trash Bin** instead of being permanently deleted immediately?
3.  **WhatsApp Privacy Wall:** Meta strictly blocks third-party apps from automatically reading personal WhatsApp chats in the background. Are you comfortable with the user selecting the file in WhatsApp and tapping **"Share -> EngBuddy"**, or were you imagining a background reader?
4.  **Google Classroom Institution Blocks:** Many universities lock down their college `.edu` / `.ac.in` Google accounts so third-party unverified apps cannot access Google Classroom APIs directly. If a college blocks API access, what is our fallback? (e.g., forwarding Classroom notification emails to an EngBuddy inbox?)
5.  **Email Noise & Spam:** If you connect Gmail, a student gets hundreds of emails (food delivery, newsletters, college spam). How do we prevent the app from importing junk? Should we only import emails from specific professor domains, or let an AI filter scan subject lines first?
6.  **Offline Classroom Reality:** College lecture halls and basement labs notoriously have terrible Wi-Fi and mobile data. If a student snaps 15 photos of notes with zero internet connection, how should the app queue these up until they get back to their hostel/home Wi-Fi?
7.  **Attendance Discrepancies:** Every college calculates attendance differently (some count a 2-hour lab as 2 classes, some count it as 1; some excuse medical leaves, some don't). How customizable does the Attendance Calculator rule-engine need to be?
8.  **Subject Overlap (Ambiguous Notes):** What happens if a chapter overlaps between two subjects (e.g., *Fourier Transforms* being taught in both *Engineering Mathematics* and *Signals & Systems* in the same semester)? How should the auto-folder organizer resolve the tie?
9.  **AI API & Kubernetes Costs:** Running Vision AI (OCR + LLM analysis) on thousands of pages of notes and running a Kubernetes cluster costs money per request. Will this be a personal project for you and your friends, or a commercial SaaS app with free/paid tiers (or "Bring Your Own Gemini API Key")?
10. **PC vs. Mobile Feature Parity:** On a PC, a user won't really be walking around snapping photos of notebooks with their webcam. Should the PC version focus more on **reading notes, the AI Explainer split-screen, and organizing**, while the Mobile version focuses on **capturing, attendance tapping, and quick checks**?
