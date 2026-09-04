package com.iykyk.app.processing.detector

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.iykyk.app.data.model.DetectedFaceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MLKitFaceDetector : AutoCloseable {

    private val detector: FaceDetector

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.06f)
            .build()

        detector = FaceDetection.getClient(options)
    }

    suspend fun detectFaces(
        frameBitmap: Bitmap,
        timestampMs: Long
    ): List<DetectedFaceInfo> = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(frameBitmap, 0)
            val faces = Tasks.await(detector.process(inputImage))
            val rawInfos = faces.mapNotNull { face ->
                buildFaceInfo(face, frameBitmap, timestampMs)
            }
            applyNonMaximumSuppression(rawInfos, iouThreshold = 0.35f)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun buildFaceInfo(
        face: Face,
        frameBitmap: Bitmap,
        timestampMs: Long
    ): DetectedFaceInfo? {
        val bounds = face.boundingBox
        val rect = RectF(
            bounds.left.toFloat().coerceIn(0f, frameBitmap.width.toFloat()),
            bounds.top.toFloat().coerceIn(0f, frameBitmap.height.toFloat()),
            bounds.right.toFloat().coerceIn(0f, frameBitmap.width.toFloat()),
            bounds.bottom.toFloat().coerceIn(0f, frameBitmap.height.toFloat())
        )

        val width = rect.width()
        val height = rect.height()

        if (width < 35f || height < 35f) {
            return null
        }

        // Geometric aspect ratio validation (filters dual-face collisions or non-face artifacts)
        val aspect = width / height
        if (aspect < 0.45f || aspect > 1.65f) {
            return null
        }

        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position?.let { PointF(it.x, it.y) }
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.let { PointF(it.x, it.y) }

        // Must have both eyes detected for ArcFace canonical alignment
        if (leftEye == null || rightEye == null) {
            return null
        }

        // Validate eye span using Euclidean distance (handles head tilt and rotations naturally)
        val eyeDistance = kotlin.math.hypot(
            (leftEye.x - rightEye.x).toDouble(),
            (leftEye.y - rightEye.y).toDouble()
        ).toFloat()

        if (eyeDistance < width * 0.12f || eyeDistance > width * 0.95f) {
            return null
        }

        val left = rect.left.toInt().coerceIn(0, frameBitmap.width - 1)
        val top = rect.top.toInt().coerceIn(0, frameBitmap.height - 1)
        val cropW = width.toInt().coerceAtMost(frameBitmap.width - left)
        val cropH = height.toInt().coerceAtMost(frameBitmap.height - top)

        if (cropW < 40 || cropH < 40) {
            return null
        }

        val faceCrop = Bitmap.createBitmap(frameBitmap, left, top, cropW, cropH)
        val sharpness = BlurDetector.computeLaplacianVariance(faceCrop)
        faceCrop.recycle()

        return DetectedFaceInfo(
            frameTimestampMs = timestampMs,
            boundingBox = rect,
            trackingId = face.trackingId,
            leftEye = leftEye,
            rightEye = rightEye,
            noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)?.position?.let { PointF(it.x, it.y) },
            leftMouth = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position?.let { PointF(it.x, it.y) },
            rightMouth = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position?.let { PointF(it.x, it.y) },
            headEulerAngleX = face.headEulerAngleX,
            headEulerAngleY = face.headEulerAngleY,
            headEulerAngleZ = face.headEulerAngleZ,
            smilingProbability = face.smilingProbability,
            leftEyeOpenProbability = face.leftEyeOpenProbability,
            rightEyeOpenProbability = face.rightEyeOpenProbability,
            sharpnessScore = sharpness,
            frameWidth = frameBitmap.width,
            frameHeight = frameBitmap.height
        )
    }

    private fun applyNonMaximumSuppression(
        faces: List<DetectedFaceInfo>,
        iouThreshold: Float
    ): List<DetectedFaceInfo> {
        if (faces.size <= 1) return faces

        val sorted = faces.sortedByDescending { it.sharpnessScore }
        val kept = mutableListOf<DetectedFaceInfo>()

        for (face in sorted) {
            val hasOverlap = kept.any { existing ->
                computeIoU(face.boundingBox, existing.boundingBox) > iouThreshold
            }
            if (!hasOverlap) {
                kept.add(face)
            }
        }
        return kept
    }

    private fun computeIoU(b1: RectF, b2: RectF): Float {
        val ix1 = maxOf(b1.left, b2.left)
        val iy1 = maxOf(b1.top, b2.top)
        val ix2 = minOf(b1.right, b2.right)
        val iy2 = minOf(b1.bottom, b2.bottom)

        val iw = (ix2 - ix1).coerceAtLeast(0f)
        val ih = (iy2 - iy1).coerceAtLeast(0f)
        val intersectionArea = iw * ih

        val area1 = (b1.right - b1.left) * (b1.bottom - b1.top)
        val area2 = (b2.right - b2.left) * (b2.bottom - b2.top)
        val unionArea = (area1 + area2 - intersectionArea).coerceAtLeast(1f)

        return (intersectionArea / unionArea).coerceIn(0f, 1f)
    }

    override fun close() {
        detector.close()
    }
}
