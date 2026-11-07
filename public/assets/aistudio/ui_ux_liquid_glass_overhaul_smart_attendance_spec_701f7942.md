# EngBuddy v2.0: Decluttered Liquid-Glass UI, Smart Bunk Engine & Student Profile Specification

This document serves as the updated **Design System & Feature Specification Prompt** for Google Antigravity / AI Studio. It replaces cluttered dashboard layouts with an airy, breathing interface featuring subtle **Liquid Glass morphism**, Google’s signature **4-Color Fluid RGBY palette**, editorial-grade typography, and zero-effort interactive notification workflows.

---

## 1. Visual Design System: "A Pinch of Salt" Liquid Glass & Google 4-Color Fluidity

### **A. The Decluttered Layout Philosophy**
* **70% Whitespace / Negative Space Rule:** Never show more than **2 primary cards** on the mobile viewport at once. Hide secondary tools inside smooth bottom sheets or swipeable pills rather than cramming buttons onto the home screen.
* **Progressive Disclosure:** Instead of showing complex tables for attendance or notes, show a single bold metric (e.g., `"You can safely bunk 3 more lectures this week"`) and let the user tap to expand the subject-by-subject breakdown.

### **B. Background & Surface Architecture ("Liquid Glass")**
* **Base Canvas:**
  * **Dark Mode (Default):** Obsidian Matte (`#0B0C0E`) — deeper and cleaner than standard grey.
  * **Light Mode:** Warm Alabaster (`#F8F9FA`).
* **The "Pinch of Salt" Ambient Fluid Animation:**
  * Place a slow-moving, ultra-blurred mesh gradient (`filter: blur(90px)`, `opacity: 0.14`) *only* behind the top hero header and the active AI/Scanner button.
  * Animate it at `0.2x` speed (a 15-second loop) using CSS keyframes or a lightweight Skia/Rive shader so it feels alive like liquid beneath frosted glass without draining battery or distracting the eye.
* **Liquid Glass Cards (Glassmorphism):**
  * Surface Fill: `rgba(255, 255, 255, 0.04)` in Dark Mode / `rgba(255, 255, 255, 0.75)` in Light Mode.
  * Backdrop Blur: `backdrop-filter: blur(24px) saturate(140%)`.
  * Specular Rim Border: `1px solid rgba(255, 255, 255, 0.09)` with a subtle top-left highlight to mimic real curved glass.

### **C. Google Fluid 4-Color Palette (Red, Green, Yellow, Blue)**
Use Google's core four colors not as harsh solid blocks, but as **luminous fluid accents** and status indicators:
* **Google Blue (`#4285F4`):** Primary AI actions, active navigation states, and scanner triggers.
* **Google Red (`#EA4335`):** Attendance danger zone ($< 75\%$), exam countdown alerts, and delete/discard actions.
* **Google Yellow (`#FBBC04`):** Attendance warning buffer ($75\% - 78\%$), pending assignments, and starred notes.
* **Google Green (`#34A853`):** Safe bunk zone ($> 80\%$), "Attended" confirmation, and completed syncs.
* **The 4-Color Fluid Gradient:** `linear-gradient(135deg, #4285F4 0%, #EA4335 35%, #FBBC04 70%, #34A853 100%)` used at `15%` opacity for liquid background blobs and at `100%` opacity for the 2px glowing border when the AI or Camera is actively processing.

---

## 2. Typography System (Editorial + Swiss Precision)

To elevate the app beyond standard default fonts, pair a neo-grotesque Swiss body font with a bold display face for numbers and percentages:

| Role | Recommended Font | Fallback / Open-Source Equivalent | Usage in App |
| :--- | :--- | :--- | :--- |
| **Primary UI & Body** | **PP Neue Montreal** or **Helvetica Now Display** | `Plus Jakarta Sans` / `Inter` | All menus, AI explanations, note titles, and clean readable paragraphs. Tracked slightly tight (`-0.015em`). |
| **Big Metrics & Headers** | **Bebas Neue** | `Oswald` / `Anton` | Huge attendance percentages (`84%`), bunk counters (`+4 BUNKS LEFT`), and timetable clock headers. |
| **Code, Formulas & Roll No.** | **JetBrains Mono** | `Fira Code` | Roll numbers, course codes (`CS-302`), equations, and syllabus dates. |

---

## 3. Feature Spec: The Smart Timetable & Interactive Bunk Calculator

This module eliminates manual attendance tracking by combining an AI timetable parser with **Actionable OS Push Notifications**.

### **A. Onboarding & Timetable Setup**
1. **Direct Timetable Upload:**
   * User can snap a photo, upload a PDF/image, or manually input their weekly class routine.
   * **Gemini Vision Extraction:** Automatically extracts Subject Name, Course Code, Start/End Time, Day of Week, and Class Type (Lecture vs. Lab).
2. **Custom College Rules Configuration:**
   * **Working Days Selector:** Toggle pill for how many days a week the college is open (e.g., **5 Days [Mon–Fri]** or **6 Days [Mon–Sat]**).
   * **Minimum Attendance Threshold:** Slider defaulting to `75%` (customizable from `60%` to `90%`).
   * **Special Class Rules:** Users can tag specific slots with custom weights or exceptions (e.g., *"3-hour Lab counts as 2 attendance points"*, *"Optional Elective — exclude from aggregate"*, or *"Professor gives +1 bonus attendance for tutorials"*).

### **B. Time-Triggered Interactive Push Notifications (Zero-App-Open Tracking)**
Instead of forcing the student to open the app every hour, the app uses background local/push notifications synchronized with the uploaded timetable:

* **Trigger Timing:** 10 minutes after a scheduled class starts (or 5 minutes before it ends).
* **Notification Content:**
  * **Title:** `Thermodynamics (ME-204) is going on`
  * **Body:** `Should I mark your attendance for this class? (Current: 78% • Safe to bunk: 1)`
* **Interactive Action Buttons (Directly inside the Android/iOS Notification):**
  * `[ ✅ Yes, Attending ]` — Immediately increments `attended_classes + 1` and `total_classes + 1` in the background without opening the app.
  * `[ ❌ No, Bunking ]` — Increments `total_classes + 1` (marks absent) and recalculates remaining safe bunks.
  * `[ 🚫 Class Cancelled ]` — Ignores the slot so neither attended nor total classes increase.

### **C. The Bunk Math Engine**
Display two crystal-clear pills on the Attendance Card:
* **If Attendance $\ge$ Target ($T$, e.g., $0.75$):**
  $$\text{Safe Bunks} = \left\lfloor \frac{\text{Attended} - (T \times \text{Total})}{T} \right\rfloor$$
  *UI Copy:* `"You can skip the next 3 classes and still stay at 75%."` (Tinted in liquid Google Green).
* **If Attendance $<$ Target ($T$):**
  $$\text{Classes Needed} = \left\lceil \frac{(T \times \text{Total}) - \text{Attended}}{1 - T} \right\rceil$$
  *UI Copy:* `"Attend the next 4 classes straight to recover to 75%."` (Tinted in liquid Google Red).

---

## 4. Feature Spec: Student Profile & Digital College ID Vault

Accessible via a clean avatar pill in the top-right corner, designed like an Apple Wallet / Google Wallet pass with a Liquid Glass finish.

### **A. Student Identity Fields**
* **Full Name** (in bold *PP Neue Montreal*)
* **College / University Name** & **Campus Location**
* **Branch / Department** (e.g., *B.Tech Computer Science & Engineering*)
* **Current Year & Semester** (e.g., *3rd Year • Sem 6*)
* **University Roll Number / Registration ID** (rendered in *JetBrains Mono* with a 1-tap `Copy to Clipboard` icon for filling out exam forms quickly).
* **Section / Lab Group** (e.g., *Section B • Group 2*)

### **B. Optional Physical ID Card Placeholder ("Quick-Flash ID")**
* **Front & Back ID Upload Slot:** A sleek glassmorphic card container with a dashed border (`"Tap to scan or upload your College ID Card — Optional"`).
* **Privacy & Quick Access:**
  * If the student uploads their ID card, it auto-crops using the camera edge-detector.
  * Includes a **"Flip Card"** button (Front/Back) and a **"Full-Screen Barcode/QR Brightness Boost"** mode so students can scan their digital ID at the college gate or library without digging through their phone gallery.
  * If left empty, it collapses gracefully into a minimal, non-intrusive `"Add ID Card"` chip so the profile never looks broken or incomplete.

---

## 5. Ready-to-Paste Prompt for Google Antigravity / AI Studio

Copy and paste the block below directly into your AI builder to apply these exact updates:

> **Prompt Update — UI Overhaul, Interactive Attendance & Student Profile:**
>
> Update the **EngBuddy** frontend and backend specifications with the following design and functional rules:
> 1. **Decluttered Liquid-Glass UI:** Use a minimalist layout with generous whitespace (max 2 primary card groups visible per viewport). Style surfaces with frosted liquid glass (`backdrop-filter: blur(24px)`, `1px` translucent specular borders) over a deep obsidian (`#0B0C0E`) or warm alabaster (`#F8F9FA`) canvas.
> 2. **Subtle Google 4-Color Fluid Animation:** Add an ultra-subtle, slow-moving (`15s` loop, `14%` opacity, `90px` blur) ambient mesh animation using Google's 4 core colors: Blue (`#4285F4`), Red (`#EA4335`), Yellow (`#FBBC04`), and Green (`#34A853`). Keep the animation lightweight ("a pinch of salt") so the UI remains fast and distraction-free.
> 3. **Typography:** Load **PP Neue Montreal** (or **Helvetica Now** / **Plus Jakarta Sans**) for clean UI body copy, **Bebas Neue** for large attendance percentages and bunk counters, and **JetBrains Mono** for roll numbers, course codes, and formulas.
> 4. **Smart Bunk Calculator & Timetable Ingestion:** Allow users to upload or input their class timetable, set how many days a week their college is open (5, 6, or custom), set their target attendance percentage, and define special rules for labs/electives.
> 5. **Actionable Attendance Push Notifications:** Implement scheduled local/push notifications synced to the user's timetable. During a scheduled class, trigger an interactive notification asking: *"Are you attending [Subject Name] right now?"* with three direct notification action buttons: **[Yes, Attending]**, **[No, Bunking]**, and **[Class Cancelled]**. Tapping an option must update the database and recalculate bunk margins in the background without opening the app.
> 6. **Student Profile & Digital ID Card Slot:** Create a sleek Liquid-Glass Student Pass profile containing Name, College Name, Branch, Year/Semester, and a one-tap copyable Roll Number. Include an optional **"College ID Card"** placeholder where users can upload/scan the front and back of their physical ID card for quick gate/library viewing, or leave it collapsed if they choose not to upload one.