package com.iykyk.app.data.model

import android.graphics.Bitmap

/**
 * Represents a single continuous appearance of a person in the video.
 */
data class AppearanceSegment(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val totalDetections: Int,
    val bestFrameTimestampMs: Long,
    var thumbnailBitmap: Bitmap? = null
) {
    val durationMs: Long
        get() = (endTimeMs - startTimeMs).coerceAtLeast(0L)
}
