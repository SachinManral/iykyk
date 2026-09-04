package com.iykyk.app.processing.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts frames and metadata from a video source.
 */
class VideoFrameExtractor(private val context: Context) {

    data class VideoMetadata(
        val durationMs: Long,
        val width: Int,
        val height: Int,
        val rotation: Int
    ) {
        val displayWidth: Int
            get() = if (rotation == 90 || rotation == 270) height else width

        val displayHeight: Int
            get() = if (rotation == 90 || rotation == 270) width else height
    }

    /**
     * Reads essential video metadata.
     */
    suspend fun extractMetadata(videoUri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)

            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val width = widthStr?.toIntOrNull() ?: 1080
            val height = heightStr?.toIntOrNull() ?: 1920
            val rotation = rotationStr?.toIntOrNull() ?: 0

            VideoMetadata(durationMs, width, height, rotation)
        } finally {
            retriever.release()
        }
    }

    /**
     * Generates a list of target timestamps in milliseconds based on adaptive sampling.
     */
    fun generateSamplingTimestamps(durationMs: Long, baseIntervalMs: Long = 333L): List<Long> {
        val timestamps = mutableListOf<Long>()
        var current = 0L
        while (current < durationMs) {
            timestamps.add(current)
            current += baseIntervalMs
        }
        if (timestamps.isEmpty() || timestamps.last() < durationMs) {
            timestamps.add(durationMs)
        }
        return timestamps.distinct().sorted()
    }

    /**
     * Extracts a frame bitmap at a specific timestamp in milliseconds,
     * ensuring proper orientation and proportional scaling.
     */
    suspend fun getFrameAt(
        retriever: MediaMetadataRetriever,
        timestampMs: Long,
        maxDimension: Int = -1,
        rotation: Int = 0
    ): Bitmap? = withContext(Dispatchers.IO) {
        val timeUs = timestampMs * 1000L
        var frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: return@withContext null

        // If video container has rotation metadata (e.g. 90/270 for phone portrait videos)
        // check if retriever returned raw unrotated sensor buffer and rotate if needed
        if (rotation != 0) {
            val needsRotate = when (rotation) {
                90, 270 -> {
                    // If video was recorded portrait (rotation 90/270) but frame is wider than tall, it's raw landscape
                    frame.width > frame.height
                }
                180 -> true
                else -> false
            }

            if (needsRotate) {
                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(frame, 0, 0, frame.width, frame.height, matrix, true)
                if (rotated != frame) {
                    frame.recycle()
                    frame = rotated
                }
            }
        }

        // Maintain exact aspect ratio when scaling down for inference
        if (maxDimension > 0 && (frame.width > maxDimension || frame.height > maxDimension)) {
            val scale = maxDimension.toFloat() / maxOf(frame.width, frame.height).toFloat()
            val targetW = (frame.width * scale).toInt().coerceAtLeast(1)
            val targetH = (frame.height * scale).toInt().coerceAtLeast(1)
            val scaled = Bitmap.createScaledBitmap(frame, targetW, targetH, true)
            if (scaled != frame) {
                frame.recycle()
                frame = scaled
            }
        }
        frame
    }
}
