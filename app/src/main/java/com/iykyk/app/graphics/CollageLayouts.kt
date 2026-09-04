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
     * Computes the layout slot coordinates for a given theme, person count, and layout variation index.
     */
    fun getSlots(theme: CollageTheme, personCount: Int, variation: Int = 0): List<LayoutSlot> {
        val count = personCount.coerceAtLeast(1)
        return when (theme) {
            CollageTheme.FLORAL_SCRAPBOOK -> getFloralSlots(count, variation)
            CollageTheme.VINTAGE_FILM -> getVintageSlots(count, variation)
            CollageTheme.CYBER_GLOW -> getCyberSlots(count, variation)
        }
    }

    private fun getFloralSlots(count: Int, variation: Int): List<LayoutSlot> {
        val slots = mutableListOf<LayoutSlot>()
        when (count) {
            1 -> {
                val cardW = 860f
                val cardH = 1180f
                val left = (CANVAS_WIDTH - cardW) / 2f
                val top = 280f
                val rot = if (variation % 2 == 0) -1.5f else 1.5f
                slots.add(LayoutSlot(RectF(left, top, left + cardW, top + cardH), rot, 90f))
            }
            2 -> {
                val cardW = 620f
                val cardH = 680f
                if (variation % 2 == 0) {
                    slots.add(LayoutSlot(RectF(90f, 260f, 90f + cardW, 260f + cardH), -3.5f, 60f))
                    slots.add(LayoutSlot(RectF(370f, 880f, 370f + cardW, 880f + cardH), 3.0f, 60f))
                } else {
                    slots.add(LayoutSlot(RectF(370f, 260f, 370f + cardW, 260f + cardH), 3.0f, 60f))
                    slots.add(LayoutSlot(RectF(90f, 880f, 90f + cardW, 880f + cardH), -3.5f, 60f))
                }
            }
            3 -> {
                when (variation % 3) {
                    0 -> {
                        // 1 Top Hero, 2 Bottom
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
                    1 -> {
                        // 2 Top, 1 Bottom Hero
                        val topW = 450f
                        val topH = 580f
                        val topY = 220f
                        slots.add(LayoutSlot(RectF(65f, topY, 65f + topW, topY + topH), -2.5f, 50f))
                        slots.add(LayoutSlot(RectF(565f, topY + 15f, 565f + topW, topY + 15f + topH), 3.0f, 50f))

                        val botW = 640f
                        val botH = 660f
                        val botLeft = (CANVAS_WIDTH - botW) / 2f
                        slots.add(LayoutSlot(RectF(botLeft, 860f, botLeft + botW, 860f + botH), -1.5f, 60f))
                    }
                    else -> {
                        // Diagonal Cascade
                        val cardW = 520f
                        val cardH = 540f
                        slots.add(LayoutSlot(RectF(70f, 200f, 70f + cardW, 200f + cardH), -3.0f, 50f))
                        slots.add(LayoutSlot(RectF(490f, 620f, 490f + cardW, 620f + cardH), 2.5f, 50f))
                        slots.add(LayoutSlot(RectF(100f, 1060f, 100f + cardW, 1060f + cardH), -2.0f, 50f))
                    }
                }
            }
            4 -> {
                when (variation % 3) {
                    0 -> {
                        // 2x2 Classic Grid with soft tilts
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
                    1 -> {
                        // 1 Large Left Feature + 2 Right Stacked + 1 Bottom Wide
                        val leftW = 450f
                        val leftH = 680f
                        slots.add(LayoutSlot(RectF(60f, 220f, 60f + leftW, 220f + leftH), -2.0f, 50f))

                        val rightW = 450f
                        val rightH = 340f
                        slots.add(LayoutSlot(RectF(570f, 220f, 570f + rightW, 220f + rightH), 2.5f, 40f))
                        slots.add(LayoutSlot(RectF(570f, 590f, 570f + rightW, 590f + rightH), -2.0f, 40f))

                        val botW = 860f
                        val botH = 520f
                        val botLeft = (CANVAS_WIDTH - botW) / 2f
                        slots.add(LayoutSlot(RectF(botLeft, 970f, botLeft + botW, 970f + botH), 1.0f, 50f))
                    }
                    else -> {
                        // Staggered Zigzag
                        val cardW = 470f
                        val cardH = 490f
                        slots.add(LayoutSlot(RectF(60f, 200f, 60f + cardW, 200f + cardH), -3.0f, 45f))
                        slots.add(LayoutSlot(RectF(550f, 380f, 550f + cardW, 380f + cardH), 2.5f, 45f))
                        slots.add(LayoutSlot(RectF(60f, 790f, 60f + cardW, 790f + cardH), 2.0f, 45f))
                        slots.add(LayoutSlot(RectF(550f, 980f, 550f + cardW, 980f + cardH), -2.5f, 45f))
                    }
                }
            }
            5 -> {
                when (variation % 3) {
                    0 -> {
                        // Classic Scrapbook X-Layout: 2 top, 1 center hero, 2 bottom
                        val cardW = 450f
                        val cardH = 510f
                        val topY = 190f
                        val botY = 1140f

                        slots.add(LayoutSlot(RectF(60f, topY, 60f + cardW, topY + cardH), -3.5f, 40f))
                        slots.add(LayoutSlot(RectF(570f, topY + 15f, 570f + cardW, topY + 15f + cardH), 3.0f, 40f))

                        val centerW = 530f
                        val centerH = 510f
                        val centerX = (CANVAS_WIDTH - centerW) / 2f
                        val centerY = 650f
                        slots.add(LayoutSlot(RectF(centerX, centerY, centerX + centerW, centerY + centerH), 0.5f, 40f))

                        slots.add(LayoutSlot(RectF(60f, botY, 60f + cardW, botY + cardH), 2.5f, 40f))
                        slots.add(LayoutSlot(RectF(560f, botY + 15f, 560f + cardW, botY + 15f + cardH), -2.5f, 40f))
                    }
                    1 -> {
                        // Top Hero Showcase: 1 Large Top Hero + 2 Middle + 2 Bottom
                        val heroW = 620f
                        val heroH = 560f
                        val heroLeft = (CANVAS_WIDTH - heroW) / 2f
                        slots.add(LayoutSlot(RectF(heroLeft, 190f, heroLeft + heroW, 190f + heroH), -1.0f, 45f))

                        val cardW = 440f
                        val cardH = 460f
                        val midY = 740f
                        val botY = 1190f
                        slots.add(LayoutSlot(RectF(65f, midY, 65f + cardW, midY + cardH), -2.5f, 40f))
                        slots.add(LayoutSlot(RectF(575f, midY + 10f, 575f + cardW, midY + 10f + cardH), 2.5f, 40f))

                        slots.add(LayoutSlot(RectF(65f, botY, 65f + cardW, botY + cardH), 2.0f, 40f))
                        slots.add(LayoutSlot(RectF(575f, botY + 10f, 575f + cardW, botY + 10f + cardH), -2.0f, 40f))
                    }
                    else -> {
                        // Editorial Pinboard: 1 Tall Left + 2 Stacked Right + 2 Bottom
                        val leftW = 440f
                        val leftH = 640f
                        slots.add(LayoutSlot(RectF(60f, 200f, 60f + leftW, 200f + leftH), -2.5f, 45f))

                        val rightW = 440f
                        val rightH = 340f
                        slots.add(LayoutSlot(RectF(580f, 200f, 580f + rightW, 200f + rightH), 2.0f, 35f))
                        slots.add(LayoutSlot(RectF(580f, 560f, 580f + rightW, 560f + rightH), -2.0f, 35f))

                        val botW = 450f
                        val botH = 500f
                        val botY = 980f
                        slots.add(LayoutSlot(RectF(60f, botY, 60f + botW, botY + botH), 2.0f, 40f))
                        slots.add(LayoutSlot(RectF(570f, botY + 10f, 570f + botW, botY + 10f + botH), -2.5f, 40f))
                    }
                }
            }
            6 -> {
                when (variation % 2) {
                    0 -> {
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
                        // 1 Hero top + 2 mid + 3 bottom columns
                        val heroW = 580f
                        val heroH = 480f
                        val heroLeft = (CANVAS_WIDTH - heroW) / 2f
                        slots.add(LayoutSlot(RectF(heroLeft, 200f, heroLeft + heroW, 200f + heroH), 0f, 40f))

                        val midW = 435f
                        val midH = 430f
                        slots.add(LayoutSlot(RectF(65f, 690f, 65f + midW, 690f + midH), -2.5f, 35f))
                        slots.add(LayoutSlot(RectF(580f, 690f, 580f + midW, 690f + midH), 2.5f, 35f))

                        val botW = 285f
                        val botH = 400f
                        val botY = 1170f
                        slots.add(LayoutSlot(RectF(60f, botY, 60f + botW, botY + botH), -2.0f, 30f))
                        slots.add(LayoutSlot(RectF(397f, botY, 397f + botW, botY + botH), 0f, 30f))
                        slots.add(LayoutSlot(RectF(735f, botY, 735f + botW, botY + botH), 2.0f, 30f))
                    }
                }
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

    private fun getVintageSlots(count: Int, variation: Int): List<LayoutSlot> {
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
                when (variation % 2) {
                    0 -> {
                        // Contact sheet: 2 Top + 3 Bottom
                        val topW = 445f
                        val topH = 560f
                        slots.add(LayoutSlot(RectF(70f, 220f, 70f + topW, 220f + topH), 0f))
                        slots.add(LayoutSlot(RectF(565f, 220f, 565f + topW, 220f + topH), 0f))

                        val botW = 295f
                        val botH = 680f
                        val botY = 880f
                        slots.add(LayoutSlot(RectF(60f, botY, 60f + botW, botY + botH), 0f))
                        slots.add(LayoutSlot(RectF(392f, botY, 392f + botW, botY + botH), 0f))
                        slots.add(LayoutSlot(RectF(725f, botY, 725f + botW, botY + botH), 0f))
                    }
                    else -> {
                        // Hero Center 35mm Slide Mount + 4 Corner Slides
                        val heroW = 540f
                        val heroH = 540f
                        val heroLeft = (CANVAS_WIDTH - heroW) / 2f
                        val heroTop = 640f

                        val slideW = 420f
                        val slideH = 430f
                        slots.add(LayoutSlot(RectF(70f, 200f, 70f + slideW, 200f + slideH), 0f))
                        slots.add(LayoutSlot(RectF(590f, 200f, 590f + slideW, 200f + slideH), 0f))
                        slots.add(LayoutSlot(RectF(heroLeft, heroTop, heroLeft + heroW, heroTop + heroH), 0f))
                        slots.add(LayoutSlot(RectF(70f, 1180f, 70f + slideW, 1180f + slideH), 0f))
                        slots.add(LayoutSlot(RectF(590f, 1180f, 590f + slideW, 1180f + slideH), 0f))
                    }
                }
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

    private fun getCyberSlots(count: Int, variation: Int): List<LayoutSlot> {
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
                when (variation % 2) {
                    0 -> {
                        // Cyber HUD: 2 top, 1 center hero card, 2 bottom
                        val topW = 460f
                        val topH = 490f
                        val topY = 220f
                        slots.add(LayoutSlot(RectF(60f, topY, 60f + topW, topY + topH), 0f))
                        slots.add(LayoutSlot(RectF(560f, topY, 560f + topW, topY + topH), 0f))

                        val centerW = 540f
                        val centerH = 490f
                        val centerX = (CANVAS_WIDTH - centerW) / 2f
                        val centerY = 710f
                        slots.add(LayoutSlot(RectF(centerX, centerY, centerX + centerW, centerY + centerH), 0f))

                        val botY = 1200f
                        slots.add(LayoutSlot(RectF(60f, botY, 60f + topW, botY + topH), 0f))
                        slots.add(LayoutSlot(RectF(560f, botY, 560f + topW, botY + topH), 0f))
                    }
                    else -> {
                        // Cyber Grid: 1 Large Left Feature, 2 Right Stacks, 2 Bottom
                        val leftW = 460f
                        val leftH = 660f
                        slots.add(LayoutSlot(RectF(60f, 220f, 60f + leftW, 220f + leftH), 0f))

                        val rightW = 460f
                        val rightH = 330f
                        slots.add(LayoutSlot(RectF(560f, 220f, 560f + rightW, 220f + rightH), 0f))
                        slots.add(LayoutSlot(RectF(560f, 570f, 560f + rightW, 570f + rightH), 0f))

                        val botW = 460f
                        val botH = 490f
                        val botY = 990f
                        slots.add(LayoutSlot(RectF(60f, botY, 60f + botW, botY + botH), 0f))
                        slots.add(LayoutSlot(RectF(560f, botY, 560f + botW, botY + botH), 0f))
                    }
                }
            }
            6 -> {
                val cardW = 465f
                val cardH = 430f
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
                val cardW = 460f
                val cardH = 340f
                var curY = 210f
                for (i in 0 until count.coerceAtMost(8)) {
                    val isLeft = (i % 2 == 0)
                    val x = if (isLeft) 60f else 560f
                    slots.add(LayoutSlot(RectF(x, curY, x + cardW, curY + cardH), 0f))
                    if (!isLeft) curY += cardH + 20f
                }
            }
        }
        return slots
    }
}
