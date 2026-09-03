package com.iykyk.app.processing.clustering

import com.iykyk.app.data.model.AppearanceSegment
import com.iykyk.app.data.model.DetectedFaceInfo

class AppearanceSegmenter(
    val maxContinuityGapMs: Long = 600L,
    val minSegmentDurationMs: Long = 0L,
    val minDetectionsPerSegment: Int = 1
) {

    fun segmentAppearances(personDetections: List<DetectedFaceInfo>): List<AppearanceSegment> {
        if (personDetections.isEmpty()) {
            return emptyList()
        }

        val sorted = personDetections.filter { isUsableDetection(it) }.sortedBy { it.frameTimestampMs }
        if (sorted.isEmpty()) {
            return emptyList()
        }

        val segments = mutableListOf<AppearanceSegment>()
        var current = mutableListOf<DetectedFaceInfo>()

        for (detection in sorted) {
            if (current.isEmpty()) {
                current.add(detection)
                continue
            }

            val previous = current.last()
            val gap = detection.frameTimestampMs - previous.frameTimestampMs

            if (gap <= maxContinuityGapMs) {
                current.add(detection)
            } else {
                buildSegment(current)?.let { segments.add(it) }
                current = mutableListOf(detection)
            }
        }

        buildSegment(current)?.let { segments.add(it) }
        return segments
    }

    private fun buildSegment(detections: List<DetectedFaceInfo>): AppearanceSegment? {
        if (detections.isEmpty()) {
            return null
        }

        if (detections.size < minDetectionsPerSegment) {
            return null
        }

        val start = detections.first().frameTimestampMs
        val end = detections.last().frameTimestampMs

        val best = detections.maxByOrNull { it.qualityScore } ?: return null

        return AppearanceSegment(
            startTimeMs = start,
            endTimeMs = end,
            totalDetections = detections.size,
            bestFrameTimestampMs = best.frameTimestampMs
        )
    }

    private fun isUsableDetection(detection: DetectedFaceInfo): Boolean {
        val box = detection.boundingBox
        val width = (box.right - box.left).coerceAtLeast(0f)
        val height = (box.bottom - box.top).coerceAtLeast(0f)

        if ((box.right != 0f || box.bottom != 0f) && (width < 40f || height < 40f)) {
            return false
        }

        // Face must have some usable detail
        if (detection.sharpnessScore > 0f && detection.sharpnessScore < 10f) {
            return false
        }

        return true
    }
}
