package com.iykyk.app

import com.iykyk.app.processing.detector.FaceAligner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FaceAlignerTest {

    @Test
    fun testSimilarityTransformComputation() {
        val srcX = floatArrayOf(100f, 200f, 150f, 120f, 180f)
        val srcY = floatArrayOf(100f, 100f, 150f, 200f, 200f)

        val dstX = floatArrayOf(38.2946f, 73.5318f, 56.0252f, 41.5493f, 70.7266f)
        val dstY = floatArrayOf(51.6963f, 51.5014f, 71.7366f, 92.3655f, 92.2041f)

        val values = FaceAligner.computeSimilarityTransform(srcX, srcY, dstX, dstY)
        assertNotNull(values)
        assertEquals(9, values!!.size)
        assertEquals(1f, values[8], 0.001f)
    }

    @Test
    fun testSimilarityTransformDegeneratePoints() {
        val srcX = floatArrayOf(100f, 100f)
        val srcY = floatArrayOf(100f, 100f)
        val dstX = floatArrayOf(38f, 73f)
        val dstY = floatArrayOf(51f, 51f)

        val values = FaceAligner.computeSimilarityTransform(srcX, srcY, dstX, dstY)
        assertNull(values)
    }
}

