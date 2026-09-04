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

import com.iykyk.app.graphics.CollageTheme

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

    private val _selectedCollageTheme = MutableStateFlow(CollageTheme.FLORAL_SCRAPBOOK)
    val selectedCollageTheme: StateFlow<CollageTheme> = _selectedCollageTheme.asStateFlow()

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

    private val prefs = application.getSharedPreferences("iykyk_favorites", Context.MODE_PRIVATE)
    private val _favoriteCollageIds = MutableStateFlow<Set<Long>>(
        prefs.getStringSet("favorite_ids", emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    )
    val favoriteCollageIds: StateFlow<Set<Long>> = _favoriteCollageIds.asStateFlow()

    private var processingJob: Job? = null

    init {
        loadAvailableVideos()
        loadSavedCollages()
    }

    fun toggleFavorite(timestampMs: Long) {
        val current = _favoriteCollageIds.value.toMutableSet()
        if (current.contains(timestampMs)) {
            current.remove(timestampMs)
        } else {
            current.add(timestampMs)
        }
        _favoriteCollageIds.value = current
        prefs.edit().putStringSet("favorite_ids", current.map { it.toString() }.toSet()).apply()
    }

    fun isFavorite(timestampMs: Long): Boolean {
        return _favoriteCollageIds.value.contains(timestampMs)
    }

    fun loadSavedCollages() {
        viewModelScope.launch {
            val saved = com.iykyk.app.data.storage.LocalCollageStorageManager.loadAllCollages(getApplication())
            _recentCollages.value = saved
        }
    }

    fun loadAvailableVideos() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
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
                        val uri = Uri.fromFile(localFile)
                        val meta = extractVideoMetadata(getApplication(), uri)
                        list.add(
                            VideoItem(
                                uri = uri,
                                title = displayName,
                                durationText = meta.durationText,
                                resolutionText = meta.resolutionText,
                                dateText = "May ${20 - index}, 2026",
                                isSample = true,
                                thumbnailBitmap = meta.thumbnail
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
        _selectedCollageTheme.value = collage.selectedTheme
        _currentCollage.value = collage
    }

    fun remixCollageStyle() {
        val current = _currentCollage.value ?: return
        val remainingThemes = CollageTheme.entries.filter { it != current.selectedTheme }
        val newTheme = if (remainingThemes.isNotEmpty()) remainingThemes.random() else CollageTheme.entries.random()
        setCollageTheme(newTheme)
    }

    fun setCollageTheme(theme: CollageTheme) {
        _selectedCollageTheme.value = theme
        val current = _currentCollage.value ?: return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val logoBitmap = try {
                getApplication<Application>().assets.open("logo.png").use { input ->
                    android.graphics.BitmapFactory.decodeStream(input)
                }
            } catch (e: Exception) {
                null
            }

            val updatedBitmap = CollageComposer.createCollageBitmap(
                people = current.people,
                totalAppearances = current.totalAppearances,
                customTitle = current.videoTitle,
                theme = theme,
                logoBitmap = logoBitmap,
                assetManager = getApplication<Application>().assets,
                seed = System.currentTimeMillis() + kotlin.random.Random.nextLong(10000)
            )

            val updatedCollage = current.copy(
                selectedTheme = theme,
                collageBitmap = updatedBitmap
            )

            _currentCollage.value = updatedCollage
            _recentCollages.value = _recentCollages.value.map {
                if (it.creationTimestampMs == updatedCollage.creationTimestampMs) updatedCollage else it
            }

            com.iykyk.app.data.storage.LocalCollageStorageManager.saveCollage(
                getApplication(),
                updatedCollage
            )
        }
    }

    fun deleteSavedCollage(collage: CollageResult) {
        viewModelScope.launch {
            com.iykyk.app.data.storage.LocalCollageStorageManager.deleteCollage(
                getApplication(),
                collage.creationTimestampMs
            )
            _recentCollages.value = _recentCollages.value.filter { it.creationTimestampMs != collage.creationTimestampMs }
            if (_currentCollage.value?.creationTimestampMs == collage.creationTimestampMs) {
                _currentCollage.value = null
            }
        }
    }

    fun selectCustomVideoUri(uri: Uri, title: String? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val resolvedTitle = title ?: queryFileName(getApplication(), uri)
            val meta = extractVideoMetadata(getApplication(), uri)
            val item = VideoItem(
                uri = uri,
                title = resolvedTitle,
                durationText = meta.durationText,
                resolutionText = meta.resolutionText,
                dateText = "Today",
                isSample = false,
                thumbnailBitmap = meta.thumbnail
            )
            val updated = listOf(item) + _videoList.value.filter { it.uri != uri }
            _videoList.value = updated
            _selectedVideo.value = item
        }
    }

    private data class ExtractedMeta(
        val thumbnail: Bitmap?,
        val durationText: String,
        val resolutionText: String
    )

    private fun extractVideoMetadata(context: Context, uri: Uri): ExtractedMeta {
        val retriever = android.media.MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bitmap = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.frameAtTime
            val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 30000L
            val width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: "1080"
            val height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: "1920"

            val totalSecs = (durationMs / 1000).coerceAtLeast(1)
            val mins = totalSecs / 60
            val secs = totalSecs % 60
            val durationText = String.format("%02d:%02d", mins, secs)
            val resolutionText = "${width} × ${height}"
            ExtractedMeta(bitmap, durationText, resolutionText)
        } catch (e: Exception) {
            ExtractedMeta(null, "00:30", "1080 × 1920")
        } finally {
            try { retriever.release() } catch (e: Exception) {}
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String {
        var name = "Uploaded Video.mp4"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        val str = it.getString(index)
                        if (!str.isNullOrBlank()) name = str
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return name
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
                val composeStartTime = System.currentTimeMillis()
                _pipelineProgress.value = _pipelineProgress.value.copy(
                    stage = PipelineStage.COMPOSING_COLLAGE,
                    progressPercent = 95,
                    statusMessage = "Composing your collage..."
                )

                // Render dynamic collage bitmap with brand logo & active theme
                val logoBitmap = try {
                    getApplication<Application>().assets.open("logo.png").use { input ->
                        android.graphics.BitmapFactory.decodeStream(input)
                    }
                } catch (e: Exception) {
                    null
                }

                val activeTheme = CollageTheme.entries.random()
                _selectedCollageTheme.value = activeTheme
                val collageBitmap = CollageComposer.createCollageBitmap(
                    people = finalAnalysis.people,
                    totalAppearances = finalAnalysis.totalAppearances,
                    customTitle = finalAnalysis.videoTitle,
                    theme = activeTheme,
                    logoBitmap = logoBitmap,
                    assetManager = getApplication<Application>().assets
                )

                val composeSecs = ((System.currentTimeMillis() - composeStartTime) / 1000).toInt().coerceAtLeast(1)
                val updatedDurations = _pipelineProgress.value.stageDurationsSeconds.toMutableMap()
                updatedDurations[PipelineStage.COMPOSING_COLLAGE] = composeSecs

                val result = CollageResult(
                    videoUri = finalAnalysis.videoUri,
                    videoTitle = finalAnalysis.videoTitle,
                    videoDurationMs = finalAnalysis.durationMs,
                    people = finalAnalysis.people,
                    totalAppearances = finalAnalysis.totalAppearances,
                    collageBitmap = collageBitmap,
                    selectedTheme = activeTheme
                )

                _currentCollage.value = result
                _recentCollages.value = listOf(result) + _recentCollages.value

                // Persist to local disk storage
                com.iykyk.app.data.storage.LocalCollageStorageManager.saveCollage(
                    getApplication(),
                    result
                )

                _pipelineProgress.value = _pipelineProgress.value.copy(
                    stage = PipelineStage.COMPLETED,
                    progressPercent = 100,
                    statusMessage = "Done! Your collage is ready.",
                    stageDurationsSeconds = updatedDurations
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
