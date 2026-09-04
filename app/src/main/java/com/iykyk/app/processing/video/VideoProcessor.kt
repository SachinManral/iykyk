package com.iykyk.app.processing.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.iykyk.app.data.model.AppearanceSegment
import com.iykyk.app.data.model.DetectedFaceInfo
import com.iykyk.app.data.model.PersonIdentity
import com.iykyk.app.data.model.PipelineProgress
import com.iykyk.app.data.model.PipelineStage
import com.iykyk.app.processing.clustering.AppearanceSegmenter
import com.iykyk.app.processing.clustering.IdentityClusterer
import com.iykyk.app.processing.detector.FaceAligner
import com.iykyk.app.processing.detector.MLKitFaceDetector
import com.iykyk.app.processing.embedder.FaceEmbedder
import com.iykyk.app.processing.scoring.ShotRanker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

class VideoProcessor(private val context: Context) {

    private val frameExtractor = VideoFrameExtractor(context)

    data class AnalysisResult(
        val videoUri: Uri,
        val videoTitle: String,
        val durationMs: Long,
        val people: List<PersonIdentity>,
        val totalAppearances: Int,
        val allDetections: List<DetectedFaceInfo>
    )

    fun processVideo(
        videoUri: Uri,
        videoTitle: String = "Selected Video"
    ): Flow<Pair<PipelineProgress, AnalysisResult?>> = flow {
        emit(
            PipelineProgress(
                stage = PipelineStage.READING_VIDEO,
                progressPercent = 5,
                statusMessage = "Reading video metadata..."
            ) to null
        )

        val metadata = frameExtractor.extractMetadata(videoUri)
        val durationMs = metadata.durationMs.coerceAtLeast(1000L)

        // 5 FPS sampling rate (200ms step)
        val sampleTimestamps = frameExtractor.generateSamplingTimestamps(
            durationMs = durationMs,
            baseIntervalMs = 200L
        )

        emit(
            PipelineProgress(
                stage = PipelineStage.DETECTING_FACES,
                progressPercent = 15,
                statusMessage = "Detecting faces across ${sampleTimestamps.size} frames..."
            ) to null
        )

        val allDetections = mutableListOf<DetectedFaceInfo>()
        val discoveredAvatars = mutableListOf<Bitmap>()

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)

        val faceDetector = MLKitFaceDetector()
        val faceEmbedder = FaceEmbedder(context)

        try {
            val totalFrames = sampleTimestamps.size
            for ((index, timestampMs) in sampleTimestamps.withIndex()) {
                if (!coroutineContext.isActive) break

                val frameBitmap = frameExtractor.getFrameAt(
                    retriever = retriever,
                    timestampMs = timestampMs,
                    targetWidth = 540,
                    targetHeight = 960
                ) ?: continue

                // 1. Run ML Kit detection on each frame -> get list of faces
                val detectedFaces = faceDetector.detectFaces(frameBitmap, timestampMs)

                val isSolo = (detectedFaces.size == 1)
                for (face in detectedFaces) {
                    // Face-level quality gate (reject extreme blur)
                    if (face.sharpnessScore > 0f && face.sharpnessScore < 8f) {
                        continue
                    }

                    // 2. 5-point similarity transformation alignment (ArcFace canonical coordinates)
                    val alignedFace = FaceAligner.alignFace5Points(
                        sourceBitmap = frameBitmap,
                        boundingBox = face.boundingBox,
                        leftEye = face.leftEye,
                        rightEye = face.rightEye,
                        noseBase = face.noseBase,
                        leftMouth = face.leftMouth,
                        rightMouth = face.rightMouth
                    ) ?: continue

                    // 3. MobileFaceNet-ArcFace ONNX inference (512-D L2-normalized embedding)
                    val embedding = try {
                        faceEmbedder.extractEmbedding(alignedFace)
                    } finally {
                        alignedFace.recycle()
                    }

                    if (embedding.isEmpty()) {
                        continue
                    }

                    // 4. Compute multi-factor quality score (prioritizing solo portraits)
                    val qualityScore = ShotRanker.computeSingleQualityScore(face, isSoloFrame = isSolo)
                    val completeFace = face.copy(
                        embedding = embedding,
                        isSoloFrame = isSolo,
                        qualityScore = qualityScore
                    )
                    allDetections.add(completeFace)

                    if (discoveredAvatars.size < 6) {
                        val avatar = FaceAligner.alignFace5Points(
                            sourceBitmap = frameBitmap,
                            boundingBox = completeFace.boundingBox,
                            leftEye = completeFace.leftEye,
                            rightEye = completeFace.rightEye,
                            noseBase = completeFace.noseBase,
                            leftMouth = completeFace.leftMouth,
                            rightMouth = completeFace.rightMouth
                        )
                        if (avatar != null) {
                            val avatarCopy = Bitmap.createScaledBitmap(avatar, 96, 96, true)
                            avatar.recycle()
                            discoveredAvatars.add(avatarCopy)
                        }
                    }
                }
                frameBitmap.recycle()

                if (index % 4 == 0 || index == totalFrames - 1) {
                    val progress = 15 + ((index.toFloat() / totalFrames) * 35f).toInt()
                    emit(
                        PipelineProgress(
                            stage = PipelineStage.DETECTING_FACES,
                            progressPercent = progress.coerceIn(15, 50),
                            statusMessage = "Found ${allDetections.size} usable faces...",
                            detectedFacesCount = allDetections.size,
                            discoveredAvatars = discoveredAvatars.toList()
                        ) to null
                    )
                }
            }

            // 5. Build continuous appearance segments via frame-to-frame box tracking & keyframe selection
            emit(
                PipelineProgress(
                    stage = PipelineStage.COUNTING_APPEARANCES,
                    progressPercent = 55,
                    statusMessage = "Tracking continuous appearances...",
                    detectedFacesCount = allDetections.size,
                    discoveredAvatars = discoveredAvatars.toList()
                ) to null
            )

            val segmenter = AppearanceSegmenter(
                maxContinuityGapMs = 800L,
                minDetectionsPerSegment = 1
            )
            val appearanceTracks = segmenter.buildAppearanceTracks(allDetections)

            // 6. Global agglomerative clustering on pooled segment keyframe embeddings
            emit(
                PipelineProgress(
                    stage = PipelineStage.GROUPING_IDENTITIES,
                    progressPercent = 70,
                    statusMessage = "Grouping unique individuals with ArcFace...",
                    detectedFacesCount = allDetections.size,
                    discoveredPeopleCount = appearanceTracks.size,
                    discoveredAvatars = discoveredAvatars.toList()
                ) to null
            )

            val clusterer = IdentityClusterer(distanceThreshold = 0.64f)
            val personClusters = clusterer.clusterSegmentTracks(appearanceTracks)

            // 7. Select best representative moments and compose person identities
            emit(
                PipelineProgress(
                    stage = PipelineStage.CHOOSING_BEST_MOMENTS,
                    progressPercent = 85,
                    statusMessage = "Selecting best representative moments...",
                    detectedFacesCount = allDetections.size,
                    discoveredPeopleCount = personClusters.size,
                    discoveredAvatars = discoveredAvatars.toList()
                ) to null
            )

            val personIdentities = mutableListOf<PersonIdentity>()
            var totalAppearancesCount = 0

            for ((clusterIdx, cluster) in personClusters.withIndex()) {
                val appearances = cluster.segments
                totalAppearancesCount += appearances.size

                val bestDetection = cluster.representativeDetection

                val fullFrame = frameExtractor.getFrameAt(retriever, bestDetection.frameTimestampMs)
                val portraitCrop = fullFrame?.let {
                    extractGenerousPortraitCrop(
                        fullFrame = it,
                        targetFace = bestDetection
                    )
                }
                fullFrame?.recycle()

                val avatar = portraitCrop?.let {
                    Bitmap.createScaledBitmap(it, 112, 112, true)
                }

                val personLabel = "Person ${('A'.code + clusterIdx).toChar()}"
                val person = PersonIdentity(
                    id = clusterIdx + 1,
                    label = personLabel,
                    appearances = appearances,
                    representativeShotTimestampMs = bestDetection.frameTimestampMs,
                    representativeQualityScore = bestDetection.qualityScore,
                    representativePortraitBitmap = portraitCrop,
                    avatarThumbnailBitmap = avatar
                )
                personIdentities.add(person)
            }

            val finalResult = AnalysisResult(
                videoUri = videoUri,
                videoTitle = videoTitle,
                durationMs = durationMs,
                people = personIdentities,
                totalAppearances = totalAppearancesCount,
                allDetections = allDetections
            )

            emit(
                PipelineProgress(
                    stage = PipelineStage.CHOOSING_BEST_MOMENTS,
                    progressPercent = 100,
                    statusMessage = "Found ${personIdentities.size} people across $totalAppearancesCount appearances",
                    detectedFacesCount = allDetections.size,
                    discoveredPeopleCount = personIdentities.size,
                    discoveredAvatars = discoveredAvatars.toList()
                ) to finalResult
            )

        } finally {
            retriever.release()
            faceDetector.close()
            faceEmbedder.close()
        }
    }.flowOn(Dispatchers.Default)

    private fun extractGenerousPortraitCrop(
        fullFrame: Bitmap,
        targetFace: DetectedFaceInfo
    ): Bitmap {
        val sourceWidth = targetFace.frameWidth
        val sourceHeight = targetFace.frameHeight
        val scaleX = if (sourceWidth > 0) fullFrame.width.toFloat() / sourceWidth.toFloat() else 1f
        val scaleY = if (sourceHeight > 0) fullFrame.height.toFloat() / sourceHeight.toFloat() else 1f

        val faceBox = targetFace.boundingBox
        val centerX = faceBox.centerX() * scaleX
        val centerY = faceBox.centerY() * scaleY
        val faceWidth = faceBox.width() * scaleX
        val faceHeight = faceBox.height() * scaleY

        val faceDimension = maxOf(faceWidth, faceHeight)
        // Focused 1.38x headshot width for aesthetic portrait framing (never captures adjacent persons)
        val cropWidth = (faceDimension * 1.38f).coerceIn(100f, fullFrame.width.toFloat())
        val cropHeight = (cropWidth * 1.33f).coerceIn(130f, fullFrame.height.toFloat())

        val left = (centerX - (cropWidth / 2f)).toInt().coerceIn(0, fullFrame.width - cropWidth.toInt())
        val top = (centerY - (cropHeight * 0.40f)).toInt().coerceIn(0, fullFrame.height - cropHeight.toInt())

        return Bitmap.createBitmap(fullFrame, left, top, cropWidth.toInt(), cropHeight.toInt())
    }
}
