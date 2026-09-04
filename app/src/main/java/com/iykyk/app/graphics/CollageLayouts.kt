package com.iykyk.app.graphics

import android.graphics.RectF

/**
 * Geometric layout definitions for collage composition.
 * Generates custom, non-overlapping photo card slots for each (CollageTheme, PersonCount) combination.
 */
object CollageLayouts {

    const val CANVAS_WIDTH = 1080f
    const val CANVAS_HEIGHT = 1920f

    data class LayoutSlot(
        val rect: RectF,
        val rotationDeg: Float = 0f,
        val polaroidBottomExtra: Float = 0f // Extra bottom padding for polaroid label space
    )

    /**
     * Computes the layout slot coordinates for a given theme and number of people.
     */
    fun getSlots(theme: CollageTheme, personCount: Int): List<LayoutSlot> {
        val count = personCount.coerceAtLeast(1)
        return when (theme) {
            CollageTheme.FLORAL_SCRAPBOOK -> getFloralSlots(count)
            CollageTheme.VINTAGE_FILM -> getVintageSlots(count)
            CollageTheme.CYBER_GLOW -> getCyberSlots(count)
        }
    }

    private fun getFloralSlots(count: Int): List<LayoutSlot> {
        val slots = mutableListOf<LayoutSlot>()
        when (count) {
            1 -> {
                val cardW = 860f
                val cardH = 1180f
                val left = (CANVAS_WIDTH - cardW) / 2f
                val top = 280f
                slots.add(LayoutSlot(RectF(left, top, left + cardW, top + cardH), -1.5f, 90f))
            }
            2 -> {
                val cardW = 620f
                val cardH = 680f
                slots.add(LayoutSlot(RectF(90f, 260f, 90f + cardW, 260f + cardH), -3.5f, 60f))
                slots.add(LayoutSlot(RectF(370f, 880f, 370f + cardW, 880f + cardH), 3.0f, 60f))
            }
            3 -> {
                val topW = 640f
                val topH = 660f
                val topLeft = (CANVAS_WIDTH - topW) / 2f
                slots.add(LayoutSlot(RectF(topLeft, 220f, topLeft + topW, 220f + topH), -1.5f, 60f))

                val botW = 450f
                val botH = 580f
                val botY = 930f
                slots.add(LayoutSlot(RectF(65f, botY, 65f + botW, botY + botH), -3.0f, 50f))
                slots.add(LayoutSlot(RectF(565f, botY + 15f, 565f + botW, botY + 15f + botH), 2.5f, 50f))
            }
            4 -> {
                val cardW = 445f
                val cardH = 590f
                val left1 = 65f
                val left2 = 570f
                val row1Y = 220f
                val row2Y = 920f

                slots.add(LayoutSlot(RectF(left1, row1Y, left1 + cardW, row1Y + cardH), -3.5f, 50f))
                slots.add(LayoutSlot(RectF(left2, row1Y + 20f, left2 + cardW, row1Y + 20f + cardH), 3.0f, 50f))
                slots.add(LayoutSlot(RectF(left1 + 10f, row2Y, left1 + 10f + cardW, row2Y + cardH), 2.5f, 50f))
                slots.add(LayoutSlot(RectF(left2, row2Y - 10f, left2 + cardW, row2Y - 10f + cardH), -3.0f, 50f))
            }
            5 -> {
                val cardW = 450f
                val cardH = 510f
                val topY = 190f
                val botY = 1140f

                // Top row (2 photos)
                slots.add(LayoutSlot(RectF(60f, topY, 60f + cardW, topY + cardH), -3.5f, 40f))
                slots.add(LayoutSlot(RectF(570f, topY + 15f, 570f + cardW, topY + 15f + cardH), 3.0f, 40f))

                // Center featured photo
                val centerW = 530f
                val centerH = 510f
                val centerX = (CANVAS_WIDTH - centerW) / 2f
                val centerY = 650f
                slots.add(LayoutSlot(RectF(centerX, centerY, centerX + centerW, centerY + centerH), 0.5f, 40f))

                // Bottom row (2 photos)
                slots.add(LayoutSlot(RectF(60f, botY, 60f + cardW, botY + cardH), 2.5f, 40f))
                slots.add(LayoutSlot(RectF(560f, botY + 15f, 560f + cardW, botY + 15f + cardH), -2.5f, 40f))
            }
            6 -> {
                val cardW = 435f
                val cardH = 460f
                val left1 = 65f
                val left2 = 580f
                val row1Y = 210f
                val row2Y = 710f
                val row3Y = 1210f

                slots.add(LayoutSlot(RectF(left1, row1Y, left1 + cardW, row1Y + cardH), -3.0f, 40f))
                slots.add(LayoutSlot(RectF(left2, row1Y + 15f, left2 + cardW, row1Y + 15f + cardH), 2.5f, 40f))
                slots.add(LayoutSlot(RectF(left1 + 10f, row2Y, left1 + 10f + cardW, row2Y + cardH), 2.0f, 40f))
                slots.add(LayoutSlot(RectF(left2, row2Y - 10f, left2 + cardW, row2Y - 10f + cardH), -3.5f, 40f))
                slots.add(LayoutSlot(RectF(left1, row3Y, left1 + cardW, row3Y + cardH), -2.5f, 40f))
                slots.add(LayoutSlot(RectF(left2 + 5f, row3Y - 5f, left2 + 5f + cardW, row3Y - 5f + cardH), 3.0f, 40f))
            }
            else -> {
                val cardW = 430f
                val cardH = 360f
                val left1 = 70f
                val left2 = 580f
                var curY = 200f
                for (i in 0 until count.coerceAtMost(8)) {
                    val isLeft = (i % 2 == 0)
                    val x = if (isLeft) left1 else left2
                    val rot = if (isLeft) -2f else 2f
                    slots.add(LayoutSlot(RectF(x, curY, x + cardW, curY + cardH), rot, 30f))
                    if (!isLeft) curY += cardH + 20f
                }
            }
        }
        return slots
    }

    private fun getVintageSlots(count: Int): List<LayoutSlot> {
        val slots = mutableListOf<LayoutSlot>()
        when (count) {
            1 -> {
                val w = 840f
                val h = 1140f
                val left = (CANVAS_WIDTH - w) / 2f
                slots.add(LayoutSlot(RectF(left, 300f, left + w, 300f + h), 0f))
            }
            2 -> {
                val w = 680f
                val h = 640f
                val left = (CANVAS_WIDTH - w) / 2f
                slots.add(LayoutSlot(RectF(left, 240f, left + w, 240f + h), 0f))
                slots.add(LayoutSlot(RectF(left, 920f, left + w, 920f + h), 0f))
            }
            3 -> {
                val w = 620f
                val h = 460f
                val left = (CANVAS_WIDTH - w) / 2f
                slots.add(LayoutSlot(RectF(left, 210f, left + w, 210f + h), 0f))
                slots.add(LayoutSlot(RectF(left, 700f, left + w, 700f + h), 0f))
                slots.add(LayoutSlot(RectF(left, 1190f, left + w, 1190f + h), 0f))
            }
            4 -> {
                val frameW = 420f
                val frameH = 610f
                val leftStripX = 85f
                val rightStripX = 575f

                slots.add(LayoutSlot(RectF(leftStripX, 220f, leftStripX + frameW, 220f + frameH), 0f))
                slots.add(LayoutSlot(RectF(leftStripX, 890f, leftStripX + frameW, 890f + frameH), 0f))

                slots.add(LayoutSlot(RectF(rightStripX, 270f, rightStripX + frameW, 270f + frameH), 0f))
                slots.add(LayoutSlot(RectF(rightStripX, 940f, rightStripX + frameW, 940f + frameH), 0f))
            }
            5 -> {
                val topW = 445f
                val topH = 560f
                slots.add(LayoutSlot(RectF(70f, 220f, 70f + topW, 220f + topH), 0f))
                slots.add(LayoutSlot(RectF(565f, 220f, 565f + topW, 220f + topH), 0f))

                val botW = 295f
                val botH = 750f
                val botY = 830f
                slots.add(LayoutSlot(RectF(60f, botY, 60f + botW, botY + botH), 0f))
                slots.add(LayoutSlot(RectF(392f, botY, 392f + botW, botY + botH), 0f))
                slots.add(LayoutSlot(RectF(725f, botY, 725f + botW, botY + botH), 0f))
            }
            6 -> {
                val frameW = 420f
                val frameH = 430f
                val leftStripX = 85f
                val rightStripX = 575f

                slots.add(LayoutSlot(RectF(leftStripX, 210f, leftStripX + frameW, 210f + frameH), 0f))
                slots.add(LayoutSlot(RectF(leftStripX, 680f, leftStripX + frameW, 680f + frameH), 0f))
                slots.add(LayoutSlot(RectF(leftStripX, 1150f, leftStripX + frameW, 1150f + frameH), 0f))

                slots.add(LayoutSlot(RectF(rightStripX, 250f, rightStripX + frameW, 250f + frameH), 0f))
                slots.add(LayoutSlot(RectF(rightStripX, 720f, rightStripX + frameW, 720f + frameH), 0f))
                slots.add(LayoutSlot(RectF(rightStripX, 1190f, rightStripX + frameW, 1190f + frameH), 0f))
            }
            else -> {
                val frameW = 420f
                val frameH = 360f
                var curY = 210f
                for (i in 0 until count.coerceAtMost(8)) {
                    val isLeft = (i % 2 == 0)
                    val x = if (isLeft) 85f else 575f
                    slots.add(LayoutSlot(RectF(x, curY, x + frameW, curY + frameH), 0f))
                    if (!isLeft) curY += frameH + 30f
                }
            }
        }
        return slots
    }

    private fun getCyberSlots(count: Int): List<LayoutSlot> {
        val slots = mutableListOf<LayoutSlot>()
        when (count) {
            1 -> {
                val cardW = 880f
                val cardH = 1200f
                val left = (CANVAS_WIDTH - cardW) / 2f
                slots.add(LayoutSlot(RectF(left, 280f, left + cardW, 280f + cardH), 0f))
            }
            2 -> {
                val cardW = 460f
                val cardH = 720f
                val top = 500f
                slots.add(LayoutSlot(RectF(60f, top, 60f + cardW, top + cardH), 0f))
                slots.add(LayoutSlot(RectF(560f, top, 560f + cardW, top + cardH), 0f))
            }
            3 -> {
                // 1 Hero top wide card + 2 lower grid cards
                val topW = 960f
                val topH = 650f
                val left = (CANVAS_WIDTH - topW) / 2f
                slots.add(LayoutSlot(RectF(left, 220f, left + topW, 220f + topH), 0f))

                val botW = 465f
                val botH = 650f
                val botY = 910f
                slots.add(LayoutSlot(RectF(60f, botY, 60f + botW, botY + botH), 0f))
                slots.add(LayoutSlot(RectF(555f, botY, 555f + botW, botY + botH), 0f))
            }
            4 -> {
                // 4 People Cyber: Asymmetric Dynamic Cyber Grid (1 Large Left Feature, 2 Right Stacks, 1 Bottom Wide)
                // Or 2x2 Sleek Cyber Glass Grid with precision neon gaps
                val cardW = 465f
                val cardH = 640f
                val left1 = 60f
                val left2 = 555f
                val row1Y = 220f
                val row2Y = 890f

                slots.add(LayoutSlot(RectF(left1, row1Y, left1 + cardW, row1Y + cardH), 0f))
                slots.add(LayoutSlot(RectF(left2, row1Y, left2 + cardW, row1Y + cardH), 0f))
                slots.add(LayoutSlot(RectF(left1, row2Y, left1 + cardW, row2Y + cardH), 0f))
                slots.add(LayoutSlot(RectF(left2, row2Y, left2 + cardW, row2Y + cardH), 0f))
            }
            5 -> {
                // 5 People Cyber: Orbital / Diamond constellation
                // Center feature + 4 corner glass pods
                val centerW = 540f
                val centerH = 460f
                val centerX = (CANVAS_WIDTH - centerW) / 2f
                val centerY = 690f

                val cardW = 445f
                val cardH = 430f
                val left1 = 60f
                val left2 = 575f
                val topY = 220f
                val botY = 1190f

                slots.add(LayoutSlot(RectF(left1, topY, left1 + cardW, topY + cardH), 0f))
                slots.add(LayoutSlot(RectF(left2, topY, left2 + cardW, topY + cardH), 0f))
                slots.add(LayoutSlot(RectF(centerX, centerY, centerX + centerW, centerY + centerH), 0f))
                slots.add(LayoutSlot(RectF(left1, botY, left1 + cardW, botY + cardH), 0f))
                slots.add(LayoutSlot(RectF(left2, botY, left2 + cardW, botY + cardH), 0f))
            }
            6 -> {
                // 6 People Cyber: 2x3 Precision Glass Matrix with glowing neon borders
                val cardW = 465f
                val cardH = 435f
                val left1 = 60f
                val left2 = 555f
                val row1Y = 210f
                val row2Y = 680f
                val row3Y = 1150f

                slots.add(LayoutSlot(RectF(left1, row1Y, left1 + cardW, row1Y + cardH), 0f))
                slots.add(LayoutSlot(RectF(left2, row1Y, left2 + cardW, row1Y + cardH), 0f))
                slots.add(LayoutSlot(RectF(left1, row2Y, left1 + cardW, row2Y + cardH), 0f))
                slots.add(LayoutSlot(RectF(left2, row2Y, left2 + cardW, row2Y + cardH), 0f))
                slots.add(LayoutSlot(RectF(left1, row3Y, left1 + cardW, row3Y + cardH), 0f))
                slots.add(LayoutSlot(RectF(left2, row3Y, left2 + cardW, row3Y + cardH), 0f))
            }
            else -> {
                val cardW = 465f
                val cardH = 345f
                val left1 = 60f
                val left2 = 555f
                var curY = 210f
                for (i in 0 until count.coerceAtMost(8)) {
                    val isLeft = (i % 2 == 0)
                    val x = if (isLeft) left1 else left2
                    slots.add(LayoutSlot(RectF(x, curY, x + cardW, curY + cardH), 0f))
                    if (!isLeft) curY += cardH + 20f
                }
            }
        }
        return slots
    }
}
