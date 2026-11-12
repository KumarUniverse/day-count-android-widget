package com.example

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.text.TextPaint
import androidx.compose.ui.graphics.luminance
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.CountdownDateUtils
import com.example.data.CountdownMode
import com.example.data.CountdownTimer
import com.example.data.DateDisplayMode
import com.example.data.WidgetShapeStyle
import com.example.data.WidgetThemeColor
import com.example.ui.components.WallpaperTheme
import com.example.ui.theme.buildGoogleSansFlexVariationSettings
import com.example.ui.theme.computeWidgetOpticalSize
import com.example.widget.DayCountWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DayCount", appName)
  }

  @Test
  fun `countdown timer calculates remaining days and custom progress bar for target date`() {
    val now = 1_000_000_000_000L
    val target = CountdownDateUtils.addLocalCalendarDays(now, 15)
    val optionalStart = CountdownDateUtils.addLocalCalendarDays(now, -15)

    val timer = CountdownTimer(
      title = "Vacation",
      mode = CountdownMode.TARGET_DATE.name,
      targetDateEpochMs = target,
      startDateEpochMs = optionalStart
    )

    assertEquals(15L, timer.daysRemaining(now))
    assertEquals(0.5f, timer.progressFraction(now), 0.01f)
    assertFalse(timer.isCompleted(now))
  }

  @Test
  fun `specified days numpad input sanitation strips non-digits leading zeros and clamps bounds`() {
    assertEquals("45", CountdownDateUtils.sanitizeDaysInput("0045"))
    assertEquals("120", CountdownDateUtils.sanitizeDaysInput("-120.5abc"))
    assertEquals("", CountdownDateUtils.sanitizeDaysInput("000"))
    assertEquals("36500", CountdownDateUtils.sanitizeDaysInput("99999"))

    assertEquals(1, CountdownDateUtils.parseSanitizedDays("", fallback = 1))
    assertEquals(30, CountdownDateUtils.parseSanitizedDays("000", fallback = 30))
    assertEquals(36500, CountdownDateUtils.parseSanitizedDays("99999", fallback = 30))
    assertEquals(75, CountdownDateUtils.parseSanitizedDays("075 days!", fallback = 30))
  }

  @Test
  fun `widget title enforces 32 character limit and shrinks font size for long titles`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val overlongTitle = "Annual International Software Engineering Summit"
    val sanitized = CountdownDateUtils.sanitizeTitleInput(overlongTitle)
    assertEquals(32, sanitized.length)
    assertEquals("Annual International Software En", sanitized)

    // Verify adaptive Compose font size shrinks for long titles
    val shortSp = CountdownDateUtils.adaptiveTitleFontSizeSp("Vacation", isSymmetricShape = true)
    val longSp = CountdownDateUtils.adaptiveTitleFontSizeSp(sanitized, isSymmetricShape = true)
    assertTrue("Expected smaller sp for 32-char title", longSp < shortSp)

    // Verify Home Screen canvas fitTitleTextPaint shrinks textSize for long titles to fit
    val shortPaint = TextPaint()
    val shortSizePx = DayCountWidgetProvider.fitTitleTextPaint(
      paint = shortPaint,
      context = context,
      title = "Last Day",
      maxWidthPx = 312f,
      maxTextSizePx = 34f,
      minTextSizePx = 16f
    )
    assertEquals(34f, shortSizePx, 0.01f)

    val longPaint = TextPaint()
    val longSizePx = DayCountWidgetProvider.fitTitleTextPaint(
      paint = longPaint,
      context = context,
      title = sanitized,
      maxWidthPx = 312f,
      maxTextSizePx = 34f,
      minTextSizePx = 16f
    )
    assertTrue("Expected smaller canvas textSize for 32-char title", longSizePx < shortSizePx)
  }

  @Test
  fun `home screen widget uses compact vertical bounding box for horizontal shapes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val squircleTimer = CountdownTimer(
      title = "Vacation",
      shapeStyle = WidgetShapeStyle.SQUIRCLE.name
    )
    val circleTimer = CountdownTimer(
      title = "Sprint",
      shapeStyle = WidgetShapeStyle.CIRCLE.name
    )

    val squircleBmp = DayCountWidgetProvider.renderWidgetBitmap(context, squircleTimer)
    assertEquals(540, squircleBmp.width)
    assertEquals(340, squircleBmp.height)

    val circleBmp = DayCountWidgetProvider.renderWidgetBitmap(context, circleTimer)
    assertEquals(460, circleBmp.width)
    assertEquals(460, circleBmp.height)
  }

  @Test
  fun `home screen widget resolves Google Pixel device dynamic MUI tonal color scheme`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val accent1_100 = ContextCompat.getColor(context, android.R.color.system_accent1_100)
    val accent1_800 = ContextCompat.getColor(context, android.R.color.system_accent1_800)
    val accent3_100 = ContextCompat.getColor(context, android.R.color.system_accent3_100)
    val accent3_800 = ContextCompat.getColor(context, android.R.color.system_accent3_800)

    val resolvedLight = DayCountWidgetProvider.resolveCanvasWidgetColors(
      context = context,
      themeColor = WidgetThemeColor.DYNAMIC,
      overrideIsDark = false
    )
    assertEquals(accent1_800, resolvedLight.primary)
    assertEquals(accent1_100, resolvedLight.surface)
    assertEquals(accent1_100, resolvedLight.container)
    assertEquals(accent1_800, resolvedLight.onSurface)
    assertEquals(accent3_800, resolvedLight.titleColor)
    assertEquals(accent1_800, resolvedLight.footerColor)

    val resolvedDark = DayCountWidgetProvider.resolveCanvasWidgetColors(
      context = context,
      themeColor = WidgetThemeColor.DYNAMIC,
      overrideIsDark = true
    )
    assertEquals(accent1_100, resolvedDark.primary)
    assertEquals(accent1_800, resolvedDark.surface)
    assertEquals(accent1_800, resolvedDark.container)
    assertEquals(accent1_100, resolvedDark.onSurface)
    assertEquals(accent3_100, resolvedDark.titleColor)
    assertEquals(accent1_100, resolvedDark.footerColor)
  }

  @Test
  fun `google sans flex applies roundness 100 and adapts optical size when widget is resized`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val smallOpsz = computeWidgetOpticalSize(baseSp = 14f, widgetMinWidthDp = 90, widgetMinHeightDp = 90)
    val normalOpsz = computeWidgetOpticalSize(baseSp = 14f, widgetMinWidthDp = 140, widgetMinHeightDp = 140)
    val largeOpsz = computeWidgetOpticalSize(baseSp = 96f, widgetMinWidthDp = 280, widgetMinHeightDp = 280)

    assertTrue("Expected smaller opsz when widget is shrunk", smallOpsz < normalOpsz)
    assertEquals(14f, normalOpsz, 0.01f)
    assertEquals(144f, largeOpsz, 0.01f)

    val variationStr = buildGoogleSansFlexVariationSettings(opticalSize = 96f, weight = 500)
    assertEquals("'ROND' 100.0, 'opsz' 96.0, 'wght' 500", variationStr)

    val paint = TextPaint()
    DayCountWidgetProvider.applyGoogleSansFlex(paint, context, weight = 500, opticalSize = 96f)
    assertNotNull(paint.typeface)
  }

  @Test
  fun `countdown timer calculates remaining days correctly for day count challenge`() {
    val now = 1_000_000_000_000L
    val start = CountdownDateUtils.addLocalCalendarDays(now, -20)

    val timer = CountdownTimer(
      title = "100-Day Challenge",
      mode = CountdownMode.DAY_COUNT.name,
      targetDaysTotal = 100,
      startDateEpochMs = start
    )

    // 100 days total, started 20 days ago -> 80 days left
    assertEquals(80L, timer.daysRemaining(now))
    assertEquals(20L, timer.daysPassed(now))
    assertFalse(timer.isCompleted(now))
  }

  @Test
  fun `date display modes format ending date and fraction of days completed for both countdown types`() {
    val now = 1_000_000_000_000L
    val start = CountdownDateUtils.addLocalCalendarDays(now, -10)
    val target = CountdownDateUtils.addLocalCalendarDays(now, 20)

    // 1. Target Date with DAYS_FRACTION
    val targetDateFractionTimer = CountdownTimer(
      title = "Conference",
      mode = CountdownMode.TARGET_DATE.name,
      targetDateEpochMs = target,
      startDateEpochMs = start,
      showTargetDate = true,
      dateDisplayMode = DateDisplayMode.DAYS_FRACTION.name
    )
    assertEquals("10 / 30 days completed", targetDateFractionTimer.formattedCountdownFooter(now, compact = false))
    assertEquals("10 / 30 days", targetDateFractionTimer.formattedCountdownFooter(now, compact = true))

    // 2. Specified Days with ENDING_DATE
    val dayCountEndingDateTimer = CountdownTimer(
      title = "30-Day Streak",
      mode = CountdownMode.DAY_COUNT.name,
      targetDaysTotal = 30,
      startDateEpochMs = start,
      showTargetDate = true,
      dateDisplayMode = DateDisplayMode.ENDING_DATE.name
    )
    assertEquals(dayCountEndingDateTimer.formattedTargetDate(), dayCountEndingDateTimer.formattedCountdownFooter(now, compact = false))

    // 3. Disabled showTargetDate returns empty string regardless of radio option
    val hiddenFooterTimer = targetDateFractionTimer.copy(showTargetDate = false)
    assertEquals("", hiddenFooterTimer.formattedCountdownFooter(now, compact = false))
  }

  @Test
  fun `selecting Nov 8th in UTC DatePicker preserves Nov 8th in US Pacific and Eastern timezones`() {
    // Material 3 DatePicker emits 00:00:00 UTC for Nov 8, 2026
    val utcNov8Cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      clear()
      set(2026, Calendar.NOVEMBER, 8, 0, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val utcNov8Millis = utcNov8Cal.timeInMillis

    val timezonesToVerify = listOf(
      TimeZone.getTimeZone("America/Los_Angeles"),
      TimeZone.getTimeZone("America/New_York"),
      TimeZone.getTimeZone("UTC"),
      TimeZone.getTimeZone("Asia/Tokyo")
    )

    for (tz in timezonesToVerify) {
      val localMillis = CountdownDateUtils.utcPickerMillisToLocalMillis(utcNov8Millis, tz)
      val formatter = SimpleDateFormat("MMM d, yyyy", Locale.US).apply {
        timeZone = tz
      }
      assertEquals(
        "Failed in timezone ${tz.id}",
        "Nov 8, 2026",
        formatter.format(Date(localMillis))
      )

      // Round-trip back to UTC DatePicker millis must still be Nov 8, 2026 00:00:00 UTC
      val roundTripUtc = CountdownDateUtils.localMillisToUtcPickerMillis(localMillis, tz)
      assertEquals(utcNov8Millis, roundTripUtc)
    }
  }

  @Test
  fun `wallpaper theme returns light gradient in light mode and dark gradient in dark mode`() {
    for (wp in WallpaperTheme.entries) {
      val lightColors = wp.gradientForTheme(isLightTheme = true)
      val darkColors = wp.gradientForTheme(isLightTheme = false)
      assertTrue(
        "Expected light wallpaper gradient for ${wp.name}",
        lightColors.first().luminance() > 0.7f
      )
      assertTrue(
        "Expected dark wallpaper gradient for ${wp.name}",
        darkColors.first().luminance() < 0.2f
      )
    }
  }

  @Test
  fun `crossing midnight decreases days remaining by 1 and updates progress bar immediately after midnight`() {
    val tz = TimeZone.getDefault()
    val day1Noon = Calendar.getInstance(tz).apply {
      clear()
      set(2026, Calendar.NOVEMBER, 8, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val day1JustBeforeMidnight = Calendar.getInstance(tz).apply {
      clear()
      set(2026, Calendar.NOVEMBER, 8, 23, 59, 59)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val day2RightAfterMidnight = CountdownDateUtils.nextMidnightEpochMs(day1JustBeforeMidnight, tz)

    // Verify nextMidnightEpochMs lands at 00:00:01 on Nov 9, 2026
    val nextMidnightCal = Calendar.getInstance(tz).apply {
      timeInMillis = day2RightAfterMidnight
    }
    assertEquals(2026, nextMidnightCal.get(Calendar.YEAR))
    assertEquals(Calendar.NOVEMBER, nextMidnightCal.get(Calendar.MONTH))
    assertEquals(9, nextMidnightCal.get(Calendar.DAY_OF_MONTH))
    assertEquals(0, nextMidnightCal.get(Calendar.HOUR_OF_DAY))
    assertEquals(0, nextMidnightCal.get(Calendar.MINUTE))
    assertEquals(1, nextMidnightCal.get(Calendar.SECOND))

    // 1. Specified Days timer (10 days starting on Nov 8)
    val specifiedDaysTimer = CountdownTimer(
      title = "10-Day Sprint",
      mode = CountdownMode.DAY_COUNT.name,
      targetDaysTotal = 10,
      startDateEpochMs = day1Noon,
      showTargetDate = true,
      dateDisplayMode = DateDisplayMode.DAYS_FRACTION.name
    )
    assertEquals(10L, specifiedDaysTimer.daysRemaining(day1JustBeforeMidnight))
    assertEquals(0.0f, specifiedDaysTimer.progressFraction(day1JustBeforeMidnight), 0.001f)
    assertEquals("0 / 10 days completed", specifiedDaysTimer.formattedCountdownFooter(day1JustBeforeMidnight))

    // Immediately after midnight (00:00:01 on Nov 9): days left decreases by 1, progress advances to 10%
    assertEquals(9L, specifiedDaysTimer.daysRemaining(day2RightAfterMidnight))
    assertEquals(0.10f, specifiedDaysTimer.progressFraction(day2RightAfterMidnight), 0.001f)
    assertEquals("1 / 10 days completed", specifiedDaysTimer.formattedCountdownFooter(day2RightAfterMidnight))

    // 2. Until Specific Date timer (Nov 8 to Nov 18 = 10 days)
    val targetDateTimer = CountdownTimer(
      title = "Trip",
      mode = CountdownMode.TARGET_DATE.name,
      startDateEpochMs = day1Noon,
      targetDateEpochMs = CountdownDateUtils.addLocalCalendarDays(day1Noon, 10, tz),
      showTargetDate = true,
      dateDisplayMode = DateDisplayMode.DAYS_FRACTION.name
    )
    assertEquals(10L, targetDateTimer.daysRemaining(day1JustBeforeMidnight))
    assertEquals(0.0f, targetDateTimer.progressFraction(day1JustBeforeMidnight), 0.001f)

    assertEquals(9L, targetDateTimer.daysRemaining(day2RightAfterMidnight))
    assertEquals(0.10f, targetDateTimer.progressFraction(day2RightAfterMidnight), 0.001f)
    assertEquals("1 / 10 days completed", targetDateTimer.formattedCountdownFooter(day2RightAfterMidnight))
  }

  @Test
  fun `widget provider schedules 24-hour midnight wakeup alarm and reschedules on midnight broadcast`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val shadowAlarmManager = shadowOf(alarmManager)

    val now = 1_762_600_000_000L
    val expectedTriggerMs = CountdownDateUtils.nextMidnightEpochMs(now)
    val scheduledMs = DayCountWidgetProvider.scheduleNextMidnightUpdate(context, now)

    assertEquals(expectedTriggerMs, scheduledMs)
    val nextAlarm = shadowAlarmManager.nextScheduledAlarm
    assertNotNull("Expected midnight widget alarm to be scheduled", nextAlarm)
    assertEquals(expectedTriggerMs, nextAlarm!!.triggerAtTime)

    // Receiving ACTION_MIDNIGHT_WIDGET_UPDATE reschedules the next midnight alarm
    val provider = DayCountWidgetProvider()
    provider.onReceive(
      context,
      Intent(context, DayCountWidgetProvider::class.java).apply {
        action = DayCountWidgetProvider.ACTION_MIDNIGHT_WIDGET_UPDATE
      }
    )
    assertNotNull(shadowAlarmManager.nextScheduledAlarm)
  }
}
