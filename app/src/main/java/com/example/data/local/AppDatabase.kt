package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.data.model.AspectRatio
import com.example.data.model.AudioSettings
import com.example.data.model.ClipModel
import com.example.data.model.ExportResolution
import com.example.data.model.ExportedVideo
import com.example.data.model.ProjectModel
import com.example.data.model.ScaleMode
import com.example.data.model.TextOverlay
import com.example.data.model.VisualAdjustments
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "projects")
data class ProjectEntity(
  @PrimaryKey val id: String,
  val title: String,
  val createdAt: Long,
  val updatedAt: Long,
  val isDraft: Boolean,
  val aspectRatio: String,
  val exportResolution: String,
  val brightness: Float,
  val contrast: Float,
  val saturation: Float,
  val originalVideoVolume: Float,
  val isOriginalAudioMuted: Boolean,
  val bgmUri: String?,
  val bgmTitle: String?,
  val bgmVolume: Float,
  val bgmStartMs: Long,
  val clipsJson: String,
  val textOverlaysJson: String
) {
  fun toDomain(): ProjectModel {
    val clipsList = mutableListOf<ClipModel>()
    try {
      val arr = JSONArray(clipsJson)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        clipsList.add(
          ClipModel(
            id = obj.getString("id"),
            sourceUri = obj.getString("sourceUri"),
            displayName = obj.optString("displayName", "Clip ${i + 1}"),
            originalDurationMs = obj.getLong("originalDurationMs"),
            startMs = obj.optLong("startMs", 0L),
            endMs = obj.optLong("endMs", obj.getLong("originalDurationMs")),
            speed = obj.optDouble("speed", 1.0).toFloat(),
            rotationDegrees = obj.optInt("rotationDegrees", 0),
            scaleMode = runCatching { ScaleMode.valueOf(obj.optString("scaleMode", "FIT")) }.getOrDefault(ScaleMode.FIT),
            isVideo = obj.optBoolean("isVideo", true)
          )
        )
      }
    } catch (_: Exception) {}

    val textList = mutableListOf<TextOverlay>()
    try {
      val arr = JSONArray(textOverlaysJson)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        textList.add(
          TextOverlay(
            id = obj.getString("id"),
            text = obj.getString("text"),
            colorHex = obj.optLong("colorHex", 0xFFFFFFFF),
            fontSizeSp = obj.optDouble("fontSizeSp", 24.0).toFloat(),
            positionX = obj.optDouble("positionX", 0.0).toFloat(),
            positionY = obj.optDouble("positionY", 0.6).toFloat(),
            startMs = obj.optLong("startMs", 0L),
            endMs = obj.optLong("endMs", 5000L)
          )
        )
      }
    } catch (_: Exception) {}

    return ProjectModel(
      id = id,
      title = title,
      createdAt = createdAt,
      updatedAt = updatedAt,
      isDraft = isDraft,
      clips = clipsList,
      textOverlays = textList,
      audioSettings = AudioSettings(
        originalVideoVolume = originalVideoVolume,
        isOriginalAudioMuted = isOriginalAudioMuted,
        bgmUri = bgmUri,
        bgmTitle = bgmTitle,
        bgmVolume = bgmVolume,
        bgmStartMs = bgmStartMs
      ),
      adjustments = VisualAdjustments(
        brightness = brightness,
        contrast = contrast,
        saturation = saturation
      ),
      aspectRatio = runCatching { AspectRatio.valueOf(aspectRatio) }.getOrDefault(AspectRatio.ORIGINAL),
      exportResolution = runCatching { ExportResolution.valueOf(exportResolution) }.getOrDefault(ExportResolution.RES_1080P)
    )
  }

  companion object {
    fun fromDomain(model: ProjectModel): ProjectEntity {
      val clipsArr = JSONArray()
      for (c in model.clips) {
        val obj = JSONObject().apply {
          put("id", c.id)
          put("sourceUri", c.sourceUri)
          put("displayName", c.displayName)
          put("originalDurationMs", c.originalDurationMs)
          put("startMs", c.startMs)
          put("endMs", c.endMs)
          put("speed", c.speed.toDouble())
          put("rotationDegrees", c.rotationDegrees)
          put("scaleMode", c.scaleMode.name)
          put("isVideo", c.isVideo)
        }
        clipsArr.put(obj)
      }

      val textArr = JSONArray()
      for (t in model.textOverlays) {
        val obj = JSONObject().apply {
          put("id", t.id)
          put("text", t.text)
          put("colorHex", t.colorHex)
          put("fontSizeSp", t.fontSizeSp.toDouble())
          put("positionX", t.positionX.toDouble())
          put("positionY", t.positionY.toDouble())
          put("startMs", t.startMs)
          put("endMs", t.endMs)
        }
        textArr.put(obj)
      }

      return ProjectEntity(
        id = model.id,
        title = model.title,
        createdAt = model.createdAt,
        updatedAt = model.updatedAt,
        isDraft = model.isDraft,
        aspectRatio = model.aspectRatio.name,
        exportResolution = model.exportResolution.name,
        brightness = model.adjustments.brightness,
        contrast = model.adjustments.contrast,
        saturation = model.adjustments.saturation,
        originalVideoVolume = model.audioSettings.originalVideoVolume,
        isOriginalAudioMuted = model.audioSettings.isOriginalAudioMuted,
        bgmUri = model.audioSettings.bgmUri,
        bgmTitle = model.audioSettings.bgmTitle,
        bgmVolume = model.audioSettings.bgmVolume,
        bgmStartMs = model.audioSettings.bgmStartMs,
        clipsJson = clipsArr.toString(),
        textOverlaysJson = textArr.toString()
      )
    }
  }
}

@Entity(tableName = "exported_videos")
data class ExportEntity(
  @PrimaryKey val id: String,
  val projectId: String,
  val projectTitle: String,
  val filePath: String,
  val resolution: String,
  val durationMs: Long,
  val fileSizeBytes: Long,
  val exportedAt: Long
) {
  fun toDomain(): ExportedVideo = ExportedVideo(
    id = id,
    projectId = projectId,
    projectTitle = projectTitle,
    filePath = filePath,
    resolution = resolution,
    durationMs = durationMs,
    fileSizeBytes = fileSizeBytes,
    exportedAt = exportedAt
  )

  companion object {
    fun fromDomain(m: ExportedVideo): ExportEntity = ExportEntity(
      id = m.id,
      projectId = m.projectId,
      projectTitle = m.projectTitle,
      filePath = m.filePath,
      resolution = m.resolution,
      durationMs = m.durationMs,
      fileSizeBytes = m.fileSizeBytes,
      exportedAt = m.exportedAt
    )
  }
}

@Dao
interface ProjectDao {
  @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
  fun getAllProjects(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects WHERE isDraft = 1 ORDER BY updatedAt DESC")
  fun getDraftProjects(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects WHERE isDraft = 0 ORDER BY updatedAt DESC")
  fun getSavedProjects(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
  suspend fun getProjectById(id: String): ProjectEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProject(project: ProjectEntity)

  @Query("DELETE FROM projects WHERE id = :id")
  suspend fun deleteProjectById(id: String)
}

@Dao
interface ExportDao {
  @Query("SELECT * FROM exported_videos ORDER BY exportedAt DESC")
  fun getAllExports(): Flow<List<ExportEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExport(export: ExportEntity)

  @Query("DELETE FROM exported_videos WHERE id = :id")
  suspend fun deleteExportById(id: String)
}

@Database(entities = [ProjectEntity::class, ExportEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun projectDao(): ProjectDao
  abstract fun exportDao(): ExportDao
}
