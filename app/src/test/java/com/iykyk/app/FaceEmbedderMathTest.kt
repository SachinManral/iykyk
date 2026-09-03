package com.iykyk.app

import com.iykyk.app.processing.embedder.FaceEmbedder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceEmbedderMathTest {

    @Test
    fun testCosineSimilarityIdenticalVectors() {
        val v1 = floatArrayOf(0.6f, 0.8f)
        val v2 = floatArrayOf(0.6f, 0.8f)
        val sim = FaceEmbedder.cosineSimilarity(v1, v2)
        assertEquals(1.0f, sim, 0.0001f)
    }

    @Test
    fun testCosineSimilarityOrthogonalVectors() {
        val v1 = floatArrayOf(1.0f, 0.0f)
        val v2 = floatArrayOf(0.0f, 1.0f)
        val sim = FaceEmbedder.cosineSimilarity(v1, v2)
        assertEquals(0.0f, sim, 0.0001f)
    }

    @Test
    fun testCosineDistanceRange() {
        val v1 = floatArrayOf(1.0f, 0.0f)
        val v2 = floatArrayOf(-1.0f, 0.0f)
        val dist = FaceEmbedder.cosineDistance(v1, v2)
        assertEquals(2.0f, dist, 0.0001f)
    }
}
