package com.iykyk.app

import android.graphics.RectF
import com.iykyk.app.data.model.DetectedFaceInfo
import com.iykyk.app.processing.clustering.AppearanceSegmenter
import com.iykyk.app.processing.clustering.IdentityClusterer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityClustererTest {

    @Test
    fun testClusteringIdenticalEmbeddings() {
        val clusterer = IdentityClusterer(distanceThreshold = 0.44f)

        // Create 2 people with distinct 512-D embedding unit vectors
        val person1Embedding = FloatArray(512).apply { this[0] = 1.0f }
        val person2Embedding = FloatArray(512).apply { this[1] = 1.0f }

        val detections = listOf(
            createDetection(timestamp = 1000L, trackingId = 1, embedding = person1Embedding),
            createDetection(timestamp = 1200L, trackingId = 1, embedding = person1Embedding),
            createDetection(timestamp = 2000L, trackingId = 2, embedding = person2Embedding),
            createDetection(timestamp = 2200L, trackingId = 2, embedding = person2Embedding),
            createDetection(timestamp = 5000L, trackingId = 3, embedding = person1Embedding) // Person 1 re-appearing
        )

        val clusters = clusterer.clusterFaces(detections)

        // Should produce exactly 2 clusters
        assertEquals(2, clusters.size)
        assertEquals(3, clusters[0].size)
        assertEquals(2, clusters[1].size)
    }

    @Test
    fun testCoOccurrenceConflictPreventsMerge() {
        val clusterer = IdentityClusterer(distanceThreshold = 0.44f)

        // Person A and Person B detected at the EXACT same frame timestamp (1000L)
        val person1Embedding = FloatArray(512).apply { this[0] = 1.0f }
        val similarEmbedding = FloatArray(512).apply {
            this[0] = 0.95f
            this[1] = 0.3122f // Unit length vector with cosine distance ~ 0.05
        }

        val detections = listOf(
            createDetection(timestamp = 1000L, trackingId = 1, embedding = person1Embedding),
            createDetection(timestamp = 1000L, trackingId = 2, embedding = similarEmbedding)
        )

        val clusters = clusterer.clusterFaces(detections)

        // Co-occurrence constraint must keep them as 2 distinct individuals
        assertEquals(2, clusters.size)
    }

    @Test
    fun testEmptyDetections() {
        val clusterer = IdentityClusterer()
        val result = clusterer.clusterFaces(emptyList())
        assertTrue(result.isEmpty())
    }

    private fun createDetection(
        timestamp: Long,
        trackingId: Int,
        embedding: FloatArray
    ): DetectedFaceInfo {
        return DetectedFaceInfo(
            frameTimestampMs = timestamp,
            boundingBox = RectF(100f, 100f, 300f, 300f),
            trackingId = trackingId,
            embedding = embedding,
            sharpnessScore = 50f,
            qualityScore = 0.9f
        )
    }
}
