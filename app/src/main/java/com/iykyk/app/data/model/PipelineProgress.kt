package com.iykyk.app.data.model

import android.graphics.Bitmap

enum class PipelineStage(val title: String) {
    READING_VIDEO("Reading video"),
    DETECTING_FACES("Detecting faces"),
    GROUPING_IDENTITIES("Grouping identities"),
    COUNTING_APPEARANCES("Counting appearances"),
    CHOOSING_BEST_MOMENTS("Choosing best moments"),
    COMPOSING_COLLAGE("Composing collage"),
    COMPLETED("Completed")
}

data class PipelineProgress(
    val stage: PipelineStage = PipelineStage.READING_VIDEO,
    val progressPercent: Int = 0,
    val statusMessage: String = "Reading video...",
    val detectedFacesCount: Int = 0,
    val discoveredPeopleCount: Int = 0,
    val discoveredAvatars: List<Bitmap> = emptyList(),
    val stageDurationsSeconds: Map<PipelineStage, Int> = emptyMap()
)
