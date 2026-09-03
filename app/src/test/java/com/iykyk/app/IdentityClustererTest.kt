package com.iykyk.app

import android.graphics.RectF
import com.iykyk.app.data.model.DetectedFaceInfo
import com.iykyk.app.processing.clustering.IdentityClusterer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityClustererTest {

    @Test
    fun testClusteringIdenticalEmbeddings() {
        val clusterer = IdentityClusterer(distanceThreshold = 0.17f)

        // Create 2 people with distinct embedding vectors (distance = 1.0)
        val person1Embedding = floatArrayOf(1.0f, 0.0f, 0.0f)
        val person2Embedding = floatArrayOf(0.0f, 1.0f, 0.0f)

        val detections = listOf(
            createDetection(timestamp = 1000L, trackingId = 1, embedding = person1Embedding),
            createDetection(timestamp = 1250L, trackingId = 1, embedding = person1Embedding),
            createDetection(timestamp = 2000L, trackingId = 2, embedding = person2Embedding),
            createDetection(timestamp = 2250L, trackingId = 2, embedding = person2Embedding),
            createDetection(timestamp = 5000L, trackingId = 3, embedding = person1Embedding) // Person 1 re-appearing later
        )

        val clusters = clusterer.clusterFaces(detections)

        // Should produce exactly 2 clusters
        assertEquals(2, clusters.size)
        assertEquals(3, clusters[0].size)
        assertEquals(2, clusters[1].size)
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
            embedding = embedding
        )
    }
}
