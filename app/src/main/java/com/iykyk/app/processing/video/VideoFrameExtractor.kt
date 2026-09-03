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
    )

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
     * Starts with a baseline sampling interval (e.g. 250ms = 4 fps).
     */
    fun generateSamplingTimestamps(durationMs: Long, baseIntervalMs: Long = 250L): List<Long> {
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
     * Extracts a frame bitmap at a specific timestamp in milliseconds.
     */
    suspend fun getFrameAt(
        retriever: MediaMetadataRetriever,
        timestampMs: Long,
        targetWidth: Int = -1,
        targetHeight: Int = -1
    ): Bitmap? = withContext(Dispatchers.IO) {
        val timeUs = timestampMs * 1000L
        val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            ?: retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

        if (frame != null && targetWidth > 0 && targetHeight > 0) {
            if (frame.width != targetWidth || frame.height != targetHeight) {
                val scaled = Bitmap.createScaledBitmap(frame, targetWidth, targetHeight, true)
                if (scaled != frame) {
                    frame.recycle()
                }
                return@withContext scaled
            }
        }
        frame
    }
}
