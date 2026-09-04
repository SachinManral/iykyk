package com.iykyk.app.processing.clustering

import android.graphics.RectF
import com.iykyk.app.data.model.AppearanceSegment
import com.iykyk.app.data.model.DetectedFaceInfo
import kotlin.math.hypot

/**
 * Tracks consecutive frame-to-frame face detections into continuous appearance segments
 * and extracts top 3-5 representative keyframes per segment for global clustering.
 */
class AppearanceSegmenter(
    val maxContinuityGapMs: Long = 800L,
    val minSegmentDurationMs: Long = 0L,
    val minDetectionsPerSegment: Int = 1
) {

    data class SegmentTrack(
        val id: Int,
        val detections: MutableList<DetectedFaceInfo> = mutableListOf(),
        var representativeKeyframes: List<DetectedFaceInfo> = emptyList(),
        var appearanceSegment: AppearanceSegment? = null
    )

    /**
     * Builds continuous appearance tracks across all detected video faces using
     * spatial frame-to-frame continuity and ML Kit tracking hints.
     */
    fun buildAppearanceTracks(allDetections: List<DetectedFaceInfo>): List<SegmentTrack> {
        val valid = allDetections.filter { isUsableDetection(it) }
        if (valid.isEmpty()) return emptyList()

        // Group by frame timestamp to process frame-by-frame
        val frameGroups = valid.groupBy { it.frameTimestampMs }.toSortedMap()

        val activeTracks = mutableListOf<SegmentTrack>()
        val finishedTracks = mutableListOf<SegmentTrack>()
        var nextTrackId = 1

        for ((timestampMs, frameFaces) in frameGroups) {
            // Check for stale active tracks that exceeded continuity gap
            val iterator = activeTracks.iterator()
            while (iterator.hasNext()) {
                val track = iterator.next()
                val lastTimestamp = track.detections.last().frameTimestampMs
                if (timestampMs - lastTimestamp > maxContinuityGapMs) {
                    finishedTracks.add(track)
                    iterator.remove()
                }
            }

            val unmatchedFaces = frameFaces.toMutableList()
            val matchedTrackIds = mutableSetOf<Int>()

            // Match faces to active tracks using spatial proximity AND embedding consistency
            for (face in unmatchedFaces.toList()) {
                var bestTrack: SegmentTrack? = null
                var bestScore = Float.MAX_VALUE

                for (track in activeTracks) {
                    if (matchedTrackIds.contains(track.id)) continue
                    val lastFace = track.detections.last()
                    val gap = face.frameTimestampMs - lastFace.frameTimestampMs
                    if (gap > maxContinuityGapMs) continue

                    val spatialDist = computeBoundingBoxDistance(lastFace.boundingBox, face.boundingBox)

                    // If both detections have ArcFace embeddings, ensure they belong to the same person
                    val embDist = if (lastFace.embedding != null && face.embedding != null &&
                        lastFace.embedding.isNotEmpty() && face.embedding.isNotEmpty()) {
                        com.iykyk.app.processing.embedder.FaceEmbedder.cosineDistance(lastFace.embedding, face.embedding)
                    } else {
                        0f
                    }

                    // Must be a plausible spatial match OR strong embedding match
                    val isSpatialMatch = spatialDist <= 0.60f
                    val isEmbeddingMatch = embDist <= 0.38f

                    if (!isSpatialMatch && !isEmbeddingMatch) continue
                    if (embDist > 0.46f) continue // definitely different person

                    val combinedScore = spatialDist * 0.5f + embDist * 0.5f
                    if (combinedScore < bestScore) {
                        bestScore = combinedScore
                        bestTrack = track
                    }
                }

                if (bestTrack != null) {
                    bestTrack.detections.add(face)
                    matchedTrackIds.add(bestTrack.id)
                    unmatchedFaces.remove(face)
                }
            }

            // Remaining unmatched faces start new appearance tracks
            for (face in unmatchedFaces) {
                val newTrack = SegmentTrack(id = nextTrackId++)
                newTrack.detections.add(face)
                activeTracks.add(newTrack)
            }
        }

        finishedTracks.addAll(activeTracks)

        // Stitch near-contiguous segments from momentary tracking gaps (<= 800ms)
        val consolidated = mutableListOf<SegmentTrack>()
        val sortedTracks = finishedTracks.filter { it.detections.isNotEmpty() }
            .sortedBy { it.detections.first().frameTimestampMs }

        for (track in sortedTracks) {
            val candidate = consolidated.firstOrNull { existing ->
                val lastDet = existing.detections.last()
                val firstDet = track.detections.first()
                val gap = firstDet.frameTimestampMs - lastDet.frameTimestampMs
                if (gap in 1L..maxContinuityGapMs) {
                    val spatialDist = computeBoundingBoxDistance(lastDet.boundingBox, firstDet.boundingBox)
                    val embDist = if (lastDet.embedding != null && firstDet.embedding != null &&
                        lastDet.embedding.isNotEmpty() && firstDet.embedding.isNotEmpty()) {
                        com.iykyk.app.processing.embedder.FaceEmbedder.cosineDistance(lastDet.embedding, firstDet.embedding)
                    } else {
                        0f
                    }
                    spatialDist <= 0.42f && embDist <= 0.38f
                } else {
                    false
                }
            }

            if (candidate != null) {
                candidate.detections.addAll(track.detections)
                candidate.detections.sortBy { it.frameTimestampMs }
            } else {
                consolidated.add(track)
            }
        }

        // Post-process each track: filter tiny flickers, select 3-5 best keyframes, build metadata
        val finalTracks = mutableListOf<SegmentTrack>()
        for (track in consolidated) {
            if (track.detections.size < minDetectionsPerSegment) continue

            val sortedByQuality = track.detections.sortedByDescending { it.qualityScore }
            val keyframeLimit = minOf(5, maxOf(1, sortedByQuality.size))
            val topKeyframes = sortedByQuality.take(keyframeLimit)

            track.representativeKeyframes = topKeyframes
            val segment = buildSegment(track.detections)
            if (segment != null) {
                track.appearanceSegment = segment
                finalTracks.add(track)
            }
        }

        return finalTracks
    }

    /**
     * Backward-compatible helper to segment appearances for a single person's detections.
     */
    fun segmentAppearances(personDetections: List<DetectedFaceInfo>): List<AppearanceSegment> {
        val tracks = buildAppearanceTracks(personDetections)
        return tracks.mapNotNull { it.appearanceSegment }
    }

    private fun buildSegment(detections: List<DetectedFaceInfo>): AppearanceSegment? {
        if (detections.isEmpty()) return null
        if (detections.size < minDetectionsPerSegment) return null

        val sorted = detections.sortedBy { it.frameTimestampMs }
        val start = sorted.first().frameTimestampMs
        val end = sorted.last().frameTimestampMs
        val best = sorted.maxByOrNull { it.qualityScore } ?: sorted.first()

        return AppearanceSegment(
            startTimeMs = start,
            endTimeMs = end,
            totalDetections = sorted.size,
            bestFrameTimestampMs = best.frameTimestampMs
        )
    }

    private fun computeBoundingBoxDistance(b1: RectF, b2: RectF): Float {
        val w1 = (b1.right - b1.left).coerceAtLeast(0f)
        val h1 = (b1.bottom - b1.top).coerceAtLeast(0f)
        val w2 = (b2.right - b2.left).coerceAtLeast(0f)
        val h2 = (b2.bottom - b2.top).coerceAtLeast(0f)

        val cx1 = (b1.left + b1.right) / 2f
        val cy1 = (b1.top + b1.bottom) / 2f
        val cx2 = (b2.left + b2.right) / 2f
        val cy2 = (b2.top + b2.bottom) / 2f

        // Normalized center distance relative to average dimension
        val avgDim = ((w1 + h1 + w2 + h2) / 4f).coerceAtLeast(1f)
        val centerDist = hypot((cx1 - cx2).toDouble(), (cy1 - cy2).toDouble()).toFloat()
        val normalizedDist = centerDist / avgDim

        // Intersection over Union
        val intersectionLeft = maxOf(b1.left, b2.left)
        val intersectionTop = maxOf(b1.top, b2.top)
        val intersectionRight = minOf(b1.right, b2.right)
        val intersectionBottom = minOf(b1.bottom, b2.bottom)

        val intersectionArea = if (intersectionRight > intersectionLeft && intersectionBottom > intersectionTop) {
            (intersectionRight - intersectionLeft) * (intersectionBottom - intersectionTop)
        } else {
            0f
        }

        val area1 = w1 * h1
        val area2 = w2 * h2
        val unionArea = (area1 + area2 - intersectionArea).coerceAtLeast(1f)
        val iou = (intersectionArea / unionArea).coerceIn(0f, 1f)

        // Combined metric: low distance is good
        return (normalizedDist * 0.6f + (1f - iou) * 0.4f).coerceAtLeast(0f)
    }

    private fun isUsableDetection(detection: DetectedFaceInfo): Boolean {
        val box = detection.boundingBox
        val width = (box.right - box.left).coerceAtLeast(0f)
        val height = (box.bottom - box.top).coerceAtLeast(0f)

        if ((box.right != 0f || box.bottom != 0f) && (width < 35f || height < 35f)) {
            return false
        }

        if (detection.sharpnessScore > 0f && detection.sharpnessScore < 8f) {
            return false
        }

        return true
    }
}
