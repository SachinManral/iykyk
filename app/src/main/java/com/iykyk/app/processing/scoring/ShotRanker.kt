package com.iykyk.app.processing.scoring

import com.iykyk.app.data.model.DetectedFaceInfo
import kotlin.math.abs

object ShotRanker {

    /**
     * Computes the immediate multi-factor quality score for a single face detection.
     */
    fun computeSingleQualityScore(
        det: DetectedFaceInfo,
        isSoloFrame: Boolean = true
    ): Float {
        val frontal = computeFrontalityScore(det.headEulerAngleX, det.headEulerAngleY)
        val sharp = computeSharpnessScore(det.sharpnessScore)
        val eyes = computeEyesOpenScore(det.leftEyeOpenProbability, det.rightEyeOpenProbability)
        val expression = computeExpressionScore(det.smilingProbability)
        val framing = computeFramingScore(det)
        val soloBonus = if (isSoloFrame) 0.20f else 0.0f

        val baseScore = 0.25f * frontal + 0.25f * sharp + 0.20f * eyes + 0.15f * expression + 0.15f * framing
        return (baseScore + soloBonus).coerceIn(0f, 1.20f)
    }

    /**
     * Ranks and smooths quality scores across a collection of candidate detections.
     */
    fun rankCandidateShots(detections: List<DetectedFaceInfo>) {
        if (detections.isEmpty()) {
            return
        }

        for (det in detections) {
            det.qualityScore = computeSingleQualityScore(det)
        }

        val sorted = detections.sortedBy { it.frameTimestampMs }
        val smoothed = FloatArray(sorted.size)

        for (i in sorted.indices) {
            val center = sorted[i].frameTimestampMs
            var sum = 0f
            var count = 0

            for (j in sorted.indices) {
                if (abs(sorted[j].frameTimestampMs - center) <= 350L) {
                    sum += sorted[j].qualityScore
                    count++
                }
            }

            val average = if (count > 0) sum / count else sorted[i].qualityScore
            smoothed[i] = sorted[i].qualityScore * 0.90f + average * 0.10f
        }

        sorted.forEachIndexed { index, detection ->
            detection.qualityScore = smoothed[index].coerceIn(0f, 1f)
        }
    }

    private fun computeFramingScore(det: DetectedFaceInfo): Float {
        val frameWidth = det.frameWidth.toFloat()
        val frameHeight = det.frameHeight.toFloat()
        if (frameWidth <= 0f || frameHeight <= 0f) {
            return 0.7f
        }

        val box = det.boundingBox
        val marginX = frameWidth * 0.04f
        val marginY = frameHeight * 0.04f
        var score = 1f

        if (box.left < marginX || box.right > frameWidth - marginX) {
            score -= 0.35f
        }
        if (box.top < marginY || box.bottom > frameHeight - marginY) {
            score -= 0.35f
        }

        return score.coerceIn(0.1f, 1f)
    }

    private fun computeFrontalityScore(pitch: Float, yaw: Float): Float {
        val totalDeviation = abs(pitch) + abs(yaw)
        return (1.0f - (totalDeviation / 50.0f)).coerceIn(0f, 1f)
    }

    private fun computeSharpnessScore(variance: Float): Float {
        return (variance / 150.0f).coerceIn(0f, 1f)
    }

    private fun computeEyesOpenScore(leftEyeProb: Float?, rightEyeProb: Float?): Float {
        val left = leftEyeProb ?: 0.7f
        val right = rightEyeProb ?: 0.7f
        if (left < 0.35f || right < 0.35f) {
            return 0.15f
        }
        return ((left + right) / 2f).coerceIn(0f, 1f)
    }

    private fun computeExpressionScore(smileProb: Float?): Float {
        val smile = smileProb ?: 0.5f
        return (0.5f + (0.5f * smile)).coerceIn(0f, 1f)
    }
}
