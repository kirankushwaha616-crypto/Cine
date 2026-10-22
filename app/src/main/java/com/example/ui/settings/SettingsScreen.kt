package com.example.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var cacheSizeMb by remember {
    mutableStateOf(calculateCacheSize(context.cacheDir))
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = StudioBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = StudioTextPrimary
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("settings_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = StudioTextPrimary
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
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // App Branding Header
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
      ) {
        Row(
          modifier = Modifier.padding(18.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(Brush.linearGradient(listOf(CineCutPurple, CineCutCyan))),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Movie,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(28.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "CineCut Video Editor",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = StudioTextPrimary
            )
            Text(
              text = "Version 1.0.0 · Professional Edition",
              style = MaterialTheme.typography.bodySmall,
              color = CineCutCyan
            )
          }
        }
      }

      // Export Defaults Section
      SettingsSection(title = "Video Processing & Output") {
        SettingsRow(
          icon = Icons.Default.HighQuality,
          title = "Default Export Resolution",
          subtitle = "1080p Full HD (Recommended for crystal-clear output)"
        )
        SettingsRow(
          icon = Icons.Default.Movie,
          title = "Media Engine",
          subtitle = "AndroidX Media3 Transformer & ExoPlayer Hardware Acceleration"
        )
      }

      // Storage & Cache Section
      SettingsSection(title = "Storage & Cache") {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(StudioSurfaceElevated),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.CleaningServices,
                contentDescription = null,
                tint = CineCutCyan,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Temporary Cache Files",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = StudioTextPrimary
              )
              Text(
                text = "$cacheSizeMb MB used by render cache",
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextSecondary
              )
            }
          }

          Button(
            onClick = {
              clearDirectory(context.cacheDir)
              cacheSizeMb = calculateCacheSize(context.cacheDir)
              scope.launch {
                snackbarHostState.showSnackbar("Temporary cache cleaned successfully.")
              }
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated, contentColor = CineCutCyan),
            modifier = Modifier.testTag("clear_cache_button")
          ) {
            Text("Clear", fontSize = 12.sp)
          }
        }
      }

      // Privacy & Security Section
      SettingsSection(title = "Privacy & Security") {
        SettingsRow(
          icon = Icons.Default.Security,
          title = "100% On-Device Processing",
          subtitle = "Your videos, photos, and music never leave your device. No cloud storage, no account registration required."
        )
        SettingsRow(
          icon = Icons.Default.Info,
          title = "Scoped Storage & Zero Permissions",
          subtitle = "Uses modern Android Photo Picker with least-privilege security standards."
        )
      }
    }
  }
}

@Composable
private fun SettingsSection(
  title: String,
  content: @Composable () -> Unit
) {
  Column {
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
      color = CineCutCyan,
      modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant),
      border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
    ) {
      Column {
        content()
      }
    }
  }
}

@Composable
private fun SettingsRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(StudioSurfaceElevated),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = CineCutPurple,
        modifier = Modifier.size(20.dp)
      )
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = StudioTextPrimary
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = StudioTextSecondary
      )
    }
  }
}

private fun calculateCacheSize(dir: File): String {
  var sizeBytes = 0L
  dir.walkBottomUp().forEach { file ->
    if (file.isFile) sizeBytes += file.length()
  }
  return "%.1f".format(sizeBytes / (1024.0 * 1024.0))
}

private fun clearDirectory(dir: File) {
  dir.listFiles()?.forEach { file ->
    if (file.isDirectory) clearDirectory(file)
    file.delete()
  }
}
