package com.example.service.media

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.ClipModel
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

@OptIn(UnstableApi::class)
class VideoPlayerManager(
  private val context: Context,
  private val scope: CoroutineScope
) {

  private var exoPlayer: ExoPlayer? = null
  private var bgmPlayer: MediaPlayer? = null
  private var progressJob: Job? = null

  private val _currentPositionMs = MutableStateFlow(0L)
  val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

  private val _activeClipIndex = MutableStateFlow(0)
  val activeClipIndex: StateFlow<Int> = _activeClipIndex.asStateFlow()

  private var currentProject: ProjectModel? = null

  fun getPlayer(): ExoPlayer {
    if (exoPlayer == null) {
      val player = ExoPlayer.Builder(context).build()
      player.addListener(object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
          _isPlaying.value = isPlaying
          if (isPlaying) {
            startProgressTracker()
            bgmPlayer?.start()
          } else {
            stopProgressTracker()
            bgmPlayer?.pause()
          }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
          if (playbackState == Player.STATE_ENDED) {
            _isPlaying.value = false
            stopProgressTracker()
            _currentPositionMs.value = currentProject?.totalDurationMs ?: 0L
          }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
          val currentIndex = exoPlayer?.currentMediaItemIndex ?: 0
          _activeClipIndex.value = currentIndex
        }
      })
      exoPlayer = player
    }
    return exoPlayer!!
  }

  fun updateProject(project: ProjectModel) {
    this.currentProject = project
    val player = getPlayer()

    // Configure background audio if present
    setupBgm(project)

    // Apply audio volume to player
    val vol = if (project.audioSettings.isOriginalAudioMuted) 0f else project.audioSettings.originalVideoVolume
    player.volume = vol.coerceIn(0f, 2f)

    if (project.clips.isEmpty()) {
      player.clearMediaItems()
      return
    }

    // Build media items for all clips
    val mediaItems = project.clips.map { clip ->
      val clipping = MediaItem.ClippingConfiguration.Builder()
        .setStartPositionMs(clip.startMs)
        .setEndPositionMs(clip.endMs)
        .build()

      MediaItem.Builder()
        .setUri(Uri.parse(clip.sourceUri))
        .setClippingConfiguration(clipping)
        .build()
    }

    // Keep current index & position if possible
    val wasPlaying = player.isPlaying
    val currentIndex = player.currentMediaItemIndex.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0))
    val currentPosition = player.currentPosition

    player.setMediaItems(mediaItems, currentIndex, currentPosition)
    player.prepare()

    // Set speed on active clip
    val activeClip = project.clips.getOrNull(currentIndex)
    if (activeClip != null) {
      player.setPlaybackSpeed(activeClip.speed)
    }

    if (wasPlaying) {
      player.play()
    }
  }

  private fun setupBgm(project: ProjectModel) {
    val bgmUriStr = project.audioSettings.bgmUri
    if (bgmUriStr == null) {
      bgmPlayer?.release()
      bgmPlayer = null
      return
    }

    try {
      if (bgmPlayer == null) {
        bgmPlayer = MediaPlayer().apply {
          setDataSource(context, Uri.parse(bgmUriStr))
          isLooping = true
          prepare()
        }
      }
      val vol = project.audioSettings.bgmVolume.coerceIn(0f, 1f)
      bgmPlayer?.setVolume(vol, vol)
    } catch (e: Exception) {
      Log.e("VideoPlayerManager", "Failed to setup BGM: ${e.message}")
    }
  }

  fun play() {
    val player = getPlayer()
    if (player.playbackState == Player.STATE_ENDED) {
      seekToTimelinePosition(0L)
    }
    player.play()
    bgmPlayer?.start()
  }

  fun pause() {
    getPlayer().pause()
    bgmPlayer?.pause()
  }

  fun togglePlayPause() {
    if (_isPlaying.value) {
      pause()
    } else {
      play()
    }
  }

  fun seekToClip(clipIndex: Int, clipOffsetMs: Long = 0L) {
    val player = getPlayer()
    val project = currentProject ?: return
    if (clipIndex in project.clips.indices) {
      val clip = project.clips[clipIndex]
      player.setPlaybackSpeed(clip.speed)
      player.seekTo(clipIndex, clipOffsetMs)
      _activeClipIndex.value = clipIndex
      updateTimelinePosition()
    }
  }

  fun seekToTimelinePosition(timelineMs: Long) {
    val project = currentProject ?: return
    var accumulated = 0L

    for (i in project.clips.indices) {
      val clip = project.clips[i]
      val clipDuration = clip.effectiveDurationMs
      if (timelineMs < accumulated + clipDuration || i == project.clips.lastIndex) {
        val offsetInClipTimeline = (timelineMs - accumulated).coerceAtLeast(0L)
        val offsetInSourceMs = (offsetInClipTimeline * clip.speed).toLong()
        seekToClip(i, offsetInSourceMs)
        _currentPositionMs.value = timelineMs
        return
      }
      accumulated += clipDuration
    }
  }

  fun updateActiveClipSpeed(speed: Float) {
    getPlayer().setPlaybackSpeed(speed)
  }

  private fun startProgressTracker() {
    progressJob?.cancel()
    progressJob = scope.launch(Dispatchers.Main) {
      while (isActive) {
        updateTimelinePosition()
        delay(100L)
      }
    }
  }

  private fun stopProgressTracker() {
    progressJob?.cancel()
    progressJob = null
  }

  private fun updateTimelinePosition() {
    val player = exoPlayer ?: return
    val project = currentProject ?: return
    val currentClipIndex = player.currentMediaItemIndex

    if (currentClipIndex !in project.clips.indices) return

    _activeClipIndex.value = currentClipIndex
    var timelineMs = 0L
    for (i in 0 until currentClipIndex) {
      timelineMs += project.clips[i].effectiveDurationMs
    }

    val activeClip = project.clips[currentClipIndex]
    val clipCurrentMs = player.currentPosition
    val effectiveClipProgress = (clipCurrentMs / activeClip.speed.coerceIn(0.25f, 4f)).toLong()
    timelineMs += effectiveClipProgress

    _currentPositionMs.value = timelineMs.coerceIn(0L, project.totalDurationMs)
  }

  fun release() {
    stopProgressTracker()
    exoPlayer?.release()
    exoPlayer = null
    bgmPlayer?.release()
    bgmPlayer = null
  }
}
