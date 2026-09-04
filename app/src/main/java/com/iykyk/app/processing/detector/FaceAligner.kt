package com.iykyk.app.processing.detector

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF

/**
 * Aligns face crops to canonical 112x112 space using a 2D similarity transform (Umeyama least-squares).
 * Standard canonical reference 5 landmarks for ArcFace / InsightFace:
 * - Left eye: (38.2946, 51.6963)
 * - Right eye: (73.5318, 51.5014)
 * - Nose base: (56.0252, 71.7366)
 * - Left mouth corner: (41.5493, 92.3655)
 * - Right mouth corner: (70.7266, 92.2041)
 */
object FaceAligner {

    const val TARGET_SIZE = 112

    private val CANONICAL_5_POINTS = arrayOf(
        PointF(38.2946f, 51.6963f),  // Left eye
        PointF(73.5318f, 51.5014f),  // Right eye
        PointF(56.0252f, 71.7366f),  // Nose base
        PointF(41.5493f, 92.3655f),  // Left mouth corner
        PointF(70.7266f, 92.2041f)   // Right mouth corner
    )

    fun alignFace5Points(
        sourceBitmap: Bitmap,
        boundingBox: RectF,
        leftEye: PointF?,
        rightEye: PointF?,
        noseBase: PointF? = null,
        leftMouth: PointF? = null,
        rightMouth: PointF? = null
    ): Bitmap? {
        val srcPoints = mutableListOf<PointF>()
        val dstPoints = mutableListOf<PointF>()

        if (leftEye != null && rightEye != null) {
            srcPoints.add(leftEye)
            dstPoints.add(CANONICAL_5_POINTS[0])

            srcPoints.add(rightEye)
            dstPoints.add(CANONICAL_5_POINTS[1])

            if (noseBase != null) {
                srcPoints.add(noseBase)
                dstPoints.add(CANONICAL_5_POINTS[2])
            }
            if (leftMouth != null) {
                srcPoints.add(leftMouth)
                dstPoints.add(CANONICAL_5_POINTS[3])
            }
            if (rightMouth != null) {
                srcPoints.add(rightMouth)
                dstPoints.add(CANONICAL_5_POINTS[4])
            }
        }

        val matrix = if (srcPoints.size >= 2) {
            computeSimilarityTransformMatrix(srcPoints, dstPoints)
        } else {
            computeFallbackMatrix(boundingBox)
        } ?: return null

        val output = Bitmap.createBitmap(TARGET_SIZE, TARGET_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(sourceBitmap, matrix, paint)
        return output
    }

    /**
     * Backward-compatible overload for 2 eye landmarks.
     */
    fun alignFace(
        sourceBitmap: Bitmap,
        boundingBox: RectF,
        leftEye: PointF?,
        rightEye: PointF?
    ): Bitmap? {
        return alignFace5Points(
            sourceBitmap = sourceBitmap,
            boundingBox = boundingBox,
            leftEye = leftEye,
            rightEye = rightEye
        )
    }

    /**
     * Computes the 2D similarity transformation values [MSCALE_X, MSKEW_X, MTRANS_X, MSKEW_Y, MSCALE_Y, MTRANS_Y, 0, 0, 1]
     * (Umeyama algorithm) from pure coordinate arrays.
     */
    fun computeSimilarityTransform(
        srcX: FloatArray,
        srcY: FloatArray,
        dstX: FloatArray,
        dstY: FloatArray
    ): FloatArray? {
        val n = srcX.size
        if (n < 2 || n != srcY.size || n != dstX.size || n != dstY.size) return null

        var srcMeanX = 0f
        var srcMeanY = 0f
        var dstMeanX = 0f
        var dstMeanY = 0f

        for (i in 0 until n) {
            srcMeanX += srcX[i]
            srcMeanY += srcY[i]
            dstMeanX += dstX[i]
            dstMeanY += dstY[i]
        }

        srcMeanX /= n
        srcMeanY /= n
        dstMeanX /= n
        dstMeanY /= n

        var varSrc = 0f
        var sxx = 0f
        var syy = 0f
        var sxy = 0f
        var syx = 0f

        for (i in 0 until n) {
            val sx = srcX[i] - srcMeanX
            val sy = srcY[i] - srcMeanY
            val dx = dstX[i] - dstMeanX
            val dy = dstY[i] - dstMeanY

            varSrc += sx * sx + sy * sy
            sxx += dx * sx
            syy += dy * sy
            sxy += dx * sy
            syx += dy * sx
        }

        varSrc /= n
        if (varSrc < 1e-6f) return null

        sxx /= n
        syy /= n
        sxy /= n
        syx /= n

        val a = sxx + syy
        val b = sxy - syx

        val scaleCos = a / varSrc
        val scaleSin = b / varSrc

        val tx = dstMeanX - (scaleCos * srcMeanX - scaleSin * srcMeanY)
        val ty = dstMeanY - (scaleSin * srcMeanX + scaleCos * srcMeanY)

        return floatArrayOf(
            scaleCos, -scaleSin, tx,
            scaleSin, scaleCos, ty,
            0f, 0f, 1f
        )
    }

    /**
     * Computes the 2D similarity transformation values from PointF lists.
     */
    fun computeSimilarityTransformValues(
        src: List<PointF>,
        dst: List<PointF>
    ): FloatArray? {
        if (src.size < 2 || src.size != dst.size) return null
        val srcX = FloatArray(src.size) { src[it].x }
        val srcY = FloatArray(src.size) { src[it].y }
        val dstX = FloatArray(dst.size) { dst[it].x }
        val dstY = FloatArray(dst.size) { dst[it].y }
        return computeSimilarityTransform(srcX, srcY, dstX, dstY)
    }

    /**
     * Computes the 2D similarity transformation Matrix (Umeyama algorithm).
     */
    fun computeSimilarityTransformMatrix(
        src: List<PointF>,
        dst: List<PointF>
    ): Matrix? {
        val values = computeSimilarityTransformValues(src, dst) ?: return null
        val matrix = Matrix()
        matrix.setValues(values)
        return matrix
    }

    private fun computeFallbackMatrix(box: RectF): Matrix? {
        val w = box.right - box.left
        val h = box.bottom - box.top
        if (w <= 0f || h <= 0f) return null
        val centerX = (box.left + box.right) / 2f
        val centerY = (box.top + box.bottom) / 2f
        val maxDim = maxOf(w, h) * 1.3f
        val scale = TARGET_SIZE / maxDim

        return Matrix().apply {
            postTranslate(-centerX, -centerY)
            postScale(scale, scale)
            postTranslate(TARGET_SIZE / 2f, TARGET_SIZE / 2f)
        }
    }
}
