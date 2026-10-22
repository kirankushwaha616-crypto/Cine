package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ProjectEntity
import com.example.data.model.AspectRatio
import com.example.data.model.ClipModel
import com.example.data.model.ExportResolution
import com.example.data.model.ProjectModel
import com.example.data.model.ScaleMode
import com.example.data.model.TextOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app_name string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CineCut", appName)
  }

  @Test
  fun `verify clip model trimming and effective duration`() {
    val clip = ClipModel(
      sourceUri = "content://media/external/video/1",
      displayName = "Test Clip",
      originalDurationMs = 10000L,
      startMs = 2000L,
      endMs = 8000L,
      speed = 2.0f
    )

    // Trimmed duration is 8000 - 2000 = 6000ms
    assertEquals(6000L, clip.trimmedDurationMs)
    // At 2x speed, effective duration is 6000 / 2 = 3000ms
    assertEquals(3000L, clip.effectiveDurationMs)
  }

  @Test
  fun `verify project entity serialization and deserialization roundtrip`() {
    val project = ProjectModel(
      id = "proj-123",
      title = "Travel Vlog 2026",
      aspectRatio = AspectRatio.RATIO_9_16,
      exportResolution = ExportResolution.RES_1080P,
      clips = listOf(
        ClipModel(
          id = "clip-1",
          sourceUri = "content://media/test.mp4",
          displayName = "Scene 1",
          originalDurationMs = 5000L,
          startMs = 500L,
          endMs = 4500L,
          speed = 1.0f,
          rotationDegrees = 90,
          scaleMode = ScaleMode.FIT
        )
      ),
      textOverlays = listOf(
        TextOverlay(
          id = "text-1",
          text = "Intro Scene",
          colorHex = 0xFF00E5FF,
          fontSizeSp = 28f,
          positionY = 0.5f,
          startMs = 0L,
          endMs = 3000L
        )
      )
    )

    val entity = ProjectEntity.fromDomain(project)
    val restored = entity.toDomain()

    assertEquals("proj-123", restored.id)
    assertEquals("Travel Vlog 2026", restored.title)
    assertEquals(AspectRatio.RATIO_9_16, restored.aspectRatio)
    assertEquals(ExportResolution.RES_1080P, restored.exportResolution)
    assertEquals(1, restored.clips.size)
    assertEquals("Scene 1", restored.clips[0].displayName)
    assertEquals(90, restored.clips[0].rotationDegrees)
    assertEquals(1, restored.textOverlays.size)
    assertEquals("Intro Scene", restored.textOverlays[0].text)
  }
}
