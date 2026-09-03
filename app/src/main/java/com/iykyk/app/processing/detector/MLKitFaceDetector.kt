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
            .enableTracking()
            .setMinFaceSize(0.05f)
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
            faces.mapNotNull { face ->
                buildFaceInfo(face, frameBitmap, timestampMs)
            }
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

        if (rect.width() < 40f || rect.height() < 40f) {
            return null
        }

        val left = rect.left.toInt().coerceIn(0, frameBitmap.width - 1)
        val top = rect.top.toInt().coerceIn(0, frameBitmap.height - 1)
        val width = rect.width().toInt().coerceAtMost(frameBitmap.width - left)
        val height = rect.height().toInt().coerceAtMost(frameBitmap.height - top)

        if (width < 40 || height < 40) {
            return null
        }

        val faceCrop = Bitmap.createBitmap(frameBitmap, left, top, width, height)
        val sharpness = BlurDetector.computeLaplacianVariance(faceCrop)
        faceCrop.recycle()

        return DetectedFaceInfo(
            frameTimestampMs = timestampMs,
            boundingBox = rect,
            trackingId = face.trackingId,
            leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position?.let { PointF(it.x, it.y) },
            rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position?.let { PointF(it.x, it.y) },
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

    override fun close() {
        detector.close()
    }
}
