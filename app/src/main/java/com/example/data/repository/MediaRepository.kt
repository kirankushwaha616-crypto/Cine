package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.model.ClipModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MediaRepository(private val context: Context) {

  suspend fun createClipFromUri(uri: Uri): ClipModel = withContext(Dispatchers.IO) {
    var displayName = "Clip"
    var durationMs = 5000L
    var isVideo = true

    // Query display name
    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
          val name = cursor.getString(nameIndex)
          if (!name.isNullOrBlank()) {
            displayName = name
          }
        }
      }
    } catch (e: Exception) {
      Log.w("MediaRepository", "Failed to query display name: ${e.message}")
    }

    // Inspect metadata via MediaMetadataRetriever
    val retriever = MediaMetadataRetriever()
    try {
      retriever.setDataSource(context, uri)
      val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
      if (durationStr != null) {
        val parsed = durationStr.toLongOrNull()
        if (parsed != null && parsed > 500L) {
          durationMs = parsed
        }
      }
      val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
      isVideo = hasVideo == "yes" || hasVideo != null
    } catch (e: Exception) {
      Log.w("MediaRepository", "Failed to extract metadata: ${e.message}")
      val mime = context.contentResolver.getType(uri)
      if (mime?.startsWith("image/") == true) {
        isVideo = false
        durationMs = 5000L // 5 seconds for photo
      }
    } finally {
      runCatching { retriever.release() }
    }

    ClipModel(
      sourceUri = uri.toString(),
      displayName = displayName,
      originalDurationMs = durationMs,
      startMs = 0L,
      endMs = durationMs,
      isVideo = isVideo
    )
  }

  suspend fun extractThumbnail(uriString: String, timeMs: Long = 1000L): Bitmap? = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
      val uri = Uri.parse(uriString)
      retriever.setDataSource(context, uri)
      val timeUs = timeMs * 1000L
      retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (e: Exception) {
      Log.w("MediaRepository", "Failed to get thumbnail frame: ${e.message}")
      null
    } finally {
      runCatching { retriever.release() }
    }
  }

  suspend fun getAudioTitle(uri: Uri): String = withContext(Dispatchers.IO) {
    var title = "Audio Track"
    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
          val name = cursor.getString(nameIndex)
          if (!name.isNullOrBlank()) {
            title = name
          }
        }
      }
    } catch (_: Exception) {}
    title
  }

  suspend fun createSampleDemoMedia(): ClipModel = withContext(Dispatchers.IO) {
    // Generates a demo color frame sample video/image in internal files if device gallery is empty
    val demoFile = File(context.filesDir, "cinecut_demo_sample.png")
    if (!demoFile.exists()) {
      val bitmap = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888)
      val canvas = android.graphics.Canvas(bitmap)
      val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

      // Gradient background
      paint.shader = android.graphics.LinearGradient(
        0f, 0f, 1280f, 720f,
        android.graphics.Color.rgb(124, 77, 255),
        android.graphics.Color.rgb(0, 229, 255),
        android.graphics.Shader.TileMode.CLAMP
      )
      canvas.drawRect(0f, 0f, 1280f, 720f, paint)

      // Text
      paint.shader = null
      paint.color = android.graphics.Color.WHITE
      paint.textSize = 64f
      paint.textAlign = android.graphics.Paint.Align.CENTER
      canvas.drawText("CineCut Demo Clip", 640f, 340f, paint)

      paint.textSize = 32f
      paint.color = android.graphics.Color.argb(220, 255, 255, 255)
      canvas.drawText("Professional Mobile Video Editor", 640f, 410f, paint)

      FileOutputStream(demoFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
      }
    }

    ClipModel(
      sourceUri = Uri.fromFile(demoFile).toString(),
      displayName = "CineCut Demo Clip",
      originalDurationMs = 6000L,
      startMs = 0L,
      endMs = 6000L,
      isVideo = false
    )
  }
}
