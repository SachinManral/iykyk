package com.iykyk.app.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.iykyk.app.data.model.AppearanceSegment
import com.iykyk.app.data.model.CollageResult
import com.iykyk.app.data.model.PersonIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Manages persistent local disk storage of generated collages, avatars, and appearance metadata.
 */
object LocalCollageStorageManager {

    private const val STORAGE_DIR = "saved_collages"

    private fun getStorageDirectory(context: Context): File {
        val dir = File(context.filesDir, STORAGE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Persists a newly created collage, its full-res bitmap, avatars, and metadata to local internal storage.
     */
    suspend fun saveCollage(context: Context, collage: CollageResult): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseDir = getStorageDirectory(context)
            val collageFolder = File(baseDir, "collage_${collage.creationTimestampMs}")
            if (!collageFolder.exists()) {
                collageFolder.mkdirs()
            }

            // 1. Save Full Collage Bitmap (PNG)
            val collageImageFile = File(collageFolder, "collage.png")
            FileOutputStream(collageImageFile).use { out ->
                collage.collageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // 2. Save each person's avatar and portrait crops
            val peopleJsonArray = JSONArray()
            for ((index, person) in collage.people.withIndex()) {
                val personJson = JSONObject()
                personJson.put("id", person.id)
                personJson.put("label", person.label)
                personJson.put("representativeShotTimestampMs", person.representativeShotTimestampMs)
                personJson.put("representativeQualityScore", person.representativeQualityScore.toDouble())

                // Save avatar thumbnail
                person.avatarThumbnailBitmap?.let { bmp ->
                    val avatarFile = File(collageFolder, "avatar_${index}.png")
                    FileOutputStream(avatarFile).use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                    personJson.put("avatarFile", avatarFile.name)
                }

                // Save portrait crop
                person.representativePortraitBitmap?.let { bmp ->
                    val portraitFile = File(collageFolder, "portrait_${index}.png")
                    FileOutputStream(portraitFile).use { out ->
                        bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
                    }
                    personJson.put("portraitFile", portraitFile.name)
                }

                // Save appearances
                val appearancesArray = JSONArray()
                for (app in person.appearances) {
                    val appJson = JSONObject()
                    appJson.put("startTimeMs", app.startTimeMs)
                    appJson.put("endTimeMs", app.endTimeMs)
                    appJson.put("totalDetections", app.totalDetections)
                    appJson.put("bestFrameTimestampMs", app.bestFrameTimestampMs)
                    appearancesArray.put(appJson)
                }
                personJson.put("appearances", appearancesArray)
                peopleJsonArray.put(personJson)
            }

            // 3. Save JSON Metadata
            val metaJson = JSONObject()
            metaJson.put("videoUri", collage.videoUri.toString())
            metaJson.put("videoTitle", collage.videoTitle)
            metaJson.put("videoDurationMs", collage.videoDurationMs)
            metaJson.put("totalAppearances", collage.totalAppearances)
            metaJson.put("creationTimestampMs", collage.creationTimestampMs)
            metaJson.put("people", peopleJsonArray)

            val metaFile = File(collageFolder, "meta.json")
            metaFile.writeText(metaJson.toString(2))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Loads all saved collages from local disk storage.
     */
    suspend fun loadAllCollages(context: Context): List<CollageResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CollageResult>()
        try {
            val baseDir = getStorageDirectory(context)
            val folders = baseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

            for (folder in folders) {
                try {
                    val metaFile = File(folder, "meta.json")
                    val collageImageFile = File(folder, "collage.png")

                    if (!metaFile.exists() || !collageImageFile.exists()) continue

                    val collageBitmap = BitmapFactory.decodeFile(collageImageFile.absolutePath) ?: continue
                    val metaJson = JSONObject(metaFile.readText())

                    val videoUri = Uri.parse(metaJson.optString("videoUri", ""))
                    val videoTitle = metaJson.optString("videoTitle", "Untitled Collage")
                    val videoDurationMs = metaJson.optLong("videoDurationMs", 0L)
                    val totalAppearances = metaJson.optInt("totalAppearances", 0)
                    val creationTimestampMs = metaJson.optLong("creationTimestampMs", folder.lastModified())

                    val peopleJsonArray = metaJson.optJSONArray("people") ?: JSONArray()
                    val people = mutableListOf<PersonIdentity>()

                    for (i in 0 until peopleJsonArray.length()) {
                        val pObj = peopleJsonArray.getJSONObject(i)
                        val id = pObj.optInt("id", i + 1)
                        val label = pObj.optString("label", "Person ${('A'.code + i).toChar()}")
                        val repTimestamp = pObj.optLong("representativeShotTimestampMs", 0L)
                        val quality = pObj.optDouble("representativeQualityScore", 0.0).toFloat()

                        val avatarFileName = pObj.optString("avatarFile", "")
                        val avatarBitmap = if (avatarFileName.isNotEmpty()) {
                            BitmapFactory.decodeFile(File(folder, avatarFileName).absolutePath)
                        } else null

                        val portraitFileName = pObj.optString("portraitFile", "")
                        val portraitBitmap = if (portraitFileName.isNotEmpty()) {
                            BitmapFactory.decodeFile(File(folder, portraitFileName).absolutePath)
                        } else null

                        val appearancesArray = pObj.optJSONArray("appearances") ?: JSONArray()
                        val appearances = mutableListOf<AppearanceSegment>()
                        for (j in 0 until appearancesArray.length()) {
                            val aObj = appearancesArray.getJSONObject(j)
                            appearances.add(
                                AppearanceSegment(
                                    startTimeMs = aObj.optLong("startTimeMs", 0L),
                                    endTimeMs = aObj.optLong("endTimeMs", 0L),
                                    totalDetections = aObj.optInt("totalDetections", 1),
                                    bestFrameTimestampMs = aObj.optLong("bestFrameTimestampMs", 0L)
                                )
                            )
                        }

                        people.add(
                            PersonIdentity(
                                id = id,
                                label = label,
                                appearances = appearances,
                                representativeShotTimestampMs = repTimestamp,
                                representativeQualityScore = quality,
                                representativePortraitBitmap = portraitBitmap,
                                avatarThumbnailBitmap = avatarBitmap
                            )
                        )
                    }

                    results.add(
                        CollageResult(
                            videoUri = videoUri,
                            videoTitle = videoTitle,
                            videoDurationMs = videoDurationMs,
                            people = people,
                            totalAppearances = totalAppearances,
                            collageBitmap = collageBitmap,
                            creationTimestampMs = creationTimestampMs
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results.sortedByDescending { it.creationTimestampMs }
    }

    /**
     * Deletes a specific collage folder by creation timestamp.
     */
    suspend fun deleteCollage(context: Context, creationTimestampMs: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val baseDir = getStorageDirectory(context)
            val collageFolder = File(baseDir, "collage_$creationTimestampMs")
            if (collageFolder.exists()) {
                collageFolder.deleteRecursively()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
