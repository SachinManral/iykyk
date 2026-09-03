package com.iykyk.app.processing.clustering

import com.iykyk.app.data.model.DetectedFaceInfo
import com.iykyk.app.processing.embedder.FaceEmbedder
import kotlin.math.sqrt

class IdentityClusterer(
    private val distanceThreshold: Float = 0.22f
) {

    data class FaceTracklet(
        val id: Int,
        val detections: MutableList<DetectedFaceInfo> = mutableListOf(),
        var centroidEmbedding: FloatArray = FloatArray(0)
    )

    var lastTrackletCount: Int = 0
        private set

    fun clusterFaces(detections: List<DetectedFaceInfo>): List<List<DetectedFaceInfo>> {
        val validDetections = detections.filter {
            val embedding = it.embedding
            embedding != null && embedding.isNotEmpty()
        }
        if (validDetections.isEmpty()) {
            lastTrackletCount = 0
            return emptyList()
        }

        val tracklets = buildTracklets(validDetections)
        lastTrackletCount = tracklets.size
        if (tracklets.isEmpty()) {
            return emptyList()
        }

        val clusters = tracklets.map { mutableListOf(it) }.toMutableList()

        while (clusters.size > 1) {
            var bestDistance = Float.MAX_VALUE
            var bestI = -1
            var bestJ = -1

            for (i in 0 until clusters.size) {
                for (j in i + 1 until clusters.size) {
                    if (hasCoOccurrenceConflict(clusters[i], clusters[j])) {
                        continue
                    }

                    val distance = averageLinkageDistance(clusters[i], clusters[j])
                    if (distance < bestDistance) {
                        bestDistance = distance
                        bestI = i
                        bestJ = j
                    }
                }
            }

            if (bestI < 0 || bestJ < 0 || bestDistance > distanceThreshold) {
                break
            }

            clusters[bestI].addAll(clusters[bestJ])
            clusters.removeAt(bestJ)
        }

        return clusters
            .map { cluster -> cluster.flatMap { it.detections }.sortedBy { it.frameTimestampMs } }
            .sortedByDescending { it.size }
    }

    private fun buildTracklets(detections: List<DetectedFaceInfo>): List<FaceTracklet> {
        val sorted = detections.sortedBy { it.frameTimestampMs }
        val active = mutableMapOf<Int, FaceTracklet>()
        val finished = mutableListOf<FaceTracklet>()
        var nextId = 1

        for (det in sorted) {
            val trackingId = det.trackingId
            if (trackingId == null || trackingId < 0) {
                val single = FaceTracklet(id = nextId++)
                single.detections.add(det)
                single.centroidEmbedding = computeCentroid(single.detections.mapNotNull { it.embedding })
                if (single.centroidEmbedding.isNotEmpty()) {
                    finished.add(single)
                }
                continue
            }

            val current = active[trackingId]
            if (current == null) {
                val tracklet = FaceTracklet(id = nextId++)
                tracklet.detections.add(det)
                active[trackingId] = tracklet
            } else {
                val last = current.detections.last()
                val gap = det.frameTimestampMs - last.frameTimestampMs

                if (gap <= 600L) {
                    current.detections.add(det)
                } else {
                    current.centroidEmbedding = computeCentroid(current.detections.mapNotNull { it.embedding })
                    if (current.centroidEmbedding.isNotEmpty()) {
                        finished.add(current)
                    }
                    val newTracklet = FaceTracklet(id = nextId++)
                    newTracklet.detections.add(det)
                    active[trackingId] = newTracklet
                }
            }
        }

        active.values.forEach { tracklet ->
            tracklet.centroidEmbedding = computeCentroid(tracklet.detections.mapNotNull { it.embedding })
            if (tracklet.centroidEmbedding.isNotEmpty()) {
                finished.add(tracklet)
            }
        }

        return finished
    }

    /**
     * Average linkage distance between clusters of tracklets.
     */
    private fun averageLinkageDistance(
        c1: List<FaceTracklet>,
        c2: List<FaceTracklet>
    ): Float {
        var total = 0f
        var count = 0
        for (a in c1) {
            for (b in c2) {
                total += FaceEmbedder.cosineDistance(a.centroidEmbedding, b.centroidEmbedding)
                count++
            }
        }
        return if (count > 0) total / count else Float.MAX_VALUE
    }

    /**
     * Two identities cannot be the same if their actual observations coexist within the same sampled time window.
     */
    private fun hasCoOccurrenceConflict(
        c1: List<FaceTracklet>,
        c2: List<FaceTracklet>
    ): Boolean {
        for (a in c1) {
            for (b in c2) {
                for (da in a.detections) {
                    for (db in b.detections) {
                        val timeDifference = kotlin.math.abs(da.frameTimestampMs - db.frameTimestampMs)
                        if (timeDifference <= 250L) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun computeCentroid(embeddings: List<FloatArray>): FloatArray {
        if (embeddings.isEmpty()) {
            return FloatArray(0)
        }
        val dimension = embeddings.first().size
        val sum = FloatArray(dimension)

        for (embedding in embeddings) {
            for (i in 0 until dimension) {
                sum[i] += embedding[i]
            }
        }

        for (i in 0 until dimension) {
            sum[i] /= embeddings.size
        }

        var normSquared = 0.0
        for (value in sum) {
            normSquared += value * value
        }

        val norm = sqrt(normSquared).toFloat().coerceAtLeast(1e-8f)
        return FloatArray(dimension) { i -> sum[i] / norm }
    }
}
