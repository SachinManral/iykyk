package com.iykyk.app.data.model

import android.graphics.PointF
import android.graphics.RectF

/**
 * Information extracted from a single face detection in a video frame.
 */
data class DetectedFaceInfo(
    val frameTimestampMs: Long,
    val boundingBox: RectF,
    val trackingId: Int? = null,
    val leftEye: PointF? = null,
    val rightEye: PointF? = null,
    val noseBase: PointF? = null,
    val leftMouth: PointF? = null,
    val rightMouth: PointF? = null,
    val headEulerAngleX: Float = 0f,
    val headEulerAngleY: Float = 0f,
    val headEulerAngleZ: Float = 0f,
    val smilingProbability: Float? = null,
    val leftEyeOpenProbability: Float? = null,
    val rightEyeOpenProbability: Float? = null,
    val sharpnessScore: Float = 0f,
    val frameWidth: Int = 0,
    val frameHeight: Int = 0,
    val embedding: FloatArray? = null,
    val isSoloFrame: Boolean = true,
    var qualityScore: Float = 0f
)
