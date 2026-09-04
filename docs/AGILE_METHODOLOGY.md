# Agile Methodology & Project Execution Plan
## Project: iykyk — Android Unique-Person Video Collage

---

### 1. Methodology Overview
This project follows an adapted **Lean-Agile (Scrumban)** framework designed for rapid, high-precision mobile & computer vision delivery. Development is partitioned into structured, sequential milestones with strict verification gates at each step.

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Milestone 1   │───▶│   Milestone 2   │───▶│   Milestone 3   │───▶│   Milestone 4   │───▶│   Milestone 5   │
│ Setup & Engine  │    │ Sampling & ML   │    │ Ranking & Layout│    │ Compose UI/UX   │    │ QA & Deliverable│
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
```

---

### 2. Milestone Breakdown & Sprint Backlog

#### Milestone 1: Project Architecture & Pipeline Foundation
* **Goal**: Establish the Android project scaffolding, clean architectural boundaries, dependency injection/wiring, and TFLite model integration.
* **Key Tasks**:
  1. Initialize Android Gradle project with Kotlin DSL (`minSdk = 26`, `targetSdk = 34`, Compose BOM, Material 3, Navigation, Coroutines, Media3, ML Kit Face Detection, TensorFlow Lite).
  2. Embed and verify the lightweight on-device face embedding model (`mobile_facenet.tflite` / `facenet.tflite`) in `app/src/main/assets`.
  3. Implement `FaceEmbedder` interface with TFLite interpreter initialization, affine alignment transformation, and unit-vector L2 normalization.
  4. Build benchmark test harness to verify on-device inference speed ($< 25\text{ms}$ per face crop).
* **Exit Gate / DoD**: TFLite model executes cleanly on Android runtime and generates reproducible 128D/512D unit embeddings for test face bitmaps.

---

#### Milestone 2: Video Ingestion, Adaptive Sampling & Identity Clustering
* **Goal**: Build the core vision pipeline for frame decoding, face detection, temporal tracking, identity grouping, and continuous appearance segmentation.
* **Key Tasks**:
  1. Implement `VideoFrameExtractor` using AndroidX Media3 / `MediaMetadataRetriever` with adaptive sampling (base 4 fps, dense 10–12.5 fps around transitions).
  2. Implement `MLKitFaceDetector` wrapping Google ML Kit Face Detection with landmark extraction, Euler angles, and smiling/eye probabilities.
  3. Build `BlurDetector` using Laplacian variance to reject whip-pan blurs.
  4. Implement `IdentityClusterer` using Cosine Distance + Agglomerative Hierarchical Clustering / DBSCAN to group tracklets into unique individuals.
  5. Implement `AppearanceSegmenter` to calculate continuous visible segments per identity, handling co-occurrence and bridging transient flickers ($< 0.6\text{s}$).
* **Exit Gate / DoD**: Pipeline accurately processes `Sample 1` video, returning **exactly 5 unique people** and **20 total appearances** (4 appearances each), matching ground truth specifications.

---

#### Milestone 3: Representative-Shot Selector & Dynamic Collage Canvas
* **Goal**: Develop the multi-factor best-moment scoring algorithm, generous portrait crop extractor, and dynamic collage layout engine.
* **Key Tasks**:
  1. Implement `ShotRanker` executing the multi-factor scoring formula:
     $$\text{Score} = 0.25 S_{\text{front}} + 0.25 S_{\text{sharp}} + 0.20 S_{\text{eyes}} + 0.15 S_{\text{smile}} + 0.10 S_{\text{frame}} + 0.05 S_{\text{temp}}$$
  2. Implement generous portrait cropping logic from full-resolution source frames ($1080 \times 1920$).
  3. Implement `CollageComposer` rendering a high-resolution bitmap canvas for $N = 1, 2, 3, 4, 5, 6\text{–}8+$ people with card borders, drop shadows, and branding.
  4. Implement image persistence (`MediaStore` exporter) and native `Intent.ACTION_SEND` share sheet integration.
* **Exit Gate / DoD**: Collage bitmap renders cleanly at high resolution with balanced aesthetics, without pixelation or tight square chops, and exports to disk/share sheet.

---

#### Milestone 4: Modern Jetpack Compose UI & Reactive State
* **Goal**: Build the complete, interactive Android UI matching the reference design in `Sample UI/image.png`.
* **Key Tasks**:
  1. Implement `HomeScreen`: Hero card, gradient CTA, recent collages gallery, and bottom navigation.
  2. Implement `VideoSelectScreen`: Video list with metadata, tab filters, and selection state.
  3. Implement `ProcessingScreen`: Animated circular progress gauge, orbiting avatar bubbles, step-by-step checklist, and cancel action.
  4. Implement `CollageResultScreen`: Interactive collage preview, celebratory confetti, "Save to Gallery" with toast feedback, and "Share".
  5. Implement `PeopleBreakdownScreen`: Person cards with appearance counts and appearance timeline filmstrips.
  6. Implement `ShareScreen`: Privacy commitment card and quick social share targets.
  7. Connect UI to `VideoProcessingViewModel` using Kotlin StateFlow and clean UI state models.
* **Exit Gate / DoD**: All 6 screens are fully responsive, smoothly animated, free of ANRs/jank, and support full navigation lifecycle and cancellation.

---

#### Milestone 5: Verification, Benchmarking & Submission Packaging
* **Goal**: Comprehensive validation across all 3 sample test videos, stress testing, documentation, and deliverable creation.
* **Key Tasks**:
  1. Validate pipeline accuracy on `Sample 1`, `Sample 2`, and `Sample 3`.
  2. Perform memory leak and bitmap recycling audits.
  3. Generate clean, reproducible debug APK.
  4. Draft comprehensive `README.md` with build steps, ML architecture, similarity thresholds, and sample analysis.
  5. Record 60-second end-to-end screen recording demonstrating the complete flow across sample videos.
* **Exit Gate / DoD**: Debug APK installs and runs flawlessly, README contains all required disclosures, and demo video is ready for submission.

---


### 4. Risk Register & Mitigation Strategy

| Risk | Impact | Likelihood | Mitigation Strategy |
| :--- | :---: | :---: | :--- |
| **Out of Memory (OOM) during video decoding** | High | Medium | Downscale frame analysis bitmaps for detection while retaining only the chosen representative full-frame timestamps for final render. Recycle bitmaps immediately. |
| **Whip-pan false detections / blur noise** | Medium | High | Apply Laplacian variance thresholding to reject blurry frames before feeding to ML Kit / TFLite. |
| **Face embedding drift under lighting changes** | Medium | Low | Use affine landmark alignment and normalized cosine metric clustering with tracklet centroid aggregation. |
| **Main thread UI freezing (ANR)** | High | Low | Encapsulate pipeline inside Kotlin Coroutines `Flow` reporting progress milestones asynchronously. |
