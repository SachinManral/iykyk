package com.iykyk.app.graphics

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.iykyk.app.data.model.PersonIdentity
import kotlin.math.cos
import kotlin.math.sin

/**
 * Dynamic aesthetic Canvas rendering engine.
 * Composes publication-ready portrait collages across multiple themes (Floral Scrapbook, Vintage Film, Cyber Glow)
 * dynamically tailored to the exact number of detected people (with dedicated templates for 4, 5, 6 people).
 */
object CollageComposer {

    const val CANVAS_WIDTH = 1080
    const val CANVAS_HEIGHT = 1920

    /**
     * Renders a high-resolution exportable collage bitmap with the requested aesthetic theme.
     * Features dynamic rotation angle variations (tilted vs connected), and prominent sticker overlays.
     */
    fun createCollageBitmap(
        people: List<PersonIdentity>,
        totalAppearances: Int,
        customTitle: String? = null,
        theme: CollageTheme = CollageTheme.FLORAL_SCRAPBOOK,
        logoBitmap: Bitmap? = null,
        assetManager: android.content.res.AssetManager? = null,
        seed: Long = System.currentTimeMillis()
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CANVAS_WIDTH, CANVAS_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val rng = java.util.Random(seed)

        val customFlowers = loadBitmapsFromAssetDir(assetManager, "collage/decorations/flowers")
        val customTapes = loadBitmapsFromAssetDir(assetManager, "collage/decorations/tape")
        val customStickers = loadBitmapsFromAssetDir(assetManager, "collage/decorations/stickers")
        val customStars = loadBitmapsFromAssetDir(assetManager, "collage/decorations/stars")
        val customBackgrounds = loadBitmapsFromAssetDir(assetManager, "collage/backgrounds")

        // 1. Layout variation & randomized slot assignment
        val layoutVariation = rng.nextInt(6)
        val renderPeople = if (people.size > 1 && rng.nextBoolean()) people.shuffled(rng) else people
        val baseSlots = CollageLayouts.getSlots(theme, renderPeople.size, layoutVariation)
        val tiltMode = rng.nextInt(3)
        val slots = baseSlots.mapIndexed { idx, slot ->
            val angleJitter = when (tiltMode) {
                0 -> slot.rotationDeg + (rng.nextFloat() * 2.5f - 1.25f)
                1 -> slot.rotationDeg * 0.7f + (rng.nextFloat() * 1.2f - 0.6f)
                else -> 0f
            }
            slot.copy(rotationDeg = angleJitter)
        }

        // 2. Theme-matched Background
        if (customBackgrounds.isNotEmpty()) {
            val matchingBackgrounds = when (theme) {
                CollageTheme.FLORAL_SCRAPBOOK -> customBackgrounds.filter { bmp ->
                    // Favor warm craft/cream/vintage textures for scrapbook
                    true
                }
                CollageTheme.VINTAGE_FILM -> customBackgrounds
                CollageTheme.CYBER_GLOW -> customBackgrounds
            }
            val bgBmp = if (matchingBackgrounds.isNotEmpty()) matchingBackgrounds[rng.nextInt(matchingBackgrounds.size)] else customBackgrounds[rng.nextInt(customBackgrounds.size)]
            drawBitmapCenterCrop(canvas, bgBmp, RectF(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat()))
            if (theme == CollageTheme.FLORAL_SCRAPBOOK) {
                drawScrapbookPaperScraps(canvas, rng)
            }
        } else {
            when (theme) {
                CollageTheme.FLORAL_SCRAPBOOK -> drawFloralBackground(canvas, rng)
                CollageTheme.VINTAGE_FILM -> drawVintageBackground(canvas, slots)
                CollageTheme.CYBER_GLOW -> drawCyberBackground(canvas)
            }
        }

        // 2.5 Underlay Decorations (Behind images, above background)
        when (theme) {
            CollageTheme.FLORAL_SCRAPBOOK -> drawFloralUnderlayDecorations(canvas, slots, customFlowers, customStickers, rng)
            CollageTheme.VINTAGE_FILM -> drawVintageUnderlayDecorations(canvas, slots, customStickers, rng)
            CollageTheme.CYBER_GLOW -> drawCyberUnderlayDecorations(canvas, slots, customStars, rng)
        }

        // 3. Render person cards
        for (i in renderPeople.indices) {
            if (i < slots.size) {
                val person = renderPeople[i]
                val slot = slots[i]
                val portrait = person.representativePortraitBitmap
                if (portrait != null) {
                    when (theme) {
                        CollageTheme.FLORAL_SCRAPBOOK -> drawFloralPolaroid(canvas, portrait, slot, i, customTapes)
                        CollageTheme.VINTAGE_FILM -> drawVintagePhotoFrame(canvas, portrait, slot, i)
                        CollageTheme.CYBER_GLOW -> drawCyberGlassCard(canvas, portrait, slot, i)
                    }
                }
            }
        }

        // 4. Overlay Decorations (stickers, flowers, film accents, sparkles)
        when (theme) {
            CollageTheme.FLORAL_SCRAPBOOK -> {
                drawFloralScrapbookDecorations(
                    canvas = canvas,
                    slots = slots,
                    customFlowers = customFlowers,
                    customStickers = customStickers,
                    rng = rng
                )
            }
            CollageTheme.VINTAGE_FILM -> {
                drawVintageFilmStamps(canvas, slots, customStickers, rng)
            }
            CollageTheme.CYBER_GLOW -> {
                drawCyberSparkles(canvas, slots, customStars, customStickers, rng)
            }
        }

        return bitmap
    }

    private fun loadBitmapsFromAssetDir(assetManager: android.content.res.AssetManager?, dirPath: String): List<Bitmap> {
        if (assetManager == null) return emptyList()
        val bitmaps = mutableListOf<Bitmap>()
        val isDecoration = dirPath.contains("decorations")
        try {
            val files = assetManager.list(dirPath) ?: emptyArray()
            for (file in files) {
                val isImage = if (isDecoration) {
                    file.endsWith(".png", true) || file.endsWith(".webp", true)
                } else {
                    file.endsWith(".png", true) || file.endsWith(".jpg", true) || file.endsWith(".jpeg", true) || file.endsWith(".webp", true)
                }
                if (isImage) {
                    try {
                        assetManager.open("$dirPath/$file").use { input ->
                            android.graphics.BitmapFactory.decodeStream(input)?.let { bmp ->
                                bitmaps.add(bmp)
                            }
                        }
                    } catch (_: Exception) {
                    }
                } else if (file.endsWith(".svg", true)) {
                    try {
                        assetManager.open("$dirPath/$file").use { input ->
                            val svg = com.caverock.androidsvg.SVG.getFromInputStream(input)
                            val viewBox = svg.documentViewBox
                            val docW = if (svg.documentWidth > 0) svg.documentWidth else (viewBox?.width() ?: 512f)
                            val docH = if (svg.documentHeight > 0) svg.documentHeight else (viewBox?.height() ?: 512f)
                            val maxDim = maxOf(docW, docH).coerceAtLeast(1f)
                            val targetSize = 512f
                            val scale = targetSize / maxDim
                            val w = (docW * scale).toInt().coerceIn(64, 1024)
                            val h = (docH * scale).toInt().coerceIn(64, 1024)
                            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val svgCanvas = Canvas(bmp)
                            svg.renderToCanvas(svgCanvas, RectF(0f, 0f, w.toFloat(), h.toFloat()))
                            bitmaps.add(bmp)
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (_: Exception) {
        }
        return bitmaps
    }

    private fun drawScrapbookPaperScraps(canvas: Canvas, rng: java.util.Random) {
        val tornScrapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8C1A3")
            style = Paint.Style.FILL
            setShadowLayer(14f, 0f, 6f, Color.parseColor("#25201505"))
        }
        val scrapPath = Path().apply {
            moveTo(35f, 130f)
            quadTo(540f, 110f, 1045f, 140f)
            quadTo(1060f, 960f, 1040f, 1780f)
            quadTo(540f, 1800f, 40f, 1770f)
            quadTo(20f, 960f, 35f, 130f)
            close()
        }
        canvas.drawPath(scrapPath, tornScrapPaint)

        val rightScrapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EFE5D5")
            style = Paint.Style.FILL
            setShadowLayer(10f, -2f, 4f, Color.parseColor("#201A1005"))
        }
        val rightScrapPath = Path().apply {
            moveTo(880f, 540f)
            lineTo(1080f, 530f)
            lineTo(1080f, 980f)
            lineTo(890f, 990f)
            quadTo(870f, 760f, 880f, 540f)
            close()
        }
        canvas.drawPath(rightScrapPath, rightScrapPaint)

        drawHandDrawnHeart(canvas, 935f, 700f, 44f)
    }

    private fun drawFloralBackground(canvas: Canvas, rng: java.util.Random) {
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f,
                CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#E9DAC4"),
                    Color.parseColor("#DEC7AC"),
                    Color.parseColor("#D4B999"),
                    Color.parseColor("#CBB08F")
                ),
                floatArrayOf(0.0f, 0.35f, 0.70f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), bgPaint)
        drawScrapbookPaperScraps(canvas, rng)

        val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8E6E4F")
            alpha = 25
            style = Paint.Style.FILL
        }
        for (i in 0 until 180) {
            val gx = rng.nextFloat() * CANVAS_WIDTH
            val gy = rng.nextFloat() * CANVAS_HEIGHT
            val gr = 0.6f + rng.nextFloat() * 1.2f
            canvas.drawCircle(gx, gy, gr, grainPaint)
        }
    }

    private fun drawFloralPolaroid(
        canvas: Canvas,
        bitmap: Bitmap,
        slot: CollageLayouts.LayoutSlot,
        index: Int,
        customTapes: List<Bitmap> = emptyList()
    ) {
        val rect = slot.rect
        val rotation = slot.rotationDeg

        canvas.save()
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        if (rotation != 0f) {
            canvas.rotate(rotation, centerX, centerY)
        }

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#25201A")
            alpha = 45
            setShadowLayer(24f, 0f, 12f, Color.parseColor("#40221105"))
        }
        val cardRadius = 14f
        canvas.drawRoundRect(rect, cardRadius, cardRadius, shadowPaint)

        val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAF8F5")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, paperPaint)

        val borderStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E7DEC8")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, borderStroke)

        val margin = 14f
        val bottomExtra = slot.polaroidBottomExtra.coerceAtLeast(36f)
        val photoRect = RectF(
            rect.left + margin,
            rect.top + margin,
            rect.right - margin,
            rect.bottom - bottomExtra
        )
        val photoRadius = 8f

        val clipPath = Path().apply {
            addRoundRect(photoRect, photoRadius, photoRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        drawBitmapCenterCrop(canvas, bitmap, photoRect)
        canvas.restore()

        if (customTapes.isNotEmpty()) {
            val tapeBmp = customTapes[(index + (slot.rect.top.toInt())) % customTapes.size]
            val tapeW = 125f
            val tapeH = (tapeW * tapeBmp.height / tapeBmp.width.coerceAtLeast(1)).coerceIn(24f, 50f)
            val isLeft = (index % 2 == 0)
            val tapeX = if (isLeft) rect.left + 35f else rect.right - tapeW - 35f
            val tapeY = rect.top - tapeH / 2f
            canvas.save()
            canvas.rotate(if (isLeft) -6f else 6f, tapeX + tapeW / 2f, tapeY + tapeH / 2f)
            canvas.drawBitmap(tapeBmp, null, RectF(tapeX, tapeY, tapeX + tapeW, tapeY + tapeH), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            val tapeStyles = listOf(
                TapeGenerator.TapeStyle.GINGHAM_PINK,
                TapeGenerator.TapeStyle.KRAFT,
                TapeGenerator.TapeStyle.SAGE_GREEN,
                TapeGenerator.TapeStyle.GINGHAM_PINK,
                TapeGenerator.TapeStyle.BEIGE
            )
            val chosenStyle = tapeStyles[(index + (slot.rect.left.toInt())) % tapeStyles.size]
            val tapeW = 140f
            val tapeH = 38f
            val tapeCenterX = rect.centerX()
            val tapeCenterY = rect.top + 2f
            val rot = if (index % 2 == 0) -4f else 3.5f

            TapeGenerator.drawTapeOnCanvas(
                canvas = canvas,
                centerX = tapeCenterX,
                centerY = tapeCenterY,
                style = chosenStyle,
                rotationDeg = rot,
                width = tapeW,
                height = tapeH,
                seed = (index * 777L + 42L)
            )
        }

        canvas.restore()
    }

    private fun isOverlappingPhoto(decoRect: RectF, slots: List<CollageLayouts.LayoutSlot>): Boolean {
        return slots.any { slot ->
            val buffer = 8f
            val innerPhoto = RectF(
                slot.rect.left + buffer,
                slot.rect.top + buffer,
                slot.rect.right - buffer,
                slot.rect.bottom - slot.polaroidBottomExtra.coerceAtLeast(32f)
            )
            RectF.intersects(decoRect, innerPhoto)
        }
    }

    private fun drawFloralUnderlayDecorations(
        canvas: Canvas,
        slots: List<CollageLayouts.LayoutSlot>,
        customFlowers: List<Bitmap>,
        customStickers: List<Bitmap>,
        rng: java.util.Random
    ) {
        val shuffledFlowers = if (customFlowers.isNotEmpty()) customFlowers.shuffled(rng) else emptyList()
        val shuffledStickers = if (customStickers.isNotEmpty()) customStickers.shuffled(rng) else emptyList()
        var flowerIdx = 0
        var stickerIdx = 0

        // 1. Prominent Canvas Corner Framing Flowers (Always rendered in all 4 corners)
        val canvasCornerSpots = listOf(
            // Top-Left Corner
            Pair(100f + rng.nextFloat() * 40f, 90f + rng.nextFloat() * 40f),
            // Top-Right Corner
            Pair(CANVAS_WIDTH - (100f + rng.nextFloat() * 40f), 90f + rng.nextFloat() * 40f),
            // Bottom-Left Corner
            Pair(100f + rng.nextFloat() * 40f, CANVAS_HEIGHT - (90f + rng.nextFloat() * 40f)),
            // Bottom-Right Corner
            Pair(CANVAS_WIDTH - (100f + rng.nextFloat() * 40f), CANVAS_HEIGHT - (90f + rng.nextFloat() * 40f))
        )

        for (spot in canvasCornerSpots) {
            val size = 350f + rng.nextFloat() * 70f
            val rot = rng.nextFloat() * 360f

            if (shuffledFlowers.isNotEmpty()) {
                val flowerBmp = shuffledFlowers[flowerIdx % shuffledFlowers.size]
                flowerIdx++
                drawBitmapDecoration(canvas, flowerBmp, spot.first, spot.second, size, rot)
            } else {
                if (flowerIdx % 2 == 0) {
                    drawRealisticDaisy(canvas, spot.first, spot.second, size * 0.8f)
                } else {
                    drawPressedPeachBlossom(canvas, spot.first, spot.second, size * 0.8f, Color.parseColor("#E09585"))
                }
                flowerIdx++
            }
        }

        // 2. Peeking flowers and stickers around each photo card
        for (slot in slots) {
            val rect = slot.rect
            val candidateSpots = listOf(
                Pair(rect.left - 15f + rng.nextFloat() * 15f, rect.top - 15f + rng.nextFloat() * 15f),
                Pair(rect.right + 15f - rng.nextFloat() * 15f, rect.top - 15f + rng.nextFloat() * 15f),
                Pair(rect.left - 15f + rng.nextFloat() * 15f, rect.bottom + 10f - rng.nextFloat() * 15f),
                Pair(rect.right + 15f - rng.nextFloat() * 15f, rect.bottom + 10f - rng.nextFloat() * 15f)
            )

            val chosen = candidateSpots.shuffled(rng).take(if (slots.size > 4) 2 else 3)
            for (spot in chosen) {
                val size = 240f + rng.nextFloat() * 80f
                val rot = rng.nextFloat() * 360f

                if (shuffledFlowers.isNotEmpty()) {
                    val flowerBmp = shuffledFlowers[flowerIdx % shuffledFlowers.size]
                    flowerIdx++
                    drawBitmapDecoration(canvas, flowerBmp, spot.first, spot.second, size, rot)
                } else if (shuffledStickers.isNotEmpty()) {
                    val stickerBmp = shuffledStickers[stickerIdx % shuffledStickers.size]
                    stickerIdx++
                    drawBitmapDecoration(canvas, stickerBmp, spot.first, spot.second, size, rot)
                } else {
                    if (flowerIdx % 2 == 0) {
                        drawRealisticDaisy(canvas, spot.first, spot.second, size * 0.8f)
                    } else {
                        drawPressedPeachBlossom(canvas, spot.first, spot.second, size * 0.8f, Color.parseColor("#E09585"))
                    }
                    flowerIdx++
                }
            }
        }
    }

    private data class ScrapbookAnchor(
        val cx: Float,
        val cy: Float,
        val size: Float,
        val rotationDeg: Float,
        val isStickerAnchor: Boolean = false,
        val proceduralType: Int = 0
    )

    private fun drawFloralScrapbookDecorations(
        canvas: Canvas,
        slots: List<CollageLayouts.LayoutSlot>,
        customFlowers: List<Bitmap> = emptyList(),
        customStickers: List<Bitmap> = emptyList(),
        rng: java.util.Random
    ) {
        drawVintageTicketStamp(canvas, 130f, 1720f, 140f, 180f, -8f)
        drawFilmStripSnippet(canvas, 920f, 1730f, 280f, 65f, 32f)

        val shuffledFlowers = if (customFlowers.isNotEmpty()) customFlowers.shuffled(rng) else emptyList()
        val shuffledStickers = if (customStickers.isNotEmpty()) customStickers.shuffled(rng) else emptyList()
        var flowerIdx = 0
        var stickerIdx = 0

        val overlayAnchors = listOf(
            // Top-Center Accent
            ScrapbookAnchor(
                cx = CANVAS_WIDTH / 2f + (rng.nextFloat() * 140f - 70f),
                cy = 75f + rng.nextFloat() * 30f,
                size = 240f + rng.nextFloat() * 50f,
                rotationDeg = rng.nextFloat() * 40f - 20f,
                isStickerAnchor = true
            ),
            // Mid-Left Margin
            ScrapbookAnchor(
                cx = 60f + rng.nextFloat() * 30f,
                cy = 960f + (rng.nextFloat() * 160f - 80f),
                size = 260f + rng.nextFloat() * 50f,
                rotationDeg = rng.nextFloat() * 360f,
                isStickerAnchor = false
            ),
            // Mid-Right Margin
            ScrapbookAnchor(
                cx = CANVAS_WIDTH - (60f + rng.nextFloat() * 30f),
                cy = 960f + (rng.nextFloat() * 160f - 80f),
                size = 260f + rng.nextFloat() * 50f,
                rotationDeg = rng.nextFloat() * 360f,
                isStickerAnchor = true
            ),
            // Bottom-Center Accent
            ScrapbookAnchor(
                cx = CANVAS_WIDTH / 2f + (rng.nextFloat() * 140f - 70f),
                cy = 1840f - rng.nextFloat() * 30f,
                size = 240f + rng.nextFloat() * 50f,
                rotationDeg = rng.nextFloat() * 40f - 20f,
                isStickerAnchor = false
            ),
            // Center Canvas Gap
            ScrapbookAnchor(
                cx = CANVAS_WIDTH / 2f,
                cy = 920f + (rng.nextFloat() * 60f - 30f),
                size = 220f + rng.nextFloat() * 50f,
                rotationDeg = rng.nextFloat() * 360f,
                isStickerAnchor = true
            )
        )

        for (anchor in overlayAnchors) {
            val halfS = anchor.size / 2f
            val decoRect = RectF(anchor.cx - halfS, anchor.cy - halfS, anchor.cx + halfS, anchor.cy + halfS)
            if (isOverlappingPhoto(decoRect, slots)) continue

            if (anchor.isStickerAnchor && shuffledStickers.isNotEmpty()) {
                val stickerBmp = shuffledStickers[stickerIdx % shuffledStickers.size]
                stickerIdx++
                drawBitmapDecoration(canvas, stickerBmp, anchor.cx, anchor.cy, anchor.size, anchor.rotationDeg)
            } else if (shuffledFlowers.isNotEmpty()) {
                val flowerBmp = shuffledFlowers[flowerIdx % shuffledFlowers.size]
                flowerIdx++
                drawBitmapDecoration(canvas, flowerBmp, anchor.cx, anchor.cy, anchor.size, anchor.rotationDeg)
            } else {
                when (anchor.proceduralType) {
                    0 -> drawRealisticDaisy(canvas, anchor.cx, anchor.cy, anchor.size)
                    1 -> drawPressedPeachBlossom(canvas, anchor.cx, anchor.cy, anchor.size, Color.parseColor("#E09585"))
                    2 -> drawButterflySticker(canvas, anchor.cx, anchor.cy, anchor.size)
                    else -> drawVintagePostageStampSticker(canvas, anchor.cx, anchor.cy, anchor.size, anchor.rotationDeg)
                }
                flowerIdx++
            }
        }
    }

    private fun drawBitmapDecoration(
        canvas: Canvas,
        bitmap: Bitmap,
        cx: Float,
        cy: Float,
        targetWidth: Float,
        rotationDeg: Float
    ) {
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)
        val aspect = bitmap.height.toFloat() / bitmap.width.coerceAtLeast(1)
        val targetHeight = targetWidth * aspect
        val halfW = targetWidth / 2f
        val halfH = targetHeight / 2f
        val rect = RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, null, rect, paint)
        canvas.restore()
    }

    private fun drawRealisticDaisy(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val radius = size / 2f

        // Soft drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#40180F05")
            setShadowLayer(16f, 0f, 8f, Color.parseColor("#45180F05"))
        }
        canvas.drawCircle(cx, cy + 5f, radius * 0.9f, shadowPaint)

        // 16 Petals in double layer
        val petalCount = 16
        val petalLength = radius * 0.92f
        val petalWidth = radius * 0.22f

        val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FCFAF5")
            style = Paint.Style.FILL
        }
        val petalShadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EDE4D4")
            style = Paint.Style.FILL
        }

        // Back layer petals
        for (i in 0 until petalCount) {
            val angle = i * (360f / petalCount) + 11.25f
            canvas.save()
            canvas.rotate(angle, cx, cy)
            val pRect = RectF(cx - petalWidth * 0.45f, cy - petalLength, cx + petalWidth * 0.45f, cy)
            canvas.drawRoundRect(pRect, petalWidth * 0.45f, petalWidth * 0.45f, petalShadePaint)
            canvas.restore()
        }

        // Front layer petals
        for (i in 0 until petalCount) {
            val angle = i * (360f / petalCount)
            canvas.save()
            canvas.rotate(angle, cx, cy)
            val pRect = RectF(cx - petalWidth * 0.5f, cy - petalLength, cx + petalWidth * 0.5f, cy)
            canvas.drawRoundRect(pRect, petalWidth * 0.5f, petalWidth * 0.5f, petalPaint)
            canvas.restore()
        }

        // Golden-amber center stamen
        val stamenRadius = radius * 0.36f
        val stamenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx, cy - stamenRadius,
                cx, cy + stamenRadius,
                intArrayOf(
                    Color.parseColor("#FFC83B"),
                    Color.parseColor("#E59114"),
                    Color.parseColor("#A86005")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, stamenRadius, stamenPaint)

        // Stamen texture dots
        val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7A4100")
            alpha = 140
            style = Paint.Style.FILL
        }
        val dotCount = 12
        for (i in 0 until dotCount) {
            val a = Math.toRadians((i * (360.0 / dotCount)))
            val dist = stamenRadius * 0.55f
            val dx = cx + (dist * cos(a)).toFloat()
            val dy = cy + (dist * sin(a)).toFloat()
            canvas.drawCircle(dx, dy, 2.5f, texturePaint)
        }
    }

    private fun drawPressedPeachBlossom(canvas: Canvas, cx: Float, cy: Float, size: Float, blossomColor: Int) {
        val radius = size / 2f

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#30150A02")
            setShadowLayer(12f, 0f, 6f, Color.parseColor("#35150A02"))
        }
        canvas.drawCircle(cx, cy + 4f, radius * 0.85f, shadowPaint)

        val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = blossomColor
            alpha = 240
            style = Paint.Style.FILL
        }

        val petals = 5
        for (i in 0 until petals) {
            val angle = i * (360.0 / petals)
            val rad = Math.toRadians(angle)
            val px = cx + (radius * 0.52f * cos(rad)).toFloat()
            val py = cy + (radius * 0.52f * sin(rad)).toFloat()
            canvas.drawCircle(px, py, radius * 0.48f, petalPaint)
        }

        // Center stamen
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8E4A3B")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius * 0.22f, centerPaint)

        val innerCenter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFE0A3")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius * 0.12f, innerCenter)
    }

    private fun drawButterflySticker(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val radius = size / 2f

        canvas.save()
        canvas.rotate(14f, cx, cy)

        // Die-cut white border with drop shadow
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(14f, 0f, 7f, Color.parseColor("#45000000"))
        }
        val wingPath = Path().apply {
            // Upper right wing
            moveTo(cx, cy)
            cubicTo(cx + radius * 0.4f, cy - radius * 0.9f, cx + radius * 1.05f, cy - radius * 0.6f, cx + radius * 0.85f, cy - radius * 0.1f)
            // Lower right wing
            cubicTo(cx + radius * 0.95f, cy + radius * 0.4f, cx + radius * 0.5f, cy + radius * 0.85f, cx, cy + radius * 0.25f)
            // Lower left wing
            cubicTo(cx - radius * 0.5f, cy + radius * 0.85f, cx - radius * 0.95f, cy + radius * 0.4f, cx - radius * 0.85f, cy - radius * 0.1f)
            // Upper left wing
            cubicTo(cx - radius * 1.05f, cy - radius * 0.6f, cx - radius * 0.4f, cy - radius * 0.9f, cx, cy)
            close()
        }
        canvas.drawPath(wingPath, borderPaint)

        // Wing Peach-Terracotta Fill
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cx, cy - radius * 0.8f,
                cx, cy + radius * 0.8f,
                intArrayOf(
                    Color.parseColor("#F5B7B1"),
                    Color.parseColor("#E59866"),
                    Color.parseColor("#BA4A00")
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        val innerWing = Path().apply {
            val s = 0.88f
            moveTo(cx, cy)
            cubicTo(cx + radius * 0.4f * s, cy - radius * 0.9f * s, cx + radius * 1.05f * s, cy - radius * 0.6f * s, cx + radius * 0.85f * s, cy - radius * 0.1f * s)
            cubicTo(cx + radius * 0.95f * s, cy + radius * 0.4f * s, cx + radius * 0.5f * s, cy + radius * 0.85f * s, cx, cy + radius * 0.25f * s)
            cubicTo(cx - radius * 0.5f * s, cy + radius * 0.85f * s, cx - radius * 0.95f * s, cy + radius * 0.4f * s, cx - radius * 0.85f * s, cy - radius * 0.1f * s)
            cubicTo(cx - radius * 1.05f * s, cy - radius * 0.6f * s, cx - radius * 0.4f * s, cy - radius * 0.9f * s, cx, cy)
            close()
        }
        canvas.drawPath(innerWing, fillPaint)

        // Wing vein accents
        val veinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A2311")
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            alpha = 180
        }
        canvas.drawLine(cx, cy, cx + radius * 0.65f, cy - radius * 0.45f, veinPaint)
        canvas.drawLine(cx, cy, cx - radius * 0.65f, cy - radius * 0.45f, veinPaint)
        canvas.drawLine(cx, cy, cx + radius * 0.5f, cy + radius * 0.4f, veinPaint)
        canvas.drawLine(cx, cy, cx - radius * 0.5f, cy + radius * 0.4f, veinPaint)

        // Butterfly body & antennae
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2C180E")
            style = Paint.Style.FILL
        }
        val bodyRect = RectF(cx - 3.5f, cy - radius * 0.5f, cx + 3.5f, cy + radius * 0.4f)
        canvas.drawRoundRect(bodyRect, 3.5f, 3.5f, bodyPaint)

        val antPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2C180E")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawLine(cx, cy - radius * 0.45f, cx - 12f, cy - radius * 0.7f, antPaint)
        canvas.drawLine(cx, cy - radius * 0.45f, cx + 12f, cy - radius * 0.7f, antPaint)

        canvas.restore()
    }

    private fun drawVintageTicketStamp(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float, rotationDeg: Float) {
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)

        val halfW = width / 2f
        val halfH = height / 2f
        val rect = RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)

        // Drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#35201505")
            setShadowLayer(12f, 0f, 6f, Color.parseColor("#35201505"))
        }
        canvas.drawRoundRect(rect, 8f, 8f, shadowPaint)

        // Ivory/Kraft ticket paper
        val ticketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5EDE0")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 8f, 8f, ticketPaint)

        // Double border line
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8A735E")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

        val innerRect = RectF(rect.left + 6f, rect.top + 6f, rect.right - 6f, rect.bottom - 6f)
        val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A89480")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(innerRect, 4f, 4f, innerBorder)

        // Text: GOOD PEOPLE / GOOD TIMES / ♡
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A4839")
            textSize = 21f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("GOOD", cx, cy - 36f, textPaint)
        canvas.drawText("PEOPLE", cx, cy - 8f, textPaint)
        canvas.drawText("GOOD", cx, cy + 20f, textPaint)
        canvas.drawText("TIMES", cx, cy + 48f, textPaint)

        // Heart symbol
        val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8A6753")
            style = Paint.Style.FILL
        }
        drawSolidHeart(canvas, cx, cy + 74f, 12f, heartPaint)

        canvas.restore()
    }

    private fun drawFilmStripSnippet(canvas: Canvas, cx: Float, cy: Float, length: Float, width: Float, rotationDeg: Float) {
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)

        val halfL = length / 2f
        val halfW = width / 2f
        val rect = RectF(cx - halfL, cy - halfW, cx + halfL, cy + halfW)

        // Film strip drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#35100802")
            setShadowLayer(10f, 0f, 5f, Color.parseColor("#35100802"))
        }
        canvas.drawRoundRect(rect, 6f, 6f, shadowPaint)

        // Dark charcoal film plastic
        val filmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1A19")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 6f, 6f, filmPaint)

        // Sprocket holes along top & bottom edges
        val sprocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DED3C4")
            style = Paint.Style.FILL
        }
        val sprocW = 10f
        val sprocH = 14f
        val step = 26f
        var curX = rect.left + 14f
        while (curX + sprocW < rect.right - 10f) {
            canvas.drawRoundRect(RectF(curX, rect.top + 5f, curX + sprocW, rect.top + 5f + sprocH), 2.5f, 2.5f, sprocketPaint)
            canvas.drawRoundRect(RectF(curX, rect.bottom - 5f - sprocH, curX + sprocW, rect.bottom - 5f), 2.5f, 2.5f, sprocketPaint)
            curX += step
        }

        canvas.restore()
    }

    private fun drawTornWashiBrandLabel(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float) {
        canvas.save()

        val halfW = width / 2f
        val halfH = height / 2f
        val left = cx - halfW
        val top = cy - halfH
        val right = cx + halfW
        val bottom = cy + halfH

        // Torn sage green paper path
        val tornPath = Path().apply {
            moveTo(left + 6f, top + 4f)
            quadTo(cx, top - 2f, right - 4f, top + 3f)
            lineTo(right + 2f, cy)
            lineTo(right - 5f, bottom - 3f)
            quadTo(cx, bottom + 4f, left + 4f, bottom - 2f)
            lineTo(left - 3f, cy)
            close()
        }

        // Drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#35101505")
            setShadowLayer(14f, 0f, 7f, Color.parseColor("#35101505"))
        }
        canvas.drawPath(tornPath, shadowPaint)

        // Sage green paper fill
        val sagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#74886F")
            style = Paint.Style.FILL
        }
        canvas.drawPath(tornPath, sagePaint)

        // Washi tape texture fibers
        val fiberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#586A54")
            alpha = 70
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        for (i in 0 until 12) {
            val fx = left + (i * 30f)
            canvas.drawLine(fx, top, fx + 16f, bottom, fiberPaint)
        }

        // Text: ✦ iykyk ✦
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F9F6F0")
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 1f, 1f, Color.parseColor("#30000000"))
        }
        canvas.drawText("✦  iykyk  ✦", cx, cy + 16f, brandPaint)

        canvas.restore()
    }

    private fun drawVintagePostageStampSticker(canvas: Canvas, cx: Float, cy: Float, size: Float, rotationDeg: Float) {
        canvas.save()
        canvas.rotate(rotationDeg, cx, cy)

        val halfW = size / 2f
        val halfH = (size * 1.22f) / 2f
        val rect = RectF(cx - halfW, cy - halfH, cx + halfW, cy + halfH)

        // Drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38150A05")
            setShadowLayer(14f, 2f, 7f, Color.parseColor("#38150A05"))
        }
        canvas.drawRoundRect(rect, 8f, 8f, shadowPaint)

        // Stamp base paper (perforated appearance)
        val stampPaper = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAF5EA")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, 6f, 6f, stampPaper)

        // Stamp inner artwork frame
        val innerRect = RectF(rect.left + 14f, rect.top + 14f, rect.right - 14f, rect.bottom - 14f)
        val artBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E4D5C1")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(innerRect, 4f, 4f, artBg)

        // Inner border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8C7259")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(innerRect, 4f, 4f, borderPaint)

        // Bird / botanical symbol in center
        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5C4533")
            textSize = 28f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("POSTAGE", cx, cy - 18f, symbolPaint)
        canvas.drawText("25¢", cx, cy + 20f, symbolPaint)

        // Postmark cancellation wavy lines across stamp
        val cancelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3B2E24")
            alpha = 180
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        for (i in -1..1) {
            val yOffset = cy + (i * 22f)
            val wave = Path().apply {
                moveTo(rect.left - 15f, yOffset)
                quadTo(cx - 20f, yOffset - 10f, cx, yOffset)
                quadTo(cx + 20f, yOffset + 10f, rect.right + 15f, yOffset)
            }
            canvas.drawPath(wave, cancelPaint)
        }

        canvas.restore()
    }

    private fun drawBotanicalLeafBranch(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        leafCount: Int,
        leafColor: Int,
        leafScale: Float = 1.0f
    ) {
        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4A3F33")
            style = Paint.Style.STROKE
            strokeWidth = 3f * leafScale
            strokeCap = Paint.Cap.ROUND
        }
        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = leafColor
            style = Paint.Style.FILL
            alpha = 230
        }

        // Curving main stem
        val midX = (startX + endX) / 2f + 25f * leafScale
        val midY = (startY + endY) / 2f - 20f * leafScale
        val stemPath = Path().apply {
            moveTo(startX, startY)
            quadTo(midX, midY, endX, endY)
        }
        canvas.drawPath(stemPath, stemPaint)

        // Paired eucalyptus/olive leaves along stem
        val lw = 36f * leafScale
        val lh = 15f * leafScale
        for (i in 1..leafCount) {
            val t = i.toFloat() / (leafCount + 1)
            val lx = (1 - t) * (1 - t) * startX + 2 * (1 - t) * t * midX + t * t * endX
            val ly = (1 - t) * (1 - t) * startY + 2 * (1 - t) * t * midY + t * t * endY

            // Left leaf
            canvas.save()
            canvas.rotate(-35f, lx, ly)
            val leftLeaf = RectF(lx - lw, ly - lh, lx + lw, ly + lh)
            canvas.drawOval(leftLeaf, leafPaint)
            canvas.restore()

            // Right leaf
            canvas.save()
            canvas.rotate(35f, lx, ly)
            val rightLeaf = RectF(lx - lw * 0.9f, ly - lh * 0.9f, lx + lw * 0.9f, ly + lh * 0.9f)
            canvas.drawOval(rightLeaf, leafPaint)
            canvas.restore()
        }
    }

    private fun drawBabysBreathSprig(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float) {
        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B5E4F")
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }
        val flowerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAF8F0")
            style = Paint.Style.FILL
        }

        canvas.drawLine(startX, startY, endX, endY, stemPaint)

        // Small blossom clusters
        val spots = listOf(
            Pair(endX, endY),
            Pair(endX - 16f, endY - 12f),
            Pair(endX + 14f, endY - 8f),
            Pair((startX + endX) / 2f - 12f, (startY + endY) / 2f - 6f),
            Pair((startX + endX) / 2f + 14f, (startY + endY) / 2f + 4f)
        )
        for (spot in spots) {
            canvas.drawLine((startX + endX) / 2f, (startY + endY) / 2f, spot.first, spot.second, stemPaint)
            canvas.drawCircle(spot.first, spot.second, 6f, flowerPaint)
            canvas.drawCircle(spot.first + 2f, spot.second - 2f, 4f, flowerPaint)
        }
    }

    private fun drawHandDrawnHeart(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7A5F48")
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path().apply {
            val top = cy - size * 0.7f
            val bottom = cy + size * 0.85f
            moveTo(cx, top + size * 0.35f)
            cubicTo(cx, top - size * 0.2f, cx - size, top - size * 0.2f, cx - size, top + size * 0.35f)
            cubicTo(cx - size, top + size * 0.7f, cx, bottom - size * 0.2f, cx, bottom)
            cubicTo(cx, bottom - size * 0.2f, cx + size, top + size * 0.7f, cx + size, top + size * 0.35f)
            cubicTo(cx + size, top - size * 0.2f, cx, top - size * 0.2f, cx, top + size * 0.35f)
        }
        canvas.drawPath(path, heartPaint)
    }

    private fun drawSolidHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path().apply {
            val top = cy - size * 0.7f
            val bottom = cy + size * 0.85f
            moveTo(cx, top + size * 0.35f)
            cubicTo(cx, top - size * 0.2f, cx - size, top - size * 0.2f, cx - size, top + size * 0.35f)
            cubicTo(cx - size, top + size * 0.7f, cx, bottom - size * 0.2f, cx, bottom)
            cubicTo(cx, bottom - size * 0.2f, cx + size, top + size * 0.7f, cx + size, top + size * 0.35f)
            cubicTo(cx + size, top - size * 0.2f, cx, top - size * 0.2f, cx, top + size * 0.35f)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawVintageBackground(canvas: Canvas, slots: List<CollageLayouts.LayoutSlot>) {
        // Dark analog darkroom film background
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f,
                0f, CANVAS_HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#171513"),
                    Color.parseColor("#1B1816"),
                    Color.parseColor("#121110")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), bgPaint)

        // Film strip black rails behind slots
        val filmRailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0C0B0A")
            style = Paint.Style.FILL
        }
        val railBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2C2825")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // Detect columns to draw continuous vertical film backing strips
        val uniqueXCols = slots.map { it.rect.left }.distinct()
        for (colX in uniqueXCols) {
            val colSlots = slots.filter { it.rect.left == colX }
            val minY = colSlots.minOf { it.rect.top } - 40f
            val maxY = colSlots.maxOf { it.rect.bottom } + 40f
            val stripWidth = colSlots.first().rect.width()
            val stripRect = RectF(colX - 25f, minY, colX + stripWidth + 25f, maxY)

            canvas.drawRoundRect(stripRect, 14f, 14f, filmRailPaint)
            canvas.drawRoundRect(stripRect, 14f, 14f, railBorderPaint)

            // Draw 35mm film perforation sprocket holes along left & right rail
            drawSprockets(canvas, stripRect.left + 8f, minY + 10f, maxY - 10f)
            drawSprockets(canvas, stripRect.right - 18f, minY + 10f, maxY - 10f)
        }
    }

    private fun drawSprockets(canvas: Canvas, x: Float, startY: Float, endY: Float) {
        val sprocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E1C1A")
            style = Paint.Style.FILL
        }
        val sprocketStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38332F")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        var curY = startY
        val sprocW = 10f
        val sprocH = 16f
        val step = 32f

        while (curY + sprocH <= endY) {
            val sRect = RectF(x, curY, x + sprocW, curY + sprocH)
            canvas.drawRoundRect(sRect, 3f, 3f, sprocketPaint)
            canvas.drawRoundRect(sRect, 3f, 3f, sprocketStroke)
            curY += step
        }
    }

    private fun drawVintagePhotoFrame(canvas: Canvas, bitmap: Bitmap, slot: CollageLayouts.LayoutSlot, index: Int) {
        val rect = slot.rect
        val cardRadius = 14f

        // 35mm Slide Mount Drop Shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#45000000")
            setShadowLayer(16f, 0f, 8f, Color.parseColor("#45000000"))
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, shadowPaint)

        // Charcoal Black 35mm Film Mount Card
        val mountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#141210")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, mountPaint)

        // Film border line
        val borderStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#332E29")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, borderStroke)

        // Inner photo cutout with margin
        val marginH = 14f
        val marginV = 28f
        val photoRect = RectF(
            rect.left + marginH,
            rect.top + marginV,
            rect.right - marginH,
            rect.bottom - marginV
        )

        canvas.save()
        canvas.clipRect(photoRect)
        drawBitmapCenterCrop(canvas, bitmap, photoRect)

        // Subtle warm vintage light leak vignette
        val leakPaint = Paint().apply {
            shader = LinearGradient(
                photoRect.left, photoRect.top,
                photoRect.right, photoRect.bottom,
                intArrayOf(
                    Color.parseColor("#22FFAA00"),
                    Color.TRANSPARENT,
                    Color.parseColor("#2AEE5500")
                ),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(photoRect, leakPaint)
        canvas.restore()

        // 4 Vintage Brass/Leather Photo Corner Mounts
        drawVintagePhotoCornerMount(canvas, photoRect.left, photoRect.top, 22f, 0)
        drawVintagePhotoCornerMount(canvas, photoRect.right, photoRect.top, 22f, 1)
        drawVintagePhotoCornerMount(canvas, photoRect.left, photoRect.bottom, 22f, 2)
        drawVintagePhotoCornerMount(canvas, photoRect.right, photoRect.bottom, 22f, 3)

        // Top Film rebate label
        val topLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D49B55")
            textSize = 15f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val filmName = if (index % 2 == 0) "KODAK PORTRA 400" else "FUJIFILM PRO 400H"
        canvas.drawText("▶  $filmName  •  SAFETY FILM", photoRect.left + 4f, rect.top + 20f, topLabelPaint)

        // Bottom Film rebate frame ID
        val frameId = String.format("%02dA", index + 1)
        canvas.drawText("▶  FRAME $frameId  •  35MM EXPOSURE", photoRect.left + 4f, rect.bottom - 10f, topLabelPaint)

        // Retro Orange LED Timestamp in bottom right corner of photo
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
        }
        canvas.drawText("'94 09 04", photoRect.right - 10f, photoRect.bottom - 12f, stampPaint)
    }

    private fun drawVintagePhotoCornerMount(canvas: Canvas, px: Float, py: Float, size: Float, corner: Int) {
        val mountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2D251E")
            style = Paint.Style.FILL
            setShadowLayer(4f, 0f, 2f, Color.parseColor("#35000000"))
        }
        val path = Path()
        when (corner) {
            0 -> {
                path.moveTo(px - 3f, py - 3f)
                path.lineTo(px + size, py - 3f)
                path.lineTo(px - 3f, py + size)
                path.close()
            }
            1 -> {
                path.moveTo(px + 3f, py - 3f)
                path.lineTo(px - size, py - 3f)
                path.lineTo(px + 3f, py + size)
                path.close()
            }
            2 -> {
                path.moveTo(px - 3f, py + 3f)
                path.lineTo(px + size, py + 3f)
                path.lineTo(px - 3f, py - size)
                path.close()
            }
            3 -> {
                path.moveTo(px + 3f, py + 3f)
                path.lineTo(px - size, py + 3f)
                path.lineTo(px + 3f, py - size)
                path.close()
            }
        }
        canvas.drawPath(path, mountPaint)
    }

    private fun drawVintageUnderlayDecorations(
        canvas: Canvas,
        slots: List<CollageLayouts.LayoutSlot>,
        customStickers: List<Bitmap>,
        rng: java.util.Random
    ) {
        for ((idx, slot) in slots.withIndex()) {
            val rect = slot.rect
            val tapeStyles = listOf(
                TapeGenerator.TapeStyle.KRAFT,
                TapeGenerator.TapeStyle.BEIGE,
                TapeGenerator.TapeStyle.TRANSPARENT
            )
            val style = tapeStyles[idx % tapeStyles.size]
            val tapeW = 130f
            val tapeH = 36f
            val cornerX = if (idx % 2 == 0) rect.left + 25f else rect.right - 25f
            val cornerY = rect.top - 6f
            val rot = if (idx % 2 == 0) -14f else 14f
            TapeGenerator.drawTapeOnCanvas(canvas, cornerX, cornerY, style, rot, tapeW, tapeH, idx * 313L + 17L)
        }
    }

    private fun drawVintageFilmStamps(
        canvas: Canvas,
        slots: List<CollageLayouts.LayoutSlot>,
        customStickers: List<Bitmap> = emptyList(),
        rng: java.util.Random
    ) {
        // Angled 35mm film strip snippets in corners & margins
        drawFilmStripSnippet(canvas, 120f, 110f, 260f, 65f, -25f)
        drawFilmStripSnippet(canvas, 960f, 110f, 260f, 65f, 25f)
        drawFilmStripSnippet(canvas, 920f, 1750f, 280f, 70f, 35f)

        // Shutter stamp and Kodak box stamps in negative space
        drawVintageCameraShutterStamp(canvas, 130f, 960f, 60f)
        drawVintageKodakBoxStamp(canvas, 950f, 960f, 130f, 68f)
        drawVintageBarcodeSticker(canvas, 140f, 1750f, 160f, 85f)

        if (customStickers.isNotEmpty()) {
            var sIdx = 0
            val candidatePositions = listOf(
                Pair(CANVAS_WIDTH / 2f, 95f),
                Pair(CANVAS_WIDTH / 2f, 1780f),
                Pair(540f, 960f)
            )
            for (pos in candidatePositions) {
                val size = 160f + rng.nextFloat() * 50f
                val rot = rng.nextFloat() * 30f - 15f
                val halfS = size / 2f
                val decoRect = RectF(pos.first - halfS, pos.second - halfS, pos.first + halfS, pos.second + halfS)
                if (!isOverlappingPhoto(decoRect, slots)) {
                    val sBmp = customStickers[sIdx % customStickers.size]
                    sIdx++
                    drawBitmapDecoration(canvas, sBmp, pos.first, pos.second, size, rot)
                }
            }
        }
    }

    private fun drawVintageCameraShutterStamp(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C48A45")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            alpha = 200
        }
        canvas.drawCircle(cx, cy, radius, stampPaint)
        canvas.drawCircle(cx, cy, radius * 0.75f, stampPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C48A45")
            textSize = 13f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("f/2.8", cx, cy - 4f, textPaint)
        canvas.drawText("1/500", cx, cy + 14f, textPaint)
    }

    private fun drawVintageKodakBoxStamp(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float) {
        val rect = RectF(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD13B")
            style = Paint.Style.FILL
            setShadowLayer(8f, 1f, 3f, Color.parseColor("#35000000"))
        }
        canvas.drawRoundRect(rect, 4f, 4f, bg)

        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D41C1C")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 4f, 4f, border)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D41C1C")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("KODAK 400", cx, cy + 6f, textPaint)
    }

    private fun drawVintageBarcodeSticker(canvas: Canvas, cx: Float, cy: Float, width: Float, height: Float) {
        val rect = RectF(cx - width / 2f, cy - height / 2f, cx + width / 2f, cy + height / 2f)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAF5EA")
            style = Paint.Style.FILL
            setShadowLayer(8f, 1f, 3f, Color.parseColor("#30000000"))
        }
        canvas.drawRoundRect(rect, 6f, 6f, bg)

        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#25201A")
            style = Paint.Style.FILL
        }
        var curX = rect.left + 14f
        val barTop = rect.top + 10f
        val barBot = rect.bottom - 22f
        val barWidths = listOf(3f, 6f, 2f, 5f, 2f, 7f, 3f, 5f, 2f, 6f, 4f, 2f, 5f, 3f, 6f, 2f, 4f)
        for (bw in barWidths) {
            if (curX + bw > rect.right - 14f) break
            canvas.drawRect(curX, barTop, curX + bw, barBot, barPaint)
            curX += bw + 5f
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#25201A")
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ISO 400 • 940904", cx, rect.bottom - 6f, textPaint)
    }

    private fun drawCyberBackground(canvas: Canvas) {
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f,
                CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#090716"),
                    Color.parseColor("#150B2E"),
                    Color.parseColor("#0B051D"),
                    Color.parseColor("#06030F")
                ),
                floatArrayOf(0f, 0.4f, 0.8f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), bgPaint)

        val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        orbPaint.shader = LinearGradient(
            0f, 0f, 400f, 400f,
            Color.parseColor("#3500F5FF"),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(180f, 400f, 260f, orbPaint)

        orbPaint.shader = LinearGradient(
            CANVAS_WIDTH.toFloat(), 1200f, CANVAS_WIDTH - 400f, 1600f,
            Color.parseColor("#35FF007F"),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(900f, 1300f, 300f, orbPaint)
    }

    private fun drawCyberUnderlayDecorations(
        canvas: Canvas,
        slots: List<CollageLayouts.LayoutSlot>,
        customStars: List<Bitmap>,
        rng: java.util.Random
    ) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        for ((idx, slot) in slots.withIndex()) {
            val rect = slot.rect
            val haloColor = if (idx % 2 == 0) Color.parseColor("#2500F5FF") else Color.parseColor("#25FF007F")
            haloPaint.color = haloColor
            canvas.drawCircle(rect.centerX(), rect.centerY(), rect.width() * 0.55f, haloPaint)
        }
    }

    private fun drawCyberGlassCard(canvas: Canvas, bitmap: Bitmap, slot: CollageLayouts.LayoutSlot, index: Int) {
        val rect = slot.rect
        val cardRadius = 24f

        val glowColor = if (index % 2 == 0) Color.parseColor("#8000F5FF") else Color.parseColor("#80FF007F")
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glowColor
            style = Paint.Style.STROKE
            strokeWidth = 6f
            setShadowLayer(22f, 0f, 0f, glowColor)
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, glowPaint)

        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1635")
            alpha = 210
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, glassPaint)

        val margin = 8f
        val photoRect = RectF(
            rect.left + margin,
            rect.top + margin,
            rect.right - margin,
            rect.bottom - margin
        )
        val photoRadius = cardRadius - 4f
        val clipPath = Path().apply {
            addRoundRect(photoRect, photoRadius, photoRadius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)
        drawBitmapCenterCrop(canvas, bitmap, photoRect)
        canvas.restore()

        val borderShader = LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            intArrayOf(
                Color.parseColor("#00F5FF"),
                Color.parseColor("#B300FF"),
                Color.parseColor("#FF007F")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = borderShader
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, borderPaint)

        drawTechCrosshair(canvas, rect.left + 16f, rect.top + 16f)
        drawTechCrosshair(canvas, rect.right - 16f, rect.bottom - 16f)
    }

    private fun drawTechCrosshair(canvas: Canvas, cx: Float, cy: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00F5FF")
            strokeWidth = 2f
            alpha = 200
        }
        val size = 6f
        canvas.drawLine(cx - size, cy, cx + size, cy, paint)
        canvas.drawLine(cx, cy - size, cx, cy + size, paint)
    }

    private fun drawCyberCornerBracket(canvas: Canvas, cx: Float, cy: Float, size: Float, corner: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (corner % 2 == 0) Color.parseColor("#00F5FF") else Color.parseColor("#FF007F")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            setShadowLayer(8f, 0f, 0f, color)
        }
        val path = Path()
        when (corner) {
            0 -> {
                path.moveTo(cx, cy + size)
                path.lineTo(cx, cy)
                path.lineTo(cx + size, cy)
            }
            1 -> {
                path.moveTo(cx - size, cy)
                path.lineTo(cx, cy)
                path.lineTo(cx, cy + size)
            }
            2 -> {
                path.moveTo(cx, cy - size)
                path.lineTo(cx, cy)
                path.lineTo(cx + size, cy)
            }
            3 -> {
                path.moveTo(cx - size, cy)
                path.lineTo(cx, cy)
                path.lineTo(cx, cy - size)
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawCyberSparkles(
        canvas: Canvas,
        slots: List<CollageLayouts.LayoutSlot>,
        customStars: List<Bitmap> = emptyList(),
        customStickers: List<Bitmap> = emptyList(),
        rng: java.util.Random
    ) {
        drawCyberCornerBracket(canvas, 50f, 50f, 60f, 0)
        drawCyberCornerBracket(canvas, CANVAS_WIDTH - 50f, 50f, 60f, 1)
        drawCyberCornerBracket(canvas, 50f, CANVAS_HEIGHT - 50f, 60f, 2)
        drawCyberCornerBracket(canvas, CANVAS_WIDTH - 50f, CANVAS_HEIGHT - 50f, 60f, 3)

        val starSpots = listOf(
            Triple(110f, 110f, 38f),
            Triple(970f, 110f, 38f),
            Triple(70f, 960f, 32f),
            Triple(1010f, 960f, 36f),
            Triple(110f, 1780f, 38f),
            Triple(970f, 1780f, 38f),
            Triple(CANVAS_WIDTH / 2f, 85f, 30f),
            Triple(CANVAS_WIDTH / 2f, 1820f, 30f)
        )

        if (customStars.isNotEmpty()) {
            for ((idx, spot) in starSpots.withIndex()) {
                val starBmp = customStars[idx % customStars.size]
                val size = spot.third * 2.2f
                val rot = rng.nextFloat() * 45f
                val halfS = size / 2f
                val decoRect = RectF(spot.first - halfS, spot.second - halfS, spot.first + halfS, spot.second + halfS)
                if (!isOverlappingPhoto(decoRect, slots)) {
                    drawBitmapDecoration(canvas, starBmp, spot.first, spot.second, size, rot)
                }
            }
        } else {
            val colors = listOf(
                Color.parseColor("#00F5FF"),
                Color.parseColor("#FF007F"),
                Color.parseColor("#B300FF"),
                Color.parseColor("#FFD93D")
            )
            for ((idx, spot) in starSpots.withIndex()) {
                val halfS = spot.third
                val decoRect = RectF(spot.first - halfS, spot.second - halfS, spot.first + halfS, spot.second + halfS)
                if (!isOverlappingPhoto(decoRect, slots)) {
                    drawStarSparkle(canvas, spot.first, spot.second, spot.third, colors[idx % colors.size])
                }
            }
        }

        if (customStickers.isNotEmpty()) {
            var sIdx = 0
            val candidateGaps = listOf(
                Pair(CANVAS_WIDTH / 2f, 960f),
                Pair(CANVAS_WIDTH / 2f, 85f),
                Pair(CANVAS_WIDTH / 2f, 1800f)
            )
            for (gap in candidateGaps) {
                val size = 150f + rng.nextFloat() * 40f
                val halfS = size / 2f
                val decoRect = RectF(gap.first - halfS, gap.second - halfS, gap.first + halfS, gap.second + halfS)
                if (!isOverlappingPhoto(decoRect, slots)) {
                    val sBmp = customStickers[sIdx % customStickers.size]
                    sIdx++
                    drawBitmapDecoration(canvas, sBmp, gap.first, gap.second, size, rng.nextFloat() * 30f - 15f)
                }
            }
        }
    }

    private fun drawStarSparkle(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val path = Path().apply {
            moveTo(cx, cy - size)
            quadTo(cx, cy, cx + size, cy)
            quadTo(cx, cy, cx, cy + size)
            quadTo(cx, cy, cx - size, cy)
            quadTo(cx, cy, cx, cy - size)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawHeader(canvas: Canvas, theme: CollageTheme, customTitle: String?, peopleCount: Int) {
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        val headerY = 120f

        when (theme) {
            CollageTheme.FLORAL_SCRAPBOOK -> {
                titlePaint.apply {
                    color = Color.parseColor("#4A3B32")
                    textSize = 44f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                }
                subPaint.apply {
                    color = Color.parseColor("#8C7A6B")
                    textSize = 24f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                val title = customTitle?.takeIf { it.isNotBlank() } ?: "sweet memories"
                canvas.drawText("🌸 $title 🌸", CANVAS_WIDTH / 2f, headerY, titlePaint)
                canvas.drawText("$peopleCount beautiful souls captured", CANVAS_WIDTH / 2f, headerY + 40f, subPaint)
            }
            CollageTheme.VINTAGE_FILM -> {
                titlePaint.apply {
                    color = Color.parseColor("#F5E6D3")
                    textSize = 38f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                }
                subPaint.apply {
                    color = Color.parseColor("#E09540")
                    textSize = 22f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                }
                val title = customTitle?.takeIf { it.isNotBlank() }?.uppercase() ?: "IYKYK ARCHIVE // 90S"
                canvas.drawText(title, CANVAS_WIDTH / 2f, headerY, titlePaint)
                canvas.drawText("ROLL 01 • FRAME [01-0$peopleCount] • 400 ISO", CANVAS_WIDTH / 2f, headerY + 36f, subPaint)
            }
            CollageTheme.CYBER_GLOW -> {
                titlePaint.apply {
                    color = Color.WHITE
                    textSize = 42f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    setShadowLayer(14f, 0f, 0f, Color.parseColor("#00F5FF"))
                }
                subPaint.apply {
                    color = Color.parseColor("#00F5FF")
                    textSize = 22f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                }
                val title = customTitle?.takeIf { it.isNotBlank() }?.uppercase() ?: "IYKYK // CAPSULE"
                canvas.drawText("✨ $title ✨", CANVAS_WIDTH / 2f, headerY, titlePaint)
                canvas.drawText("SYNCHRONIZED MEMORIES · $peopleCount NODES", CANVAS_WIDTH / 2f, headerY + 38f, subPaint)
            }
        }
    }

    private fun drawFooter(
        canvas: Canvas,
        theme: CollageTheme,
        peopleCount: Int,
        appearancesCount: Int,
        logoBitmap: Bitmap?
    ) {
        val footerY = CANVAS_HEIGHT - 90f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        when (theme) {
            CollageTheme.FLORAL_SCRAPBOOK -> {
                textPaint.apply {
                    color = Color.parseColor("#6E5D4F")
                    textSize = 28f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                }
                canvas.drawText("crafted with love · iykyk ✨", CANVAS_WIDTH / 2f, footerY + 20f, textPaint)
            }
            CollageTheme.VINTAGE_FILM -> {
                textPaint.apply {
                    color = Color.parseColor("#A89F91")
                    textSize = 24f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                }
                canvas.drawText("IYKYK ANALOG LAB · $peopleCount SUBJECTS · $appearancesCount SHOTS", CANVAS_WIDTH / 2f, footerY + 20f, textPaint)
            }
            CollageTheme.CYBER_GLOW -> {
                if (logoBitmap != null) {
                    val targetHeight = 84f
                    val aspectRatio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat().coerceAtLeast(1f)
                    val targetWidth = targetHeight * aspectRatio
                    val left = (CANVAS_WIDTH - targetWidth) / 2f
                    val top = footerY - 20f
                    canvas.drawBitmap(logoBitmap, null, RectF(left, top, left + targetWidth, top + targetHeight), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                } else {
                    textPaint.apply {
                        color = Color.WHITE
                        textSize = 30f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        setShadowLayer(10f, 0f, 0f, Color.parseColor("#FF007F"))
                    }
                    canvas.drawText("captured with  ✨  iykyk", CANVAS_WIDTH / 2f, footerY + 20f, textPaint)
                }
            }
        }
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
}
