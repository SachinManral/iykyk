package com.iykyk.app.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.iykyk.app.data.model.PersonIdentity
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic canvas rendering engine that composes Instagram Story-style portrait collages
 * adapted to the exact number of detected individuals (1 to 8+ people).
 */
object CollageComposer {

    private const val CANVAS_WIDTH = 1080
    private const val CANVAS_HEIGHT = 1920

    data class CardSlot(
        val rect: RectF,
        val rotationDeg: Float = 0f
    )

    /**
     * Renders a high-resolution exportable collage bitmap.
     */
    fun createCollageBitmap(
        people: List<PersonIdentity>,
        totalAppearances: Int,
        customTitle: String? = null,
        logoBitmap: Bitmap? = null
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw rich dark gradient background
        drawBackground(canvas)

        // 2. Draw subtle decorative confetti / sparkle elements
        drawConfettiAccents(canvas)

        // 3. Compute dynamic layout slots based on person count
        val slots = computeLayoutSlots(people.size)

        // 4. Render each person card with shadow, border, and generous portrait crop
        for (i in people.indices) {
            if (i < slots.size) {
                val person = people[i]
                val slot = slots[i]
                val portrait = person.representativePortraitBitmap
                if (portrait != null) {
                    drawPhotoCard(canvas, portrait, slot)
                }
            }
        }

        // 5. Draw brand footer with logo
        drawBrandingFooter(canvas, people.size, totalAppearances, logoBitmap)

        return bitmap
    }

    private fun drawBackground(canvas: Canvas) {
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f,
                CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#0F0C20"),
                    Color.parseColor("#15102A"),
                    Color.parseColor("#0D0B18")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), bgPaint)
    }

    private fun drawConfettiAccents(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        val colors = intArrayOf(
            Color.parseColor("#8A58FF"),
            Color.parseColor("#FF5BAE"),
            Color.parseColor("#4E9FFF"),
            Color.parseColor("#FFD93D")
        )

        val sparkles = listOf(
            Triple(120f, 180f, 0),
            Triple(940f, 220f, 1),
            Triple(80f, 960f, 2),
            Triple(990f, 1020f, 3),
            Triple(140f, 1680f, 1),
            Triple(920f, 1720f, 0)
        )

        for ((x, y, colorIdx) in sparkles) {
            paint.color = colors[colorIdx % colors.size]
            paint.alpha = 140
            canvas.drawCircle(x, y, 4f, paint)
        }
    }

    private fun computeLayoutSlots(count: Int): List<CardSlot> {
        val slots = mutableListOf<CardSlot>()
        val startY = 160f
        val availableHeight = 1500f
        val padding = 28f

        when (count) {
            1 -> {
                val cardW = 860f
                val cardH = 1140f
                val left = (CANVAS_WIDTH - cardW) / 2f
                val top = startY + (availableHeight - cardH) / 2f
                slots.add(CardSlot(RectF(left, top, left + cardW, top + cardH), 0f))
            }
            2 -> {
                val cardW = 460f
                val cardH = 680f
                val top = startY + 380f
                slots.add(CardSlot(RectF(60f, top, 60f + cardW, top + cardH), -2.5f))
                slots.add(CardSlot(RectF(560f, top + 40f, 560f + cardW, top + 40f + cardH), 2.5f))
            }
            3 -> {
                // Top 1 large, bottom 2 side by side
                val topW = 600f
                val topH = 680f
                val topLeft = (CANVAS_WIDTH - topW) / 2f
                slots.add(CardSlot(RectF(topLeft, startY + 60f, topLeft + topW, startY + 60f + topH), 0f))

                val botW = 450f
                val botH = 560f
                val botY = startY + 790f
                slots.add(CardSlot(RectF(60f, botY, 60f + botW, botY + botH), -2f))
                slots.add(CardSlot(RectF(570f, botY, 570f + botW, botY + botH), 2f))
            }
            4 -> {
                // 2x2 grid with subtle alternating tilt
                val cardW = 450f
                val cardH = 580f
                val left1 = 65f
                val left2 = 565f
                val row1Y = startY + 120f
                val row2Y = row1Y + cardH + 40f

                slots.add(CardSlot(RectF(left1, row1Y, left1 + cardW, row1Y + cardH), -2.5f))
                slots.add(CardSlot(RectF(left2, row1Y, left2 + cardW, row1Y + cardH), 2.0f))
                slots.add(CardSlot(RectF(left1, row2Y, left1 + cardW, row2Y + cardH), 2.0f))
                slots.add(CardSlot(RectF(left2, row2Y, left2 + cardW, row2Y + cardH), -2.5f))
            }
            5 -> {
                // 5 People: Top row 2, Middle 1, Bottom row 2 (matching reference design)
                val cardW = 450f
                val cardH = 460f
                val left1 = 65f
                val left2 = 565f

                val row1Y = startY + 40f
                slots.add(CardSlot(RectF(left1, row1Y, left1 + cardW, row1Y + cardH), -2f))
                slots.add(CardSlot(RectF(left2, row1Y, left2 + cardW, row1Y + cardH), 2f))

                // Middle centered card
                val midW = 480f
                val midH = 480f
                val midX = (CANVAS_WIDTH - midW) / 2f
                val midY = row1Y + cardH + 20f
                slots.add(CardSlot(RectF(midX, midY, midX + midW, midY + midH), 0f))

                // Bottom row 2
                val row3Y = midY + midH + 20f
                slots.add(CardSlot(RectF(left1, row3Y, left1 + cardW, row3Y + cardH), 2f))
                slots.add(CardSlot(RectF(left2, row3Y, left2 + cardW, row3Y + cardH), -2f))
            }
            else -> {
                // 6+ people: 3 rows x 2 columns adaptive layout
                val cardW = 440f
                val cardH = 420f
                val left1 = 70f
                val left2 = 570f
                var currentY = startY + 30f

                for (i in 0 until count.coerceAtMost(8)) {
                    val isLeft = (i % 2 == 0)
                    val x = if (isLeft) left1 else left2
                    val rot = if (isLeft) -1.5f else 1.5f
                    slots.add(CardSlot(RectF(x, currentY, x + cardW, currentY + cardH), rot))
                    if (!isLeft) {
                        currentY += cardH + 25f
                    }
                }
            }
        }
        return slots
    }

    private fun drawPhotoCard(canvas: Canvas, sourcePortrait: Bitmap, slot: CardSlot) {
        val rect = slot.rect
        val rotation = slot.rotationDeg

        canvas.save()
        val centerX = rect.centerX()
        val centerY = rect.centerY()

        if (rotation != 0f) {
            canvas.rotate(rotation, centerX, centerY)
        }

        // Draw shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40000000")
            setShadowLayer(20f, 0f, 10f, Color.parseColor("#60000000"))
        }
        val cardRadius = 24f
        canvas.drawRoundRect(rect, cardRadius, cardRadius, shadowPaint)

        // Draw white card border / frame
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAFAFA")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, borderPaint)

        // Inner photo frame
        val borderMargin = 10f
        val photoRect = RectF(
            rect.left + borderMargin,
            rect.top + borderMargin,
            rect.right - borderMargin,
            rect.bottom - borderMargin
        )
        val photoRadius = cardRadius - 4f

        // Clip rounded rect path for portrait
        val clipPath = Path().apply {
            addRoundRect(photoRect, photoRadius, photoRadius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)

        // Center-crop portrait bitmap into photoRect
        drawBitmapCenterCrop(canvas, sourcePortrait, photoRect)

        canvas.restore()
        canvas.restore()
    }

    private fun drawBitmapCenterCrop(canvas: Canvas, bitmap: Bitmap, destRect: RectF) {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        val destW = destRect.width()
        val destH = destRect.height()

        val scale = maxOf(destW / srcW, destH / srcH)
        val scaledW = srcW * scale
        val scaledH = srcH * scale

        val left = destRect.left + (destW - scaledW) / 2f
        val top = destRect.top + (destH - scaledH) / 2f

        val dest = RectF(left, top, left + scaledW, top + scaledH)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, dest, paint)
    }

    private fun drawBrandingFooter(
        canvas: Canvas,
        peopleCount: Int,
        appearancesCount: Int,
        logoBitmap: Bitmap? = null
    ) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D0D0E0")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val footerY = CANVAS_HEIGHT - 110f

        canvas.drawText(
            "$peopleCount people · $appearancesCount appearances",
            CANVAS_WIDTH / 2f,
            footerY - 24f,
            textPaint
        )

        if (logoBitmap != null) {
            val targetHeight = 98f
            val aspectRatio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat().coerceAtLeast(1f)
            val targetWidth = targetHeight * aspectRatio

            val left = (CANVAS_WIDTH - targetWidth) / 2f
            val top = footerY - 8f
            val destRect = RectF(left, top, left + targetWidth, top + targetHeight)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(logoBitmap, null, destRect, paint)
        } else {
            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 32f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "Captured with  ✨ iykyk",
                CANVAS_WIDTH / 2f,
                footerY + 36f,
                brandPaint
            )
        }
    }
}
