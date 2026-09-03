package com.iykyk.app.processing.detector

import android.graphics.Bitmap

object BlurDetector {

    fun computeLaplacianVariance(bitmap: Bitmap, sampleStep: Int = 2): Float {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 8 || height < 8) {
            return 0f
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var sum = 0.0
        var sumSquares = 0.0
        var count = 0

        fun luminance(pixel: Int): Float {
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            return 0.299f * r + 0.587f * g + 0.114f * b
        }

        val step = sampleStep.coerceAtLeast(1)
        for (y in 1 until height - 1 step step) {
            for (x in 1 until width - 1 step step) {
                val center = luminance(pixels[y * width + x])
                val top = luminance(pixels[(y - 1) * width + x])
                val bottom = luminance(pixels[(y + 1) * width + x])
                val left = luminance(pixels[y * width + (x - 1)])
                val right = luminance(pixels[y * width + (x + 1)])

                val lap = top + bottom + left + right - 4f * center
                sum += lap
                sumSquares += lap * lap
                count++
            }
        }

        if (count == 0) return 0f
        val mean = sum / count
        val variance = (sumSquares / count) - mean * mean
        return variance.toFloat().coerceAtLeast(0f)
    }

    fun normalizedSharpness(variance: Float): Float {
        return (variance / 150f).coerceIn(0f, 1f)
    }
}
