package com.example.ui.navigation

sealed class Screen(val route: String) {
  object Home : Screen("home")
  object Editor : Screen("editor/{projectId}") {
    fun createRoute(projectId: String): String = "editor/$projectId"
  }
  object Settings : Screen("settings")
}
