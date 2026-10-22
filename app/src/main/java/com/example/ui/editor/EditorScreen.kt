package com.example.ui.editor

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ExportResolution
import com.example.data.model.TextOverlay
import com.example.service.media.ExportState
import com.example.ui.theme.CineCutAmber
import com.example.ui.theme.CineCutCoral
import com.example.ui.theme.CineCutCyan
import com.example.ui.theme.CineCutPurple
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
  viewModel: EditorViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val project by viewModel.project.collectAsState()
  val selectedClipIndex by viewModel.selectedClipIndex.collectAsState()
  val activeTool by viewModel.activeTool.collectAsState()
  val isPlaying by viewModel.isPlaying.collectAsState()
  val currentPositionMs by viewModel.currentPositionMs.collectAsState()
  val canUndo by viewModel.canUndo.collectAsState()
  val canRedo by viewModel.canRedo.collectAsState()
  val exportState by viewModel.exportState.collectAsState()

  var showExportDialog by remember { mutableStateOf(false) }
  var showRenameDialog by remember { mutableStateOf(false) }
  var renameInput by remember { mutableStateOf(project.title) }

  var editingTextOverlay by remember { mutableStateOf<TextOverlay?>(null) }
  var isNewTextOverlay by remember { mutableStateOf(false) }

  // Custom state back handler
  BackHandler {
    if (activeTool != EditorTool.NONE) {
      viewModel.setActiveTool(EditorTool.NONE)
    } else {
      onNavigateBack()
    }
  }

  // Audio picker launcher
  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let { viewModel.setBgm(it) }
  }

  // Media picker to add more clips
  val mediaPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris ->
    if (uris.isNotEmpty()) {
      viewModel.addClips(uris)
    }
  }

  val activeClip = project.clips.getOrNull(selectedClipIndex)

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = StudioBackground,
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
              renameInput = project.title
              showRenameDialog = true
            }
          ) {
            Text(
              text = project.title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = StudioTextPrimary,
              maxLines = 1,
              modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Rename Project",
              tint = StudioTextSecondary,
              modifier = Modifier.size(16.dp)
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("editor_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Home",
              tint = StudioTextPrimary
            )
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.undo() },
            enabled = canUndo,
            modifier = Modifier.testTag("editor_undo_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Undo,
              contentDescription = stringResource(R.string.action_undo),
              tint = if (canUndo) StudioTextPrimary else StudioTextMuted
            )
          }
          IconButton(
            onClick = { viewModel.redo() },
            enabled = canRedo,
            modifier = Modifier.testTag("editor_redo_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Redo,
              contentDescription = stringResource(R.string.action_redo),
              tint = if (canRedo) StudioTextPrimary else StudioTextMuted
            )
          }
          Button(
            onClick = { showExportDialog = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = CineCutCyan,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .padding(end = 8.dp)
              .testTag("editor_export_button")
          ) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = stringResource(R.string.action_export),
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = StudioSurface)
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // 1. Preview Area (Upper half)
      PreviewArea(
        player = viewModel.playerManager.getPlayer(),
        project = project,
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        activeClipRotation = activeClip?.rotationDegrees ?: 0,
        onTogglePlayPause = { viewModel.togglePlayPause() },
        modifier = Modifier
          .fillMaxWidth()
          .weight(1.1f)
      )

      // 2. Multi-Clip Timeline Area
      TimelineView(
        project = project,
        selectedClipIndex = selectedClipIndex,
        currentPositionMs = currentPositionMs,
        onSelectClip = { viewModel.selectClip(it) },
        onSeekTo = { viewModel.seekTo(it) },
        onAddMediaClick = {
          mediaPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
          )
        },
        modifier = Modifier.fillMaxWidth()
      )

      // 3. Tool Specific Sub-Panel (if active tool selected)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(StudioSurfaceVariant)
      ) {
        when (activeTool) {
          EditorTool.TRIM -> activeClip?.let {
            TrimPanel(
              clip = it,
              clipIndex = selectedClipIndex,
              currentPlayheadMs = currentPositionMs,
              onUpdateTrim = { idx, s, e -> viewModel.updateTrim(idx, s, e) },
              onResetTrim = { idx -> viewModel.resetTrim(idx) }
            )
          }
          EditorTool.SPLIT -> SplitPanel(
            currentPlayheadMs = currentPositionMs,
            onSplitClick = {
              viewModel.splitSelectedClip()
              viewModel.setActiveTool(EditorTool.NONE)
            }
          )
          EditorTool.RATIO -> RatioPanel(
            currentRatio = project.aspectRatio,
            onSelectRatio = { viewModel.setAspectRatio(it) }
          )
          EditorTool.SPEED -> activeClip?.let {
            SpeedPanel(
              currentSpeed = it.speed,
              onSelectSpeed = { s -> viewModel.setSpeed(s) }
            )
          }
          EditorTool.TEXT -> TextOverlayPanel(
            textOverlays = project.textOverlays,
            onAddTextClick = {
              isNewTextOverlay = true
              editingTextOverlay = TextOverlay(
                text = "New Title",
                startMs = currentPositionMs,
                endMs = (currentPositionMs + 4000L).coerceAtMost(project.totalDurationMs.coerceAtLeast(4000L))
              )
            },
            onEditTextClick = {
              isNewTextOverlay = false
              editingTextOverlay = it
            },
            onDeleteTextClick = { viewModel.deleteTextOverlay(it) }
          )
          EditorTool.AUDIO -> AudioPanel(
            project = project,
            onSelectMusicClick = { audioPickerLauncher.launch("audio/*") },
            onRemoveMusicClick = { viewModel.removeBgm() },
            onBgmVolumeChange = { viewModel.setBgmVolume(it) }
          )
          EditorTool.VOLUME -> VolumePanel(
            project = project,
            onVolumeChange = { viewModel.setOriginalVideoVolume(it) },
            onToggleMute = { viewModel.toggleOriginalAudioMute() }
          )
          EditorTool.ADJUST -> AdjustPanel(
            project = project,
            onAdjustmentsChange = { b, c, s -> viewModel.setVisualAdjustments(b, c, s) },
            onResetAdjustments = { viewModel.resetVisualAdjustments() }
          )
          EditorTool.ROTATE -> {
            // Instant rotate handled in button
          }
          EditorTool.REORDER -> ReorderPanel(
            clipCount = project.clips.size,
            selectedIndex = selectedClipIndex,
            onMoveLeft = { viewModel.moveClip(selectedClipIndex, selectedClipIndex - 1) },
            onMoveRight = { viewModel.moveClip(selectedClipIndex, selectedClipIndex + 1) },
            onDeleteClip = {
              viewModel.deleteSelectedClip()
              viewModel.setActiveTool(EditorTool.NONE)
            }
          )
          EditorTool.NONE -> {
            // No sub-panel open
          }
        }
      }

      // 4. Primary Tool Actions Bar (Bottom)
      EditorBottomBar(
        activeTool = activeTool,
        onToolClick = { tool ->
          if (tool == EditorTool.ROTATE) {
            viewModel.rotateSelectedClip()
          } else {
            viewModel.setActiveTool(tool)
          }
        }
      )
    }
  }

  // Rename Dialog
  if (showRenameDialog) {
    AlertDialog(
      onDismissRequest = { showRenameDialog = false },
      containerColor = StudioSurfaceElevated,
      title = { Text("Rename Project", color = StudioTextPrimary) },
      text = {
        OutlinedTextField(
          value = renameInput,
          onValueChange = { renameInput = it },
          label = { Text("Project Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("editor_rename_input")
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.renameProject(renameInput)
            showRenameDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = CineCutPurple)
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRenameDialog = false }) {
          Text("Cancel", color = StudioTextSecondary)
        }
      }
    )
  }

  // Text Overlay Add/Edit Dialog
  editingTextOverlay?.let { overlay ->
    var textValue by remember { mutableStateOf(overlay.text) }
    var colorHexValue by remember { mutableStateOf(overlay.colorHex) }
    var fontSizeValue by remember { mutableStateOf(overlay.fontSizeSp) }
    var posYValue by remember { mutableStateOf(overlay.positionY) }

    AlertDialog(
      onDismissRequest = { editingTextOverlay = null },
      containerColor = StudioSurfaceElevated,
      title = { Text(if (isNewTextOverlay) "Add Text Overlay" else "Edit Text Overlay", color = StudioTextPrimary) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = textValue,
            onValueChange = { textValue = it },
            label = { Text("Caption Text") },
            modifier = Modifier.fillMaxWidth().testTag("overlay_text_input")
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Color palette options
          Text("Color", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 6.dp)
          ) {
            val colors = listOf(0xFFFFFFFF, 0xFF00E5FF, 0xFFFFD600, 0xFFFF5252, 0xFF00E676, 0xFFE040FB)
            colors.forEach { cHex ->
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(Color(cHex))
                  .border(
                    width = if (colorHexValue == cHex) 3.dp else 1.dp,
                    color = if (colorHexValue == cHex) CineCutCyan else StudioBorder,
                    shape = CircleShape
                  )
                  .clickable { colorHexValue = cHex }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Font Size Slider
          Text("Size: ${fontSizeValue.toInt()}sp", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
          Slider(
            value = fontSizeValue,
            onValueChange = { fontSizeValue = it },
            valueRange = 16f..48f,
            colors = SliderDefaults.colors(thumbColor = CineCutCyan, activeTrackColor = CineCutCyan)
          )

          // Vertical Position Slider
          Text("Position (Top to Bottom)", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
          Slider(
            value = posYValue,
            onValueChange = { posYValue = it },
            valueRange = -0.8f..0.8f,
            colors = SliderDefaults.colors(thumbColor = CineCutPurple, activeTrackColor = CineCutPurple)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val updated = overlay.copy(
              text = textValue,
              colorHex = colorHexValue,
              fontSizeSp = fontSizeValue,
              positionY = posYValue
            )
            if (isNewTextOverlay) {
              viewModel.addTextOverlay(
                text = updated.text,
                colorHex = updated.colorHex,
                fontSizeSp = updated.fontSizeSp,
                positionY = updated.positionY,
                startMs = updated.startMs,
                endMs = updated.endMs
              )
            } else {
              viewModel.updateTextOverlay(updated)
            }
            editingTextOverlay = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CineCutPurple)
        ) {
          Text("Apply")
        }
      },
      dismissButton = {
        TextButton(onClick = { editingTextOverlay = null }) {
          Text("Cancel", color = StudioTextSecondary)
        }
      }
    )
  }

  // Export Settings Dialog
  if (showExportDialog) {
    var selectedRes by remember { mutableStateOf(project.exportResolution) }

    AlertDialog(
      onDismissRequest = { showExportDialog = false },
      containerColor = StudioSurfaceElevated,
      title = { Text("Export Video", color = StudioTextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "Choose output resolution for your finalized MP4 video:",
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextSecondary
          )
          Spacer(modifier = Modifier.height(14.dp))

          ExportResolution.values().forEach { res ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { selectedRes = res }
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedRes == res,
                onClick = { selectedRes = res },
                colors = RadioButtonDefaults.colors(selectedColor = CineCutCyan)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(res.label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = StudioTextPrimary)
                Text("${res.width} × ${res.height}", style = MaterialTheme.typography.labelSmall, color = StudioTextSecondary)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.setExportResolution(selectedRes)
            showExportDialog = false
            viewModel.startExport()
          },
          colors = ButtonDefaults.buttonColors(containerColor = CineCutCyan, contentColor = Color.Black),
          modifier = Modifier.testTag("start_export_button")
        ) {
          Text("Start Export", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showExportDialog = false }) {
          Text("Cancel", color = StudioTextSecondary)
        }
      }
    )
  }

  // Export State Dialogs
  when (val state = exportState) {
    is ExportState.InProgress -> {
      AlertDialog(
        onDismissRequest = { /* Cannot dismiss without clicking cancel */ },
        containerColor = StudioSurfaceElevated,
        title = { Text("Exporting Video...", color = StudioTextPrimary) },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            LinearProgressIndicator(
              progress = { state.progressPercent / 100f },
              modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
              color = CineCutCyan,
              trackColor = StudioBorder
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "${state.progressPercent}% Completed",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = CineCutCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Rendering frames and mixing audio tracks on-device...",
              style = MaterialTheme.typography.bodySmall,
              color = StudioTextSecondary
            )
          }
        },
        confirmButton = {
          OutlinedButton(
            onClick = { viewModel.cancelExport() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CineCutCoral)
          ) {
            Text("Cancel")
          }
        }
      )
    }
    is ExportState.Success -> {
      AlertDialog(
        onDismissRequest = {
          viewModel.onExportFinished(state.exportedVideo)
          viewModel.exportManager.resetState()
        },
        containerColor = StudioSurfaceElevated,
        icon = {
          Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CineCutCyan, modifier = Modifier.size(48.dp))
        },
        title = { Text("Export Completed!", color = StudioTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "Your video has been saved to device storage.",
              style = MaterialTheme.typography.bodyMedium,
              color = StudioTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Resolution: ${state.exportedVideo.resolution}",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextPrimary
            )
            Text(
              text = "Duration: ${formatDetailedTime(state.exportedVideo.durationMs)}",
              style = MaterialTheme.typography.labelSmall,
              color = StudioTextPrimary
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(state.exportedVideo.filePath), "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              runCatching { context.startActivity(intent) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CineCutCyan, contentColor = Color.Black)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Play Video", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          Row {
            OutlinedButton(
              onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                  type = "video/*"
                  putExtra(Intent.EXTRA_STREAM, Uri.parse(state.exportedVideo.filePath))
                  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runCatching { context.startActivity(Intent.createChooser(intent, "Share Finished Video")) }
              }
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Share")
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
              onClick = {
                viewModel.onExportFinished(state.exportedVideo)
                viewModel.exportManager.resetState()
              }
            ) {
              Text("Done", color = StudioTextSecondary)
            }
          }
        }
      )
    }
    is ExportState.Failed -> {
      AlertDialog(
        onDismissRequest = { viewModel.exportManager.resetState() },
        containerColor = StudioSurfaceElevated,
        icon = {
          Icon(Icons.Default.Error, contentDescription = null, tint = CineCutCoral, modifier = Modifier.size(48.dp))
        },
        title = { Text("Export Failed", color = StudioTextPrimary) },
        text = {
          Text(state.errorMessage, color = StudioTextSecondary)
        },
        confirmButton = {
          Button(
            onClick = { viewModel.exportManager.resetState() },
            colors = ButtonDefaults.buttonColors(containerColor = CineCutPurple)
          ) {
            Text("Dismiss")
          }
        }
      )
    }
    ExportState.Idle -> {}
  }
}

@Composable
private fun EditorBottomBar(
  activeTool: EditorTool,
  onToolClick: (EditorTool) -> Unit
) {
  val tools = listOf(
    ToolItem(EditorTool.TRIM, stringResource(R.string.action_trim), Icons.Default.ContentCut),
    ToolItem(EditorTool.SPLIT, stringResource(R.string.action_split), Icons.Default.ContentCut),
    ToolItem(EditorTool.RATIO, stringResource(R.string.action_crop), Icons.Default.AspectRatio),
    ToolItem(EditorTool.ROTATE, stringResource(R.string.action_rotate), Icons.Default.RotateRight),
    ToolItem(EditorTool.SPEED, stringResource(R.string.action_speed), Icons.Default.Speed),
    ToolItem(EditorTool.TEXT, stringResource(R.string.action_text), Icons.Default.TextFields),
    ToolItem(EditorTool.AUDIO, stringResource(R.string.action_audio), Icons.Default.MusicNote),
    ToolItem(EditorTool.VOLUME, stringResource(R.string.action_volume), Icons.Default.VolumeUp),
    ToolItem(EditorTool.ADJUST, stringResource(R.string.action_adjust), Icons.Default.Tune),
    ToolItem(EditorTool.REORDER, "Manage", Icons.Default.Reorder)
  )

  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .background(StudioSurface)
      .padding(vertical = 8.dp, horizontal = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    items(tools) { item ->
      val isSelected = activeTool == item.tool
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(if (isSelected) CineCutPurple.copy(alpha = 0.25f) else Color.Transparent)
          .clickable { onToolClick(item.tool) }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("tool_${item.tool.name.lowercase()}")
      ) {
        Icon(
          imageVector = item.icon,
          contentDescription = item.label,
          tint = if (isSelected) CineCutCyan else StudioTextSecondary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
          text = item.label,
          style = MaterialTheme.typography.labelSmall,
          color = if (isSelected) CineCutCyan else StudioTextSecondary,
          fontSize = 11.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
      }
    }
  }
}

private data class ToolItem(
  val tool: EditorTool,
  val label: String,
  val icon: ImageVector
)
