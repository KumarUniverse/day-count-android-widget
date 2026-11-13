package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.CountdownTimer
import com.example.ui.components.CreateEditTimerDialog
import com.example.ui.components.HomeScreenWidgetGuideDialog
import com.example.ui.components.MaterialWidgetCard
import com.example.ui.components.WidgetPreviewShowcase
import com.example.widget.WidgetUpdateHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownAppScreen(
  viewModel: CountdownViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val timers by viewModel.timers.collectAsStateWithLifecycle()
  val selectedTimer by viewModel.selectedTimer.collectAsStateWithLifecycle()
  val currentFilter by viewModel.filter.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val currentTimeMs by viewModel.currentTimeMs.collectAsStateWithLifecycle()
  val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
  val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()

  var timerToDelete by remember { mutableStateOf<CountdownTimer?>(null) }
  var isSearchVisible by remember { mutableStateOf(false) }
  var showThemeMenu by remember { mutableStateOf(false) }

  val handleAddToHomeScreen: (CountdownTimer) -> Unit = { timer ->
    viewModel.selectTimer(timer)
    WidgetUpdateHelper.requestAddWidgetToHomeScreen(context, timer.id)
  }

  val cs = MaterialTheme.colorScheme

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .testTag("countdown_app_scaffold"),
    containerColor = cs.background,
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Image(
              painter = painterResource(id = R.drawable.ic_app_icon_art),
              contentDescription = "DayCount Logo",
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, cs.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            )
            Column {
              Text(
                text = "DayCount",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = cs.onSurface
              )
              Text(
                text = "Day Countdown Widgets",
                fontSize = 11.sp,
                color = cs.primary
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = { isSearchVisible = !isSearchVisible },
            modifier = Modifier.testTag("toggle_search_button")
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = if (isSearchVisible) cs.primary else cs.onSurface.copy(alpha = 0.8f)
            )
          }

          // App Theme Selector Menu (Light Theme, Dark Theme, System Default)
          Box {
            IconButton(
              onClick = { showThemeMenu = true },
              modifier = Modifier.testTag("app_theme_menu_button")
            ) {
              val themeIcon = when (appThemeMode) {
                AppThemeMode.LIGHT -> Icons.Default.LightMode
                AppThemeMode.DARK -> Icons.Default.DarkMode
                AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
              }
              Icon(
                imageVector = themeIcon,
                contentDescription = "App Theme Setting",
                tint = cs.onSurface.copy(alpha = 0.85f)
              )
            }

            DropdownMenu(
              expanded = showThemeMenu,
              onDismissRequest = { showThemeMenu = false }
            ) {
              AppThemeMode.entries.forEach { mode ->
                val icon = when (mode) {
                  AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                  AppThemeMode.LIGHT -> Icons.Default.LightMode
                  AppThemeMode.DARK -> Icons.Default.DarkMode
                }
                DropdownMenuItem(
                  text = {
                    Text(
                      text = mode.displayName,
                      fontWeight = if (appThemeMode == mode) FontWeight.Bold else FontWeight.Normal
                    )
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = icon,
                      contentDescription = null,
                      tint = if (appThemeMode == mode) cs.primary else cs.onSurfaceVariant
                    )
                  },
                  trailingIcon = {
                    if (appThemeMode == mode) {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = cs.primary
                      )
                    }
                  },
                  onClick = {
                    viewModel.setAppThemeMode(mode)
                    showThemeMenu = false
                  },
                  modifier = Modifier.testTag("theme_option_${mode.name.lowercase()}")
                )
              }
            }
          }

          IconButton(
            onClick = { viewModel.showGuideDialog() },
            modifier = Modifier.testTag("home_screen_guide_action")
          ) {
            Icon(
              imageVector = Icons.Default.AddHome,
              contentDescription = "Widget installation guide",
              tint = cs.onSurface.copy(alpha = 0.8f)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = cs.surface
        )
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { viewModel.showCreateDialog() },
        modifier = Modifier.testTag("create_new_widget_fab"),
        containerColor = cs.primary,
        contentColor = cs.onPrimary,
        shape = RoundedCornerShape(20.dp),
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("New Widget", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Search Bar Row (collapsible)
      if (isSearchVisible) {
        item(key = "search_bar") {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by widget label...") },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = cs.onSurface)
                }
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("search_text_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = cs.surfaceVariant,
              unfocusedContainerColor = cs.surfaceVariant,
              focusedBorderColor = cs.primary,
              unfocusedBorderColor = cs.outline,
              focusedTextColor = cs.onSurface,
              unfocusedTextColor = cs.onSurface
            )
          )
        }
      }

      // Live Widget Showcase Header
      item(key = "showcase_section") {
        WidgetPreviewShowcase(
          timer = selectedTimer,
          currentTimeMs = currentTimeMs,
          onUpdateTimerQuickly = { updated -> viewModel.updateTimerQuickly(updated) },
          onAddToHomeScreen = { timer -> handleAddToHomeScreen(timer) },
          onEditTimer = { timer -> viewModel.showEditDialog(timer) },
          onOpenHomeGuide = { viewModel.showGuideDialog() }
        )
      }

      // Filter Chips & Theme Quick Switcher
      item(key = "filter_chips_row") {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "CUSTOM COUNTDOWN WIDGETS",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = cs.onSurfaceVariant,
              letterSpacing = 1.sp
            )

            Text(
              text = "Theme: ${appThemeMode.displayName}",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = cs.primary
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(
              "ALL" to "All (${timers.size})",
              "TARGET_DATE" to "Specific Date",
              "DAY_COUNT" to "Specified Days"
            ).forEach { (mode, label) ->
              FilterChip(
                selected = currentFilter == mode,
                onClick = { viewModel.setFilter(mode) },
                label = { Text(label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = cs.primary,
                  selectedLabelColor = cs.onPrimary,
                  containerColor = cs.surfaceVariant,
                  labelColor = cs.onSurfaceVariant
                )
              )
            }
          }
        }
      }

      // Empty State
      if (timers.isEmpty()) {
        item(key = "empty_state") {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant),
            border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.4f))
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Text(
                text = "No countdown widgets found",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
              )
              Text(
                text = "Tap 'New Widget' to create a countdown with custom labels and shapes.",
                fontSize = 12.sp,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
              )
              TextButton(onClick = { viewModel.showCreateDialog() }) {
                Text("+ Create Countdown Widget", color = cs.primary)
              }
            }
          }
        }
      } else {
        // List of Timers
        items(
          items = timers,
          key = { it.id }
        ) { timer ->
          MaterialWidgetCard(
            timer = timer,
            currentTimeMs = currentTimeMs,
            onCardClick = { viewModel.selectTimer(timer) },
            onEditClick = { viewModel.showEditDialog(timer) },
            onDeleteClick = { timerToDelete = timer },
            onAddToHomeScreenClick = { handleAddToHomeScreen(timer) },
            modifier = Modifier.fillMaxWidth()
          )
        }
      }

      // Bottom Spacer for FAB
      item(key = "bottom_fab_spacer") {
        Spacer(modifier = Modifier.height(72.dp))
      }
    }
  }

  // Dialogs
  when (val state = dialogState) {
    is DialogState.Create -> {
      CreateEditTimerDialog(
        initialTimer = null,
        onDismiss = { viewModel.dismissDialog() },
        onSave = { newTimer -> viewModel.saveTimer(newTimer) }
      )
    }
    is DialogState.Edit -> {
      CreateEditTimerDialog(
        initialTimer = state.timer,
        onDismiss = { viewModel.dismissDialog() },
        onSave = { updatedTimer -> viewModel.saveTimer(updatedTimer) }
      )
    }
    is DialogState.HomeGuide -> {
      HomeScreenWidgetGuideDialog(
        onDismiss = { viewModel.dismissDialog() }
      )
    }
    DialogState.None -> {
      // No dialog
    }
  }

  // Delete Confirmation Dialog
  timerToDelete?.let { timer ->
    AlertDialog(
      onDismissRequest = { timerToDelete = null },
      title = { Text("Delete Countdown Widget?") },
      text = { Text("Are you sure you want to delete '${timer.title}'? This action cannot be undone.") },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteTimer(timer)
            timerToDelete = null
          }
        ) {
          Text("Delete", color = cs.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { timerToDelete = null }) {
          Text("Cancel")
        }
      },
      containerColor = cs.surface,
      titleContentColor = cs.onSurface,
      textContentColor = cs.onSurfaceVariant
    )
  }
}
