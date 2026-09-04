package com.iykyk.app.processing.embedder

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * Generates 512-D L2-normalized feature embeddings from aligned face bitmaps (112x112)
 * using on-device MobileFaceNet-ArcFace ONNX model (w600k_mbf.onnx) via ONNX Runtime.
 */
class FaceEmbedder(
    context: Context,
    modelFileName: String = "w600k_mbf.onnx"
) : AutoCloseable {

    private val ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession: OrtSession
    private val inputName: String
    private val embeddingDim: Int = 512
    private val inputSize: Int = 112

    init {
        val sessionOptions = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(4)
        }

        val modelBytes = context.assets.open(modelFileName).use { it.readBytes() }
        ortSession = ortEnvironment.createSession(modelBytes, sessionOptions)

        inputName = ortSession.inputNames.iterator().next()
    }

    fun getEmbeddingDimension(): Int = embeddingDim

    /**
     * Extracts a 512-D unit-normalized feature embedding from an aligned 112x112 face bitmap.
     */
    fun extractEmbedding(faceBitmap: Bitmap): FloatArray {
        val scaled = if (faceBitmap.width != inputSize || faceBitmap.height != inputSize) {
            Bitmap.createScaledBitmap(faceBitmap, inputSize, inputSize, true)
        } else {
            faceBitmap
        }

        val floatBuffer = convertBitmapToNchwBuffer(scaled)

        if (scaled != faceBitmap) {
            scaled.recycle()
        }

        val inputTensor = OnnxTensor.createTensor(
            ortEnvironment,
            floatBuffer,
            longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
        )

        val results = ortSession.run(mapOf(inputName to inputTensor))
        return try {
            val outputTensor = results.get(0)
            @Suppress("UNCHECKED_CAST")
            val outputArray = outputTensor.value as Array<FloatArray>
            val rawEmbedding = outputArray[0]
            l2Normalize(rawEmbedding)
        } finally {
            inputTensor.close()
            results.close()
        }
    }

    /**
     * Prepares NCHW FloatBuffer normalized as (pixel - 127.5) / 128.0.
     * InsightFace ArcFace models standard order: BGR, NCHW layout.
     */
    private fun convertBitmapToNchwBuffer(bitmap: Bitmap): FloatBuffer {
        val totalPixels = inputSize * inputSize
        val intValues = IntArray(totalPixels)
        bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        val floatBuffer = FloatBuffer.allocate(1 * 3 * totalPixels)

        // Channels: 0 = Blue, 1 = Green, 2 = Red (BGR NCHW)
        for (c in 0 until 3) {
            var idx = 0
            for (h in 0 until inputSize) {
                for (w in 0 until inputSize) {
                    val pixel = intValues[idx++]
                    val channelValue = when (c) {
                        0 -> (pixel and 0xFF)          // Blue
                        1 -> ((pixel shr 8) and 0xFF)  // Green
                        2 -> ((pixel shr 16) and 0xFF) // Red
                        else -> 0
                    }
                    floatBuffer.put((channelValue - 127.5f) / 128.0f)
                }
            }
        }
        floatBuffer.rewind()
        return floatBuffer
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0.0
        for (v in vector) {
            sumSquares += (v * v).toDouble()
        }
        val norm = sqrt(sumSquares).toFloat().coerceAtLeast(1e-10f)
        val result = FloatArray(vector.size)
        for (i in vector.indices) {
            result[i] = vector[i] / norm
        }
        return result
    }

    companion object {
        /**
         * Calculates cosine similarity between two unit-normalized embedding vectors.
         * Value ranges from -1.0 to +1.0 (higher = closer match).
         */
        fun cosineSimilarity(e1: FloatArray, e2: FloatArray): Float {
            if (e1.size != e2.size || e1.isEmpty()) return 0f
            var dot = 0f
            for (i in e1.indices) {
                dot += e1[i] * e2[i]
            }
            return dot.coerceIn(-1.0f, 1.0f)
        }

        /**
         * Calculates cosine distance: 1.0 - cosine similarity.
         * Value ranges from 0.0 to 2.0 (lower = closer match).
         */
        fun cosineDistance(e1: FloatArray, e2: FloatArray): Float {
            return 1.0f - cosineSimilarity(e1, e2)
        }
    }

    override fun close() {
        ortSession.close()
        ortEnvironment.close()
    }
}
