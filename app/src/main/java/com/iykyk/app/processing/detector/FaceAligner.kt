package com.iykyk.app.processing.detector

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.sqrt

object FaceAligner {

    private const val TARGET_SIZE = 112f
    private const val LEFT_EYE_X = 0.35f * TARGET_SIZE
    private const val RIGHT_EYE_X = 0.65f * TARGET_SIZE
    private const val EYE_Y = 0.38f * TARGET_SIZE

    fun alignFace(
        sourceBitmap: Bitmap,
        boundingBox: RectF,
        leftEye: PointF?,
        rightEye: PointF?
    ): Bitmap? {
        if (leftEye == null || rightEye == null) {
            return null
        }

        val dx = rightEye.x - leftEye.x
        val dy = rightEye.y - leftEye.y
        val eyeDistance = sqrt(dx * dx + dy * dy)

        if (eyeDistance < 8f) {
            return null
        }

        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val targetEyeDistance = RIGHT_EYE_X - LEFT_EYE_X
        val scale = targetEyeDistance / eyeDistance

        val sourceEyeCenterX = (leftEye.x + rightEye.x) / 2f
        val sourceEyeCenterY = (leftEye.y + rightEye.y) / 2f
        val targetEyeCenterX = (LEFT_EYE_X + RIGHT_EYE_X) / 2f

        val matrix = Matrix().apply {
            postTranslate(-sourceEyeCenterX, -sourceEyeCenterY)
            postRotate(-angle)
            postScale(scale, scale)
            postTranslate(targetEyeCenterX, EYE_Y)
        }

        val output = Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(
            sourceBitmap,
            matrix,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        return output
    }
}
