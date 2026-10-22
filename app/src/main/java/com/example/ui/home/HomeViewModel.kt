package com.example.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ExportedVideo
import com.example.data.model.ProjectModel
import com.example.data.repository.MediaRepository
import com.example.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
  private val projectRepository: ProjectRepository,
  private val mediaRepository: MediaRepository
) : ViewModel() {

  val savedProjects: StateFlow<List<ProjectModel>> =
    projectRepository.savedProjects.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val draftProjects: StateFlow<List<ProjectModel>> =
    projectRepository.draftProjects.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val exportedVideos: StateFlow<List<ExportedVideo>> =
    projectRepository.allExports.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  fun createProjectFromUris(uris: List<Uri>, onProjectCreated: (String) -> Unit) {
    if (uris.isEmpty()) return
    viewModelScope.launch {
      _isLoading.value = true
      try {
        val clips = uris.map { uri ->
          mediaRepository.createClipFromUri(uri)
        }
        val project = ProjectModel(
          title = "Project ${System.currentTimeMillis() % 10000}",
          isDraft = true,
          clips = clips
        )
        projectRepository.saveProject(project)
        onProjectCreated(project.id)
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun createDemoProject(onProjectCreated: (String) -> Unit) {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        val sampleClip = mediaRepository.createSampleDemoMedia()
        val project = ProjectModel(
          title = "Demo Video Project",
          isDraft = true,
          clips = listOf(sampleClip)
        )
        projectRepository.saveProject(project)
        onProjectCreated(project.id)
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun deleteProject(id: String) {
    viewModelScope.launch {
      projectRepository.deleteProject(id)
    }
  }

  fun deleteExport(id: String) {
    viewModelScope.launch {
      projectRepository.deleteExport(id)
    }
  }

  fun renameProject(project: ProjectModel, newTitle: String) {
    if (newTitle.isBlank()) return
    viewModelScope.launch {
      val updated = project.copy(title = newTitle.trim())
      projectRepository.saveProject(updated)
    }
  }
}
