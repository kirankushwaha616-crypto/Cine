package com.example.data.model

import java.util.UUID

enum class AspectRatio(val displayName: String, val ratio: Float) {
  ORIGINAL("Original", 0f),
  RATIO_16_9("16:9", 16f / 9f),
  RATIO_9_16("9:16", 9f / 16f),
  RATIO_1_1("1:1", 1f),
  RATIO_4_5("4:5", 4f / 5f)
}

enum class ExportResolution(val label: String, val width: Int, val height: Int) {
  RES_720P("720p HD", 1280, 720),
  RES_1080P("1080p Full HD", 1920, 1080)
}

enum class ScaleMode(val label: String) {
  FIT("Fit"),
  CROP("Fill & Crop")
}

data class ClipModel(
  val id: String = UUID.randomUUID().toString(),
  val sourceUri: String,
  val displayName: String,
  val originalDurationMs: Long,
  val startMs: Long = 0L,
  val endMs: Long = originalDurationMs,
  val speed: Float = 1.0f,
  val rotationDegrees: Int = 0,
  val scaleMode: ScaleMode = ScaleMode.FIT,
  val isVideo: Boolean = true
) {
  val trimmedDurationMs: Long
    get() = (endMs - startMs).coerceAtLeast(100L)

  val effectiveDurationMs: Long
    get() = ((endMs - startMs).coerceAtLeast(100L) / speed.coerceIn(0.25f, 4f)).toLong()
}

data class TextOverlay(
  val id: String = UUID.randomUUID().toString(),
  val text: String,
  val colorHex: Long = 0xFFFFFFFF,
  val fontSizeSp: Float = 24f,
  val positionX: Float = 0f, // -1f (left) to 1f (right)
  val positionY: Float = 0.6f, // -1f (top) to 1f (bottom), 0.6f is caption area
  val startMs: Long = 0L,
  val endMs: Long = 5000L
)

data class AudioSettings(
  val originalVideoVolume: Float = 1.0f,
  val isOriginalAudioMuted: Boolean = false,
  val bgmUri: String? = null,
  val bgmTitle: String? = null,
  val bgmVolume: Float = 0.5f,
  val bgmStartMs: Long = 0L
)

data class VisualAdjustments(
  val brightness: Float = 0.0f, // -0.5f to 0.5f
  val contrast: Float = 1.0f,   // 0.5f to 1.5f
  val saturation: Float = 1.0f  // 0.0f to 2.0f
) {
  val isDefault: Boolean
    get() = brightness == 0f && contrast == 1f && saturation == 1f
}

data class ProjectModel(
  val id: String = UUID.randomUUID().toString(),
  val title: String = "Untitled Project",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val isDraft: Boolean = true,
  val clips: List<ClipModel> = emptyList(),
  val textOverlays: List<TextOverlay> = emptyList(),
  val audioSettings: AudioSettings = AudioSettings(),
  val adjustments: VisualAdjustments = VisualAdjustments(),
  val aspectRatio: AspectRatio = AspectRatio.ORIGINAL,
  val exportResolution: ExportResolution = ExportResolution.RES_1080P
) {
  val totalDurationMs: Long
    get() = clips.sumOf { it.effectiveDurationMs }
}

data class ExportedVideo(
  val id: String = UUID.randomUUID().toString(),
  val projectId: String,
  val projectTitle: String,
  val filePath: String,
  val resolution: String,
  val durationMs: Long,
  val fileSizeBytes: Long,
  val exportedAt: Long = System.currentTimeMillis()
)
