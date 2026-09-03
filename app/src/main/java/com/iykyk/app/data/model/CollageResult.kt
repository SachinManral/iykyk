package com.iykyk.app.data.model

import android.graphics.Bitmap
import android.net.Uri

data class CollageResult(
    val videoUri: Uri,
    val videoTitle: String,
    val videoDurationMs: Long,
    val people: List<PersonIdentity>,
    val totalAppearances: Int,
    val collageBitmap: Bitmap,
    val creationTimestampMs: Long = System.currentTimeMillis()
)
