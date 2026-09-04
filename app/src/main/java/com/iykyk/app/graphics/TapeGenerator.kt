package com.iykyk.app.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import java.util.Random

/**
 * Procedural Washi & Masking Tape Generator.
 * Synthesizes realistic translucent tape strips with rough torn edges, paper fiber texture,
 * subtle noise grain, and organic rotation angles on Android Canvas.
 */
object TapeGenerator {

    enum class TapeStyle(
        val baseColorHex: String,
        val alpha: Int,
        val fiberColorHex: String,
        val hasGlossHighlight: Boolean = false
    ) {
        KRAFT(
            baseColorHex = "#C89E6C",
            alpha = 210,
            fiberColorHex = "#8E6738"
        ),
        BEIGE(
            baseColorHex = "#EFE6D6",
            alpha = 205,
            fiberColorHex = "#C4B69E"
        ),
        PINK(
            baseColorHex = "#F0B4BE",
            alpha = 200,
            fiberColorHex = "#D08593"
        ),
        TRANSPARENT(
            baseColorHex = "#F5F7FA",
            alpha = 115,
            fiberColorHex = "#FFFFFF",
            hasGlossHighlight = true
        )
    }

    /**
     * Synthesizes a standalone high-resolution translucent tape Bitmap with rough torn edges and noise grain.
     */
    fun generateTapeBitmap(
        style: TapeStyle,
        width: Float = 180f,
        height: Float = 48f,
        seed: Long = 42L
    ): Bitmap {
        val padding = 12f
        val bmpWidth = (width + padding * 2).toInt()
        val bmpHeight = (height + padding * 2).toInt()
        val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val rng = Random(seed)
        val left = padding
        val top = padding
        val right = left + width
        val bottom = top + height

        // 1. Build Rough / Torn Edge Path (jagged fiber edges on left and right)
        val tapePath = createTornTapePath(left, top, right, bottom, rng)

        // 2. Subtle Contact Drop Shadow under tape
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1208")
            alpha = 35
            setShadowLayer(8f, 0f, 4f, Color.parseColor("#30000000"))
        }
        canvas.drawPath(tapePath, shadowPaint)

        // 3. Translucent Base Rectangle Fill
        val baseColor = Color.parseColor(style.baseColorHex)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = baseColor
            alpha = style.alpha
            this.style = Paint.Style.FILL
        }
        canvas.drawPath(tapePath, fillPaint)

        // Clip to tape path for inner texture & noise grain
        canvas.save()
        canvas.clipPath(tapePath)

        // 4. Subtle Noise & Paper Fiber Flecks
        drawTapeNoise(canvas, left, top, right, bottom, style, rng)

        // 5. Gloss Highlight (for transparent tape) or Subtle Edge Highlight
        if (style.hasGlossHighlight) {
            val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    left, top,
                    left, top + height * 0.45f,
                    intArrayOf(
                        Color.argb(80, 255, 255, 255),
                        Color.argb(10, 255, 255, 255),
                        Color.TRANSPARENT
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(left, top, right, top + height * 0.45f, glossPaint)
        } else {
            // Subtle top edge paper crease
            val edgeHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 60
                strokeWidth = 1.5f
            }
            canvas.drawLine(left + 4f, top + 1.5f, right - 4f, top + 1.5f, edgeHighlight)
        }

        canvas.restore()
        return bitmap
    }

    /**
     * Renders a procedurally generated tape strip directly on target Canvas with rotation.
     */
    fun drawTapeOnCanvas(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        style: TapeStyle,
        rotationDeg: Float = 0f,
        width: Float = 140f,
        height: Float = 36f,
        seed: Long = 100L
    ) {
        val tapeBitmap = generateTapeBitmap(style, width, height, seed)
        canvas.save()
        canvas.rotate(rotationDeg, centerX, centerY)
        canvas.drawBitmap(tapeBitmap, centerX - tapeBitmap.width / 2f, centerY - tapeBitmap.height / 2f, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }

    /**
     * Generates a jagged, rough-edged polygonal path representing torn washi/masking tape.
     */
    private fun createTornTapePath(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        rng: Random
    ): Path {
        val path = Path()
        path.moveTo(left, top + 4f)

        // Top smooth edge with micro-imperfections
        val topSteps = 6
        val stepW = (right - left) / topSteps
        for (i in 1..topSteps) {
            val x = left + i * stepW
            val yJitter = (rng.nextFloat() - 0.5f) * 1.2f
            path.lineTo(x, top + yJitter)
        }

        // Right Torn / Serrated Edge (hand-torn paper fibers)
        val rightSteps = 8
        val stepH = (bottom - top) / rightSteps
        for (i in 1..rightSteps) {
            val y = top + i * stepH
            val toothJitter = (rng.nextFloat() * 6.5f) - (if (i % 2 == 0) 2.5f else -1.5f)
            path.lineTo(right - toothJitter, y)
        }

        // Bottom smooth edge with micro-imperfections
        for (i in topSteps - 1 downTo 0) {
            val x = left + i * stepW
            val yJitter = (rng.nextFloat() - 0.5f) * 1.2f
            path.lineTo(x, bottom + yJitter)
        }

        // Left Torn / Serrated Edge
        for (i in rightSteps - 1 downTo 0) {
            val y = top + i * stepH
            val toothJitter = (rng.nextFloat() * 6.5f) - (if (i % 2 == 0) 2.5f else -1.5f)
            path.lineTo(left + toothJitter, y)
        }

        path.close()
        return path
    }

    /**
     * Simulates organic noise grain, paper fibers, and micro-texture on the tape surface.
     */
    private fun drawTapeNoise(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        style: TapeStyle,
        rng: Random
    ) {
        val fiberColor = Color.parseColor(style.fiberColorHex)
        val fiberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fiberColor
            alpha = 35
            this.style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Diagonal translucent washi grain lines
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 25
            strokeWidth = 1.2f
        }
        var curX = left - 20f
        while (curX < right + 30f) {
            canvas.drawLine(curX, top, curX + 18f, bottom, linePaint)
            curX += 8f
        }

        // Subtle randomized fiber specks
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fiberColor
            alpha = 45
            this.style = Paint.Style.FILL
        }
        val totalSpecks = ((right - left) * (bottom - top) / 160f).toInt().coerceIn(15, 60)
        for (i in 0 until totalSpecks) {
            val sx = left + rng.nextFloat() * (right - left)
            val sy = top + rng.nextFloat() * (bottom - top)
            val sRadius = 0.6f + rng.nextFloat() * 0.8f
            canvas.drawCircle(sx, sy, sRadius, dotPaint)
        }
    }
}
