package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.TextPaint
import android.text.TextUtils
import android.widget.RemoteViews
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.toArgb
import com.example.MainActivity
import com.example.R
import com.example.data.CountdownDatabase
import com.example.data.CountdownDateUtils
import com.example.data.CountdownTimer
import com.example.data.WidgetShapeStyle
import com.example.data.WidgetThemeColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class CanvasWidgetColors(
  val primary: Int,
  val surface: Int,
  val container: Int,
  val onSurface: Int
)

class DayCountWidgetProvider : AppWidgetProvider() {

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action
    if (action == Intent.ACTION_TIMEZONE_CHANGED) {
      TimeZone.setDefault(null)
    }

    when (action) {
      ACTION_MIDNIGHT_WIDGET_UPDATE,
      Intent.ACTION_DATE_CHANGED,
      Intent.ACTION_TIME_CHANGED,
      Intent.ACTION_TIMEZONE_CHANGED,
      Intent.ACTION_BOOT_COMPLETED,
      Intent.ACTION_MY_PACKAGE_REPLACED -> {
        scheduleNextMidnightUpdate(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, DayCountWidgetProvider::class.java)
        val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        if (allWidgetIds.isNotEmpty()) {
          updateWidgetsInternal(context, appWidgetManager, allWidgetIds, goAsync())
        }
      }
      else -> {
        super.onReceive(context, intent)
      }
    }
  }

  override fun onEnabled(context: Context) {
    super.onEnabled(context)
    scheduleNextMidnightUpdate(context)
  }

  override fun onDisabled(context: Context) {
    super.onDisabled(context)
    cancelMidnightUpdate(context)
  }

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    scheduleNextMidnightUpdate(context)
    updateWidgetsInternal(context, appWidgetManager, appWidgetIds, goAsync())
  }

  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle
  ) {
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    scheduleNextMidnightUpdate(context)
    updateWidgetsInternal(context, appWidgetManager, intArrayOf(appWidgetId), goAsync())
  }

  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    super.onDeleted(context, appWidgetIds)
    for (id in appWidgetIds) {
      WidgetUpdateHelper.removeMappedWidget(context, id)
    }
  }

  companion object {
    const val ACTION_MIDNIGHT_WIDGET_UPDATE = "com.example.widget.ACTION_MIDNIGHT_WIDGET_UPDATE"
    private const val MIDNIGHT_ALARM_REQUEST_CODE = 94021

    private fun createMidnightPendingIntent(context: Context): PendingIntent {
      val intent = Intent(context, DayCountWidgetProvider::class.java).apply {
        action = ACTION_MIDNIGHT_WIDGET_UPDATE
      }
      return PendingIntent.getBroadcast(
        context,
        MIDNIGHT_ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
    }

    /**
     * Schedules an alarm for 00:00:01 local time of the next calendar day so all home screen
     * countdown widgets automatically refresh every 24 hours immediately after midnight.
     */
    fun scheduleNextMidnightUpdate(
      context: Context,
      nowMs: Long = System.currentTimeMillis()
    ): Long {
      val triggerAtMs = CountdownDateUtils.nextMidnightEpochMs(nowMs)
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
      val pendingIntent = createMidnightPendingIntent(context)
      if (alarmManager != null) {
        try {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
              alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
              )
            } else {
              alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                1_000L,
                pendingIntent
              )
            }
          } else {
            alarmManager.setExactAndAllowWhileIdle(
              AlarmManager.RTC_WAKEUP,
              triggerAtMs,
              pendingIntent
            )
          }
        } catch (_: SecurityException) {
          alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMs,
            pendingIntent
          )
        }
      }
      return triggerAtMs
    }

    fun cancelMidnightUpdate(context: Context) {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
      alarmManager?.cancel(createMidnightPendingIntent(context))
    }

    fun updateAllWidgets(context: Context) {
      scheduleNextMidnightUpdate(context)
      val appWidgetManager = AppWidgetManager.getInstance(context)
      val thisWidget = ComponentName(context, DayCountWidgetProvider::class.java)
      val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
      if (allWidgetIds.isNotEmpty()) {
        updateWidgetsInternal(context, appWidgetManager, allWidgetIds, null)
      }
    }

    /**
     * Resolves widget colors for Canvas rendering, matching MaterialWidgetCard's resolveWidgetColors
     * in the app so Device Dynamic (MUI) uses the real Android dynamic color palette on the home screen.
     */
    fun resolveCanvasWidgetColors(
      context: Context,
      themeColor: WidgetThemeColor
    ): CanvasWidgetColors {
      if (themeColor == WidgetThemeColor.DYNAMIC) {
        val prefs = context.getSharedPreferences("daycount_app_prefs", Context.MODE_PRIVATE)
        val savedThemeMode = prefs.getString("app_theme_mode", "SYSTEM") ?: "SYSTEM"
        val isDark = when (savedThemeMode) {
          "LIGHT" -> false
          "DARK" -> true
          else -> {
            val nightModeFlags =
              context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightModeFlags == Configuration.UI_MODE_NIGHT_YES
          }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val cs = if (isDark) {
            dynamicDarkColorScheme(context)
          } else {
            dynamicLightColorScheme(context)
          }
          return CanvasWidgetColors(
            primary = cs.primary.toArgb(),
            surface = cs.surfaceVariant.toArgb(),
            container = cs.primaryContainer.toArgb(),
            onSurface = cs.onPrimaryContainer.toArgb()
          )
        } else {
          // Pre-Android 12 fallback matching Theme.kt LightColorScheme / DarkColorScheme
          return if (isDark) {
            CanvasWidgetColors(
              primary = 0xFF38BDF8.toInt(),
              surface = 0xFF1E293B.toInt(),
              container = 0xFF0C4A6E.toInt(),
              onSurface = 0xFFE0F2FE.toInt()
            )
          } else {
            CanvasWidgetColors(
              primary = 0xFF0284C7.toInt(),
              surface = 0xFFF1F5F9.toInt(),
              container = 0xFFE0F2FE.toInt(),
              onSurface = 0xFF0369A1.toInt()
            )
          }
        }
      }

      return CanvasWidgetColors(
        primary = themeColor.primaryHex.toInt(),
        surface = themeColor.surfaceHex.toInt(),
        container = themeColor.containerHex.toInt(),
        onSurface = themeColor.onSurfaceHex.toInt()
      )
    }

    private fun updateWidgetsInternal(
      context: Context,
      appWidgetManager: AppWidgetManager,
      appWidgetIds: IntArray,
      pendingResult: BroadcastReceiver.PendingResult? = null
    ) {
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val database = CountdownDatabase.getDatabase(context)
          val dao = database.countdownDao()
          val nowMs = System.currentTimeMillis()

          for (appWidgetId in appWidgetIds) {
            var mappedTimerId = WidgetUpdateHelper.getMappedTimerIdForWidget(context, appWidgetId)

            if (mappedTimerId == 0L) {
              val pendingId = WidgetUpdateHelper.getPendingWidgetTimerId(context)
              if (pendingId > 0L) {
                mappedTimerId = pendingId
                WidgetUpdateHelper.setMappedTimerIdForWidget(context, appWidgetId, mappedTimerId)
                WidgetUpdateHelper.setPendingWidgetTimerId(context, 0L)
              }
            }

            val timer = if (mappedTimerId > 0L) {
              dao.getTimerByIdDirect(mappedTimerId) ?: dao.getFirstTimerDirect()
            } else {
              val first = dao.getFirstTimerDirect()
              if (first != null) {
                WidgetUpdateHelper.setMappedTimerIdForWidget(context, appWidgetId, first.id)
              }
              first
            }

            val views = RemoteViews(context.packageName, R.layout.widget_daycount)
            val bitmap = renderWidgetBitmap(context, timer, nowMs)
            views.setImageViewBitmap(R.id.widget_canvas_image, bitmap)

            // Open MainActivity on click
            val intent = Intent(context, MainActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
              context,
              appWidgetId,
              intent,
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
          }
        } finally {
          pendingResult?.finish()
        }
      }
    }

    internal fun renderWidgetBitmap(
      context: Context,
      timer: CountdownTimer?,
      now: Long = System.currentTimeMillis()
    ): Bitmap {
      if (timer == null) {
        val bitmap = Bitmap.createBitmap(520, 320, Bitmap.Config.ARGB_8888)
        drawEmptyWidget(Canvas(bitmap), 520, 320)
        return bitmap
      }

      val shapeStyle = timer.getResolvedShape()
      val isRadialShape = shapeStyle == WidgetShapeStyle.CIRCLE ||
        shapeStyle == WidgetShapeStyle.FLOWER ||
        shapeStyle == WidgetShapeStyle.HEXAGON

      // Use a tight horizontal bounding box (540x340) for Squircle/Rounded/Pill
      // and a compact square bounding box (460x460) for Circle/Hexagon/Flower
      val widthPx = if (isRadialShape) 460 else 540
      val heightPx = if (isRadialShape) 460 else 340

      val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      val days = timer.daysRemaining(now)
      val isOverdue = timer.isCompleted(now)
      val progress = timer.progressFraction(now).coerceIn(0f, 1f)
      val resolvedColors = resolveCanvasWidgetColors(context, timer.getResolvedTheme())
      val opacityFraction = (timer.opacityPercent.coerceIn(0, 100) / 100f)

      val pad = 8f
      val shapePath = buildShapePath(
        style = shapeStyle,
        left = pad,
        top = pad,
        right = widthPx - pad,
        bottom = heightPx - pad
      )

      // 1. Background Gradient Fill with Opacity
      if (opacityFraction > 0f) {
        val bgAlpha = (opacityFraction * 255).toInt().coerceIn(0, 255)
        val startColor = applyAlpha(resolvedColors.container, (bgAlpha * 0.92f).toInt())
        val endColor = applyAlpha(resolvedColors.surface, bgAlpha)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          shader = LinearGradient(
            pad,
            pad,
            widthPx - pad,
            heightPx - pad,
            startColor,
            endColor,
            Shader.TileMode.CLAMP
          )
        }
        canvas.drawPath(shapePath, fillPaint)
      }

      // 2. Crisp Shape Outline Border
      val strokeAlpha = ((0.2f + 0.5f * opacityFraction) * 255).toInt().coerceIn(40, 220)
      val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = applyAlpha(resolvedColors.primary, strokeAlpha)
      }
      canvas.drawPath(shapePath, borderPaint)

      val isSymmetric = isRadialShape || shapeStyle == WidgetShapeStyle.PILL
      val maxTextWidth = when {
        isRadialShape -> widthPx * 0.64f
        shapeStyle == WidgetShapeStyle.PILL -> widthPx * 0.72f
        else -> widthPx * 0.84f
      }
      val cx = widthPx / 2f
      val cy = heightPx / 2f

      val onSurfaceColor = resolvedColors.onSurface
      val primaryColor = resolvedColors.primary

      val hasSubtitle = timer.subtitle.isNotBlank()
      val footerText = timer.formattedCountdownFooter(nowMs = now, compact = isSymmetric)
      val hasFooter = footerText.isNotBlank()

      // Vertically center all content relative to cy so there is zero wasted vertical space
      val titleY = cy - (if (hasSubtitle) 92f else 78f)
      val subY = cy - 60f
      val numberY = cy + 22f
      val labelY = cy + 56f
      val barTop = cy + 76f
      val barBottom = cy + 88f
      val footerY = cy + 120f

      // 3. Title
      val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = onSurfaceColor
        textSize = if (isRadialShape) 34f else 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
      val safeTitle = TextUtils.ellipsize(
        timer.title,
        titlePaint,
        maxTextWidth,
        TextUtils.TruncateAt.END
      ).toString()
      canvas.drawText(safeTitle, cx, titleY, titlePaint)

      // Optional Subtitle
      if (hasSubtitle) {
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = applyAlpha(onSurfaceColor, 185)
          textSize = 23f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
          textAlign = Paint.Align.CENTER
        }
        val safeSub = TextUtils.ellipsize(
          timer.subtitle,
          subPaint,
          maxTextWidth,
          TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(safeSub, cx, subY, subPaint)
      }

      // 4. Big Days Number
      val displayDays = abs(days).toString()
      val numberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryColor
        textSize = when {
          displayDays.length >= 4 -> 88f
          isRadialShape -> 104f
          else -> 108f
        }
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
      canvas.drawText(displayDays, cx, numberY, numberPaint)

      // 5. Days Label
      val daysLabel = when {
        isOverdue && abs(days) == 0L -> "TODAY IS THE DAY!"
        isOverdue -> "DAYS AGO"
        abs(days) == 1L -> "DAY REMAINING"
        else -> "DAYS REMAINING"
      }
      val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = applyAlpha(onSurfaceColor, 220)
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        letterSpacing = 0.08f
        textAlign = Paint.Align.CENTER
      }
      canvas.drawText(daysLabel, cx, labelY, labelPaint)

      // 6. Progress Bar
      val barWidth = maxTextWidth * 0.88f
      val barLeft = cx - barWidth / 2f
      val barRight = cx + barWidth / 2f
      val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = applyAlpha(onSurfaceColor, 50)
      }
      canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barBottom), 6f, 6f, trackPaint)

      if (progress > 0f) {
        val minFillWidth = min(barWidth, 12f)
        val fillRight = (barLeft + barWidth * progress).coerceAtLeast(barLeft + minFillWidth)
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          color = primaryColor
        }
        canvas.drawRoundRect(RectF(barLeft, barTop, fillRight, barBottom), 6f, 6f, progressPaint)
      }

      // 7. Optional Countdown Date / Fraction of Days Completed Footer
      if (hasFooter) {
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = applyAlpha(onSurfaceColor, 195)
          textSize = 22f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
          textAlign = Paint.Align.CENTER
        }
        val safeFooter = TextUtils.ellipsize(
          footerText,
          footerPaint,
          maxTextWidth,
          TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(safeFooter, cx, footerY, footerPaint)
      }

      return bitmap
    }

    private fun drawEmptyWidget(canvas: Canvas, widthPx: Int, heightPx: Int) {
      val pad = 8f
      val rect = RectF(pad, pad, widthPx - pad, heightPx - pad)
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0F172A.toInt()
      }
      canvas.drawRoundRect(rect, 64f, 64f, bgPaint)

      val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF8FAFC.toInt()
        textSize = 38f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
      }
      canvas.drawText("DayCount", widthPx / 2f, heightPx / 2f - 8f, titlePaint)

      val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF94A3B8.toInt()
        textSize = 24f
        textAlign = Paint.Align.CENTER
      }
      canvas.drawText("Tap to create countdown", widthPx / 2f, heightPx / 2f + 32f, subPaint)
    }

    private fun buildShapePath(
      style: WidgetShapeStyle,
      left: Float,
      top: Float,
      right: Float,
      bottom: Float
    ): Path {
      val path = Path()
      val w = right - left
      val h = bottom - top
      val cx = left + w / 2f
      val cy = top + h / 2f

      when (style) {
        WidgetShapeStyle.SQUIRCLE -> {
          path.addRoundRect(RectF(left, top, right, bottom), 76f, 76f, Path.Direction.CW)
        }
        WidgetShapeStyle.ROUNDED -> {
          path.addRoundRect(RectF(left, top, right, bottom), 44f, 44f, Path.Direction.CW)
        }
        WidgetShapeStyle.PILL -> {
          val rect = RectF(left, top, right, bottom)
          val radius = rect.height() / 2f
          path.addRoundRect(rect, radius, radius, Path.Direction.CW)
        }
        WidgetShapeStyle.CIRCLE -> {
          val radius = min(w, h) / 2f
          path.addCircle(cx, cy, radius, Path.Direction.CW)
        }
        WidgetShapeStyle.HEXAGON -> {
          val rx = w / 2f
          val ry = h / 2f
          val smoothing = 0.18f
          val pts = (0 until 6).map { i ->
            val angle = (PI / 3.0) * i - (PI / 6.0)
            Pair(
              cx + (rx * cos(angle)).toFloat(),
              cy + (ry * sin(angle)).toFloat()
            )
          }
          for (i in 0 until 6) {
            val prev = pts[(i + 5) % 6]
            val curr = pts[i]
            val next = pts[(i + 1) % 6]
            val sx = curr.first + (prev.first - curr.first) * smoothing
            val sy = curr.second + (prev.second - curr.second) * smoothing
            val ex = curr.first + (next.first - curr.first) * smoothing
            val ey = curr.second + (next.second - curr.second) * smoothing
            if (i == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
            path.quadTo(curr.first, curr.second, ex, ey)
          }
          path.close()
        }
        WidgetShapeStyle.FLOWER -> {
          val petals = 12
          val baseRx = w * 0.45f
          val baseRy = h * 0.45f
          val ampX = w * 0.05f
          val ampY = h * 0.05f
          val steps = 180
          for (step in 0..steps) {
            val theta = (2.0 * PI * step) / steps
            val wave = (1.0 + cos(petals * theta)) * 0.5
            val rx = baseRx + ampX * wave.toFloat()
            val ry = baseRy + ampY * wave.toFloat()
            val x = cx + rx * cos(theta).toFloat()
            val y = cy + ry * sin(theta).toFloat()
            if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
          }
          path.close()
        }
      }
      return path
    }

    private fun applyAlpha(color: Int, alpha: Int): Int {
      val clamped = alpha.coerceIn(0, 255)
      return (color and 0x00FFFFFF) or (clamped shl 24)
    }
  }
}
