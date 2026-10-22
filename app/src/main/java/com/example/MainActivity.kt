package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.repository.MediaRepository
import com.example.data.repository.ProjectRepository
import com.example.ui.editor.EditorScreen
import com.example.ui.editor.EditorViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.navigation.Screen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.CineCutTheme

class MainActivity : ComponentActivity() {

  private val database by lazy {
    Room.databaseBuilder(
      applicationContext,
      AppDatabase::class.java,
      "cinecut_editor.db"
    ).fallbackToDestructiveMigration().build()
  }

  private val projectRepository by lazy { ProjectRepository(database) }
  private val mediaRepository by lazy { MediaRepository(applicationContext) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      CineCutTheme {
        CineCutApp(
          projectRepository = projectRepository,
          mediaRepository = mediaRepository
        )
      }
    }
  }
}

@Composable
fun CineCutApp(
  projectRepository: ProjectRepository,
  mediaRepository: MediaRepository
) {
  val navController = rememberNavController()

  NavHost(
    navController = navController,
    startDestination = Screen.Home.route,
    modifier = Modifier.fillMaxSize()
  ) {
    // 1. Home Destination
    composable(Screen.Home.route) {
      val homeViewModel: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(projectRepository, mediaRepository) as T
          }
        }
      )

      HomeScreen(
        viewModel = homeViewModel,
        onOpenProject = { projectId ->
          navController.navigate(Screen.Editor.createRoute(projectId))
        },
        onOpenSettings = {
          navController.navigate(Screen.Settings.route)
        }
      )
    }

    // 2. Video Editor Destination
    composable(
      route = Screen.Editor.route,
      arguments = listOf(navArgument("projectId") { type = NavType.StringType })
    ) { backStackEntry ->
      val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
      val context = androidx.compose.ui.platform.LocalContext.current

      val editorViewModel: EditorViewModel = viewModel(
        key = "editor_$projectId",
        factory = object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(
              projectId = projectId,
              projectRepository = projectRepository,
              mediaRepository = mediaRepository,
              context = context.applicationContext
            ) as T
          }
        }
      )

      EditorScreen(
        viewModel = editorViewModel,
        onNavigateBack = {
          navController.popBackStack()
        }
      )
    }

    // 3. Settings Destination
    composable(Screen.Settings.route) {
      SettingsScreen(
        onNavigateBack = {
          navController.popBackStack()
        }
      )
    }
  }
}
