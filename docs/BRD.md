# Business Requirements Document (BRD)
## Project: iykyk (Android Unique-Person Video Collage)

---

### 1. Executive Summary
**iykyk** ("If You Know You Know") is a mobile-first, privacy-focused Android application designed to transform handheld portrait videos into high-aesthetic, shareable portrait collages. By running an intelligent computer vision pipeline entirely on-device, the app identifies distinct individuals, quantifies their temporal presence (appearance counts), selects their most flattering representative moments, and assembles them into an Instagram Story-ready visual layout.

---

### 2. Business Objectives & Value Proposition
* **Effortless Memory Curation**: Eliminates manual screenshotting and collage editing from handheld event videos, group trips, and social gatherings.
* **Complete On-Device Privacy**: 100% of video ingestion, face detection, embedding extraction, and collage generation occurs locally on the device with zero cloud connectivity or data transmission.
* **High-Aesthetic Social Artifacts**: Generates polished, dynamic collages tailored to the exact number of people detected, ready for instant export and social sharing (Instagram, WhatsApp, System Share).
* **Transparent Video Intelligence**: Exposes meaningful temporal analytics (who was there, how many times they appeared, and their individual key moments) rather than an opaque black-box output.

---

### 3. Stakeholder & User Personas
* **Primary Persona (The Social Creator / Memory Keeper)**:
  * Records short portrait video clips (15–60s) of friends, parties, concerts, and trips.
  * Seeks a fast, automatic way to generate a group summary image without tedious manual editing.
  * Values aesthetic typography, clean photo framing, and instant sharing.
* **Secondary Persona (Privacy-Conscious User)**:
  * Wants smart photo/video processing without uploading personal biometric data or family videos to external cloud servers.
* **Technical Reviewer / Evaluator**:
  * Assesses pipeline robustness, identity clustering accuracy, appearance segmentation logic, code cleanliness, and adherence to assignment criteria.

---

### 4. Scope of Work

#### 4.1 In Scope
1. **Video Ingestion**: Local storage and gallery picker supporting MP4 portrait video files (specifically 9:16 aspect ratio, 1080x1920, 25–60 fps).
2. **On-Device Vision Pipeline**:
   * Frame extraction and adaptive sampling.
   * Face detection and 5-point facial landmark extraction via Google ML Kit.
   * Face alignment and normalization.
   * 128/512-dimensional face feature embedding generation via lightweight TensorFlow Lite model (MobileFaceNet / FaceNet).
   * Short-term tracking association + long-term metric identity clustering.
   * Continuous visible appearance segmentation (filtering out rapid whip-pan blurs and handling co-occurring individuals).
3. **Representative Shot Selection**: Multi-factor scoring (frontality, eye openness, sharpness, pleasant expression, full face framing) with temporal window stability.
4. **Dynamic Collage Engine**:
   * Algorithmic template generation supporting 1 to 8+ detected individuals.
   * Generous portrait cropping preserving original high-resolution frame data.
   * Aesthetic composition with polaroid-style card borders, subtle rotations, drop shadows, and branding accents.
5. **Interactive UI / UX**:
   * Home dashboard with recent collages.
   * Video selector with metadata previews.
   * Step-by-step progress view with stage descriptions and animated avatars.
   * Interactive Collage view with Save to Gallery and System Share Sheet integration.
   * People breakdown view detailing individual appearance counts and moment thumbnails.

#### 4.2 Out of Scope
* Cloud backend services, account authentication, or remote synchronization.
* Live real-time camera recording and streaming analysis.
* Video editing / video rendering (the output is a high-resolution static collage image).
* Non-portrait video optimization (handled gracefully via user guidance).

---

### 5. Success Metrics & Key Performance Indicators (KPIs)
| Metric | Target | Measurement Method |
| :--- | :--- | :--- |
| **Identity Clustering Accuracy** | $\ge 95\%$ correct person grouping | Evaluated against Sample 1 (5 unique people) and test sets. |
| **Appearance Count Accuracy** | $100\%$ match on continuous segments | Evaluated against Sample 1 ground truth (20 appearances total; 4 per person). |
| **Processing Latency** | $< 15$ seconds for a 30s 1080p video | Measured on standard Android test devices / emulators. |
| **Crash & ANR Rate** | $0\%$ | Stress tested across concurrent coroutines and background dispatchers. |
| **Visual Presentation Quality** | Instagram Story aesthetic grade | Verified against UI design specs, crisp cropping, and clean canvas rendering. |

---

### 6. Assumptions & Technical Constraints
1. **Target Platform**: Android OS (Minimum SDK 26 - Android 8.0 Oreo, Target SDK 34/35).
2. **Language & Framework**: Kotlin with modern Jetpack Compose for declarative UI, Coroutines/Flow for asynchronous processing, and AndroidX Media3 / MediaMetadataRetriever for video frame decoding.
3. **Hardware Independence**: Must function reliably without requiring proprietary hardware acceleration (GPU/NNAPI delegates used where available, with CPU fallback).
4. **Self-Contained Artifact**: Embedding models and weights packaged in app assets (`assets/mobile_facenet.tflite`).
