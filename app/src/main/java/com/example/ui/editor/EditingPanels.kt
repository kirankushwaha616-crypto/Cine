package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AspectRatio
import com.example.data.model.ClipModel
import com.example.data.model.ProjectModel
import com.example.data.model.TextOverlay
import com.example.ui.theme.CineCutAmber
import com.example.ui.theme.CineCutCoral
import com.example.ui.theme.CineCutCyan
import com.example.ui.theme.CineCutPurple
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun TrimPanel(
  clip: ClipModel,
  clipIndex: Int,
  currentPlayheadMs: Long,
  onUpdateTrim: (Int, Long, Long) -> Unit,
  onResetTrim: (Int) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp)
      .testTag("trim_panel")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Trim #${clipIndex + 1}: ${clip.displayName}",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = StudioTextPrimary
      )
      OutlinedButton(
        onClick = { onResetTrim(clipIndex) },
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = StudioTextSecondary)
        Spacer(modifier = Modifier.width(4.dp))
        Text("Reset", fontSize = 11.sp, color = StudioTextSecondary)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Start Trim Slider
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "Start: ${formatDetailedTime(clip.startMs)}",
        style = MaterialTheme.typography.labelSmall,
        color = CineCutCyan,
        modifier = Modifier.width(90.dp)
      )
      Slider(
        value = clip.startMs.toFloat(),
        onValueChange = { newStart ->
          onUpdateTrim(clipIndex, newStart.toLong(), clip.endMs)
        },
        valueRange = 0f..(clip.endMs - 100L).coerceAtLeast(100L).toFloat(),
        modifier = Modifier
          .weight(1f)
          .testTag("trim_start_slider"),
        colors = SliderDefaults.colors(thumbColor = CineCutCyan, activeTrackColor = CineCutCyan)
      )
    }

    // End Trim Slider
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "End: ${formatDetailedTime(clip.endMs)}",
        style = MaterialTheme.typography.labelSmall,
        color = CineCutPurple,
        modifier = Modifier.width(90.dp)
      )
      Slider(
        value = clip.endMs.toFloat(),
        onValueChange = { newEnd ->
          onUpdateTrim(clipIndex, clip.startMs, newEnd.toLong())
        },
        valueRange = (clip.startMs + 100L).toFloat()..clip.originalDurationMs.toFloat(),
        modifier = Modifier
          .weight(1f)
          .testTag("trim_end_slider"),
        colors = SliderDefaults.colors(thumbColor = CineCutPurple, activeTrackColor = CineCutPurple)
      )
    }

    Text(
      text = "Trimmed duration: ${formatDetailedTime(clip.trimmedDurationMs)} (Original: ${formatDetailedTime(clip.originalDurationMs)})",
      style = MaterialTheme.typography.bodySmall,
      color = StudioTextSecondary
    )
  }
}

@Composable
fun SplitPanel(
  currentPlayheadMs: Long,
  onSplitClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp)
      .testTag("split_panel"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "Split Selected Clip",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = StudioTextPrimary
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = "Divides the current clip into two segments at the playhead position: ${formatDetailedTime(currentPlayheadMs)}",
      style = MaterialTheme.typography.bodySmall,
      color = StudioTextSecondary,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(modifier = Modifier.height(12.dp))
    Button(
      onClick = onSplitClick,
      colors = ButtonDefaults.buttonColors(containerColor = CineCutPurple),
      shape = RoundedCornerShape(10.dp),
      modifier = Modifier.testTag("confirm_split_button")
    ) {
      Text("Split at Playhead")
    }
  }
}

@Composable
fun RatioPanel(
  currentRatio: AspectRatio,
  onSelectRatio: (AspectRatio) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp)
      .testTag("ratio_panel")
  ) {
    Text(
      text = "Framing & Aspect Ratio",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = StudioTextPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(AspectRatio.values()) { ratio ->
        FilterChip(
          selected = ratio == currentRatio,
          onClick = { onSelectRatio(ratio) },
          label = { Text(ratio.displayName) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CineCutPurple,
            selectedLabelColor = Color.White
          ),
          modifier = Modifier.testTag("chip_ratio_${ratio.name}")
        )
      }
    }
  }
}

@Composable
fun SpeedPanel(
  currentSpeed: Float,
  onSelectSpeed: (Float) -> Unit
) {
  val speedPresets = listOf(0.5f, 1.0f, 1.5f, 2.0f)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp)
      .testTag("speed_panel")
  ) {
    Text(
      text = "Video Playback Speed",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = StudioTextPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      speedPresets.forEach { speed ->
        FilterChip(
          selected = speed == currentSpeed,
          onClick = { onSelectSpeed(speed) },
          label = { Text("${speed}x") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CineCutCyan,
            selectedLabelColor = Color.Black
          ),
          modifier = Modifier.testTag("chip_speed_$speed")
        )
      }
    }
  }
}

@Composable
fun TextOverlayPanel(
  textOverlays: List<TextOverlay>,
  onAddTextClick: () -> Unit,
  onEditTextClick: (TextOverlay) -> Unit,
  onDeleteTextClick: (String) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp)
      .testTag("text_panel")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Text Overlays",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = StudioTextPrimary
      )
      Button(
        onClick = onAddTextClick,
        colors = ButtonDefaults.buttonColors(containerColor = CineCutPurple),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("add_text_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Text", fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (textOverlays.isEmpty()) {
      Text(
        text = "No text overlays added. Tap \"Add Text\" to insert titles or captions.",
        style = MaterialTheme.typography.bodySmall,
        color = StudioTextSecondary
      )
    } else {
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(textOverlays, key = { it.id }) { item ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(StudioSurfaceElevated)
              .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Column {
                Text(
                  text = item.text,
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color(item.colorHex),
                  maxLines = 1
                )
                Text(
                  text = "${formatDetailedTime(item.startMs)} - ${formatDetailedTime(item.endMs)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = StudioTextSecondary,
                  fontSize = 10.sp
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              IconButton(
                onClick = { onEditTextClick(item) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit text", tint = CineCutCyan, modifier = Modifier.size(16.dp))
              }
              IconButton(
                onClick = { onDeleteTextClick(item.id) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete text", tint = CineCutCoral, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun AudioPanel(
  project: ProjectModel,
  onSelectMusicClick: () -> Unit,
  onRemoveMusicClick: () -> Unit,
  onBgmVolumeChange: (Float) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp)
      .testTag("audio_panel")
  ) {
    Text(
      text = "Background Music",
      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
      color = StudioTextPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (project.audioSettings.bgmUri == null) {
      Button(
        onClick = onSelectMusicClick,
        colors = ButtonDefaults.buttonColors(containerColor = CineCutAmber),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.testTag("select_music_button")
      ) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Select Audio File", color = Color.Black, fontWeight = FontWeight.Bold)
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = project.audioSettings.bgmTitle ?: "Audio Track",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = CineCutAmber
          )
          Text(
            text = "Volume: ${(project.audioSettings.bgmVolume * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextSecondary
          )
        }
        IconButton(onClick = onRemoveMusicClick) {
          Icon(Icons.Default.Delete, contentDescription = "Remove music", tint = CineCutCoral)
        }
      }

      Slider(
        value = project.audioSettings.bgmVolume,
        onValueChange = onBgmVolumeChange,
        valueRange = 0f..1f,
        colors = SliderDefaults.colors(thumbColor = CineCutAmber, activeTrackColor = CineCutAmber),
        modifier = Modifier.testTag("bgm_volume_slider")
      )
    }
  }
}

@Composable
fun VolumePanel(
  project: ProjectModel,
  onVolumeChange: (Float) -> Unit,
  onToggleMute: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp)
      .testTag("volume_panel")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Original Video Audio",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = StudioTextPrimary
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = if (project.audioSettings.isOriginalAudioMuted) "Muted" else "Mute",
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextSecondary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Switch(
          checked = project.audioSettings.isOriginalAudioMuted,
          onCheckedChange = { onToggleMute() },
          colors = SwitchDefaults.colors(checkedThumbColor = CineCutCoral),
          modifier = Modifier.testTag("mute_switch")
        )
      }
    }

    if (!project.audioSettings.isOriginalAudioMuted) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.VolumeDown, contentDescription = null, tint = StudioTextSecondary, modifier = Modifier.size(18.dp))
        Slider(
          value = project.audioSettings.originalVideoVolume,
          onValueChange = onVolumeChange,
          valueRange = 0f..2f,
          colors = SliderDefaults.colors(thumbColor = CineCutCyan, activeTrackColor = CineCutCyan),
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
            .testTag("original_volume_slider")
        )
        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = StudioTextSecondary, modifier = Modifier.size(18.dp))
      }
      Text(
        text = "Level: ${(project.audioSettings.originalVideoVolume * 100).toInt()}%",
        style = MaterialTheme.typography.labelSmall,
        color = CineCutCyan
      )
    }
  }
}

@Composable
fun AdjustPanel(
  project: ProjectModel,
  onAdjustmentsChange: (Float, Float, Float) -> Unit,
  onResetAdjustments: () -> Unit
) {
  val adj = project.adjustments

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp)
      .testTag("adjust_panel")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Visual Adjustments",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = StudioTextPrimary
      )
      OutlinedButton(
        onClick = onResetAdjustments,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
      ) {
        Text("Reset", fontSize = 11.sp, color = StudioTextSecondary)
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Brightness
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("Brightness", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary, modifier = Modifier.width(76.dp))
      Slider(
        value = adj.brightness,
        onValueChange = { onAdjustmentsChange(it, adj.contrast, adj.saturation) },
        valueRange = -0.5f..0.5f,
        colors = SliderDefaults.colors(thumbColor = CineCutCyan, activeTrackColor = CineCutCyan),
        modifier = Modifier.weight(1f).testTag("brightness_slider")
      )
      Text("${(adj.brightness * 100).toInt()}", style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary, modifier = Modifier.width(36.dp))
    }

    // Contrast
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("Contrast", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary, modifier = Modifier.width(76.dp))
      Slider(
        value = adj.contrast,
        onValueChange = { onAdjustmentsChange(adj.brightness, it, adj.saturation) },
        valueRange = 0.5f..1.5f,
        colors = SliderDefaults.colors(thumbColor = CineCutPurple, activeTrackColor = CineCutPurple),
        modifier = Modifier.weight(1f).testTag("contrast_slider")
      )
      Text("${(adj.contrast * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary, modifier = Modifier.width(36.dp))
    }

    // Saturation
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text("Saturation", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary, modifier = Modifier.width(76.dp))
      Slider(
        value = adj.saturation,
        onValueChange = { onAdjustmentsChange(adj.brightness, adj.contrast, it) },
        valueRange = 0f..2f,
        colors = SliderDefaults.colors(thumbColor = CineCutAmber, activeTrackColor = CineCutAmber),
        modifier = Modifier.weight(1f).testTag("saturation_slider")
      )
      Text("${(adj.saturation * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = StudioTextPrimary, modifier = Modifier.width(36.dp))
    }
  }
}

@Composable
fun ReorderPanel(
  clipCount: Int,
  selectedIndex: Int,
  onMoveLeft: () -> Unit,
  onMoveRight: () -> Unit,
  onDeleteClip: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp)
      .testTag("reorder_panel"),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      OutlinedButton(
        onClick = onMoveLeft,
        enabled = selectedIndex > 0,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.testTag("move_clip_left")
      ) {
        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Move Left")
      }
      Spacer(modifier = Modifier.width(8.dp))
      OutlinedButton(
        onClick = onMoveRight,
        enabled = selectedIndex < clipCount - 1,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
        modifier = Modifier.testTag("move_clip_right")
      ) {
        Text("Move Right")
        Spacer(modifier = Modifier.width(4.dp))
        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
      }
    }

    if (clipCount > 1) {
      Button(
        onClick = onDeleteClip,
        colors = ButtonDefaults.buttonColors(containerColor = CineCutCoral),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("delete_clip_button")
      ) {
        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Delete Clip")
      }
    }
  }
}
