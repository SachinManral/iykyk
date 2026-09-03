package com.iykyk.app.processing.embedder

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Generates L2-normalized feature embeddings from aligned face bitmaps
 * using on-device TensorFlow Lite MobileFaceNet model.
 */
class FaceEmbedder(context: Context, modelFileName: String = "mobile_face_net.tflite") : AutoCloseable {

    private val interpreter: Interpreter
    private val inputImageSize: Int
    private val embeddingDim: Int

    init {
        val modelBuffer = loadModelFile(context, modelFileName)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        interpreter = Interpreter(modelBuffer, options)

        val inputShape = interpreter.getInputTensor(0).shape() // [1, 112, 112, 3]
        inputImageSize = inputShape[1]

        val outputShape = interpreter.getOutputTensor(0).shape() // e.g. [1, 192] or [1, 128]
        embeddingDim = outputShape[1]
    }

    fun getEmbeddingDimension(): Int = embeddingDim

    /**
     * Extracts an L2-normalized feature embedding vector from an aligned face bitmap.
     */
    fun extractEmbedding(faceBitmap: Bitmap): FloatArray {
        val scaled = if (faceBitmap.width != inputImageSize || faceBitmap.height != inputImageSize) {
            Bitmap.createScaledBitmap(faceBitmap, inputImageSize, inputImageSize, true)
        } else {
            faceBitmap
        }

        val inputBuffer = convertBitmapToByteBuffer(scaled)
        val outputBuffer = Array(1) { FloatArray(embeddingDim) }

        interpreter.run(inputBuffer, outputBuffer)

        if (scaled != faceBitmap) {
            scaled.recycle()
        }

        return l2Normalize(outputBuffer[0])
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * inputImageSize * inputImageSize * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.rewind()

        val intValues = IntArray(inputImageSize * inputImageSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var pixelIndex = 0
        for (i in 0 until inputImageSize) {
            for (j in 0 until inputImageSize) {
                val pixel = intValues[pixelIndex++]
                // Normalize pixels from [0, 255] to [-1, 1] standard: (val - 127.5) / 128.0
                val r = (((pixel shr 16) and 0xFF) - 127.5f) / 128.0f
                val g = (((pixel shr 8) and 0xFF) - 127.5f) / 128.0f
                val b = ((pixel and 0xFF) - 127.5f) / 128.0f

                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }
        return byteBuffer
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var sumSquares = 0f
        for (v in vector) {
            sumSquares += v * v
        }
        val norm = sqrt(sumSquares.toDouble()).toFloat().coerceAtLeast(1e-10f)
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
            if (e1.size != e2.size) return 0f
            var dot = 0f
            for (i in e1.indices) {
                dot += e1[i] * e2[i]
            }
            return dot.coerceIn(-1.0f, 1.0f)
        }

        /**
         * Calculates cosine distance: 1 - cosine similarity.
         * Value ranges from 0.0 to 2.0 (lower = closer match).
         */
        fun cosineDistance(e1: FloatArray, e2: FloatArray): Float {
            return 1.0f - cosineSimilarity(e1, e2)
        }
    }

    private fun loadModelFile(context: Context, modelFileName: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override fun close() {
        interpreter.close()
    }
}
