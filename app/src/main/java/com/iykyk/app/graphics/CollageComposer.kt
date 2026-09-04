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

        // Load custom user assets from app/src/main/assets/collage/ if available
        val customFlowers = loadBitmapsFromAssetDir(assetManager, "collage/decorations/flowers")
        val customTapes = loadBitmapsFromAssetDir(assetManager, "collage/decorations/tape")
        val customStickers = loadBitmapsFromAssetDir(assetManager, "collage/decorations/stickers")
        val customStars = loadBitmapsFromAssetDir(assetManager, "collage/decorations/stars")
        val customBackgrounds = loadBitmapsFromAssetDir(assetManager, "collage/backgrounds")

        // 1. Compute dynamic layout slots for the current theme and person count
        val baseSlots = CollageLayouts.getSlots(theme, people.size)

        // Apply randomized tilt mode: 70% organic playful tilts, 30% connected straight
        val tiltMode = rng.nextInt(3) // 0: Playful tilts, 1: Moderate tilts, 2: Straight / connected
        val slots = baseSlots.mapIndexed { idx, slot ->
            val angleJitter = when (tiltMode) {
                0 -> slot.rotationDeg + (rng.nextFloat() * 4f - 2f)
                1 -> slot.rotationDeg * 0.5f + (rng.nextFloat() * 2f - 1f)
                else -> 0f // Connected / straight alignment
            }
            slot.copy(rotationDeg = angleJitter)
        }

        // 2. Draw Theme-specific Background & Ambient Texture (or custom user background)
        if (customBackgrounds.isNotEmpty()) {
            val bgBmp = customBackgrounds[rng.nextInt(customBackgrounds.size)]
            drawBitmapCenterCrop(canvas, bgBmp, RectF(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat()))
        } else {
            when (theme) {
                CollageTheme.FLORAL_SCRAPBOOK -> drawFloralBackground(canvas)
                CollageTheme.VINTAGE_FILM -> drawVintageBackground(canvas, slots)
                CollageTheme.CYBER_GLOW -> drawCyberBackground(canvas)
            }
        }

        // 3. Draw Header Title & Theme Accents
        drawHeader(canvas, theme, customTitle, people.size)

        // 4. Render each person card with theme styling
        for (i in people.indices) {
            if (i < slots.size) {
                val person = people[i]
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

        // 5. Draw Prominent Sticker & Flower Overlays (overlapping photo corners & margins)
        drawProminentAestheticStickers(
            canvas = canvas,
            slots = slots,
            customFlowers = customFlowers,
            customStickers = customStickers,
            customStars = customStars,
            rng = rng
        )

        // 6. Draw Theme-tailored footer & brand signature
        drawFooter(canvas, theme, people.size, totalAppearances, logoBitmap)

        return bitmap
    }

    private fun loadBitmapsFromAssetDir(assetManager: android.content.res.AssetManager?, dirPath: String): List<Bitmap> {
        if (assetManager == null) return emptyList()
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val files = assetManager.list(dirPath) ?: emptyArray()
            for (file in files) {
                if (file.endsWith(".png", true) || file.endsWith(".jpg", true) || file.endsWith(".webp", true)) {
                    try {
                        assetManager.open("$dirPath/$file").use { input ->
                            android.graphics.BitmapFactory.decodeStream(input)?.let { bmp ->
                                bitmaps.add(bmp)
                            }
                        }
                    } catch (e: Exception) {
                        // ignore unreadable file
                    }
                }
            }
        } catch (e: Exception) {
            // ignore listing errors
        }
        return bitmaps
    }

    // =========================================================================
    // THEME 1: FLORAL SCRAPBOOK RENDERER
    // =========================================================================

    private fun drawFloralBackground(canvas: Canvas) {
        // Warm parchment cream gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f,
                CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(),
                intArrayOf(
                    Color.parseColor("#FAF7F2"),
                    Color.parseColor("#F4ECE1"),
                    Color.parseColor("#EFE5D8"),
                    Color.parseColor("#E8DBC9")
                ),
                floatArrayOf(0.0f, 0.35f, 0.70f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, CANVAS_WIDTH.toFloat(), CANVAS_HEIGHT.toFloat(), bgPaint)

        // Subtle soft botanical texture circles
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        auraPaint.color = Color.parseColor("#FCE7D6")
        auraPaint.alpha = 60
        canvas.drawCircle(150f, 300f, 220f, auraPaint)
        auraPaint.color = Color.parseColor("#E8F0E4")
        auraPaint.alpha = 50
        canvas.drawCircle(950f, 900f, 260f, auraPaint)
        auraPaint.color = Color.parseColor("#FDE4E4")
        auraPaint.alpha = 60
        canvas.drawCircle(200f, 1500f, 240f, auraPaint)
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

        // Soft realistic drop shadow for paper
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#25201A")
            alpha = 40
            setShadowLayer(26f, 0f, 14f, Color.parseColor("#35221144"))
        }
        val cardRadius = 16f
        canvas.drawRoundRect(rect, cardRadius, cardRadius, shadowPaint)

        // White/Ivory Polaroid paper card
        val paperPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, paperPaint)

        // Subtle warm border
        val borderStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EFE8DE")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, borderStroke)

        // Photo cutout (with polaroid bottom border)
        val margin = 16f
        val bottomExtra = slot.polaroidBottomExtra.coerceAtLeast(36f)
        val photoRect = RectF(
            rect.left + margin,
            rect.top + margin,
            rect.right - margin,
            rect.bottom - bottomExtra
        )
        val photoRadius = 10f

        val clipPath = Path().apply {
            addRoundRect(photoRect, photoRadius, photoRadius, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        drawBitmapCenterCrop(canvas, bitmap, photoRect)
        canvas.restore()

        // Washi Tape on top edge
        if (customTapes.isNotEmpty()) {
            val tapeBmp = customTapes[index % customTapes.size]
            val tapeW = 120f
            val tapeH = (tapeW * tapeBmp.height / tapeBmp.width.coerceAtLeast(1)).coerceIn(24f, 60f)
            val isLeft = (index % 2 == 0)
            val tapeX = if (isLeft) rect.left + 20f else rect.right - tapeW - 20f
            val tapeY = rect.top - tapeH / 2f
            canvas.save()
            canvas.rotate(if (isLeft) -8f else 7f, tapeX + tapeW / 2f, tapeY + tapeH / 2f)
            canvas.drawBitmap(tapeBmp, null, RectF(tapeX, tapeY, tapeX + tapeW, tapeY + tapeH), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            canvas.restore()
        } else {
            // Procedurally synthesized translucent tape: kraft, beige, pink, transparent
            val tapeStyles = listOf(
                TapeGenerator.TapeStyle.KRAFT,
                TapeGenerator.TapeStyle.BEIGE,
                TapeGenerator.TapeStyle.PINK,
                TapeGenerator.TapeStyle.TRANSPARENT
            )
            val chosenStyle = tapeStyles[index % tapeStyles.size]
            val isLeft = (index % 2 == 0)
            val tapeW = 130f
            val tapeH = 36f
            val centerX = if (isLeft) rect.left + 55f else rect.right - 55f
            val centerY = rect.top + 2f
            val rot = if (isLeft) -8.5f else 7.5f

            TapeGenerator.drawTapeOnCanvas(
                canvas = canvas,
                centerX = centerX,
                centerY = centerY,
                style = chosenStyle,
                rotationDeg = rot,
                width = tapeW,
                height = tapeH,
                seed = (index * 777L + 42L)
            )
        }

        canvas.restore()
    }

    private fun drawProminentAestheticStickers(
        canvas: Canvas,
        slots: List<CollageLayouts.LayoutSlot>,
        customFlowers: List<Bitmap> = emptyList(),
        customStickers: List<Bitmap> = emptyList(),
        customStars: List<Bitmap> = emptyList(),
        rng: java.util.Random
    ) {
        val userAssets = (customFlowers + customStickers + customStars)

        // 1. Draw stickers overlapping photo card corners for authentic scrapbook look
        for ((idx, slot) in slots.withIndex()) {
            val rect = slot.rect
            val cornerSpot = when (idx % 4) {
                0 -> Pair(rect.right - 10f, rect.bottom - 10f) // Bottom-Right
                1 -> Pair(rect.left + 10f, rect.top + 10f)     // Top-Left
                2 -> Pair(rect.left + 15f, rect.bottom - 15f) // Bottom-Left
                else -> Pair(rect.right - 15f, rect.top + 15f) // Top-Right
            }

            if (userAssets.isNotEmpty()) {
                val assetBmp = userAssets[idx % userAssets.size]
                val size = 70f + rng.nextFloat() * 20f
                val destRect = RectF(cornerSpot.first - size / 2f, cornerSpot.second - size / 2f, cornerSpot.first + size / 2f, cornerSpot.second + size / 2f)
                canvas.save()
                canvas.rotate((rng.nextFloat() * 24f - 12f), cornerSpot.first, cornerSpot.second)
                // Draw 3D sticker drop shadow
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#40000000")
                    setShadowLayer(10f, 2f, 4f, Color.parseColor("#50000000"))
                }
                canvas.drawCircle(cornerSpot.first, cornerSpot.second + 3f, size / 2f, shadowPaint)
                canvas.drawBitmap(assetBmp, null, destRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            } else {
                // Built-in die-cut stickers placed over card corners
                when (idx % 4) {
                    0 -> drawDaisySticker(canvas, cornerSpot.first, cornerSpot.second, 32f, Color.parseColor("#F582A8"), true)
                    1 -> drawSmileySticker(canvas, cornerSpot.first, cornerSpot.second, 28f)
                    2 -> drawBlossomSticker(canvas, cornerSpot.first, cornerSpot.second, 30f, Color.parseColor("#FFBE53"))
                    else -> drawStarSparkle(canvas, cornerSpot.first, cornerSpot.second, 26f, Color.parseColor("#00F5FF"))
                }
            }
        }

        // 2. Draw aesthetic stickers in header, margins, and negative spaces
        val marginSpots = listOf(
            Triple(110f, 150f, 46f),
            Triple(970f, 160f, 48f),
            Triple(65f, 960f, 42f),
            Triple(1015f, 980f, 46f),
            Triple(130f, 1690f, 50f),
            Triple(950f, 1680f, 48f)
        )

        for ((idx, spot) in marginSpots.withIndex()) {
            val (x, y, size) = spot
            if (userAssets.isNotEmpty()) {
                val assetBmp = userAssets[(idx + 2) % userAssets.size]
                val destRect = RectF(x - size / 2f, y - size / 2f, x + size / 2f, y + size / 2f)
                canvas.drawBitmap(assetBmp, null, destRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            } else {
                when (idx % 3) {
                    0 -> drawDaisySticker(canvas, x, y, 26f, Color.parseColor("#FFBE53"), false)
                    1 -> drawStarSparkle(canvas, x, y, 20f, Color.parseColor("#F582A8"))
                    else -> drawBlossomSticker(canvas, x, y, 24f, Color.parseColor("#A3D9C9"))
                }
            }
        }

        // Golden center sparkles
        drawStarSparkle(canvas, 540f, 175f, 18f, Color.parseColor("#E5A93B"))
        drawStarSparkle(canvas, 540f, 880f, 16f, Color.parseColor("#F582A8"))
    }

    private fun drawSmileySticker(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        // Die-cut white border
        val whiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 4f, Color.parseColor("#40000000"))
        }
        canvas.drawCircle(cx, cy, radius + 4f, whiteBorderPaint)

        // Yellow face
        val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD93D")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius, facePaint)

        // Eyes
        val featurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2B2B2B")
            style = Paint.Style.FILL
        }
        val eyeOffset = radius * 0.35f
        canvas.drawCircle(cx - eyeOffset, cy - radius * 0.2f, radius * 0.14f, featurePaint)
        canvas.drawCircle(cx + eyeOffset, cy - radius * 0.2f, radius * 0.14f, featurePaint)

        // Smile arc
        val smilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2B2B2B")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        val smileRect = RectF(cx - radius * 0.45f, cy - radius * 0.1f, cx + radius * 0.45f, cy + radius * 0.55f)
        canvas.drawArc(smileRect, 20f, 140f, false, smilePaint)
    }

    private fun drawBlossomSticker(canvas: Canvas, cx: Float, cy: Float, radius: Float, blossomColor: Int) {
        // Die-cut white border
        val whiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            this.style = Paint.Style.FILL
            setShadowLayer(8f, 0f, 4f, Color.parseColor("#40000000"))
        }
        canvas.drawCircle(cx, cy, radius + 4f, whiteBorderPaint)

        val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = blossomColor
            this.style = Paint.Style.FILL
            alpha = 230
        }
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            this.style = Paint.Style.FILL
        }

        val petals = 5
        for (i in 0 until petals) {
            val angle = (i * (360.0 / petals)).toFloat()
            val rad = Math.toRadians(angle.toDouble())
            val px = cx + (radius * 0.6f * cos(rad)).toFloat()
            val py = cy + (radius * 0.6f * sin(rad)).toFloat()
            canvas.drawCircle(px, py, radius * 0.5f, petalPaint)
        }
        canvas.drawCircle(cx, cy, radius * 0.35f, centerPaint)
    }

    private fun drawDaisySticker(canvas: Canvas, cx: Float, cy: Float, radius: Float, petalColor: Int, withDieCutBorder: Boolean = false) {
        if (withDieCutBorder) {
            val whiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                this.style = Paint.Style.FILL
                setShadowLayer(8f, 0f, 4f, Color.parseColor("#40000000"))
            }
            canvas.drawCircle(cx, cy, radius + 5f, whiteBorderPaint)
        }

        val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = petalColor
            this.style = Paint.Style.FILL
            alpha = 230
        }
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFE66D")
            style = Paint.Style.FILL
        }

        val petals = 6
        for (i in 0 until petals) {
            val angle = (i * (360.0 / petals)).toFloat()
            val rad = Math.toRadians(angle.toDouble())
            val px = cx + (radius * 0.9f * cos(rad)).toFloat()
            val py = cy + (radius * 0.9f * sin(rad)).toFloat()
            canvas.drawCircle(px, py, radius * 0.55f, petalPaint)
        }
        canvas.drawCircle(cx, cy, radius * 0.45f, centerPaint)
    }

    // =========================================================================
    // THEME 2: VINTAGE FILM (35mm Film Strip & Sprockets)
    // =========================================================================

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

        // Frame border
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#050505")
            style = Paint.Style.FILL
        }
        val borderMargin = 6f
        val photoRect = RectF(
            rect.left + borderMargin,
            rect.top + borderMargin,
            rect.right - borderMargin,
            rect.bottom - borderMargin
        )

        canvas.drawRect(rect, framePaint)

        // Clip and draw photo
        canvas.save()
        canvas.clipRect(photoRect)
        drawBitmapCenterCrop(canvas, bitmap, photoRect)

        // Subtle warm vintage vignette / light leak on photo
        val leakPaint = Paint().apply {
            shader = LinearGradient(
                photoRect.left, photoRect.top,
                photoRect.right, photoRect.bottom,
                intArrayOf(
                    Color.parseColor("#25FFAA00"),
                    Color.TRANSPARENT,
                    Color.parseColor("#30FF5500")
                ),
                floatArrayOf(0f, 0.4f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(photoRect, leakPaint)
        canvas.restore()

        // Film edge label
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E09540")
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val frameId = String.format("%02dA", index + 1)
        canvas.drawText("▶ $frameId  KODAK 400", photoRect.left + 6f, rect.bottom - 8f, labelPaint)

        // Retro Orange Timestamp Stamp in bottom right corner of photo
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF9800")
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
        }
        canvas.drawText("'26 09 04", photoRect.right - 12f, photoRect.bottom - 16f, stampPaint)
    }

    private fun drawVintageFilmStamps(canvas: Canvas) {
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E67E22")
            textSize = 16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            alpha = 180
        }
        canvas.drawText("• SAFETY FILM • 35MM EXPOSURE •", CANVAS_WIDTH / 2f - 140f, 170f, stampPaint)
    }

    // =========================================================================
    // THEME 3: CYBER GLOW (Neon Glassmorphism & Cyber Nebula)
    // =========================================================================

    private fun drawCyberBackground(canvas: Canvas) {
        // Deep midnight cosmic violet background
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

        // Glowing ambient neon orbs
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

    private fun drawCyberGlassCard(canvas: Canvas, bitmap: Bitmap, slot: CollageLayouts.LayoutSlot, index: Int) {
        val rect = slot.rect
        val cardRadius = 24f

        // Outer Neon Glow
        val glowColor = if (index % 2 == 0) Color.parseColor("#8000F5FF") else Color.parseColor("#80FF007F")
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glowColor
            style = Paint.Style.STROKE
            strokeWidth = 6f
            setShadowLayer(22f, 0f, 0f, glowColor)
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, glowPaint)

        // Frosted glass background
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1635")
            alpha = 210
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, glassPaint)

        // Photo cutout inside glass card
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

        // Gradient neon border stroke
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

        // Cyber corner crosshairs (+)
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

    private fun drawCyberSparkles(canvas: Canvas, customStars: List<Bitmap> = emptyList()) {
        if (customStars.isNotEmpty()) {
            val starSpots = listOf(
                Pair(120f, 180f),
                Pair(960f, 190f),
                Pair(80f, 960f),
                Pair(1000f, 1020f),
                Pair(140f, 1700f),
                Pair(940f, 1710f)
            )
            for ((idx, spot) in starSpots.withIndex()) {
                val starBmp = customStars[idx % customStars.size]
                val size = 48f
                val destRect = RectF(spot.first - size / 2f, spot.second - size / 2f, spot.first + size / 2f, spot.second + size / 2f)
                canvas.drawBitmap(starBmp, null, destRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            }
        } else {
            drawStarSparkle(canvas, 120f, 180f, 18f, Color.parseColor("#00F5FF"))
            drawStarSparkle(canvas, 960f, 190f, 16f, Color.parseColor("#FF007F"))
            drawStarSparkle(canvas, 80f, 960f, 14f, Color.parseColor("#00F5FF"))
            drawStarSparkle(canvas, 1000f, 1020f, 20f, Color.parseColor("#FFD93D"))
            drawStarSparkle(canvas, 140f, 1700f, 16f, Color.parseColor("#FF007F"))
            drawStarSparkle(canvas, 940f, 1710f, 18f, Color.parseColor("#00F5FF"))
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

    // =========================================================================
    // HEADER & FOOTER RENDERING
    // =========================================================================

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

    // =========================================================================
    // HELPER: CENTER CROP BITMAP DRAWING
    // =========================================================================

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
