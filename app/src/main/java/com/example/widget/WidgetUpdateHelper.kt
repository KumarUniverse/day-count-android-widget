package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

object WidgetUpdateHelper {

  private const val PREFS_NAME = "daycount_widget_prefs"
  private const val KEY_PENDING_TIMER_ID = "pending_timer_id"
  private const val KEY_PREFIX_WIDGET_TIMER = "widget_timer_map_"

  fun setPendingWidgetTimerId(context: Context, timerId: Long) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putLong(KEY_PENDING_TIMER_ID, timerId)
      .apply()
  }

  fun getPendingWidgetTimerId(context: Context): Long {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getLong(KEY_PENDING_TIMER_ID, 0L)
  }

  fun getMappedTimerIdForWidget(context: Context, appWidgetId: Int): Long {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getLong("$KEY_PREFIX_WIDGET_TIMER$appWidgetId", 0L)
  }

  fun setMappedTimerIdForWidget(context: Context, appWidgetId: Int, timerId: Long) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putLong("$KEY_PREFIX_WIDGET_TIMER$appWidgetId", timerId)
      .apply()
  }

  fun removeMappedWidget(context: Context, appWidgetId: Int) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .remove("$KEY_PREFIX_WIDGET_TIMER$appWidgetId")
      .apply()
  }

  fun notifyWidgetDataChanged(context: Context) {
    try {
      DayCountWidgetProvider.scheduleNextMidnightUpdate(context)
      DayCountWidgetProvider.updateAllWidgets(context)
    } catch (_: Exception) {
      // Ignored if widget not added yet
    }
  }

  fun requestAddWidgetToHomeScreen(context: Context, timerId: Long = 0L): Boolean {
    if (timerId > 0L) {
      setPendingWidgetTimerId(context, timerId)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val appWidgetManager = AppWidgetManager.getInstance(context)
      if (appWidgetManager.isRequestPinAppWidgetSupported) {
        val myProvider = ComponentName(context, DayCountWidgetProvider::class.java)
        return appWidgetManager.requestPinAppWidget(myProvider, null, null)
      }
    }
    notifyWidgetDataChanged(context)
    return false
  }
}
