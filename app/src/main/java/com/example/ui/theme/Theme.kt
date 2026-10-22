package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CineCutColorScheme = darkColorScheme(
  primary = CineCutPurple,
  onPrimary = StudioTextPrimary,
  primaryContainer = CineCutPurpleDark,
  onPrimaryContainer = CineCutPurpleLight,
  secondary = CineCutCyan,
  onSecondary = StudioBackground,
  secondaryContainer = StudioSurfaceElevated,
  onSecondaryContainer = CineCutCyan,
  tertiary = CineCutAmber,
  background = StudioBackground,
  onBackground = StudioTextPrimary,
  surface = StudioSurface,
  onSurface = StudioTextPrimary,
  surfaceVariant = StudioSurfaceVariant,
  onSurfaceVariant = StudioTextSecondary,
  outline = StudioBorder,
  error = CineCutCoral
)

@Composable
fun CineCutTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = CineCutColorScheme,
    typography = Typography,
    content = content
  )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  CineCutTheme(content = content)
}
