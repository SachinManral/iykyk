package com.iykyk.app.data.model

import android.graphics.Bitmap
import android.net.Uri

import com.iykyk.app.graphics.CollageTheme

data class CollageResult(
    val videoUri: Uri,
    val videoTitle: String,
    val videoDurationMs: Long,
    val people: List<PersonIdentity>,
    val totalAppearances: Int,
    val collageBitmap: Bitmap,
    val selectedTheme: CollageTheme = CollageTheme.FLORAL_SCRAPBOOK,
    val creationTimestampMs: Long = System.currentTimeMillis()
)
