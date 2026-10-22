package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ExportEntity
import com.example.data.local.ProjectEntity
import com.example.data.model.ExportedVideo
import com.example.data.model.ProjectModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(private val database: AppDatabase) {

  val allProjects: Flow<List<ProjectModel>> =
    database.projectDao().getAllProjects().map { list -> list.map { it.toDomain() } }

  val savedProjects: Flow<List<ProjectModel>> =
    database.projectDao().getSavedProjects().map { list -> list.map { it.toDomain() } }

  val draftProjects: Flow<List<ProjectModel>> =
    database.projectDao().getDraftProjects().map { list -> list.map { it.toDomain() } }

  val allExports: Flow<List<ExportedVideo>> =
    database.exportDao().getAllExports().map { list -> list.map { it.toDomain() } }

  suspend fun getProjectById(id: String): ProjectModel? {
    return database.projectDao().getProjectById(id)?.toDomain()
  }

  suspend fun saveProject(project: ProjectModel) {
    val updated = project.copy(updatedAt = System.currentTimeMillis())
    database.projectDao().upsertProject(ProjectEntity.fromDomain(updated))
  }

  suspend fun deleteProject(id: String) {
    database.projectDao().deleteProjectById(id)
  }

  suspend fun saveExport(export: ExportedVideo) {
    database.exportDao().insertExport(ExportEntity.fromDomain(export))
  }

  suspend fun deleteExport(id: String) {
    database.exportDao().deleteExportById(id)
  }
}
