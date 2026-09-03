package com.iykyk.app

import android.graphics.RectF
import com.iykyk.app.data.model.DetectedFaceInfo
import com.iykyk.app.processing.clustering.AppearanceSegmenter
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceSegmenterTest {

    @Test
    fun testContinuousAppearanceSegmentation() {
        val segmenter = AppearanceSegmenter(
            maxContinuityGapMs = 600L,
            minSegmentDurationMs = 0L,
            minDetectionsPerSegment = 1
        )

        val detections = mutableListOf<DetectedFaceInfo>()

        // Appearance 1: 0.0s - 1.0s (every 200ms)
        for (t in listOf(0L, 200L, 400L, 600L, 800L, 1000L)) {
            detections.add(createDetection(t, quality = 0.8f))
        }

        // Appearance 2: 7.0s - 8.0s
        for (t in listOf(7000L, 7200L, 7400L, 7600L, 7800L, 8000L)) {
            detections.add(createDetection(t, quality = 0.85f))
        }

        // Appearance 3: 15.0s - 16.0s
        for (t in listOf(15000L, 15200L, 15400L, 15600L, 15800L, 16000L)) {
            detections.add(createDetection(t, quality = 0.9f))
        }

        // Appearance 4: 24.0s - 25.0s
        for (t in listOf(24000L, 24200L, 24400L, 24600L, 24800L, 25000L)) {
            detections.add(createDetection(t, quality = 0.95f))
        }

        val segments = segmenter.segmentAppearances(detections)

        // Must count exactly 4 appearances
        assertEquals(4, segments.size)
        assertEquals(0L, segments[0].startTimeMs)
        assertEquals(1000L, segments[0].endTimeMs)
        assertEquals(7000L, segments[1].startTimeMs)
        assertEquals(8000L, segments[1].endTimeMs)
    }

    @Test
    fun testSingleFrameDetectionCounted() {
        val segmenter = AppearanceSegmenter(
            maxContinuityGapMs = 600L,
            minSegmentDurationMs = 0L,
            minDetectionsPerSegment = 1
        )

        val detections = listOf(
            createDetection(1000L, quality = 0.8f),
            createDetection(1200L, quality = 0.8f),
            createDetection(8000L, quality = 0.8f) // Legitimate isolated detection
        )

        val segments = segmenter.segmentAppearances(detections)
        assertEquals(2, segments.size)
    }

    private fun createDetection(timestamp: Long, quality: Float): DetectedFaceInfo {
        return DetectedFaceInfo(
            frameTimestampMs = timestamp,
            boundingBox = RectF(100f, 100f, 300f, 300f),
            sharpnessScore = 50f,
            qualityScore = quality
        )
    }
}
