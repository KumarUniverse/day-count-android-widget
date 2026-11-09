package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppThemeMode
import com.example.ui.CountdownAppScreen
import com.example.ui.CountdownViewModel
import com.example.ui.theme.MyApplicationTheme
import java.util.TimeZone

class MainActivity : ComponentActivity() {

  private val viewModel: CountdownViewModel by viewModels()

  private val timeChangeReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == Intent.ACTION_TIMEZONE_CHANGED) {
        TimeZone.setDefault(null)
      }
      val forceWidgetRefresh = intent?.action != Intent.ACTION_TIME_TICK
      viewModel.refreshTimeAndWidgets(forceWidgetUpdate = forceWidgetRefresh)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val themeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
      val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
      }

      MyApplicationTheme(darkTheme = isDark) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          CountdownAppScreen(viewModel = viewModel)
        }
      }
    }
  }

  override fun onStart() {
    super.onStart()
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_TIME_TICK)
      addAction(Intent.ACTION_DATE_CHANGED)
      addAction(Intent.ACTION_TIME_CHANGED)
      addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }
    ContextCompat.registerReceiver(
      this,
      timeChangeReceiver,
      filter,
      ContextCompat.RECEIVER_NOT_EXPORTED
    )
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshTimeAndWidgets(forceWidgetUpdate = true)
  }

  override fun onStop() {
    try {
      unregisterReceiver(timeChangeReceiver)
    } catch (_: IllegalArgumentException) {
      // Already unregistered
    }
    super.onStop()
  }
}
