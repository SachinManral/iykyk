# iykyk — people, remembered.

An on-device Android application that analyzes portrait event videos, identifies unique individuals, tracks appearance counts, selects their most flattering representative moments, and composes an Instagram Story-ready portrait collage.

---

## 🌟 Overview & Product Philosophy

When people take handheld videos of social gatherings, trips, or parties, key moments get buried in camera rolls. `iykyk` automatically curates these moments into a single, high-aesthetic group artifact.

* **100% On-Device & Private**: All video decoding, face detection, neural embedding extraction, and collage rendering happen locally on the phone. No video data or biometrics ever leave the device.
* **Transparent Video Intelligence**: Rather than an opaque black box, the app tracks and displays continuous appearance counts for each person with timestamped filmstrips.
* **Aesthetic Layout Engine**: Composes polaroid-style portrait tiles with real washi tape strips and watercolor floral stickers on an atmospheric 9:16 canvas.

---

## 📱 Screens & User Flow

The app follows a modern dark glassmorphic design system:

1. **Home Screen**: Hero recap, quick-start gradient action (`+ Create New Collage`), recent collages dual-segment gallery, and a floating capsule navigation dock.
2. **Video Selection Screen**: Video gallery browser with duration and resolution metadata, plus bundled test clips.
3. **Processing Screen**: Real-time progress gauge with orbiting face avatars and a live pipeline checklist.
4. **Collage Result Screen**: Finished portrait collage with celebratory confetti, direct native system sharing, Save to Gallery, and quick access to people breakdown.
5. **People Breakdown Screen**: Detailed list of identified individuals with appearance counts and key-moment filmstrips.
6. **History Tab**: Search, filter (Recent, Favorites, All), sort (Newest, Oldest, Most People, Duration), and deletion management.
7. **Profile Tab**: Minimal, distraction-free "Coming Soon" screen with smooth back navigation.

---

## 🧠 Machine Learning & Vision Pipeline Architecture

The pipeline processes video frames off the main thread using Kotlin Coroutines and StateFlow:

```
Video Input (.mp4 / .mov)
  │
  ▼ [Adaptive Frame Sampling] (5 FPS / 200ms off main thread via MediaMetadataRetriever)
  │
  ▼ [Google ML Kit Face Detection] (5 facial landmarks, Euler angles, eye/smile probabilities, tracking IDs)
  │
  ▼ [Face-Level Laplacian Variance Quality Gate] (Rejects blurred face crops; preserves usable faces)
  │
  ▼ [5-Point Similarity Face Alignment] (Umeyama least-squares mapping to canonical 112x112 space)
  │
  ▼ [On-Device ArcFace Neural Embeddings] (InsightFace MobileFaceNet w600k_mbf.onnx -> 512D L2-normalized unit vector)
  │
  ▼ [Spatial Tracklet Appearance Tracking] (Continuous frame-to-frame box/IoU overlap grouping; max gap 800ms)
  │
  ▼ [Keyframe Subsampling] (Top 3-5 best-quality frames per appearance segment)
  │
  ▼ [Global Agglomerative Clustering] (Average Linkage on pooled keyframes + Co-occurrence exclusion)
  │
  ▼ [Multi-Factor Shot Scoring] (Frontality + Sharpness + Eyes Open + Smile + Framing)
  │
  ▼ [Generous High-Res Portrait Cropping] (2.4x expansion from full 1080x1920 source frames)
  │
  ▼ [Dynamic Collage Canvas Engine] (Layered polaroids + floral stickers + washi tape on 9:16 canvas)
```

---

## 🔬 Model & Algorithm Specifications

### 1. Face Detection & 5-Point Landmark Alignment
* **Detector**: Google ML Kit Face Detection (`PERFORMANCE_MODE_ACCURATE`, `LANDMARK_MODE_ALL`, `CLASSIFICATION_MODE_ALL`, `enableTracking()`, `minFaceSize = 0.05f`).
* **5-Point Alignment**: Computes a 2D similarity transformation (Umeyama least-squares) mapping detected landmarks (left eye, right eye, nose base, left mouth corner, right mouth corner) to standard ArcFace canonical anchor points:
  - Left Eye: $(38.2946, 51.6963)$
  - Right Eye: $(73.5318, 51.5014)$
  - Nose Base: $(56.0252, 71.7366)$
  - Left Mouth: $(41.5493, 92.3655)$
  - Right Mouth: $(70.7266, 92.2041)$
* **Result**: Rotation-invariant, scale-normalized $112 \times 112$ canonical RGB crops that maximize ArcFace embedding fidelity.

### 2. Embedding Model Used
* **Model**: **MobileFaceNet-ArcFace ONNX** (`w600k_mbf.onnx`, packaged locally in `app/src/main/assets/`).
* **Architecture**: MobileFaceNet with ArcFace (Additive Angular Margin Loss) feature extractor trained on WebFace600k / Glint360k.
* **Runtime**: Microsoft ONNX Runtime Android (`ai.onnxruntime:onnxruntime-android`).
* **Input**: $1 \times 3 \times 112 \times 112$ NCHW Float32 tensor normalized to $[-1.0, 1.0]$:
  $$\text{pixel}_{\text{norm}} = \frac{\text{pixel} - 127.5}{128.0}$$
* **Output**: 512-dimensional unit-normalized embedding vector on the hypersphere ($L_2$ norm $\|\mathbf{e}\|_2 = 1.0$).

### 3. Similarity & Distance Threshold Chosen
* **Metric**: Pairwise Cosine Distance & Cosine Similarity:
  $$S(\mathbf{u}, \mathbf{v}) = \mathbf{u} \cdot \mathbf{v}, \quad D(\mathbf{u}, \mathbf{v}) = 1.0 - S(\mathbf{u}, \mathbf{v})$$
* **Chosen Threshold**:
  - **Cosine Distance Threshold**: **$D_{\text{threshold}} = 0.64$**
  - **Equivalent Cosine Similarity**: **$S_{\text{threshold}} = 0.36$**
* **Why this threshold was chosen**:
  1. **Angular Margin Distribution**: In 512-D ArcFace embedding space, embeddings of the same individual across challenging lighting, head poses (yaw/pitch up to $45^\circ$), facial expressions, and video compression artifacts typically yield cosine distances between $0.30$ and $0.60$ ($S \in [0.40, 0.70]$). Different individuals have distances $> 0.75$ ($S < 0.25$).
  2. **Average Linkage Agglomeration**: Using average linkage across the top 3–5 representative keyframes per tracklet creates a robust cluster centroid, smoothing out single-frame outliers.
  3. **Temporal Co-Occurrence Conflict Exclusion**: A hard constraint ensures that if two tracks appear in the same video frame simultaneously ($\Delta t \le 200\text{ms}$), they are strictly prohibited from merging, regardless of embedding distance. This mathematically prevents distinct people from collapsing into a single cluster.

### 4. Appearance Segmentation & Counting
* **Frame-to-Frame Spatial Tracking**: Consecutive frame detections are associated using spatial proximity (center distance and bounding-box IoU) and temporal continuity ($\Delta t \le 800\text{ms}$).
* **Keyframe Subsampling**: To avoid noisy frames degrading clustering accuracy, only the top **3–5 highest quality-scored frames** from each continuous segment are retained and pooled for global clustering.
* **Appearance Count**: Each merged cluster represents a unique individual, and the count of distinct temporal segments mapped to that cluster equals their total appearance count.

### 5. Multi-Factor Representative Shot Ranking
Every candidate frame within a person's cluster is scored:
$$\text{Score} = 0.25 S_{\text{front}} + 0.25 S_{\text{sharp}} + 0.20 S_{\text{eyes}} + 0.15 S_{\text{smile}} + 0.15 S_{\text{frame}}$$

* **$S_{\text{front}}$**: Frontality score penalizing yaw and pitch deviations beyond $50^\circ$.
* **$S_{\text{sharp}}$**: Laplacian variance of luminance channel normalized up to 150.
* **$S_{\text{eyes}}$**: Probability of both eyes open; heavily penalizes blinking ($< 0.35$).
* **$S_{\text{smile}}$**: Smiling probability mapped to $[0.5, 1.0]$.
* **$S_{\text{frame}}$**: Distance from frame borders; penalizes bounding boxes positioned against frame boundaries to avoid clipped chins or foreheads.

---

## 🎨 Visual Composition & Floral Layering

The collage engine generates a $1080 \times 1920$ (9:16) image designed for social media sharing:
* **Background Layer**: Atmospheric blurred video snapshot with dark vignette.
* **Underlay Layer**: Organic watercolor floral stickers dynamically positioned behind photos.
* **Polaroid Tiles**: White photo frames with realistic drop shadows, random tilt angles ($\pm 4^\circ$), and procedural translucent washi tape strips at the corners.
* **Overlay Layer**: Subtle floral corner accents and handwritten branding typography.

---

## 📊 Sample Video Test Results

| Video Clip | Duration | Ground Truth / Pipeline Result | Appearance Count |
| :--- | :--- | :--- | :--- |
| **Sample 1** (`iykyk_handheld_chaos_sample_1.mp4`) | 30.0s | **5 distinct individuals** | **20 appearances total** (4 appearances each) |
| **Sample 2** (`iykyk_handheld_chaos_sample_2.mp4`) | 30.0s | Identified unique individuals & appearances | Continuous segments accurately grouped |
| **Sample 3** (`iykyk_handheld_chaos_sample_3.mp4`) | 30.0s | Identified unique individuals & appearances | Continuous segments accurately grouped |

---

## 🛠️ Tech Stack & Requirements

* **Language**: 100% Kotlin
* **Min SDK**: API 26 (Android 8.0 Oreo)
* **Target / Compile SDK**: API 34 / 35
* **UI**: Jetpack Compose, Material 3, Navigation Compose
* **Vision & ML**: Google ML Kit Face Detection, Microsoft ONNX Runtime Android (`w600k_mbf.onnx`)
* **Video Decoding**: AndroidX Media3 / `MediaMetadataRetriever`
* **Concurrency**: Kotlin Coroutines & StateFlow

---

## 🚀 Build & Setup Instructions

### Prerequisites
* Android Studio (Ladybug / Meerkat or newer) or Command Line Tools
* JDK 17 or JDK 21
* Android SDK Platform 34/35 & Build Tools 34.0.0
* Connected Android device (API 26+) or emulator

### 1. Clone the Repository
```bash
git clone https://github.com/SachinManral/iykyk.git
cd iykyk
```

### 2. Run Automated Unit Tests
```bash
./gradlew testDebugUnitTest
```

### 3. Build Debug APK
```bash
./gradlew assembleDebug
```

### 4. Install Directly on Connected Device
```bash
./gradlew installDebug
```
Or via ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📦 Deliverables & Submission Links

* **Git Repository**: [https://github.com/SachinManral/iykyk](https://github.com/SachinManral/iykyk)
* **Google Drive (Demo Video & Debug APK)**: [https://drive.google.com/drive/folders/18tAKBXmvk0J04EZNU8RhbW2rnITWdqBv?usp=drive_link](https://drive.google.com/drive/folders/18tAKBXmvk0J04EZNU8RhbW2rnITWdqBv?usp=drive_link)
* **Local Debug APK Build**:
  ```
  app/build/outputs/apk/debug/app-debug.apk
  ```

---
## 📄 License
MIT License.

