package com.example.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ExportedVideo
import com.example.data.model.ProjectModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: HomeViewModel,
  onOpenProject: (String) -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val savedProjects by viewModel.savedProjects.collectAsState()
  val draftProjects by viewModel.draftProjects.collectAsState()
  val exportedVideos by viewModel.exportedVideos.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()

  var selectedTabIndex by remember { mutableIntStateOf(0) }
  var projectToRename by remember { mutableStateOf<ProjectModel?>(null) }
  var renameInput by remember { mutableStateOf("") }
  var projectToDelete by remember { mutableStateOf<ProjectModel?>(null) }
  var exportToDelete by remember { mutableStateOf<ExportedVideo?>(null) }

  // Photo Picker launcher for videos and photos
  val mediaPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris ->
    if (uris.isNotEmpty()) {
      viewModel.createProjectFromUris(uris) { newId ->
        onOpenProject(newId)
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = StudioBackground,
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                  Brush.linearGradient(listOf(CineCutPurple, CineCutCyan))
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Movie,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = StudioTextPrimary
              )
              Text(
                text = "Studio Video Editor",
                style = MaterialTheme.typography.labelSmall,
                color = CineCutCyan
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = stringResource(R.string.settings_title),
              tint = StudioTextSecondary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = StudioSurface
        )
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Quick Action Hero Banner
        HeroCreationCard(
          onNewProjectClick = {
            mediaPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
          },
          onDemoProjectClick = {
            viewModel.createDemoProject { newId ->
              onOpenProject(newId)
            }
          }
        )

        // Tabs
        TabRow(
          selectedTabIndex = selectedTabIndex,
          containerColor = StudioSurface,
          contentColor = CineCutCyan,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
              color = CineCutCyan,
              height = 3.dp
            )
          }
        ) {
          Tab(
            selected = selectedTabIndex == 0,
            onClick = { selectedTabIndex = 0 },
            text = {
              Text(
                text = "${stringResource(R.string.tab_recent)} (${savedProjects.size})",
                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
              )
            },
            selectedContentColor = CineCutCyan,
            unselectedContentColor = StudioTextSecondary,
            modifier = Modifier.testTag("tab_projects")
          )
          Tab(
            selected = selectedTabIndex == 1,
            onClick = { selectedTabIndex = 1 },
            text = {
              Text(
                text = "${stringResource(R.string.tab_drafts)} (${draftProjects.size})",
                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
              )
            },
            selectedContentColor = CineCutCyan,
            unselectedContentColor = StudioTextSecondary,
            modifier = Modifier.testTag("tab_drafts")
          )
          Tab(
            selected = selectedTabIndex == 2,
            onClick = { selectedTabIndex = 2 },
            text = {
              Text(
                text = "${stringResource(R.string.tab_exports)} (${exportedVideos.size})",
                fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal
              )
            },
            selectedContentColor = CineCutCyan,
            unselectedContentColor = StudioTextSecondary,
            modifier = Modifier.testTag("tab_exports")
          )
        }

        // Tab Content
        when (selectedTabIndex) {
          0 -> ProjectList(
            projects = savedProjects,
            emptyTitle = stringResource(R.string.empty_projects_title),
            emptySubtitle = stringResource(R.string.empty_projects_subtitle),
            onOpenProject = onOpenProject,
            onRenameProject = {
              projectToRename = it
              renameInput = it.title
            },
            onDeleteProject = { projectToDelete = it },
            onCreateClick = {
              mediaPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
              )
            }
          )
          1 -> ProjectList(
            projects = draftProjects,
            emptyTitle = "No active drafts",
            emptySubtitle = "Unfinished projects and work in progress will be saved here automatically",
            onOpenProject = onOpenProject,
            onRenameProject = {
              projectToRename = it
              renameInput = it.title
            },
            onDeleteProject = { projectToDelete = it },
            onCreateClick = {
              mediaPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
              )
            }
          )
          2 -> ExportList(
            exports = exportedVideos,
            onPlayExport = { export ->
              val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(export.filePath), "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              runCatching { context.startActivity(intent) }
            },
            onShareExport = { export ->
              val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, Uri.parse(export.filePath))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
              runCatching {
                context.startActivity(Intent.createChooser(intent, "Share Finished Video"))
              }
            },
            onDeleteExport = { exportToDelete = it }
          )
        }
      }

      // Loading overlay
      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = CineCutCyan)
        }
      }
    }
  }

  // Rename Dialog
  projectToRename?.let { project ->
    AlertDialog(
      onDismissRequest = { projectToRename = null },
      containerColor = StudioSurfaceElevated,
      title = { Text("Rename Project", color = StudioTextPrimary) },
      text = {
        OutlinedTextField(
          value = renameInput,
          onValueChange = { renameInput = it },
          label = { Text("Project Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("rename_input")
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.renameProject(project, renameInput)
            projectToRename = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CineCutPurple)
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { projectToRename = null }) {
          Text("Cancel", color = StudioTextSecondary)
        }
      }
    )
  }

  // Delete Project Dialog
  projectToDelete?.let { project ->
    AlertDialog(
      onDismissRequest = { projectToDelete = null },
      containerColor = StudioSurfaceElevated,
      title = { Text("Delete Project?", color = StudioTextPrimary) },
      text = {
        Text("Are you sure you want to delete \"${project.title}\"? This action cannot be undone.", color = StudioTextSecondary)
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteProject(project.id)
            projectToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { projectToDelete = null }) {
          Text("Cancel", color = StudioTextSecondary)
        }
      }
    )
  }

  // Delete Export Dialog
  exportToDelete?.let { export ->
    AlertDialog(
      onDismissRequest = { exportToDelete = null },
      containerColor = StudioSurfaceElevated,
      title = { Text("Delete Export Record?", color = StudioTextPrimary) },
      text = {
        Text("Remove \"${export.projectTitle}\" from your exports list?", color = StudioTextSecondary)
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteExport(export.id)
            exportToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { exportToDelete = null }) {
          Text("Cancel", color = StudioTextSecondary)
        }
      }
    )
  }
}

@Composable
private fun HeroCreationCard(
  onNewProjectClick: () -> Unit,
  onDemoProjectClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(CineCutPurple.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = null,
            tint = CineCutPurple,
            modifier = Modifier.size(24.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Create Masterpiece",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = StudioTextPrimary
          )
          Text(
            text = "Select footage or start with sample media",
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = onNewProjectClick,
          modifier = Modifier
            .weight(1.3f)
            .height(48.dp)
            .testTag("new_project_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = CineCutPurple,
            contentColor = Color.White
          )
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.action_new_project),
            fontWeight = FontWeight.SemiBold
          )
        }

        OutlinedButton(
          onClick = onDemoProjectClick,
          modifier = Modifier
            .weight(1.1f)
            .height(48.dp)
            .testTag("demo_project_button"),
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, CineCutCyan.copy(alpha = 0.6f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = CineCutCyan)
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.action_demo_project),
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
          )
        }
      }
    }
  }
}

@Composable
private fun ProjectList(
  projects: List<ProjectModel>,
  emptyTitle: String,
  emptySubtitle: String,
  onOpenProject: (String) -> Unit,
  onRenameProject: (ProjectModel) -> Unit,
  onDeleteProject: (ProjectModel) -> Unit,
  onCreateClick: () -> Unit
) {
  if (projects.isEmpty()) {
    EmptyState(
      icon = Icons.Default.Movie,
      title = emptyTitle,
      subtitle = emptySubtitle,
      actionText = stringResource(R.string.action_new_project),
      onActionClick = onCreateClick
    )
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(projects, key = { it.id }) { project ->
        ProjectCard(
          project = project,
          onOpen = { onOpenProject(project.id) },
          onRename = { onRenameProject(project) },
          onDelete = { onDeleteProject(project) }
        )
      }
    }
  }
}

@Composable
private fun ProjectCard(
  project: ProjectModel,
  onOpen: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit
) {
  var menuExpanded by remember { mutableStateOf(false) }
  val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onOpen)
      .testTag("project_card_${project.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
    border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Thumbnail placeholder block
      Box(
        modifier = Modifier
          .size(68.dp, 50.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(StudioSurfaceElevated)
          .border(1.dp, StudioBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Movie,
          contentDescription = null,
          tint = CineCutPurple,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = project.title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = StudioTextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${project.clips.size} clip${if (project.clips.size != 1) "s" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = CineCutCyan
          )
          Text(
            text = " · ${formatDuration(project.totalDurationMs)}",
            style = MaterialTheme.typography.bodySmall,
            color = StudioTextSecondary
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = dateFormat.format(Date(project.updatedAt)),
          style = MaterialTheme.typography.labelSmall,
          color = StudioTextMuted
        )
      }

      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier.testTag("project_menu_${project.id}")
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Project options",
            tint = StudioTextSecondary
          )
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
          containerColor = StudioSurfaceElevated
        ) {
          DropdownMenuItem(
            text = { Text("Rename", color = StudioTextPrimary) },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = CineCutCyan) },
            onClick = {
              menuExpanded = false
              onRename()
            }
          )
          DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            onClick = {
              menuExpanded = false
              onDelete()
            }
          )
        }
      }
    }
  }
}

@Composable
private fun ExportList(
  exports: List<ExportedVideo>,
  onPlayExport: (ExportedVideo) -> Unit,
  onShareExport: (ExportedVideo) -> Unit,
  onDeleteExport: (ExportedVideo) -> Unit
) {
  if (exports.isEmpty()) {
    EmptyState(
      icon = Icons.Default.VideoLibrary,
      title = stringResource(R.string.empty_exports_title),
      subtitle = stringResource(R.string.empty_exports_subtitle),
      actionText = null,
      onActionClick = null
    )
  } else {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()) }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(exports, key = { it.id }) { export ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("export_card_${export.id}"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
          border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = export.projectTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StudioTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(CineCutPurple.copy(alpha = 0.2f))
                  .padding(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Text(
                  text = export.resolution,
                  style = MaterialTheme.typography.labelSmall,
                  color = CineCutPurple,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = formatDuration(export.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = CineCutCyan
              )
              if (export.fileSizeBytes > 0) {
                val sizeMb = "%.1f MB".format(export.fileSizeBytes / (1024.0 * 1024.0))
                Text(
                  text = " · $sizeMb",
                  style = MaterialTheme.typography.bodySmall,
                  color = StudioTextSecondary
                )
              }
              Text(
                text = " · ${dateFormat.format(Date(export.exportedAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextMuted
              )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              IconButton(
                onClick = { onDeleteExport(export) },
                modifier = Modifier.testTag("delete_export_${export.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete export",
                  tint = StudioTextMuted
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              OutlinedButton(
                onClick = { onShareExport(export) },
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.testTag("share_export_${export.id}")
              ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = StudioTextSecondary)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share", color = StudioTextSecondary, fontSize = 12.sp)
              }
              Spacer(modifier = Modifier.width(8.dp))
              Button(
                onClick = { onPlayExport(export) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CineCutCyan, contentColor = Color.Black),
                modifier = Modifier.testTag("play_export_${export.id}")
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyState(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  actionText: String?,
  onActionClick: (() -> Unit)?
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(StudioSurfaceElevated),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = StudioTextSecondary,
        modifier = Modifier.size(40.dp)
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = StudioTextPrimary
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodyMedium,
      color = StudioTextSecondary,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )

    if (actionText != null && onActionClick != null) {
      Spacer(modifier = Modifier.height(20.dp))
      Button(
        onClick = onActionClick,
        colors = ButtonDefaults.buttonColors(containerColor = CineCutPurple)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(actionText)
      }
    }
  }
}

fun formatDuration(ms: Long): String {
  val totalSec = (ms / 1000).coerceAtLeast(0)
  val minutes = totalSec / 60
  val seconds = totalSec % 60
  return "%02d:%02d".format(minutes, seconds)
}
