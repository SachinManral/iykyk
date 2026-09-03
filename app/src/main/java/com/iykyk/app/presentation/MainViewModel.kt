package com.iykyk.app.presentation

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iykyk.app.data.model.CollageResult
import com.iykyk.app.data.model.PersonIdentity
import com.iykyk.app.data.model.PipelineProgress
import com.iykyk.app.data.model.PipelineStage
import com.iykyk.app.graphics.CollageComposer
import com.iykyk.app.processing.video.VideoProcessor
import com.iykyk.app.utils.ImagePersistenceManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class VideoItem(
    val uri: Uri,
    val title: String,
    val durationText: String,
    val resolutionText: String,
    val dateText: String,
    val isSample: Boolean = false,
    val thumbnailBitmap: Bitmap? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val videoProcessor = VideoProcessor(application)

    private val _videoList = MutableStateFlow<List<VideoItem>>(emptyList())
    val videoList: StateFlow<List<VideoItem>> = _videoList.asStateFlow()

    private val _selectedVideo = MutableStateFlow<VideoItem?>(null)
    val selectedVideo: StateFlow<VideoItem?> = _selectedVideo.asStateFlow()

    private val _pipelineProgress = MutableStateFlow(PipelineProgress())
    val pipelineProgress: StateFlow<PipelineProgress> = _pipelineProgress.asStateFlow()

    private val _currentCollage = MutableStateFlow<CollageResult?>(null)
    val currentCollage: StateFlow<CollageResult?> = _currentCollage.asStateFlow()

    private val _recentCollages = MutableStateFlow<List<CollageResult>>(emptyList())
    val recentCollages: StateFlow<List<CollageResult>> = _recentCollages.asStateFlow()

    private val _isSavingToGallery = MutableStateFlow(false)
    val isSavingToGallery: StateFlow<Boolean> = _isSavingToGallery.asStateFlow()

    private val _saveToastMessage = MutableStateFlow<String?>(null)
    val saveToastMessage: StateFlow<String?> = _saveToastMessage.asStateFlow()

    private var processingJob: Job? = null

    init {
        loadAvailableVideos()
    }

    fun loadAvailableVideos() {
        viewModelScope.launch {
            val list = mutableListOf<VideoItem>()

            // 1. Load bundled sample videos from assets to local cache files
            try {
                val assetManager = getApplication<Application>().assets
                val sampleFiles = assetManager.list("sample_videos") ?: emptyArray()

                for ((index, fileName) in sampleFiles.withIndex()) {
                    val localFile = copyAssetToCache(getApplication(), "sample_videos/$fileName", fileName)
                    if (localFile != null) {
                        val displayName = when {
                            fileName.contains("sample_1") -> "Sample 1.mp4"
                            fileName.contains("sample_2") -> "Sample 2.mp4"
                            fileName.contains("sample_3") -> "Sample 3.mp4"
                            else -> fileName
                        }
                        list.add(
                            VideoItem(
                                uri = Uri.fromFile(localFile),
                                title = displayName,
                                durationText = "00:30",
                                resolutionText = "1080×1920",
                                dateText = "May ${20 - index}, 2026",
                                isSample = true
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore asset listing errors
            }

            _videoList.value = list
            if (list.isNotEmpty() && _selectedVideo.value == null) {
                _selectedVideo.value = list.first()
            }
        }
    }

    fun selectVideo(item: VideoItem) {
        _selectedVideo.value = item
    }

    fun openCollage(collage: CollageResult) {
        _currentCollage.value = collage
    }

    fun selectCustomVideoUri(uri: Uri, title: String = "Custom Video.mp4") {
        val item = VideoItem(
            uri = uri,
            title = title,
            durationText = "00:30",
            resolutionText = "1080×1920",
            dateText = "Today",
            isSample = false
        )
        val updated = listOf(item) + _videoList.value.filter { it.uri != uri }
        _videoList.value = updated
        _selectedVideo.value = item
    }

    fun startProcessing(onComplete: () -> Unit) {
        val targetVideo = _selectedVideo.value ?: return

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _pipelineProgress.value = PipelineProgress(
                stage = PipelineStage.READING_VIDEO,
                progressPercent = 5,
                statusMessage = "Analyzing your moment..."
            )

            var analysisResult: VideoProcessor.AnalysisResult? = null

            videoProcessor.processVideo(targetVideo.uri, targetVideo.title).collect { (progress, result) ->
                _pipelineProgress.value = progress
                if (result != null) {
                    analysisResult = result
                }
            }

            val finalAnalysis = analysisResult
            if (finalAnalysis != null) {
                _pipelineProgress.value = _pipelineProgress.value.copy(
                    stage = PipelineStage.COMPOSING_COLLAGE,
                    progressPercent = 95,
                    statusMessage = "Composing your collage..."
                )

                // Render dynamic collage bitmap with brand logo
                val logoBitmap = try {
                    getApplication<Application>().assets.open("logo.png").use { input ->
                        android.graphics.BitmapFactory.decodeStream(input)
                    }
                } catch (e: Exception) {
                    null
                }

                val collageBitmap = CollageComposer.createCollageBitmap(
                    people = finalAnalysis.people,
                    totalAppearances = finalAnalysis.totalAppearances,
                    customTitle = finalAnalysis.videoTitle,
                    logoBitmap = logoBitmap
                )

                val result = CollageResult(
                    videoUri = finalAnalysis.videoUri,
                    videoTitle = finalAnalysis.videoTitle,
                    videoDurationMs = finalAnalysis.durationMs,
                    people = finalAnalysis.people,
                    totalAppearances = finalAnalysis.totalAppearances,
                    collageBitmap = collageBitmap
                )

                _currentCollage.value = result
                _recentCollages.value = listOf(result) + _recentCollages.value

                _pipelineProgress.value = _pipelineProgress.value.copy(
                    stage = PipelineStage.COMPLETED,
                    progressPercent = 100,
                    statusMessage = "Done! Your collage is ready."
                )

                onComplete()
            }
        }
    }

    fun cancelProcessing() {
        processingJob?.cancel()
        _pipelineProgress.value = PipelineProgress(
            stage = PipelineStage.READING_VIDEO,
            progressPercent = 0,
            statusMessage = "Processing cancelled"
        )
    }

    fun saveCurrentCollageToGallery(context: Context) {
        val collage = _currentCollage.value ?: return
        viewModelScope.launch {
            _isSavingToGallery.value = true
            val uri = ImagePersistenceManager.saveCollageToGallery(
                context = context,
                bitmap = collage.collageBitmap,
                filenamePrefix = "iykyk_${collage.videoTitle.replace(" ", "_")}"
            )
            _isSavingToGallery.value = false
            if (uri != null) {
                _saveToastMessage.value = "Saved to your gallery ✨"
            } else {
                _saveToastMessage.value = "Could not save to gallery."
            }
        }
    }

    fun shareCurrentCollage(context: Context) {
        val collage = _currentCollage.value ?: return
        viewModelScope.launch {
            val uri = ImagePersistenceManager.prepareShareableImageUri(context, collage.collageBitmap)
            if (uri != null) {
                ImagePersistenceManager.launchShareChooser(context, uri)
            }
        }
    }

    fun clearToastMessage() {
        _saveToastMessage.value = null
    }

    private fun copyAssetToCache(context: Context, assetPath: String, outputName: String): File? {
        return try {
            val outFile = File(context.cacheDir, outputName)
            if (!outFile.exists() || outFile.length() == 0L) {
                context.assets.open(assetPath).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            outFile
        } catch (e: Exception) {
            null
        }
    }
}
