# iykyk — people, remembered.

An on-device Android application that analyzes portrait event videos, identifies unique individuals, tracks appearance counts, selects their most flattering representative moments, and composes an Instagram Story-ready portrait collage.

---

## 🌟 Overview & Product Philosophy

When people take short handheld videos of social gatherings, trips, or parties, key moments get buried in camera rolls. `iykyk` automatically curates these moments into a single, high-aesthetic group artifact.

* **100% On-Device & Private**: All video decoding, face detection, neural embedding extraction, and collage rendering happen locally on the phone. No video data or biometrics ever leave the device.
* **Transparent Video Intelligence**: Rather than an opaque black box, the app tracks and displays continuous appearance counts for each person.
* **Aesthetic Layout Engine**: Adapts dynamically to the number of people detected ($N = 1 \dots 8+$), preserving high-resolution background framing without tight square chops.

---

## 📱 Screens & User Flow

The app follows a modern dark glassmorphic design system:

1. **Home Screen**: Hero recap, quick-start gradient action (`+ Create New Collage`), recent collages gallery, and a floating capsule navigation dock.
2. **Video Selection Screen**: Video gallery browser with duration and resolution metadata, plus sample test clips.
3. **Processing Screen**: Real-time progress gauge with orbiting face avatars and a live pipeline checklist.
4. **Your Collage Screen**: Dynamic portrait collage with celebratory confetti, "Save to Gallery", "Share", and quick access to people breakdown.
5. **People Breakdown Screen**: Detailed list of identified individuals with appearance counts and key-moment filmstrips.
6. **Share Sheet**: "Private by design" assurance and native Android sharing targets.

---

## 🧠 Machine Learning & Vision Pipeline Architecture

The pipeline processes video frames off the main thread using Kotlin Coroutines and StateFlow:

```
Video Input
  │
  ▼ [Adaptive Frame Sampling] (5 FPS / 200ms)
  │
  ▼ [ML Kit Face Detection] (5 facial landmarks, Euler angles, eye/smile probabilities, tracking IDs)
  │
  ▼ [Face-Level Laplacian Variance Quality Gate] (Rejects blurred face crops; preserves usable faces)
  │
  ▼ [Canonical Affine Face Alignment] (2D eye-line normalization to 112x112 canonical space)
  │
  ▼ [On-Device Face Embeddings] (MobileFaceNet TFLite -> 128D/192D L2-normalized unit vector)
  │
  ▼ [Tracklet-Based Identity Clustering] (Temporal tracklets + Average Linkage + Co-occurrence exclusion)
  │
  ▼ [Continuous Appearance Segmentation] (600ms gap thresholding: continuous segment = 1 appearance)
  │
  ▼ [Multi-Factor Shot Scoring] (Frontality + Sharpness + Eyes Open + Smile + Framing)
  │
  ▼ [Generous High-Res Portrait Cropping] (2.4x expansion from full source frames)
  │
  ▼ [Dynamic Collage Canvas Engine] (Instagram Story polaroid composition)
```

---

## 🔬 Model & Algorithm Specifications

### 1. Face Detection & Landmark Alignment
* **Detector**: Google ML Kit Face Detection (`PERFORMANCE_MODE_ACCURATE`, `LANDMARK_MODE_ALL`, `CLASSIFICATION_MODE_ALL`, `enableTracking()`, `minFaceSize = 0.05f`).
* **Alignment**: Affine transformation matrix maps detected left and right eye coordinates to canonical fixed positions $(0.38 \cdot \text{size}, 0.40 \cdot \text{size})$ and $(0.62 \cdot \text{size}, 0.40 \cdot \text{size})$ in a $112 \times 112$ image space. Distorted or unaligned crops are rejected to protect embedding quality.

### 2. Face Embedding Model
* **Model**: **MobileFaceNet** (`mobile_face_net.tflite`, packaged in `app/src/main/assets/`).
* **Input**: $112 \times 112 \times 3$ RGB bitmap normalized to $[-1.0, 1.0]$:
  $$\text{pixel}_{\text{norm}} = \frac{\text{pixel} - 127.5}{128.0}$$
* **Output**: Unit-normalized embedding vector on the hypersphere ($L_2$ norm $= 1.0$).

### 3. Tracklet-Based Identity Clustering & Co-Occurrence
* **Clustering Unit**: Detections are first aggregated into continuous temporal **tracklets** ($\Delta t \le 600\text{ms}$) with normalized centroid embeddings.
* **Metric**: Pairwise Cosine Distance:
  $$D(\mathbf{u}, \mathbf{v}) = 1 - (\mathbf{u} \cdot \mathbf{v})$$
* **Calibrated Threshold**: **$0.22$** with **Average Linkage**.
* **Co-Occurrence Conflict Constraint**: If any two tracklets overlap in time ($\Delta t \le 250\text{ms}$), they are strictly prohibited from merging into the same identity. This guarantees that distinct people sharing the same shot (e.g. Persons A & B at 10.1–11.5s, Persons C & D at 20.2–21.6s) never collapse into one person.

### 4. Continuous Appearance Counting
* **Definition**: An appearance is one continuous visible segment.
* **Temporal Continuity Gap ($\Delta t_{\text{gap}}$)**: Set to **$600\text{ms}$** (3 missed frames at 200ms sampling rate) to bridge brief head-turn occlusions without artificially splitting a continuous appearance.
* **Quality Gate**: Discards microscopic ($< 40\text{px}$) or heavily blurred face detections.

### 5. Multi-Factor Representative Shot Ranking
Every candidate frame within a person's cluster is scored:
$$\text{Score} = 0.25 S_{\text{front}} + 0.25 S_{\text{sharp}} + 0.20 S_{\text{eyes}} + 0.15 S_{\text{smile}} + 0.10 S_{\text{frame}}$$

* **$S_{\text{front}}$**: Frontality score penalizing yaw and pitch deviations beyond $55^\circ$.
* **$S_{\text{sharp}}$**: Laplacian variance of luminance channel normalized up to 150.
* **$S_{\text{eyes}}$**: Probability of both eyes open; heavily penalizes blinking ($< 0.35$).
* **$S_{\text{smile}}$**: Smiling probability mapped to $[0.5, 1.0]$.
* **$S_{\text{frame}}$**: Evaluated against actual detection frame dimensions; penalizes bounding boxes positioned directly against frame boundaries to avoid clipped chins or foreheads.

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
* **Vision & ML**: Google ML Kit Face Detection, TensorFlow Lite (`mobile_face_net.tflite`)
* **Video Decoding**: AndroidX Media3 / `MediaMetadataRetriever`
* **Concurrency**: Kotlin Coroutines & StateFlow

---

## 🚀 Build & Setup Instructions

### Prerequisites
* Android Studio Ladybug / Meerkat or Command Line Tools
* JDK 17 or JDK 21
* Android SDK Platform 34/35 & Build Tools 34.0.0

### Steps to Build:
```bash
# 1. Clone repository
git clone https://github.com/SachinManral/iykyk.git
cd iykyk

# 2. Run unit tests
./gradlew testDebugUnitTest

# 3. Build Debug APK
./gradlew assembleDebug
```

The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
MIT License.
