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
  ▼ [Adaptive Frame Sampling] (5 FPS / 200ms off main thread)
  │
  ▼ [ML Kit Face Detection] (5 facial landmarks, Euler angles, eye/smile probabilities, tracking IDs)
  │
  ▼ [Face-Level Laplacian Variance Quality Gate] (Rejects blurred face crops; preserves usable faces)
  │
  ▼ [5-Point Similarity Face Alignment] (Umeyama least-squares mapping to canonical 112x112 space)
  │
  ▼ [On-Device ArcFace Embeddings] (InsightFace MobileFaceNet w600k_mbf.onnx -> 512D L2-normalized unit vector)
  │
  ▼ [Spatial Tracklet Appearance Tracking] (Continuous frame-to-frame box/IoU overlap grouping)
  │
  ▼ [Keyframe Subsampling] (Top 3-5 best-quality frames per appearance segment)
  │
  ▼ [Global Agglomerative Clustering] (Average Linkage on pooled keyframes + Co-occurrence exclusion)
  │
  ▼ [Multi-Factor Shot Scoring] (Frontality + Sharpness + Eyes Open + Smile + Framing)
  │
  ▼ [Generous High-Res Portrait Cropping] (2.4x expansion from full 1080x1920 source frames)
  │
  ▼ [Dynamic Collage Canvas Engine] (Instagram Story polaroid composition)
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

### 2. Face Embedding Model
* **Model**: **MobileFaceNet-ArcFace ONNX** (`w600k_mbf.onnx`, packaged in `app/src/main/assets/`).
* **Runtime**: `com.microsoft.onnxruntime:onnxruntime-android`.
* **Input**: $1 \times 3 \times 112 \times 112$ NCHW Float32 tensor normalized to $[-1.0, 1.0]$:
  $$\text{pixel}_{\text{norm}} = \frac{\text{pixel} - 127.5}{128.0}$$
* **Output**: 512-dimensional unit-normalized embedding vector on the hypersphere ($L_2$ norm $= 1.0$).

### 3. Appearance Segmentation & Keyframe Pooling
* **Frame-to-Frame Spatial Tracking**: Consecutive frame detections are associated using spatial proximity (center distance and bounding-box IoU) and temporal continuity ($\Delta t \le 600\text{ms}$).
* **Keyframe Subsampling**: To avoid noisy frames degrading clustering accuracy, only the top **3–5 highest quality-scored frames** from each continuous segment are retained and pooled for global clustering.

### 4. Global Agglomerative Clustering & Co-Occurrence Exclusion
* **Clustering Strategy**: Global (order-independent) Agglomerative Hierarchical Clustering with **Average Linkage** on pooled segment keyframe embeddings.
* **Metric**: Pairwise Cosine Distance:
  $$D(\mathbf{u}, \mathbf{v}) = 1 - (\mathbf{u} \cdot \mathbf{v})$$
* **Calibrated Threshold**: **$0.44$** (tuned for 512-D ArcFace hypersphere embeddings).
* **Co-Occurrence Conflict Constraint**: If any two segments/tracks overlap in time ($\Delta t \le 200\text{ms}$), they are strictly prohibited from merging into the same identity. This guarantees that co-occurring individuals sharing the same frame never collapse into one person.
* **Appearance Counting**: Each merged cluster represents a single individual, and the count of continuous segments mapped to that cluster equals their total appearance count.

### 5. Multi-Factor Representative Shot Ranking
Every candidate frame within a person's cluster is scored:
$$\text{Score} = 0.25 S_{\text{front}} + 0.25 S_{\text{sharp}} + 0.20 S_{\text{eyes}} + 0.15 S_{\text{smile}} + 0.15 S_{\text{frame}}$$

* **$S_{\text{front}}$**: Frontality score penalizing yaw and pitch deviations beyond $50^\circ$.
* **$S_{\text{sharp}}$**: Laplacian variance of luminance channel normalized up to 150.
* **$S_{\text{eyes}}$**: Probability of both eyes open; heavily penalizes blinking ($< 0.35$).
* **$S_{\text{smile}}$**: Smiling probability mapped to $[0.5, 1.0]$.
* **$S_{\text{frame}}$**: Distance from frame borders; penalizes bounding boxes positioned against frame boundaries to avoid clipped chins or foreheads.

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
