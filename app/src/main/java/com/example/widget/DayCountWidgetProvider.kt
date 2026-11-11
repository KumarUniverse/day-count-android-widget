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
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.TextPaint
import android.text.TextUtils
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CountdownDatabase
import com.example.data.CountdownDateUtils
import com.example.data.CountdownTimer
import com.example.data.WidgetShapeStyle
import com.example.data.WidgetThemeColor
import com.example.ui.theme.GOOGLE_SANS_FLEX_ROUNDNESS
import com.example.ui.theme.buildGoogleSansFlexVariationSettings
import com.example.ui.theme.computeWidgetOpticalSize
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

    @Volatile
    private var cachedGoogleSansFlexTypeface: Typeface? = null

    private fun getGoogleSansFlexTypeface(context: Context): Typeface {
      cachedGoogleSansFlexTypeface?.let { return it }
      val loaded = try {
        ResourcesCompat.getFont(context, R.font.google_sans_flex)
      } catch (_: Exception) {
        null
      } ?: Typeface.DEFAULT
      cachedGoogleSansFlexTypeface = loaded
      return loaded
    }

    /**
     * Applies the bundled Google Sans Flex variable font with Roundness 100 ('ROND' 100) and
     * dynamic Optical Size ('opsz') so letters remain crisp and legible when the widget is resized.
     */
    internal fun applyGoogleSansFlex(
      paint: TextPaint,
      context: Context,
      weight: Int,
      opticalSize: Float,
      roundness: Float = GOOGLE_SANS_FLEX_ROUNDNESS
    ) {
      paint.typeface = getGoogleSansFlexTypeface(context)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        paint.fontVariationSettings = buildGoogleSansFlexVariationSettings(
          opticalSize = opticalSize,
          weight = weight,
          roundness = roundness
        )
      }
    }

    /**
     * Dynamically steps down the title font size (and recalculates the Google Sans Flex 'opsz'
     * optical size axis) so longer titles (up to 32 characters) fit inside the widget without
     * being cut off by "...".
     */
    internal fun fitTitleTextPaint(
      paint: TextPaint,
      context: Context,
      title: String,
      maxWidthPx: Float,
      maxTextSizePx: Float,
      minTextSizePx: Float,
      widgetMinWidthDp: Int = 140,
      widgetMinHeightDp: Int = 140
    ): Float {
      val len = title.trim().length
      val lengthBasedStartPx = when {
        len <= 12 -> maxTextSizePx
        len <= 18 -> (maxTextSizePx * 0.85f)
        len <= 24 -> (maxTextSizePx * 0.72f)
        else -> (maxTextSizePx * 0.60f)
      }.coerceIn(minTextSizePx, maxTextSizePx)

      var candidateSizePx = lengthBasedStartPx
      while (candidateSizePx >= minTextSizePx) {
        paint.textSize = candidateSizePx
        val baseSp = (candidateSizePx / 2.4f).coerceAtLeast(6f)
        val opsz = computeWidgetOpticalSize(
          baseSp = baseSp,
          widgetMinWidthDp = widgetMinWidthDp,
          widgetMinHeightDp = widgetMinHeightDp
        )
        applyGoogleSansFlex(paint, context, weight = 600, opticalSize = opsz)
        if (paint.measureText(title) <= maxWidthPx || candidateSizePx == minTextSizePx) {
          break
        }
        candidateSizePx = (candidateSizePx - 1f).coerceAtLeast(minTextSizePx)
      }
      return paint.textSize
    }

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
     * Resolves widget colors for Canvas rendering using the exact Android Material You tonal
     * palette (system_accent1_800 / system_accent1_100) used by first-party Google Pixel widgets.
     */
    fun resolveCanvasWidgetColors(
      context: Context,
      themeColor: WidgetThemeColor,
      overrideIsDark: Boolean? = null
    ): CanvasWidgetColors {
      if (themeColor == WidgetThemeColor.DYNAMIC) {
        val isDark = overrideIsDark ?: run {
          val prefs = context.getSharedPreferences("daycount_app_prefs", Context.MODE_PRIVATE)
          val savedThemeMode = prefs.getString("app_theme_mode", "SYSTEM") ?: "SYSTEM"
          when (savedThemeMode) {
            "LIGHT" -> false
            "DARK" -> true
            else -> {
              val nightModeFlags =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
              nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            }
          }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val bgColor = ContextCompat.getColor(
            context,
            if (isDark) android.R.color.system_accent1_800 else android.R.color.system_accent1_100
          )
          val fgColor = ContextCompat.getColor(
            context,
            if (isDark) android.R.color.system_accent1_100 else android.R.color.system_accent1_800
          )
          return CanvasWidgetColors(
            primary = fgColor,
            surface = bgColor,
            container = bgColor,
            onSurface = fgColor
          )
        } else {
          // Pre-Android 12 fallback matching Pixel tonal surface + soft pastel foreground
          return if (isDark) {
            CanvasWidgetColors(
              primary = 0xFFD0E4FF.toInt(),
              surface = 0xFF003258.toInt(),
              container = 0xFF003258.toInt(),
              onSurface = 0xFFD0E4FF.toInt()
            )
          } else {
            CanvasWidgetColors(
              primary = 0xFF003258.toInt(),
              surface = 0xFFD0E4FF.toInt(),
              container = 0xFFD0E4FF.toInt(),
              onSurface = 0xFF003258.toInt()
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

            val options = try {
              appWidgetManager.getAppWidgetOptions(appWidgetId)
            } catch (_: Exception) {
              null
            }
            val minWidthDp =
              options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 140)?.takeIf { it > 0 }
                ?: 140
            val minHeightDp =
              options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140)?.takeIf { it > 0 }
                ?: 140

            val views = RemoteViews(context.packageName, R.layout.widget_daycount)
            val bitmap = renderWidgetBitmap(
              context = context,
              timer = timer,
              now = nowMs,
              widgetMinWidthDp = minWidthDp,
              widgetMinHeightDp = minHeightDp
            )
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
      now: Long = System.currentTimeMillis(),
      widgetMinWidthDp: Int = 140,
      widgetMinHeightDp: Int = 140,
      overrideIsDark: Boolean? = null
    ): Bitmap {
      if (timer == null) {
        val bitmap = Bitmap.createBitmap(520, 320, Bitmap.Config.ARGB_8888)
        drawEmptyWidget(context, Canvas(bitmap), 520, 320, overrideIsDark)
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
      val resolvedColors = resolveCanvasWidgetColors(
        context = context,
        themeColor = timer.getResolvedTheme(),
        overrideIsDark = overrideIsDark
      )
      val opacityFraction = (timer.opacityPercent.coerceIn(0, 100) / 100f)

      val pad = 8f
      val shapePath = buildShapePath(
        style = shapeStyle,
        left = pad,
        top = pad,
        right = widthPx - pad,
        bottom = heightPx - pad
      )

      // 1. Solid Flat Background Fill with Opacity (No color gradient, no shape outline border)
      if (opacityFraction > 0f) {
        val bgAlpha = (opacityFraction * 255).toInt().coerceIn(0, 255)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.FILL
          color = applyAlpha(resolvedColors.surface, bgAlpha)
        }
        canvas.drawPath(shapePath, fillPaint)
      }

      val isSymmetric = isRadialShape || shapeStyle == WidgetShapeStyle.PILL
      val maxTextWidth = when {
        isRadialShape -> widthPx * 0.68f
        shapeStyle == WidgetShapeStyle.PILL -> widthPx * 0.76f
        else -> widthPx * 0.88f
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
      val numberY = cy + 24f
      val labelY = cy + 58f
      val barTop = cy + 76f
      val barBottom = cy + 88f
      val footerY = cy + 120f

      // 2. Title (Google Sans Flex, Roundness 100, auto-shrunk textSize + dynamic Optical Size)
      val clampedTitle = CountdownDateUtils.sanitizeTitleInput(timer.title)
      val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = onSurfaceColor
        textAlign = Paint.Align.CENTER
      }
      fitTitleTextPaint(
        paint = titlePaint,
        context = context,
        title = clampedTitle,
        maxWidthPx = maxTextWidth,
        maxTextSizePx = if (isRadialShape) 34f else 36f,
        minTextSizePx = if (isRadialShape) 16f else 17f,
        widgetMinWidthDp = widgetMinWidthDp,
        widgetMinHeightDp = widgetMinHeightDp
      )
      val safeTitle = TextUtils.ellipsize(
        clampedTitle,
        titlePaint,
        maxTextWidth,
        TextUtils.TruncateAt.END
      ).toString()
      canvas.drawText(safeTitle, cx, titleY, titlePaint)

      // Optional Subtitle
      if (hasSubtitle) {
        val subOpsz = computeWidgetOpticalSize(
          baseSp = 11f,
          widgetMinWidthDp = widgetMinWidthDp,
          widgetMinHeightDp = widgetMinHeightDp
        )
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = applyAlpha(onSurfaceColor, 200)
          textSize = 23f
          textAlign = Paint.Align.CENTER
          applyGoogleSansFlex(this, context, weight = 500, opticalSize = subOpsz)
        }
        val safeSub = TextUtils.ellipsize(
          timer.subtitle,
          subPaint,
          maxTextWidth,
          TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(safeSub, cx, subY, subPaint)
      }

      // 3. Hero Days Number (Google Sans Flex, Roundness 100, high Optical Size matching Pixel Weather)
      val displayDays = abs(days).toString()
      val numberOpsz = computeWidgetOpticalSize(
        baseSp = 96f,
        widgetMinWidthDp = widgetMinWidthDp,
        widgetMinHeightDp = widgetMinHeightDp
      )
      val numberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryColor
        textSize = when {
          displayDays.length >= 4 -> 90f
          isRadialShape -> 108f
          else -> 112f
        }
        textAlign = Paint.Align.CENTER
        applyGoogleSansFlex(this, context, weight = 500, opticalSize = numberOpsz)
      }
      canvas.drawText(displayDays, cx, numberY, numberPaint)

      // 4. Days Label
      val daysLabel = when {
        isOverdue && abs(days) == 0L -> "TODAY IS THE DAY!"
        isOverdue -> "DAYS AGO"
        abs(days) == 1L -> "DAY REMAINING"
        else -> "DAYS REMAINING"
      }
      val labelOpsz = computeWidgetOpticalSize(
        baseSp = 11f,
        widgetMinWidthDp = widgetMinWidthDp,
        widgetMinHeightDp = widgetMinHeightDp
      )
      val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = applyAlpha(onSurfaceColor, 225)
        textSize = 22f
        letterSpacing = 0.06f
        textAlign = Paint.Align.CENTER
        applyGoogleSansFlex(this, context, weight = 600, opticalSize = labelOpsz)
      }
      canvas.drawText(daysLabel, cx, labelY, labelPaint)

      // 5. Progress Bar
      val barWidth = maxTextWidth * 0.88f
      val barLeft = cx - barWidth / 2f
      val barRight = cx + barWidth / 2f
      val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = applyAlpha(onSurfaceColor, 55)
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

      // 6. Optional Countdown Date / Fraction of Days Completed Footer
      if (hasFooter) {
        val footerOpsz = computeWidgetOpticalSize(
          baseSp = 11f,
          widgetMinWidthDp = widgetMinWidthDp,
          widgetMinHeightDp = widgetMinHeightDp
        )
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
          color = applyAlpha(onSurfaceColor, 205)
          textSize = 22f
          textAlign = Paint.Align.CENTER
          applyGoogleSansFlex(this, context, weight = 500, opticalSize = footerOpsz)
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

    private fun drawEmptyWidget(
      context: Context,
      canvas: Canvas,
      widthPx: Int,
      heightPx: Int,
      overrideIsDark: Boolean? = null
    ) {
      val pad = 8f
      val rect = RectF(pad, pad, widthPx - pad, heightPx - pad)
      val colors = resolveCanvasWidgetColors(context, WidgetThemeColor.DYNAMIC, overrideIsDark)
      val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = colors.surface
      }
      canvas.drawRoundRect(rect, 64f, 64f, bgPaint)

      val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colors.onSurface
        textSize = 38f
        textAlign = Paint.Align.CENTER
        applyGoogleSansFlex(this, context, weight = 600, opticalSize = 22f)
      }
      canvas.drawText("DayCount", widthPx / 2f, heightPx / 2f - 8f, titlePaint)

      val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = applyAlpha(colors.onSurface, 200)
        textSize = 24f
        textAlign = Paint.Align.CENTER
        applyGoogleSansFlex(this, context, weight = 500, opticalSize = 12f)
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
