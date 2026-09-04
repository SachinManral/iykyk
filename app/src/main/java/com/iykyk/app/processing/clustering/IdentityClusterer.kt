package com.iykyk.app.processing.clustering

import com.iykyk.app.data.model.AppearanceSegment
import com.iykyk.app.data.model.DetectedFaceInfo
import com.iykyk.app.processing.embedder.FaceEmbedder
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Global Agglomerative (Hierarchical) Clustering with Average Linkage
 * and Co-Occurrence Conflict Constraints for ArcFace 512-D embeddings.
 */
class IdentityClusterer(
    private val distanceThreshold: Float = 0.64f
) {

    data class PersonCluster(
        val clusterId: Int,
        val segments: List<AppearanceSegment>,
        val allDetections: List<DetectedFaceInfo>,
        val representativeDetection: DetectedFaceInfo
    )

    data class TrackClusterNode(
        val id: Int,
        val tracks: MutableList<AppearanceSegmenter.SegmentTrack> = mutableListOf(),
        var centroidEmbedding: FloatArray = FloatArray(0)
    )

    /**
     * Clusters appearance segment tracks into unique person identities.
     */
    fun clusterSegmentTracks(tracks: List<AppearanceSegmenter.SegmentTrack>): List<PersonCluster> {
        val validTracks = tracks.filter { track ->
            track.representativeKeyframes.any { it.embedding != null && it.embedding.isNotEmpty() }
        }

        if (validTracks.isEmpty()) return emptyList()

        val nodes = validTracks.mapIndexed { index, track ->
            val embeddings = track.representativeKeyframes.mapNotNull { it.embedding }
            val centroid = computeCentroid(embeddings)
            TrackClusterNode(
                id = index + 1,
                tracks = mutableListOf(track),
                centroidEmbedding = centroid
            )
        }.toMutableList()

        // Global Agglomerative Clustering
        while (nodes.size > 1) {
            var bestDistance = Float.MAX_VALUE
            var bestI = -1
            var bestJ = -1

            for (i in 0 until nodes.size) {
                for (j in i + 1 until nodes.size) {
                    if (hasCoOccurrenceConflict(nodes[i], nodes[j])) {
                        continue
                    }

                    val dist = computeClusterDistance(nodes[i], nodes[j])
                    if (dist < bestDistance) {
                        bestDistance = dist
                        bestI = i
                        bestJ = j
                    }
                }
            }

            if (bestI < 0 || bestJ < 0 || bestDistance > distanceThreshold) {
                break
            }

            // Merge bestJ into bestI
            nodes[bestI].tracks.addAll(nodes[bestJ].tracks)
            val allEmbeddings = nodes[bestI].tracks.flatMap { t ->
                t.representativeKeyframes.mapNotNull { it.embedding }
            }
            nodes[bestI].centroidEmbedding = computeCentroid(allEmbeddings)
            nodes.removeAt(bestJ)
        }

        // Convert merged nodes into PersonClusters, filtering out isolated noise clusters if excessive clusters exist
        val filteredNodes = if (nodes.size > 8) {
            val maxDetectionsInAnyCluster = nodes.maxOfOrNull { n -> n.tracks.sumOf { it.detections.size } } ?: 0
            val minDetectionsThreshold = if (maxDetectionsInAnyCluster >= 6) 2 else 1
            nodes.filter { n -> n.tracks.sumOf { it.detections.size } >= minDetectionsThreshold }.ifEmpty { nodes }
        } else {
            nodes
        }

        return filteredNodes.mapIndexed { index, node ->
            val allDetections = node.tracks.flatMap { it.detections }.sortedBy { it.frameTimestampMs }
            val segments = node.tracks.mapNotNull { it.appearanceSegment }.sortedBy { it.startTimeMs }
            val soloDetections = allDetections.filter { it.isSoloFrame }
            val candidatePool = if (soloDetections.isNotEmpty()) soloDetections else allDetections
            val bestDetection = candidatePool.maxByOrNull { it.qualityScore } ?: allDetections.first()

            PersonCluster(
                clusterId = index + 1,
                segments = segments,
                allDetections = allDetections,
                representativeDetection = bestDetection
            )
        }.sortedByDescending { it.allDetections.size }
    }

    /**
     * Backward-compatible clustering method for raw detections list.
     */
    fun clusterFaces(detections: List<DetectedFaceInfo>): List<List<DetectedFaceInfo>> {
        val segmenter = AppearanceSegmenter(minDetectionsPerSegment = 1)
        val tracks = segmenter.buildAppearanceTracks(detections)
        val clusters = clusterSegmentTracks(tracks)
        return clusters.map { it.allDetections }
    }

    private fun computeClusterDistance(
        n1: TrackClusterNode,
        n2: TrackClusterNode
    ): Float {
        // 1. Centroid Cosine Distance
        val centroidDist = FaceEmbedder.cosineDistance(n1.centroidEmbedding, n2.centroidEmbedding)

        // 2. Pairwise distances between representative keyframes
        val pairDistances = mutableListOf<Float>()
        for (t1 in n1.tracks) {
            for (t2 in n2.tracks) {
                val e1 = t1.representativeKeyframes.mapNotNull { it.embedding }
                val e2 = t2.representativeKeyframes.mapNotNull { it.embedding }
                for (v1 in e1) {
                    for (v2 in e2) {
                        pairDistances.add(FaceEmbedder.cosineDistance(v1, v2))
                    }
                }
            }
        }

        if (pairDistances.isEmpty()) return centroidDist

        pairDistances.sort()
        val minPairDist = pairDistances.first()
        val takeCount = maxOf(1, (pairDistances.size * 0.50f).toInt())
        var sum = 0f
        for (i in 0 until takeCount) {
            sum += pairDistances[i]
        }
        val topAvgDist = sum / takeCount

        // Robust metric combining centroid, top-k average and min pair
        return minOf(centroidDist, (topAvgDist * 0.6f + minPairDist * 0.4f))
    }

    private fun hasCoOccurrenceConflict(
        n1: TrackClusterNode,
        n2: TrackClusterNode
    ): Boolean {
        val timestamps1 = HashSet<Long>()
        for (t in n1.tracks) {
            for (d in t.detections) {
                timestamps1.add(d.frameTimestampMs)
            }
        }

        var coOccurCount = 0
        val checked = HashSet<Long>()
        for (t in n2.tracks) {
            for (d in t.detections) {
                val ts = d.frameTimestampMs
                if (timestamps1.contains(ts) && checked.add(ts)) {
                    coOccurCount++
                    // Genuine co-occurrence requires at least 2 distinct frame timestamps together
                    // (prevents 1-frame transient glitches or reflections from blocking valid merges)
                    if (coOccurCount >= 2) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun computeCentroid(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) return FloatArray(0)
        val dim = embeddings.first().size
        val sum = FloatArray(dim)

        for (emb in embeddings) {
            for (i in 0 until dim) {
                sum[i] += emb[i]
            }
        }

        var sumSq = 0.0
        for (i in 0 until dim) {
            sum[i] /= embeddings.size
            sumSq += (sum[i] * sum[i]).toDouble()
        }

        val norm = sqrt(sumSq).toFloat().coerceAtLeast(1e-10f)
        return FloatArray(dim) { i -> sum[i] / norm }
    }
}
