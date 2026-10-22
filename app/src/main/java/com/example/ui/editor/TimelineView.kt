package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClipModel
import com.example.data.model.ProjectModel
import com.example.ui.theme.CineCutAmber
import com.example.ui.theme.CineCutCyan
import com.example.ui.theme.CineCutPurple
import com.example.ui.theme.PlayheadColor
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderActive
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.TimelineBackground
import com.example.ui.theme.TimelineTrack

@Composable
fun TimelineView(
  project: ProjectModel,
  selectedClipIndex: Int,
  currentPositionMs: Long,
  onSelectClip: (Int) -> Unit,
  onSeekTo: (Long) -> Unit,
  onAddMediaClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val totalDuration = project.totalDurationMs.coerceAtLeast(1000L)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(TimelineBackground)
      .padding(vertical = 8.dp)
  ) {
    // 1. Time Scrubber & Quick Progress Slider
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = formatDetailedTime(currentPositionMs),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = PlayheadColor
      )

      Slider(
        value = currentPositionMs.toFloat().coerceIn(0f, totalDuration.toFloat()),
        onValueChange = { onSeekTo(it.toLong()) },
        valueRange = 0f..totalDuration.toFloat(),
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 8.dp)
          .testTag("timeline_scrubber"),
        colors = SliderDefaults.colors(
          thumbColor = PlayheadColor,
          activeTrackColor = PlayheadColor,
          inactiveTrackColor = StudioBorder
        )
      )

      Text(
        text = formatDetailedTime(totalDuration),
        style = MaterialTheme.typography.labelSmall,
        color = StudioTextSecondary
      )
    }

    // 2. Video Clips Track
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      project.clips.forEachIndexed { index, clip ->
        val isSelected = index == selectedClipIndex
        ClipBlock(
          clip = clip,
          index = index,
          isSelected = isSelected,
          onClick = { onSelectClip(index) }
        )
      }

      // Add Media Clip Button
      Box(
        modifier = Modifier
          .size(width = 64.dp, height = 64.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(StudioSurfaceVariant)
          .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
          .clickable(onClick = onAddMediaClick)
          .testTag("timeline_add_clip_button"),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Clip",
            tint = CineCutCyan,
            modifier = Modifier.size(22.dp)
          )
          Text("Add", fontSize = 10.sp, color = CineCutCyan)
        }
      }
    }

    // 3. Audio Track (if present)
    if (project.audioSettings.bgmUri != null) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CineCutAmber.copy(alpha = 0.15f))
            .border(1.dp, CineCutAmber.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = null,
              tint = CineCutAmber,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = project.audioSettings.bgmTitle ?: "Background Music",
              fontSize = 11.sp,
              color = CineCutAmber,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }

    // 4. Text Overlays Track (if present)
    if (project.textOverlays.isNotEmpty()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(CineCutCyan.copy(alpha = 0.12f))
            .border(1.dp, CineCutCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.TextFields,
              contentDescription = null,
              tint = CineCutCyan,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${project.textOverlays.size} Text Overlays",
              fontSize = 11.sp,
              color = CineCutCyan
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ClipBlock(
  clip: ClipModel,
  index: Int,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  // Compute width roughly proportional to duration: min 110dp, max 240dp
  val blockWidth = (110 + (clip.effectiveDurationMs / 1000) * 12).coerceIn(110L, 240L).toInt().dp

  Box(
    modifier = Modifier
      .size(width = blockWidth, height = 64.dp)
      .clip(RoundedCornerShape(10.dp))
      .background(if (isSelected) StudioSurfaceElevated else StudioSurfaceVariant)
      .border(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) StudioBorderActive else StudioBorder,
        shape = RoundedCornerShape(10.dp)
      )
      .clickable(onClick = onClick)
      .padding(8.dp)
      .testTag("clip_block_$index"),
    contentAlignment = Alignment.CenterStart
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "#${index + 1} ${clip.displayName}",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = if (isSelected) CineCutCyan else StudioTextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f)
        )

        if (clip.speed != 1.0f) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(CineCutPurple.copy(alpha = 0.3f))
              .padding(horizontal = 4.dp, vertical = 1.dp)
          ) {
            Text(
              text = "${clip.speed}x",
              fontSize = 9.sp,
              color = CineCutPurple,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = formatDetailedTime(clip.effectiveDurationMs),
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextSecondary,
          fontSize = 11.sp
        )

        if (clip.rotationDegrees != 0) {
          Text(
            text = "${clip.rotationDegrees}°",
            style = MaterialTheme.typography.labelSmall,
            color = CineCutAmber,
            fontSize = 10.sp
          )
        }
      }
    }
  }
}
