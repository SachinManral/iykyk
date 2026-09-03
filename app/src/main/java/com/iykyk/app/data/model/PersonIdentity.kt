package com.iykyk.app.data.model

import android.graphics.Bitmap

/**
 * Unique person identified across the entire video.
 */
data class PersonIdentity(
    val id: Int,
    val label: String, // e.g., "Person A", "Person B"
    val appearances: List<AppearanceSegment>,
    val representativeShotTimestampMs: Long,
    val representativeQualityScore: Float,
    var representativePortraitBitmap: Bitmap? = null,
    var avatarThumbnailBitmap: Bitmap? = null
) {
    val appearanceCount: Int
        get() = appearances.size
}
