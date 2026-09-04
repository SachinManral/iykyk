package com.iykyk.app.processing.detector

import android.graphics.Bitmap

object BlurDetector {

    fun computeLaplacianVarianceRegion(
        bitmap: Bitmap,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        sampleStep: Int = 3
    ): Float {
        if (width < 8 || height < 8) return 0f

        val safeX = x.coerceIn(0, bitmap.width - 1)
        val safeY = y.coerceIn(0, bitmap.height - 1)
        val safeW = width.coerceAtMost(bitmap.width - safeX)
        val safeH = height.coerceAtMost(bitmap.height - safeY)

        if (safeW < 8 || safeH < 8) return 0f

        val pixels = IntArray(safeW * safeH)
        bitmap.getPixels(pixels, 0, safeW, safeX, safeY, safeW, safeH)

        var sum = 0.0
        var sumSquares = 0.0
        var count = 0

        val step = sampleStep.coerceAtLeast(1)
        for (row in 1 until safeH - 1 step step) {
            val rowOffset = row * safeW
            val prevRowOffset = (row - 1) * safeW
            val nextRowOffset = (row + 1) * safeW
            for (col in 1 until safeW - 1 step step) {
                val p = pixels[rowOffset + col]
                val center = 0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF)
                val pt = pixels[prevRowOffset + col]
                val top = 0.299f * ((pt shr 16) and 0xFF) + 0.587f * ((pt shr 8) and 0xFF) + 0.114f * (pt and 0xFF)
                val pb = pixels[nextRowOffset + col]
                val bottom = 0.299f * ((pb shr 16) and 0xFF) + 0.587f * ((pb shr 8) and 0xFF) + 0.114f * (pb and 0xFF)
                val pl = pixels[rowOffset + (col - 1)]
                val left = 0.299f * ((pl shr 16) and 0xFF) + 0.587f * ((pl shr 8) and 0xFF) + 0.114f * (pl and 0xFF)
                val pr = pixels[rowOffset + (col + 1)]
                val right = 0.299f * ((pr shr 16) and 0xFF) + 0.587f * ((pr shr 8) and 0xFF) + 0.114f * (pr and 0xFF)

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

    fun computeLaplacianVariance(bitmap: Bitmap, sampleStep: Int = 2): Float {
        return computeLaplacianVarianceRegion(bitmap, 0, 0, bitmap.width, bitmap.height, sampleStep)
    }

    fun normalizedSharpness(variance: Float): Float {
        return (variance / 150f).coerceIn(0f, 1f)
    }
}
