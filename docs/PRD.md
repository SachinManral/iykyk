# Product Requirements Document (PRD)
## Project: iykyk — People, Remembered
**Document Version:** 1.0  
**Target Platform:** Android (minSdk 26, targetSdk 34/35)  
**Primary Language & UI:** Kotlin, Jetpack Compose, Material 3, AndroidX Media3 / ML Kit / TFLite  

---

## 1. Product Overview & Vision
`iykyk` turns chaotic, handheld portrait event videos into curated, high-aesthetic group collages. It automates the extraction, identification, best-shot selection, and layout design of people captured in video clips, completely on-device without cloud dependencies.

```
       [ Handheld Video ]
               │
               ▼
   [ Intelligent Vision Pipeline ]
   ├── Adaptive Frame Sampling
   ├── ML Kit Face & Landmark Detection
   ├── Affine Face Alignment
   ├── TFLite Face Embeddings
   ├── Temporal Tracking + Identity Clustering
   ├── Continuous Appearance Segmentation
   └── Multi-Factor Best-Moment Ranking
               │
               ▼
   [ Dynamic Aesthetic Collage Canvas ]
   ├── 1 to 8+ Person Layout Engine
   ├── Generous High-Res Framing
   ├── Save to Gallery & Native Share Sheet
   └── Individual Appearance Timeline Breakdown
```

---

## 2. Core User Flows & Screen Specifications

### 2.1 Screen 1: Home Dashboard (`HomeScreen`)
* **Header**: App brand mark `iykyk` with subtle group glyph and settings icon.
* **Hero Section**:
  * Title: *"Moments fade. People don't."*
  * Subtitle: *"We find everyone in your videos and create a beautiful collage you can keep."*
  * Primary Action: High-emphasis gradient button: `+ Create New Collage`.
* **Recent Collages Section**:
  * Carousel/Grid of previously processed collages stored locally in room database / app storage.
  * Card preview displays: Collage thumbnail, Video title, Date, People count badge (e.g. `5 people · 20 appearances`), and Favorite action.
* **Navigation Bar**: Clean bottom navigation (Home, History, Tips, About/Settings).

### 2.2 Screen 2: Video Selection (`VideoSelectScreen`)
* **Header**: Back navigation arrow, Title: *"Select Video"*, Subtitle: *"Choose a video to analyze"*.
* **Filter Tabs**: `Videos` (active), `Gallery`, `Folders`.
* **Video List Item**:
  * Video thumbnail with timestamp pill (`00:30`).
  * File name, duration, resolution (`1080x1920`), and creation date.
  * Selection indicator (radio button / checkmark).
* **CTA**: Bottom sticky button: `Next ->` (activates when a video is selected).

### 2.3 Screen 3: Live Pipeline Progress (`ProcessingScreen`)
* **Header**: Title: *"Processing"*, Subtitle: *"Sit tight, magic is happening ✨"*.
* **Visual Anchor**:
  * Centered circular progress gauge displaying numeric percentage (e.g. `73%`) and status subtitle (`Almost there`).
  * Dynamic orbiting avatar bubbles populated in real-time as unique faces are discovered.
* **Transparent Pipeline Stage Checklist**:
  1. `Reading video` (Completed ✓)
  2. `Detecting faces` (Completed ✓)
  3. `Grouping identities` (Completed ✓)
  4. `Counting appearances` (In progress ◉)
  5. `Choosing best moments` (Waiting ○)
  6. `Composing collage` (Waiting ○)
* **Tip Card**: Dynamic contextual hints (e.g. *"Tip: We pick the clearest, most natural moments for the best results."*).
* **Controls**: Accessible `Cancel` button allowing safe coroutine job cancellation without memory leaks.

### 2.4 Screen 4: Collage Presentation (`CollageResultScreen`)
* **Header**: Back button, Title: *"Your Collage"*, Home shortcut.
* **Summary Banner**: Headline: *"Everyone's here! 🎉"* + Subtitle: *"5 people · 20 appearances"*.
* **Collage Viewport**:
  * Interactive preview of the rendered collage canvas.
  * Dynamic layout based on person count ($N = 1, 2, 3, 4, 5, 6, 7, 8+$).
  * Generous portrait framing (avoiding tight square face crops).
  * Polaroid/card styling with subtle rotations, drop shadows, and confetti accents.
  * Brand badge at bottom: *"Captured with iykyk"*.
* **Bottom Action Bar**:
  * `Save to Gallery` (writes full-resolution JPEG/PNG to MediaStore with confirmation toast).
  * `Share` (opens native Android Intent chooser with image URI).
  * `View People` (navigates to the individual breakdown screen).

### 2.5 Screen 5: People & Appearances Breakdown (`PeopleBreakdownScreen`)
* **Header**: Back button, Title: *"People"*, Subtitle: *"Tap a person to see their moments"*.
* **Person Row Item**:
  * Main representative portrait avatar.
  * Person identifier label (`Person A`, `Person B`, etc.) and appearance count (e.g. `4 appearances`).
  * Horizontal filmstrip of thumbnails corresponding to each distinct appearance segment.
* **Footer Card**: Total summary badge (`Total: 5 people · 20 appearances`).

### 2.6 Screen 6: Share & Privacy Sheet (`ShareScreen`)
* **Privacy Badge**: *"Private by design — Your video and results never leave this device."*
* **Collage Preview Thumbnail**: High-resolution thumbnail summary.
* **Direct Share Targets**: Quick intent triggers for Instagram Stories/Feed, WhatsApp, Messages, Gmail, Copy Link, and System More.

---

## 3. End-to-End Machine Learning & Vision Pipeline

```
[ Video Input ]
      │
      ▼ (Step 1: Adaptive Sampling)
[ Keyframes: ~3-5 FPS base, dense on motion/transitions ]
      │
      ▼ (Step 2: Face Detection - ML Kit)
[ Bounding Boxes, 5 Facial Landmarks, Euler Angles, Probabilities ]
      │
      ▼ (Step 3: Quality Gate & Motion Blur Filter)
[ Filter out extreme angles (>45°), tiny faces (<60px), blurry whip-pans ]
      │
      ▼ (Step 4: Affine Face Alignment)
[ 112x112 Normalized Aligned Crop (Eye line horizontal, canonical scale) ]
      │
      ▼ (Step 5: TFLite Face Embeddings)
[ 128D/512D Unit-Normalized Embedding Vector ]
      │
      ▼ (Step 6: Temporal Tracking + Metric Identity Clustering)
[ Cosine Similarity Matrix + Agglomerative / DBSCAN Clustering ]
      │
      ▼ (Step 7: Continuous Appearance Segmentation)
[ Temporal gap thresholding: contiguous segment = 1 appearance ]
      │
      ▼ (Step 8: Multi-Factor Best-Moment Scoring)
[ Frontality + Sharpness + Eyes Open + Smile + Temporal Window ]
      │
      ▼ (Step 9: Generous Source Frame Cropping)
[ Aspect-preserving 4:5 / 3:4 portrait crop from full 1080x1920 frame ]
      │
      ▼ (Step 10: Canvas Collage Assembly)
[ Dynamic multi-card layout with typography, cards, and export ]
```

---

### 3.1 Step 1: Adaptive Video Ingestion & Frame Sampling
* **Base Extraction Rate**: Sample candidate frames at regular intervals (e.g., $4\text{ fps} = 250\text{ms}$ step).
* **Adaptive Densification**: When a face enters, exits, or undergoes significant positional displacement between adjacent samples, trigger denser sampling ($10\text{–}12.5\text{ fps}$) around that transition window ($\pm 500\text{ms}$).
* **Implementation**: Utilizes `MediaMetadataRetriever` / `MediaCodec` surface rendering with `OPTION_CLOSEST_SYNC` / exact frame lookup.

---

### 3.2 Step 2: Face Detection & Landmark Extraction (Google ML Kit)
* Uses `FaceDetection.getClient(FaceDetectorOptions)` configured for:
  * `PERFORMANCE_MODE_ACCURATE`
  * `LANDMARK_MODE_ALL` (Left eye, right eye, nose base, left mouth corner, right mouth corner)
  * `CLASSIFICATION_MODE_ALL` (Smiling probability, Left eye open probability, Right eye open probability)
  * `enableTracking()` for short-term frame-to-frame tracking IDs.

---

### 3.3 Step 3: Quality Gating & Whip-Pan Rejection
* Discard invalid candidate faces to avoid clustering noise:
  * **Size filter**: Face bounding box width and height must be $\ge 60\text{ px}$.
  * **Angle filter**: $|\text{Euler Y (Yaw)}| \le 45^\circ$, $|\text{Euler Z (Roll)}| \le 35^\circ$.
  * **Sharpness / Blur filter**: Laplacian variance of the crop:
    $$\text{Var}(\nabla^2 I) \ge \tau_{\text{blur}}$$
    Frames during rapid whip-pans have high motion blur and low edge variance; they are immediately marked as non-visible passes.

---

### 3.4 Step 4: Facial Landmark Alignment
* Instead of directly feeding an unaligned rectangular bounding box to the neural network, apply a 2D affine transformation:
  * Align the left eye $(x_l, y_l)$ and right eye $(x_r, y_r)$ to fixed canonical coordinate positions in a $112 \times 112$ destination image:
    $$\theta = \arctan2(y_r - y_l, x_r - x_l)$$
    $$\text{scale} = \frac{d_{\text{target}}}{\sqrt{(x_r - x_l)^2 + (y_r - y_l)^2}}$$
* Produces rotation-invariant, scale-normalized face crops that significantly enhance embedding clustering fidelity.

---

### 3.5 Step 5: On-Device Face Embedding Generation
* **Model**: MobileFaceNet / FaceNet TFLite (quantized / float16, size $< 4\text{ MB}$, input $112 \times 112 \times 3$, output $128\text{-D}$ or $512\text{-D}$ vector).
* **Preprocessing**: Pixel normalization:
  $$x_{\text{norm}} = \frac{x - 127.5}{128.0}$$
* **L2 Normalization**: Embedding output is projected to the unit hypersphere:
  $$\hat{\mathbf{e}} = \frac{\mathbf{e}}{\|\mathbf{e}\|_2}$$

---

### 3.6 Step 6: Temporal Tracking + Metric Identity Clustering
* **Short-Term Association**: When ML Kit tracking ID is continuous and embeddings maintain high cosine similarity ($> 0.85$), associate detections into a tracklet.
* **Global Clustering**: For all tracklet centroid embeddings, construct a pairwise cosine distance matrix:
  $$D_{ij} = 1 - (\hat{\mathbf{e}}_i \cdot \hat{\mathbf{e}}_j)$$
* **Algorithm**: Agglomerative Hierarchical Clustering with Average / Complete Linkage using a tuned distance threshold $\tau_{\text{dist}} \approx 0.40 - 0.45$ (or DBSCAN with $\varepsilon = 0.42, \text{minPts} = 2$).
* **Output**: Partitions all detected faces across the video into $K$ unique identity clusters $C_1, C_2, \dots, C_K$.

---

### 3.7 Step 7: Continuous Appearance Segmentation & Counting
* **Definition of Appearance**: A continuous temporal segment where a person is clearly visible without an interruption longer than the tolerance threshold $\Delta t_{\text{gap}} \approx 0.6\text{s}$ (to bridge minor single-frame detection flickers).
* **Algorithm**:
  1. For each identity $C_k$, sort all timestamped detections in chronological order $t_1 < t_2 < \dots < t_m$.
  2. Iterate through timestamps: if $t_{i+1} - t_i > \Delta t_{\text{gap}}$, end current appearance segment and begin a new one.
  3. Filter out transient false-positive flickers (segments with total duration $< 0.25\text{s}$ and fewer than 2 valid detections).
  4. Co-occurring faces (e.g., Persons A & B sharing the frame) are detected in the same frame and independently assigned to their respective appearance segments.
* **Ground Truth Verification**:
  * Sample 1: Yields exactly 5 people, each with 4 distinct appearance segments $= 20$ total appearances.

---

### 3.8 Step 8: Multi-Factor Best-Moment Scoring Engine
To choose the single most flattering and representative shot for each person, every candidate frame within the person's identity cluster is evaluated:

$$\text{Score} = w_{\text{front}} S_{\text{front}} + w_{\text{sharp}} S_{\text{sharp}} + w_{\text{eyes}} S_{\text{eyes}} + w_{\text{smile}} S_{\text{smile}} + w_{\text{frame}} S_{\text{frame}} + w_{\text{temp}} S_{\text{temp}}$$

#### Component Formulations:
1. **Frontality Score ($S_{\text{front}}$)**:
   $$S_{\text{front}} = \max\left(0, 1 - \frac{|\text{yaw}| + |\text{pitch}|}{60^\circ}\right)$$
2. **Sharpness Score ($S_{\text{sharp}}$)**:
   $$S_{\text{sharp}} = \min\left(1.0, \frac{\text{Var}(\nabla^2 I)}{\tau_{\text{sharp\_max}}}\right)$$
3. **Eyes Open Score ($S_{\text{eyes}}$)**:
   $$S_{\text{eyes}} = \frac{P(\text{left\_eye\_open}) + P(\text{right\_eye\_open})}{2}$$
   *(Heavily penalizes closed eyes if either eye $< 0.4$)*
4. **Pleasant Expression / Smile Score ($S_{\text{smile}}$)**:
   $$S_{\text{smile}} = 0.5 + 0.5 \times P(\text{smile})$$
5. **Face Framing & Completeness Score ($S_{\text{frame}}$)**:
   Evaluates distance from frame borders to prevent clipped chins or foreheads:
   $$S_{\text{frame}} = 1.0 - \text{Penalty if bounding box is within 5\% of frame edge}$$
6. **Temporal Window Stability ($S_{\text{temp}}$)**:
   Computes the moving average of quality scores in a $\pm 250\text{ms}$ window to avoid isolated awkward micro-expressions.

#### Default Weights:
$$w_{\text{front}} = 0.25,\; w_{\text{sharp}} = 0.25,\; w_{\text{eyes}} = 0.20,\; w_{\text{smile}} = 0.15,\; w_{\text{frame}} = 0.10,\; w_{\text{temp}} = 0.05$$

---

### 3.9 Step 9: Generous Source Frame Extraction
* Avoids low-resolution tight crops.
* For the winning representative frame timestamp, extracts the full $1080 \times 1920$ uncompressed bitmap.
* Calculates a generous portrait bounding box:
  * Centers on the face center $(c_x, c_y)$.
  * Expands dimensions by $2.2\times - 2.8\times$ the face bounding box to capture hair, shoulders, and background context in a standardized $3:4$ or $4:5$ portrait aspect ratio.
  * Clamps to video frame boundaries safely without distortion.

---

### 3.10 Step 10: Dynamic Collage Layout Engine
The canvas rendering engine selects a bespoke layout depending on the number of detected individuals $N$:

* **$N = 1$**: Centered featured hero card with decorative frame and appearance metadata.
* **$N = 2$**: Two balanced vertical/tilted polaroid cards side-by-side.
* **$N = 3$**: Top hero card with two paired cards below.
* **$N = 4$**: $2 \times 2$ asymmetric grid with gentle alternating tilt angles ($-3^\circ, +2^\circ$).
* **$N = 5$ (Sample 1 format)**:
  * Top row: 2 portrait cards.
  * Middle row: 1 centered wide/prominent card.
  * Bottom row: 2 portrait cards.
  * Decorated with soft rounded borders ($16\text{dp}$), subtle drop shadows ($8\text{dp}$ blur), and clean typography.
* **$N = 6\text{–}8+$**: Dynamic masonry / staggered card arrangement with balanced padding.

---

## 4. Non-Functional Requirements

| Category | Specification |
| :--- | :--- |
| **Performance** | Asynchronous execution on `Dispatchers.Default` / `Dispatchers.IO`. Zero main thread blocking. Memory ceiling $< 250\text{ MB}$ heap usage. |
| **Battery & Thermal** | Immediate recycling of intermediate `Bitmap` objects. Graceful GC behavior. |
| **Offline Privacy** | Zero internet permissions required in `AndroidManifest.xml` (or strict absence of network calls). 100% on-device. |
| **Robust Error Handling** | Clear, non-technical feedback for edge cases: zero faces found, corrupted video file, non-portrait aspect ratio, out of storage. |
| **Testability** | Deterministic clustering and appearance count evaluation test suite for Sample 1, 2, and 3. |
