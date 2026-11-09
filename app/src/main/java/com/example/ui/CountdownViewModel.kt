package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CountdownDatabase
import com.example.data.CountdownDateUtils
import com.example.data.CountdownMode
import com.example.data.CountdownRepository
import com.example.data.CountdownTimer
import com.example.widget.DayCountWidgetProvider
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppThemeMode(val displayName: String) {
  SYSTEM("System Default"),
  LIGHT("Light Theme"),
  DARK("Dark Theme")
}

sealed class DialogState {
  data object None : DialogState()
  data object Create : DialogState()
  data class Edit(val timer: CountdownTimer) : DialogState()
  data object HomeGuide : DialogState()
}

class CountdownViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: CountdownRepository
  private val prefs = application.getSharedPreferences("daycount_app_prefs", Context.MODE_PRIVATE)

  private val _appThemeMode = MutableStateFlow(loadSavedThemeMode())
  val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

  private val _currentTimeMs = MutableStateFlow(System.currentTimeMillis())
  val currentTimeMs: StateFlow<Long> = _currentTimeMs.asStateFlow()

  private var lastObservedDayOrdinal: Long =
    CountdownDateUtils.toLocalDayOrdinal(_currentTimeMs.value)

  private val _selectedTimer = MutableStateFlow<CountdownTimer?>(null)
  val selectedTimer: StateFlow<CountdownTimer?> = _selectedTimer.asStateFlow()

  private val _filter = MutableStateFlow("ALL")
  val filter: StateFlow<String> = _filter.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _dialogState = MutableStateFlow<DialogState>(DialogState.None)
  val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

  init {
    val database = CountdownDatabase.getDatabase(application)
    repository = CountdownRepository(database.countdownDao())

    // Ensure 24-hour midnight home screen widget update alarm is scheduled
    DayCountWidgetProvider.scheduleNextMidnightUpdate(application)

    viewModelScope.launch {
      repository.seedSampleDataIfEmpty()
    }

    // Live clock ticker every 1 second so midnight day rollovers are reflected immediately
    viewModelScope.launch {
      while (true) {
        delay(1000L)
        refreshTimeAndWidgets(forceWidgetUpdate = false)
      }
    }
  }

  /**
   * Updates the current timestamp for all in-app countdown views and refreshes all home screen
   * widgets immediately when the local calendar day changes (or when forced on resume/time change).
   */
  fun refreshTimeAndWidgets(forceWidgetUpdate: Boolean = false) {
    val now = System.currentTimeMillis()
    _currentTimeMs.value = now
    val currentDayOrdinal = CountdownDateUtils.toLocalDayOrdinal(now)
    if (forceWidgetUpdate || currentDayOrdinal != lastObservedDayOrdinal) {
      lastObservedDayOrdinal = currentDayOrdinal
      WidgetUpdateHelper.notifyWidgetDataChanged(getApplication())
    }
  }

  private fun loadSavedThemeMode(): AppThemeMode {
    val saved = prefs.getString("app_theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
    return try {
      AppThemeMode.valueOf(saved)
    } catch (_: Exception) {
      AppThemeMode.SYSTEM
    }
  }

  fun setAppThemeMode(mode: AppThemeMode) {
    _appThemeMode.value = mode
    prefs.edit().putString("app_theme_mode", mode.name).apply()
    WidgetUpdateHelper.notifyWidgetDataChanged(getApplication())
  }

  val timers: StateFlow<List<CountdownTimer>> = combine(
    repository.allTimers,
    _filter,
    _searchQuery
  ) { list, filterMode, query ->
    var filtered = when (filterMode) {
      "TARGET_DATE" -> list.filter { it.getResolvedMode() == CountdownMode.TARGET_DATE }
      "DAY_COUNT" -> list.filter { it.getResolvedMode() == CountdownMode.DAY_COUNT }
      else -> list
    }

    if (query.isNotBlank()) {
      filtered = filtered.filter {
        it.title.contains(query, ignoreCase = true) ||
          it.subtitle.contains(query, ignoreCase = true)
      }
    }

    // Keep selectedTimer synchronized with latest DB updates
    val currentSel = _selectedTimer.value
    if (currentSel == null && filtered.isNotEmpty()) {
      _selectedTimer.value = filtered.first()
    } else if (currentSel != null) {
      val updatedFromDb = list.find { it.id == currentSel.id }
      if (updatedFromDb != null) {
        _selectedTimer.value = updatedFromDb
      } else {
        _selectedTimer.value = filtered.firstOrNull()
      }
    }

    filtered
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = emptyList()
  )

  fun selectTimer(timer: CountdownTimer) {
    _selectedTimer.value = timer
  }

  fun setFilter(filter: String) {
    _filter.value = filter
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun showCreateDialog() {
    _dialogState.value = DialogState.Create
  }

  fun showEditDialog(timer: CountdownTimer) {
    _dialogState.value = DialogState.Edit(timer)
  }

  fun showGuideDialog() {
    _dialogState.value = DialogState.HomeGuide
  }

  fun dismissDialog() {
    _dialogState.value = DialogState.None
  }

  fun saveTimer(timer: CountdownTimer) {
    viewModelScope.launch {
      if (timer.id == 0L) {
        val newId = repository.insert(timer)
        val inserted = timer.copy(id = newId)
        _selectedTimer.value = inserted
      } else {
        repository.update(timer)
        _selectedTimer.value = timer
      }
      dismissDialog()
      WidgetUpdateHelper.notifyWidgetDataChanged(getApplication())
    }
  }

  fun updateTimerQuickly(timer: CountdownTimer) {
    viewModelScope.launch {
      repository.update(timer)
      _selectedTimer.value = timer
      WidgetUpdateHelper.notifyWidgetDataChanged(getApplication())
    }
  }

  fun deleteTimer(timer: CountdownTimer) {
    viewModelScope.launch {
      repository.delete(timer)
      if (_selectedTimer.value?.id == timer.id) {
        _selectedTimer.value = null
      }
      WidgetUpdateHelper.notifyWidgetDataChanged(getApplication())
    }
  }
}
