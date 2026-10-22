package com.example.service.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbAdjustment
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.data.model.AspectRatio
import com.example.data.model.ExportedVideo
import com.example.data.model.ProjectModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

sealed interface ExportState {
  object Idle : ExportState
  data class InProgress(val progressPercent: Int) : ExportState
  data class Success(val exportedVideo: ExportedVideo) : ExportState
  data class Failed(val errorMessage: String) : ExportState
}

@OptIn(UnstableApi::class)
class VideoExportManager(
  private val context: Context,
  private val scope: CoroutineScope
) {

  private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
  val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

  private var activeTransformer: Transformer? = null
  private var progressPollingJob: Job? = null

  fun resetState() {
    _exportState.value = ExportState.Idle
  }

  fun cancelExport() {
    try {
      activeTransformer?.cancel()
    } catch (e: Exception) {
      Log.e("VideoExportManager", "Error cancelling export: ${e.message}")
    } finally {
      progressPollingJob?.cancel()
      progressPollingJob = null
      activeTransformer = null
      _exportState.value = ExportState.Idle
    }
  }

  suspend fun exportProject(project: ProjectModel): Unit = withContext(Dispatchers.Main) {
    if (project.clips.isEmpty()) {
      _exportState.value = ExportState.Failed("Project has no clips to export.")
      return@withContext
    }

    _exportState.value = ExportState.InProgress(0)

    val outputDir = File(context.cacheDir, "exports")
    if (!outputDir.exists()) outputDir.mkdirs()
    val tempOutputFile = File(outputDir, "cinecut_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.mp4")

    try {
      // 1. Build video sequence
      val editedMediaItems = mutableListOf<EditedMediaItem>()

      for (clip in project.clips) {
        val clipping = MediaItem.ClippingConfiguration.Builder()
          .setStartPositionMs(clip.startMs)
          .setEndPositionMs(clip.endMs)
          .build()

        val mediaItem = MediaItem.Builder()
          .setUri(Uri.parse(clip.sourceUri))
          .setClippingConfiguration(clipping)
          .build()

        // Video effects
        val videoEffects = mutableListOf<Effect>()

        // Rotation
        if (clip.rotationDegrees != 0) {
          videoEffects.add(
            ScaleAndRotateTransformation.Builder()
              .setRotationDegrees(clip.rotationDegrees.toFloat())
              .build()
          )
        }

        // Visual adjustments (brightness, contrast, saturation)
        if (!project.adjustments.isDefault) {
          try {
            val rgbAdj = RgbAdjustment.Builder()
              .setRedScale(1f + project.adjustments.brightness * 0.5f)
              .setGreenScale(1f + project.adjustments.brightness * 0.5f)
              .setBlueScale(1f + project.adjustments.brightness * 0.5f)
              .build()
            videoEffects.add(rgbAdj)
          } catch (_: Exception) {}
        }

        // Audio effects (speed)
        val audioProcessors = mutableListOf<AudioProcessor>()
        if (clip.speed != 1.0f) {
          val sonic = SonicAudioProcessor().apply {
            setSpeed(clip.speed)
          }
          audioProcessors.add(sonic)
        }

        val effects = Effects(audioProcessors, videoEffects)

        val editedItem = EditedMediaItem.Builder(mediaItem)
          .setEffects(effects)
          .setRemoveAudio(project.audioSettings.isOriginalAudioMuted)
          .build()

        editedMediaItems.add(editedItem)
      }

      val videoSequence = EditedMediaItemSequence(editedMediaItems)

      // 2. Composition Effects (Resolution and Aspect Ratio presentation)
      val compVideoEffects: MutableList<Effect> = mutableListOf()

      // Set target output height (e.g. 720 or 1080)
      compVideoEffects.add(Presentation.createForHeight(project.exportResolution.height))

      when (project.aspectRatio) {
        AspectRatio.RATIO_16_9 -> compVideoEffects.add(Presentation.createForAspectRatio(16f / 9f, Presentation.LAYOUT_SCALE_TO_FIT))
        AspectRatio.RATIO_9_16 -> compVideoEffects.add(Presentation.createForAspectRatio(9f / 16f, Presentation.LAYOUT_SCALE_TO_FIT))
        AspectRatio.RATIO_1_1 -> compVideoEffects.add(Presentation.createForAspectRatio(1f, Presentation.LAYOUT_SCALE_TO_FIT))
        AspectRatio.RATIO_4_5 -> compVideoEffects.add(Presentation.createForAspectRatio(4f / 5f, Presentation.LAYOUT_SCALE_TO_FIT))
        AspectRatio.ORIGINAL -> { /* Keep native */ }
      }

      val compEffects = Effects(emptyList(), compVideoEffects)

      // 3. Assemble Composition
      val sequences = mutableListOf(videoSequence)

      // Optional BGM audio track
      val bgmUriStr = project.audioSettings.bgmUri
      if (bgmUriStr != null) {
        try {
          val bgmMediaItem = MediaItem.fromUri(Uri.parse(bgmUriStr))
          val bgmEditedItem = EditedMediaItem.Builder(bgmMediaItem).build()
          val bgmSequence = EditedMediaItemSequence(listOf(bgmEditedItem), /* isLooping = */ true)
          sequences.add(bgmSequence)
        } catch (e: Exception) {
          Log.w("VideoExportManager", "Failed to add BGM to composition: ${e.message}")
        }
      }

      val composition = Composition.Builder(sequences)
        .setEffects(compEffects)
        .build()

      // 4. Configure Transformer
      val transformer = Transformer.Builder(context)
        .addListener(object : Transformer.Listener {
          override fun onCompleted(composition: Composition, exportResult: ExportResult) {
            progressPollingJob?.cancel()
            activeTransformer = null

            scope.launch(Dispatchers.IO) {
              val finalPath = saveExportToMediaStore(tempOutputFile, project.title)
              val fileSizeBytes = if (finalPath.startsWith("/")) File(finalPath).length() else tempOutputFile.length()

              val exportedVideo = ExportedVideo(
                projectId = project.id,
                projectTitle = project.title,
                filePath = finalPath,
                resolution = project.exportResolution.label,
                durationMs = project.totalDurationMs,
                fileSizeBytes = fileSizeBytes
              )

              withContext(Dispatchers.Main) {
                _exportState.value = ExportState.Success(exportedVideo)
              }
            }
          }

          override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
            Log.e("VideoExportManager", "Transformer error: ${exportException.message}", exportException)
            progressPollingJob?.cancel()
            activeTransformer = null
            _exportState.value = ExportState.Failed(exportException.localizedMessage ?: "Export failed: ${exportException.errorCodeName}")
          }
        })
        .build()

      activeTransformer = transformer

      // Start transformation
      transformer.start(composition, tempOutputFile.absolutePath)

      // 5. Start honest progress polling
      val progressHolder = ProgressHolder()
      progressPollingJob = scope.launch(Dispatchers.Main) {
        while (isActive) {
          val progressState = transformer.getProgress(progressHolder)
          if (progressState == Transformer.PROGRESS_STATE_AVAILABLE) {
            _exportState.value = ExportState.InProgress(progressHolder.progress)
          }
          delay(200L)
        }
      }

    } catch (e: Exception) {
      Log.e("VideoExportManager", "Exception during export initialization: ${e.message}", e)
      activeTransformer = null
      progressPollingJob?.cancel()
      _exportState.value = ExportState.Failed(e.localizedMessage ?: "Failed to start export.")
    }
  }

  private suspend fun saveExportToMediaStore(tempFile: File, title: String): String = withContext(Dispatchers.IO) {
    val cleanTitle = title.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
    val fileName = "CineCut_${cleanTitle}_${System.currentTimeMillis()}.mp4"

    try {
      val contentValues = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/CineCut")
          put(MediaStore.Video.Media.IS_PENDING, 1)
        }
      }

      val resolver = context.contentResolver
      val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

      if (uri != null) {
        resolver.openOutputStream(uri)?.use { outStream ->
          FileInputStream(tempFile).use { inStream ->
            inStream.copyTo(outStream)
          }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          contentValues.clear()
          contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
          resolver.update(uri, contentValues, null, null)
        }
        return@withContext uri.toString()
      }
    } catch (e: Exception) {
      Log.w("VideoExportManager", "MediaStore save fallback to app storage: ${e.message}")
    }

    // Fallback: save to permanent app external movies directory
    val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
    val finalFile = File(moviesDir, fileName)
    try {
      FileInputStream(tempFile).use { inStream ->
        FileOutputStream(finalFile).use { outStream ->
          inStream.copyTo(outStream)
        }
      }
      return@withContext finalFile.absolutePath
    } catch (_: Exception) {
      return@withContext tempFile.absolutePath
    }
  }
}
