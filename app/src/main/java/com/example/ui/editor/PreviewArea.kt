package com.example.ui.editor

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.AspectRatio
import com.example.data.model.ProjectModel
import com.example.data.model.TextOverlay
import com.example.ui.theme.CineCutCyan
import com.example.ui.theme.PlayheadColor
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun PreviewArea(
  player: ExoPlayer,
  project: ProjectModel,
  isPlaying: Boolean,
  currentPositionMs: Long,
  activeClipRotation: Int,
  onTogglePlayPause: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .background(StudioBackground),
    contentAlignment = Alignment.Center
  ) {
    // Aspect ratio constrained container
    val targetRatio = when (project.aspectRatio) {
      AspectRatio.RATIO_16_9 -> 16f / 9f
      AspectRatio.RATIO_9_16 -> 9f / 16f
      AspectRatio.RATIO_1_1 -> 1f
      AspectRatio.RATIO_4_5 -> 4f / 5f
      AspectRatio.ORIGINAL -> 16f / 9f
    }

    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      contentAlignment = Alignment.Center
    ) {
      val containerWidth = maxWidth
      val containerHeight = maxHeight

      Box(
        modifier = Modifier
          .aspectRatio(targetRatio, matchHeightConstraintsFirst = targetRatio < 1f)
          .clip(RoundedCornerShape(12.dp))
          .background(Color.Black)
          .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
          .rotate(activeClipRotation.toFloat())
          .clickable(onClick = onTogglePlayPause)
          .testTag("preview_surface"),
        contentAlignment = Alignment.Center
      ) {
        // ExoPlayer view
        AndroidView(
          factory = { context ->
            PlayerView(context).apply {
              this.player = player
              useController = false
              resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
              layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
            }
          },
          update = { view ->
            if (view.player != player) {
              view.player = player
            }
          },
          modifier = Modifier.fillMaxSize()
        )

        // Text Overlays
        val activeTexts = remember(project.textOverlays, currentPositionMs) {
          project.textOverlays.filter { currentPositionMs in it.startMs..it.endMs }
        }

        for (textOverlay in activeTexts) {
          RenderTextOverlay(
            overlay = textOverlay,
            containerWidthPx = containerWidth.value,
            containerHeightPx = containerHeight.value
          )
        }
      }

      // Center Play / Pause Indicator Button
      if (!isPlaying) {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(1.dp, CineCutCyan.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onTogglePlayPause)
            .testTag("play_pause_overlay_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
          )
        }
      }

      // Timecode Pill in bottom-start corner
      Box(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(12.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color.Black.copy(alpha = 0.7f))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(if (isPlaying) Color.Green else PlayheadColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${formatDetailedTime(currentPositionMs)} / ${formatDetailedTime(project.totalDurationMs)}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            fontSize = 11.sp
          )
        }
      }

      // Aspect Ratio Label Pill in top-end corner
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(12.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(Color.Black.copy(alpha = 0.7f))
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          text = project.aspectRatio.displayName,
          style = MaterialTheme.typography.labelSmall,
          color = CineCutCyan,
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}

@Composable
private fun RenderTextOverlay(
  overlay: TextOverlay,
  containerWidthPx: Float,
  containerHeightPx: Float
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    val yOffset = (overlay.positionY * 120).roundToInt()
    val xOffset = (overlay.positionX * 120).roundToInt()

    Box(
      modifier = Modifier
        .offset { IntOffset(xOffset, yOffset) }
        .clip(RoundedCornerShape(6.dp))
        .background(Color.Black.copy(alpha = 0.45f))
        .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Text(
        text = overlay.text,
        color = Color(overlay.colorHex),
        fontSize = overlay.fontSizeSp.sp,
        fontWeight = FontWeight.Bold
      )
    }
  }
}

fun formatDetailedTime(ms: Long): String {
  val totalSec = ms / 1000
  val minutes = totalSec / 60
  val seconds = totalSec % 60
  val tenths = (ms % 1000) / 100
  return "%02d:%02d.%01d".format(minutes, seconds, tenths)
}
