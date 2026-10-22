package com.example.ui.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AspectRatio
import com.example.data.model.ClipModel
import com.example.data.model.ExportResolution
import com.example.data.model.ProjectModel
import com.example.data.model.TextOverlay
import com.example.data.model.VisualAdjustments
import com.example.data.repository.MediaRepository
import com.example.data.repository.ProjectRepository
import com.example.service.media.ExportState
import com.example.service.media.VideoExportManager
import com.example.service.media.VideoPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class EditorTool(val label: String) {
  NONE("Edit"),
  TRIM("Trim"),
  SPLIT("Split"),
  RATIO("Aspect Ratio"),
  ROTATE("Rotate"),
  SPEED("Speed"),
  TEXT("Text Overlay"),
  AUDIO("Music"),
  VOLUME("Volume"),
  ADJUST("Adjust"),
  REORDER("Reorder")
}

class EditorViewModel(
  val projectId: String,
  private val projectRepository: ProjectRepository,
  private val mediaRepository: MediaRepository,
  context: Context
) : ViewModel() {

  val playerManager = VideoPlayerManager(context, viewModelScope)
  val exportManager = VideoExportManager(context, viewModelScope)

  private val _project = MutableStateFlow(ProjectModel(id = projectId))
  val project: StateFlow<ProjectModel> = _project.asStateFlow()

  private val _selectedClipIndex = MutableStateFlow(0)
  val selectedClipIndex: StateFlow<Int> = _selectedClipIndex.asStateFlow()

  private val _activeTool = MutableStateFlow(EditorTool.NONE)
  val activeTool: StateFlow<EditorTool> = _activeTool.asStateFlow()

  private val _selectedTextOverlayId = MutableStateFlow<String?>(null)
  val selectedTextOverlayId: StateFlow<String?> = _selectedTextOverlayId.asStateFlow()

  // Undo / Redo Stacks
  private val undoStack = mutableListOf<ProjectModel>()
  private val redoStack = mutableListOf<ProjectModel>()

  private val _canUndo = MutableStateFlow(false)
  val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

  private val _canRedo = MutableStateFlow(false)
  val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

  val isPlaying = playerManager.isPlaying
  val currentPositionMs = playerManager.currentPositionMs
  val exportState = exportManager.exportState

  init {
    loadProject()
  }

  private fun loadProject() {
    viewModelScope.launch {
      val loaded = projectRepository.getProjectById(projectId)
      if (loaded != null) {
        _project.value = loaded
        playerManager.updateProject(loaded)
        if (loaded.clips.isNotEmpty()) {
          _selectedClipIndex.value = 0
        }
      }
    }
  }

  private fun pushUndoState(newState: ProjectModel) {
    undoStack.add(_project.value.copy())
    if (undoStack.size > 30) undoStack.removeAt(0)
    redoStack.clear()
    updateUndoRedoStates()
    applyProjectChange(newState)
  }

  private fun applyProjectChange(newState: ProjectModel) {
    _project.value = newState
    playerManager.updateProject(newState)
    viewModelScope.launch {
      projectRepository.saveProject(newState)
    }
  }

  private fun updateUndoRedoStates() {
    _canUndo.value = undoStack.isNotEmpty()
    _canRedo.value = redoStack.isNotEmpty()
  }

  fun undo() {
    if (undoStack.isEmpty()) return
    val current = _project.value.copy()
    redoStack.add(current)
    val previous = undoStack.removeAt(undoStack.lastIndex)
    updateUndoRedoStates()
    _project.value = previous
    playerManager.updateProject(previous)
    viewModelScope.launch {
      projectRepository.saveProject(previous)
    }
  }

  fun redo() {
    if (redoStack.isEmpty()) return
    val current = _project.value.copy()
    undoStack.add(current)
    val next = redoStack.removeAt(redoStack.lastIndex)
    updateUndoRedoStates()
    _project.value = next
    playerManager.updateProject(next)
    viewModelScope.launch {
      projectRepository.saveProject(next)
    }
  }

  fun selectClip(index: Int) {
    val clips = _project.value.clips
    if (index in clips.indices) {
      _selectedClipIndex.value = index
      playerManager.seekToClip(index, 0L)
    }
  }

  fun setActiveTool(tool: EditorTool) {
    _activeTool.value = if (_activeTool.value == tool) EditorTool.NONE else tool
  }

  fun togglePlayPause() {
    playerManager.togglePlayPause()
  }

  fun seekTo(positionMs: Long) {
    playerManager.seekToTimelinePosition(positionMs)
  }

  // --- EDITING OPERATIONS ---

  fun updateTrim(clipIndex: Int, startMs: Long, endMs: Long) {
    val clips = _project.value.clips.toMutableList()
    if (clipIndex !in clips.indices) return
    val clip = clips[clipIndex]

    val validStart = startMs.coerceIn(0L, clip.originalDurationMs - 100L)
    val validEnd = endMs.coerceIn(validStart + 100L, clip.originalDurationMs)

    clips[clipIndex] = clip.copy(startMs = validStart, endMs = validEnd)
    pushUndoState(_project.value.copy(clips = clips))
  }

  fun resetTrim(clipIndex: Int) {
    val clips = _project.value.clips.toMutableList()
    if (clipIndex !in clips.indices) return
    val clip = clips[clipIndex]
    clips[clipIndex] = clip.copy(startMs = 0L, endMs = clip.originalDurationMs)
    pushUndoState(_project.value.copy(clips = clips))
  }

  fun splitSelectedClip() {
    val clips = _project.value.clips.toMutableList()
    val clipIdx = _selectedClipIndex.value
    if (clipIdx !in clips.indices) return

    val clip = clips[clipIdx]
    val playhead = currentPositionMs.value

    // Calculate timeline start of selected clip
    var clipTimelineStart = 0L
    for (i in 0 until clipIdx) {
      clipTimelineStart += clips[i].effectiveDurationMs
    }

    val offsetInTimeline = playhead - clipTimelineStart
    val offsetInClip = (offsetInTimeline * clip.speed).toLong()

    val splitPoint = clip.startMs + offsetInClip
    // Validate split point is inside clip range with at least 200ms padding
    if (splitPoint <= clip.startMs + 200L || splitPoint >= clip.endMs - 200L) {
      return
    }

    val firstSegment = clip.copy(
      endMs = splitPoint,
      displayName = "${clip.displayName} (Part 1)"
    )
    val secondSegment = clip.copy(
      id = java.util.UUID.randomUUID().toString(),
      startMs = splitPoint,
      displayName = "${clip.displayName} (Part 2)"
    )

    clips.removeAt(clipIdx)
    clips.add(clipIdx, secondSegment)
    clips.add(clipIdx, firstSegment)

    pushUndoState(_project.value.copy(clips = clips))
  }

  fun setAspectRatio(ratio: AspectRatio) {
    pushUndoState(_project.value.copy(aspectRatio = ratio))
  }

  fun rotateSelectedClip() {
    val clips = _project.value.clips.toMutableList()
    val clipIdx = _selectedClipIndex.value
    if (clipIdx !in clips.indices) return
    val clip = clips[clipIdx]
    val nextRotation = (clip.rotationDegrees + 90) % 360
    clips[clipIdx] = clip.copy(rotationDegrees = nextRotation)
    pushUndoState(_project.value.copy(clips = clips))
  }

  fun setSpeed(speed: Float) {
    val clips = _project.value.clips.toMutableList()
    val clipIdx = _selectedClipIndex.value
    if (clipIdx !in clips.indices) return
    val clip = clips[clipIdx]
    clips[clipIdx] = clip.copy(speed = speed)
    playerManager.updateActiveClipSpeed(speed)
    pushUndoState(_project.value.copy(clips = clips))
  }

  fun addTextOverlay(
    text: String,
    colorHex: Long = 0xFFFFFFFF,
    fontSizeSp: Float = 24f,
    positionY: Float = 0.6f,
    startMs: Long = currentPositionMs.value,
    endMs: Long = (currentPositionMs.value + 4000L).coerceAtMost(_project.value.totalDurationMs.coerceAtLeast(4000L))
  ) {
    if (text.isBlank()) return
    val list = _project.value.textOverlays.toMutableList()
    val overlay = TextOverlay(
      text = text.trim(),
      colorHex = colorHex,
      fontSizeSp = fontSizeSp,
      positionY = positionY,
      startMs = startMs,
      endMs = endMs
    )
    list.add(overlay)
    _selectedTextOverlayId.value = overlay.id
    pushUndoState(_project.value.copy(textOverlays = list))
  }

  fun updateTextOverlay(overlay: TextOverlay) {
    val list = _project.value.textOverlays.toMutableList()
    val idx = list.indexOfFirst { it.id == overlay.id }
    if (idx != -1) {
      list[idx] = overlay
      pushUndoState(_project.value.copy(textOverlays = list))
    }
  }

  fun deleteTextOverlay(id: String) {
    val list = _project.value.textOverlays.toMutableList()
    list.removeAll { it.id == id }
    if (_selectedTextOverlayId.value == id) _selectedTextOverlayId.value = null
    pushUndoState(_project.value.copy(textOverlays = list))
  }

  fun setBgm(uri: Uri) {
    viewModelScope.launch {
      val title = mediaRepository.getAudioTitle(uri)
      val audio = _project.value.audioSettings.copy(
        bgmUri = uri.toString(),
        bgmTitle = title
      )
      pushUndoState(_project.value.copy(audioSettings = audio))
    }
  }

  fun removeBgm() {
    val audio = _project.value.audioSettings.copy(
      bgmUri = null,
      bgmTitle = null
    )
    pushUndoState(_project.value.copy(audioSettings = audio))
  }

  fun setBgmVolume(volume: Float) {
    val audio = _project.value.audioSettings.copy(bgmVolume = volume)
    pushUndoState(_project.value.copy(audioSettings = audio))
  }

  fun setOriginalVideoVolume(volume: Float) {
    val audio = _project.value.audioSettings.copy(originalVideoVolume = volume)
    pushUndoState(_project.value.copy(audioSettings = audio))
  }

  fun toggleOriginalAudioMute() {
    val currentMuted = _project.value.audioSettings.isOriginalAudioMuted
    val audio = _project.value.audioSettings.copy(isOriginalAudioMuted = !currentMuted)
    pushUndoState(_project.value.copy(audioSettings = audio))
  }

  fun setVisualAdjustments(brightness: Float, contrast: Float, saturation: Float) {
    val adjustments = VisualAdjustments(brightness, contrast, saturation)
    pushUndoState(_project.value.copy(adjustments = adjustments))
  }

  fun resetVisualAdjustments() {
    pushUndoState(_project.value.copy(adjustments = VisualAdjustments()))
  }

  fun deleteSelectedClip() {
    val clips = _project.value.clips.toMutableList()
    if (clips.size <= 1) return // Keep at least 1 clip
    val idx = _selectedClipIndex.value
    if (idx in clips.indices) {
      clips.removeAt(idx)
      _selectedClipIndex.value = idx.coerceAtMost(clips.lastIndex)
      pushUndoState(_project.value.copy(clips = clips))
    }
  }

  fun moveClip(from: Int, to: Int) {
    val clips = _project.value.clips.toMutableList()
    if (from in clips.indices && to in clips.indices && from != to) {
      val item = clips.removeAt(from)
      clips.add(to, item)
      _selectedClipIndex.value = to
      pushUndoState(_project.value.copy(clips = clips))
    }
  }

  fun addClips(uris: List<Uri>) {
    viewModelScope.launch {
      val newClips = uris.map { mediaRepository.createClipFromUri(it) }
      val combined = _project.value.clips + newClips
      pushUndoState(_project.value.copy(clips = combined))
    }
  }

  fun renameProject(newTitle: String) {
    if (newTitle.isBlank()) return
    val updated = _project.value.copy(title = newTitle.trim())
    pushUndoState(updated)
  }

  fun setExportResolution(resolution: ExportResolution) {
    _project.value = _project.value.copy(exportResolution = resolution)
  }

  fun startExport() {
    viewModelScope.launch {
      // Pause playback during export
      playerManager.pause()
      exportManager.exportProject(_project.value)
    }
  }

  fun cancelExport() {
    exportManager.cancelExport()
  }

  fun onExportFinished(exported: com.example.data.model.ExportedVideo) {
    viewModelScope.launch {
      projectRepository.saveExport(exported)
      // Also mark project as not draft
      val finishedProject = _project.value.copy(isDraft = false)
      projectRepository.saveProject(finishedProject)
      _project.value = finishedProject
    }
  }

  override fun onCleared() {
    super.onCleared()
    playerManager.release()
  }
}
